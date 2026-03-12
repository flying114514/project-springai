package com.ai.infrastructure.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件内容类型检测服务
 * 使用 Apache Tika 进行精确的 MIME 类型检测
 */
@Slf4j
@Service
public class ContentTypeDetectionService {
    private final Tika tika;

    public ContentTypeDetectionService(Tika tika) {
        this.tika = tika;
    }

    /**
     * 检测文件的 MIME 类型
     * 使用 Tika 进行基于内容的检测，比 HTTP 头部更准确
     *
     * @param file MultipartFile 文件
     * @return MIME 类型字符串
     */
    public String detectContentType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        } catch (IOException e) {
            log.warn("无法检测文件类型，使用 Content-Type 头部: {}", e.getMessage());
            return file.getContentType();
        }
    }
}
