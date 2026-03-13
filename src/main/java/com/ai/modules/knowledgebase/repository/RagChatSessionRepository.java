package com.ai.modules.knowledgebase.repository;

import com.ai.modules.knowledgebase.model.RagChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RagChatSessionRepository extends JpaRepository<RagChatSessionEntity, Long> {

    /**
     * 根据知识库ID查找相关会话
     */
    @Query("SELECT DISTINCT s FROM RagChatSessionEntity s JOIN s.knowledgeBases kb WHERE kb.id IN :kbIds ORDER BY s.updatedAt DESC")
    List<RagChatSessionEntity> findByKnowledgeBaseIds(@Param("kbIds") List<Long> knowledgeBaseIds);
}
