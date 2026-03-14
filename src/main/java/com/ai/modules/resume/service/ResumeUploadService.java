package com.ai.modules.resume.service;

import com.ai.common.config.AppConfigProperties;
import com.ai.common.exception.BusinessException;
import com.ai.common.exception.ErrorCode;
import com.ai.common.model.AsyncTaskStatus;
import com.ai.infrastructure.file.FileStorageService;
import com.ai.infrastructure.file.FileValidationService;
import com.ai.modules.interview.model.ResumeAnalysisResponse;
import com.ai.modules.resume.listener.AnalyzeStreamProducer;
import com.ai.modules.resume.model.ResumeEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

/**
 * 上传并分析简历
 * AI分析的过程使用redisStream处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeUploadService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private final FileStorageService storageService;
    private final FileValidationService fileValidationService;
    private final ResumeParseService parseService;
    private final AppConfigProperties appConfig;
    private final ResumePersistenceService persistenceService;
    private final AnalyzeStreamProducer analyzeStreamProducer;

    /**
     * 上传并分析简历（异步）
     *
     * @param file 简历文件
     * @return 上传结果（分析将异步进行）
     */
    public Map<String, Object> uploadAndAnalyze(MultipartFile file) {
        // 1.验证文件,简单验证文件大小以及是否为空
        fileValidationService.validateFile(file, MAX_FILE_SIZE, "简历");

        String fileName = file.getOriginalFilename();
        log.info("收到简历上传请求: {}, 大小: {} bytes", fileName, file.getSize());

        // 2.验证文件类型
        String contentType = parseService.detectContentType(file);
        validateContentType(contentType);

        // 3. 检查简历是否已存在（去重）
        Optional<ResumeEntity> existingResume = persistenceService.findExistingResume(file);
        // existingResume.isPresent()表示简历已存在
        if (existingResume.isPresent()) {
            // 返回历史分析结果,不用再分析一次
            return handleDuplicateResume(existingResume.get());
        }

        // 4. 解析简历文本
        String resumeText = parseService.parseResume(file);
        if (resumeText == null || resumeText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED, "无法从文件中提取文本内容，请确保文件不是扫描版PDF");
        }

        // 5. 保存简历到RustFS
        // 这个方法即把文件上传到了RustFS,也返回一个文件键
        String fileKey = storageService.uploadResume(file);

        // 获取url是为了之后存入数据库中,方便定位
        String fileUrl = storageService.getFileUrl(fileKey);
        log.info("简历已存储到RustFS: {}", fileKey);

        // 6. 保存简历到数据库（状态为 PENDING）
        ResumeEntity savedResume = persistenceService.saveResume(file, resumeText, fileKey, fileUrl);

        // 7. 发送分析任务到 Redis Stream（异步处理）
        /**
         * 这是一个无返回值的方法,如果没能将任务添加成功会根据子类自己的逻辑处理失败
         * 我们这里是在数据库中更新了这条简历数据的状态为PENDING
         */
        analyzeStreamProducer.sendAnalyzeTask(savedResume.getId(), resumeText);

        log.info("简历上传完成，分析任务已入队: {}, resumeId={}", fileName, savedResume.getId());

        // 8. 返回结果（状态为 PENDING，前端可轮询获取最新状态）
        return Map.of(
                "resume", Map.of(
                        "id", savedResume.getId(),
                        "filename", savedResume.getOriginalFilename(),
                        "analyzeStatus", AsyncTaskStatus.PENDING.name()
                ),
                "storage", Map.of(
                        "fileKey", fileKey,
                        "fileUrl", fileUrl,
                        "resumeId", savedResume.getId()
                ),
                "duplicate", false
        );
    }

    private Map<String, Object> handleDuplicateResume(ResumeEntity resume) {
        log.info("检测到重复简历，返回历史分析结果: resumeId={}", resume.getId());

        // 获取历史分析结果
        Optional<ResumeAnalysisResponse> analysisOpt = persistenceService.getLatestAnalysisAsDTO(resume.getId());

        // 已有分析结果，直接返回
        // 没有分析结果（可能之前分析失败），返回当前状态
        return analysisOpt.map(resumeAnalysisResponse -> Map.of(
                "analysis", resumeAnalysisResponse,
                "storage", Map.of(
                        "fileKey", resume.getStorageKey() != null ? resume.getStorageKey() : "",
                        "fileUrl", resume.getStorageUrl() != null ? resume.getStorageUrl() : "",
                        "resumeId", resume.getId()
                ),
                "duplicate", true//上面是重复简历执行操作
        )).orElseGet(() -> Map.of(
                "resume", Map.of(
                        "id", resume.getId(),
                        "filename", resume.getOriginalFilename(),
                        "analyzeStatus", resume.getAnalyzeStatus() != null ? resume.getAnalyzeStatus().name() : AsyncTaskStatus.PENDING.name()
                ),
                "storage", Map.of(
                        "fileKey", resume.getStorageKey() != null ? resume.getStorageKey() : "",
                        "fileUrl", resume.getStorageUrl() != null ? resume.getStorageUrl() : "",
                        "resumeId", resume.getId()
                ),
                "duplicate", true
        ));
    }


    private void validateContentType(String contentType) {
        fileValidationService.validateContentTypeByList(
                contentType,
                // 将我们项目所支持的文件类型存在配置类中方便获取
                appConfig.getAllowedTypes(),
                "不支持的文件类型: " + contentType
        );

    }
}
