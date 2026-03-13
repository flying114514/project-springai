package com.ai.modules.knowledgebase.service;

import com.ai.modules.knowledgebase.repository.VectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识库向量存储服务
 * 负责文档分块、向量化和检索
 */
@Slf4j
@Service
public class KnowledgeBaseVectorService {
    private final VectorRepository vectorRepository;

    public KnowledgeBaseVectorService(VectorRepository vectorRepository) {
        this.vectorRepository = vectorRepository;
    }

    /**
     * 删除指定知识库的所有向量数据
     * 委托给 VectorRepository 处理
     *
     * @param knowledgeBaseId 知识库ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        try {
            vectorRepository.deleteByKnowledgeBaseId(knowledgeBaseId);
        } catch (Exception e) {
            log.error("删除向量数据失败: kbId={}, error={}", knowledgeBaseId, e.getMessage(), e);
            // 不抛出异常，允许继续执行其他删除操作
            // 如果确实需要严格保证，可以取消下面的注释
            // throw new RuntimeException("删除向量数据失败: " + e.getMessage(), e);
        }
    }
}
