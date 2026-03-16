package com.ai.modules.knowledgebase.service;

import com.ai.common.exception.BusinessException;
import com.ai.common.exception.ErrorCode;
import com.ai.infrastructure.file.FileStorageService;
import com.ai.modules.knowledgebase.model.KnowledgeBaseEntity;
import com.ai.modules.knowledgebase.model.RagChatSessionEntity;
import com.ai.modules.knowledgebase.repository.KnowledgeBaseRepository;
import com.ai.modules.knowledgebase.repository.RagChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库删除服务
 * 负责知识库的删除操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseDeleteService {
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RagChatSessionRepository sessionRepository;
    private final KnowledgeBaseVectorService vectorService;
    private final FileStorageService storageService;
    /**
     * 删除知识库
     * 包括：RAG会话关联、向量数据、RustFS文件、数据库记录
     */
    @Transactional(rollbackFor = Exception.class) // 当这个方法中出现任意错误都会回滚
    public void deleteKnowledgeBase(Long id) {
        // 1. 获取知识库信息
        KnowledgeBaseEntity kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));

        // 2. 删除所有RAG会话中的知识库关联（必须先删除关联，否则外键约束会阻止删除）
        // 查询所有关联了该知识库的 RAG 会话sessions,RAG会话和知识库的关系是多对多
        List<RagChatSessionEntity> sessions = sessionRepository.findByKnowledgeBaseIds(List.of(id));
        for (RagChatSessionEntity session : sessions) {
            // 条件删除,这里只是删除了关联会话和即将要被删除的知识库的关联,并不是直接把会话给删了
            session.getKnowledgeBases().removeIf(kbEntity -> kbEntity.getId().equals(id));
            sessionRepository.save(session);
            log.debug("已从会话中移除知识库关联: sessionId={}, kbId={}", session.getId(), id);
        }

        // 3. 删除向量数据
        try {
            vectorService.deleteByKnowledgeBaseId(id);
        } catch (Exception e) {
            log.warn("删除向量数据失败，继续删除知识库: kbId={}, error={}", id, e.getMessage());
        }

        // 4. 删除RustFS中的文件（FileStorageService 已内置存在性检查）
        try {
            storageService.deleteKnowledgeBase(kb.getStorageKey());
        } catch (Exception e) {
            log.warn("删除RustFS文件失败，继续删除知识库记录: kbId={}, error={}", id, e.getMessage());
        }

        // 5. 删除知识库记录（在事务中）
        knowledgeBaseRepository.deleteById(id);
        log.info("知识库已删除: id={}", id);
    }
}
