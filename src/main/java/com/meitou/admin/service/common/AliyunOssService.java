package com.meitou.admin.service.common;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.meitou.admin.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 阿里云OSS服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunOssService {

    private final FileStorageConfig fileStorageConfig;
    private final RestTemplate restTemplate;

    /**
     * 上传网络图片/视频到OSS
     *
     * @param url 网络文件URL
     * @param directory 目录前缀 (e.g., "images/", "videos/")
     * @return OSS访问URL
     */
    public String uploadFromUrl(String url, String directory) {
        try {
            // 下载文件
            byte[] fileBytes = restTemplate.getForObject(url, byte[].class);
            if (fileBytes == null || fileBytes.length == 0) {
                throw new RuntimeException("下载文件失败: " + url);
            }

            // 获取文件扩展名
            String extension = getExtensionFromUrl(url);
            if (extension == null || extension.isEmpty()) {
                extension = "png"; // 默认扩展名
            }

            // 生成文件名
            String fileName = directory + generateFileName(extension);

            // 上传到OSS
            return uploadBytes(fileBytes, fileName);

        } catch (Exception e) {
            log.error("上传文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 上传字节数组到OSS
     */
    public String uploadBytes(byte[] bytes, String fileName) {
        OSS ossClient = null;
        try {
            FileStorageConfig.AliyunConfig config = fileStorageConfig.getAliyun();
            
            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(
                    config.getEndpoint(),
                    config.getAccessKeyId(),
                    config.getAccessKeySecret());

            // 创建上传请求
            InputStream inputStream = new ByteArrayInputStream(bytes);
            PutObjectRequest putObjectRequest = new PutObjectRequest(config.getBucketName(), fileName, inputStream);
            
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            putObjectRequest.setMetadata(metadata);

            // 上传
            ossClient.putObject(putObjectRequest);

            // 构建返回URL
            String domain = config.getDomain();
            if (domain == null || domain.isEmpty()) {
                domain = "https://" + config.getBucketName() + "." + config.getEndpoint();
            }
            
            // 确保域名以http/https开头
            if (!domain.startsWith("http")) {
                domain = "https://" + domain;
            }
            
            // 确保域名末尾有斜杠
            if (!domain.endsWith("/")) {
                domain = domain + "/";
            }

            return domain + fileName;

        } catch (Exception e) {
            log.error("OSS上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("OSS上传失败: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 生成文件名: yyyyMMdd/uuid.ext
     */
    private String generateFileName(String extension) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String datePath = sdf.format(new Date());
        return datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }

    /**
     * 从URL获取扩展名
     */
    private String getExtensionFromUrl(String url) {
        try {
            // 移除查询参数
            if (url.contains("?")) {
                url = url.substring(0, url.indexOf("?"));
            }
            // 获取最后一个点之后的内容
            int lastDotIndex = url.lastIndexOf(".");
            if (lastDotIndex > 0 && lastDotIndex < url.length() - 1) {
                return url.substring(lastDotIndex + 1);
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }
}
