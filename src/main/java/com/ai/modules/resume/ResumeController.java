package com.ai.modules.resume;


import com.ai.annotation.RateLimit;
import com.ai.common.result.Result;
import com.ai.modules.resume.service.ResumeUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 简历控制器
 * Resume Controller for upload and analysis
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeUploadService uploadService;
    private final ResumeDeleteService deleteService;
    private final ResumeHistoryService historyService;

    /**
     * 上传简历并获取分析结果
     *
     * @param file 简历文件（支持PDF、DOCX、DOC、TXT、MD等）
     * @return 简历分析结果，包含评分和建议
     */
    @PostMapping(value = "/api/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // 表示同时进行全局限流（系统总共 5 次/秒）和IP级限流（每个IP 5 次/秒）
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Result<Map<String, Object>> uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = uploadService.uploadAndAnalyze(file);
        // result是分析结果,duplicate字段表示是否是重复简历
        boolean isDuplicate = (Boolean) result.get("duplicate");
        if (isDuplicate) {
            return Result.success("检测到相同简历，已返回历史分析结果", result);
        }
        return Result.success(result);
    }
}
