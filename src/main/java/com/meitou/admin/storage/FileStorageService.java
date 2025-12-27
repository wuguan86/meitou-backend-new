package com.meitou.admin.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 * 定义通用的文件上传方法，支持多种云存储实现（腾讯云COS、阿里云OSS等）
 */
public interface FileStorageService {
    
    /**
     * 上传文件到云存储
     * 
     * @param file 要上传的文件
     * @param folder 存储文件夹路径（可选，如 "images/", "videos/" 等）
     * @return 文件的访问URL
     * @throws Exception 上传失败时抛出异常
     */
    String upload(MultipartFile file, String folder) throws Exception;
    
    /**
     * 上传文件到云存储（默认文件夹）
     * 
     * @param file 要上传的文件
     * @return 文件的访问URL
     * @throws Exception 上传失败时抛出异常
     */
    default String upload(MultipartFile file) throws Exception {
        return upload(file, "");
    }
}

