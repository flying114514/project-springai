package com.ai.modules.knowledgebase.repository;

import com.ai.modules.knowledgebase.model.KnowledgeBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用于操作数据库中的 KnowledgeBaseEntity 表。
 * 继承 Spring Data JPA 提供的通用 Repository
 * 泛型参数：实体类型 + 主键类型
 * <KnowledgeBaseEntity, Long>,KnowledgeBaseEntity是要操作表名,Long是主键类型
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntity, Long> {
}
