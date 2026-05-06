package com.ruici.ai.common.config;

import lombok.Data;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PGVector 运行时配置。
 *
 * <p>第二阶段开始，知识库检索与向量化会基于运行时 embedding 快照动态创建 PgVectorStore，
 * 因此需要显式读取与自动配置一致的向量表参数，避免业务代码散落 `@Value`。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.vectorstore.pgvector")
public class PgVectorStoreRuntimeProperties {

    private int dimensions = PgVectorStore.INVALID_EMBEDDING_DIMENSION;
    private PgVectorStore.PgIndexType indexType = PgVectorStore.PgIndexType.HNSW;
    private PgVectorStore.PgDistanceType distanceType = PgVectorStore.PgDistanceType.COSINE_DISTANCE;
    private boolean removeExistingVectorStoreTable = false;
    private boolean initializeSchema = true;
    private boolean schemaValidation = false;
    private String schemaName = PgVectorStore.DEFAULT_SCHEMA_NAME;
    private String tableName = PgVectorStore.DEFAULT_TABLE_NAME;
    private int maxDocumentBatchSize = PgVectorStore.MAX_DOCUMENT_BATCH_SIZE;
}
