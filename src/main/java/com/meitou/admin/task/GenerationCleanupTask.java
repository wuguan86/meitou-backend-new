package com.meitou.admin.task;

import com.meitou.admin.common.SiteContext;
import com.meitou.admin.entity.GenerationRecord;
import com.meitou.admin.entity.User;
import com.meitou.admin.entity.UserTransaction;
import com.meitou.admin.mapper.GenerationRecordMapper;
import com.meitou.admin.mapper.UserMapper;
import com.meitou.admin.mapper.UserTransactionMapper;
import com.meitou.admin.service.app.GenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生成任务清理定时任务
 * 处理长时间卡在 processing 状态的任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationCleanupTask {

    private final GenerationRecordMapper generationRecordMapper;
    private final UserMapper userMapper;
    private final UserTransactionMapper userTransactionMapper;
    private final TransactionTemplate transactionTemplate;
    private final GenerationService generationService;

    @Value("${generation.task.sync.batchSize:50}")
    private int syncBatchSize;

    @Value("${generation.task.timeout.minutes:60}")
    private int timeoutMinutes;

    @Value("${generation.task.timeout.batchSize:50}")
    private int timeoutBatchSize;

    @Scheduled(fixedRateString = "${generation.task.sync.fixedRateMs:60000}")
    public void syncProcessingTasks() {
        List<GenerationRecord> processingRecords = generationRecordMapper.selectProcessingIgnoreTenant(syncBatchSize);
        if (processingRecords.isEmpty()) {
            return;
        }

        for (GenerationRecord record : processingRecords) {
            if (record.getSiteId() == null) {
                continue;
            }
            runWithSiteContext(record.getSiteId(), () -> {
                try {
                    generationService.getTaskStatus(record.getId());
                } catch (Exception e) {
                    log.warn("同步任务状态失败 ID={}: {}", record.getId(), e.getMessage());
                }
            });
        }
    }

    @Scheduled(fixedRateString = "${generation.task.timeout.fixedRateMs:300000}")
    public void cleanupStuckTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        
        List<GenerationRecord> stuckRecords = generationRecordMapper.selectProcessingBeforeIgnoreTenant(threshold, timeoutBatchSize);
        
        if (stuckRecords.isEmpty()) {
            return;
        }
        
        log.info("发现 {} 个卡死的生成任务，开始处理...", stuckRecords.size());
        
        for (GenerationRecord record : stuckRecords) {
            try {
                if (record.getSiteId() == null) {
                    continue;
                }
                runWithSiteContext(record.getSiteId(), () -> processStuckRecord(record));
            } catch (Exception e) {
                log.error("处理卡死任务失败 ID={}: {}", record.getId(), e.getMessage());
            }
        }
    }

    private void processStuckRecord(GenerationRecord record) {
        try {
            generationService.getTaskStatus(record.getId());
        } catch (Exception e) {
            log.warn("处理超时任务前同步状态失败 ID={}: {}", record.getId(), e.getMessage());
        }

        transactionTemplate.execute(status -> {
            // 再次检查状态（防止并发处理）
            GenerationRecord r = generationRecordMapper.selectById(record.getId());
            if (r != null && "processing".equals(r.getStatus())) {
                log.info("任务超时自动失败退款 ID={}, Cost={}", r.getId(), r.getCost());
                
                // 标记失败
                r.setStatus("failed");
                r.setFailureReason("任务执行超时，系统自动退款");
                generationRecordMapper.updateById(r);
                
                // 退款
                if (r.getCost() != null && r.getCost() > 0) {
                    userMapper.deductBalance(r.getUserId(), -r.getCost());
                    
                    // 记录流水
                    User user = userMapper.selectById(r.getUserId());
                    UserTransaction transaction = new UserTransaction();
                    transaction.setUserId(r.getUserId());
                    transaction.setType("REFUND");
                    transaction.setAmount(r.getCost());
                    transaction.setBalanceAfter(user.getBalance());
                    transaction.setReferenceId(r.getId());
                    transaction.setDescription("任务超时自动退款");
                    transaction.setSiteId(r.getSiteId());
                    userTransactionMapper.insert(transaction);
                }
            }
            return null;
        });
    }

    private void runWithSiteContext(Long siteId, Runnable runnable) {
        Long originalSiteId = SiteContext.getSiteId();
        try {
            SiteContext.setSiteId(siteId);
            runnable.run();
        } finally {
            if (originalSiteId == null) {
                SiteContext.clear();
            } else {
                SiteContext.setSiteId(originalSiteId);
            }
        }
    }
}
