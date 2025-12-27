package com.meitou.admin.storage.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.meitou.admin.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云OSS文件存储服务实现类
 * 使用 @ConditionalOnProperty 注解，只有当配置文件中 file.storage.type=aliyun 时才会启用
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "aliyun", matchIfMissing = false)
public class AliOssServiceImpl implements FileStorageService {
    
    /**
     * 阿里云OSS AccessKeyId
     */
    @Value("${file.storage.aliyun.access-key-id:}")
    private String accessKeyId;
    
    /**
     * 阿里云OSS AccessKeySecret
     */
    @Value("${file.storage.aliyun.access-key-secret:}")
    private String accessKeySecret;
    
    /**
     * 阿里云OSS Endpoint（如：oss-cn-hangzhou.aliyuncs.com）
     */
    @Value("${file.storage.aliyun.endpoint:}")
    private String endpoint;
    
    /**
     * 阿里云OSS Bucket名称
     */
    @Value("${file.storage.aliyun.bucket-name:}")
    private String bucketName;
    
    /**
     * 阿里云OSS访问域名（可选）
     */
    @Value("${file.storage.aliyun.domain:}")
    private String domain;
    
    /**
     * OSS客户端
     */
    private OSS ossClient;
    
    /**
     * 初始化OSS客户端
     */
    @PostConstruct
    public void init() {
        try {
            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            
            log.info("阿里云OSS客户端初始化成功，Endpoint：{}，Bucket：{}", endpoint, bucketName);
        } catch (Exception e) {
            log.error("阿里云OSS客户端初始化失败", e);
            throw new RuntimeException("阿里云OSS客户端初始化失败", e);
        }
    }
    
    /**
     * 销毁OSS客户端，释放资源
     */
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("阿里云OSS客户端已关闭");
        }
    }
    
    /**
     * 上传文件到阿里云OSS
     * 
     * @param file 要上传的文件
     * @param folder 存储文件夹路径
     * @return 文件的访问URL
     * @throws Exception 上传失败时抛出异常
     */
    @Override
    public String upload(MultipartFile file, String folder) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        
        // 构建对象键（文件路径）
        String objectKey;
        if (folder != null && !folder.isEmpty()) {
            // 确保文件夹路径以 / 结尾
            if (!folder.endsWith("/")) {
                folder = folder + "/";
            }
            objectKey = folder + fileName;
        } else {
            objectKey = fileName;
        }
        
        try {
            // 获取文件输入流
            InputStream inputStream = file.getInputStream();
            
            // 设置对象元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (file.getContentType() != null) {
                metadata.setContentType(file.getContentType());
            }
            
            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    objectKey,
                    inputStream,
                    metadata
            );
            
            // 执行上传
            ossClient.putObject(putObjectRequest);
            
            // 构建文件访问URL
            String fileUrl;
            if (domain != null && !domain.isEmpty()) {
                // 使用自定义域名
                if (domain.endsWith("/")) {
                    fileUrl = domain + objectKey;
                } else {
                    fileUrl = domain + "/" + objectKey;
                }
            } else {
                // 使用默认域名
                // 格式：https://bucket-name.endpoint/object-key
                fileUrl = String.format("https://%s.%s/%s", bucketName, endpoint, objectKey);
            }
            
            log.info("文件上传成功：{} -> {}", originalFilename, fileUrl);
            return fileUrl;
            
        } catch (OSSException e) {
            log.error("阿里云OSS上传失败：{}", e.getMessage(), e);
            throw new Exception("文件上传失败：" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("阿里云OSS上传失败：{}", e.getMessage(), e);
            throw new Exception("文件上传失败：" + e.getMessage(), e);
        }
    }
}

