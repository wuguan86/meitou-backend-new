package com.meitou.admin.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.meitou.admin.entity.GenerationRecord;
import com.meitou.admin.entity.User;
import com.meitou.admin.entity.UserTransaction;
import com.meitou.admin.mapper.GenerationRecordMapper;
import com.meitou.admin.mapper.UserMapper;
import com.meitou.admin.mapper.UserTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 每分钟执行一次，清理超过10分钟仍处于processing状态的任务
     */
    @Scheduled(fixedRate = 120000)
    public void cleanupStuckTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        
        QueryWrapper<GenerationRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "processing");
        queryWrapper.lt("created_at", threshold);
        
        List<GenerationRecord> stuckRecords = generationRecordMapper.selectList(queryWrapper);
        
        if (stuckRecords.isEmpty()) {
            return;
        }
        
        log.info("发现 {} 个卡死的生成任务，开始处理...", stuckRecords.size());
        
        for (GenerationRecord record : stuckRecords) {
            try {
                processStuckRecord(record);
            } catch (Exception e) {
                log.error("处理卡死任务失败 ID={}: {}", record.getId(), e.getMessage());
            }
        }
    }

    private void processStuckRecord(GenerationRecord record) {
        transactionTemplate.execute(status -> {
            // 再次检查状态（防止并发处理）
            GenerationRecord r = generationRecordMapper.selectById(record.getId());
            if (r != null && "processing".equals(r.getStatus())) {
                log.info("任务超时自动失败退款 ID={}, Cost={}", r.getId(), r.getCost());
                
                // 标记失败
                r.setStatus("failed");
                // r.setErrorMessage("任务执行超时，系统自动退款"); // 实体类无此字段，暂不设置
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
}
