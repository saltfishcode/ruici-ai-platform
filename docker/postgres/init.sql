-- PostgreSQL initialization script for Ruici AI Platform
--
-- Current responsibilities:
-- 1. Ensure pgvector extension exists
-- 2. Initialize AI runtime control-plane tables for first-time container startup
--
-- Notes:
-- - PostgreSQL docker entrypoint only runs this script on first initialization of an empty data directory.
-- - For existing databases, use the synchronized manual compensation script:

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_runtime_config (
  id BIGSERIAL PRIMARY KEY,
  config_key VARCHAR(64) NOT NULL,
  domain VARCHAR(32) NOT NULL,
  scene VARCHAR(32) NOT NULL,
  provider_id VARCHAR(64),
  model_name VARCHAR(128),
  fallback_model_name VARCHAR(128),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  priority INTEGER NOT NULL DEFAULT 100,
  config_version BIGINT NOT NULL DEFAULT 1,
  remark VARCHAR(500),
  updated_by VARCHAR(64),
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_runtime_config IS
'AI 运行时模型配置表。只存储 provider/model/作用域等非敏感控制信息，不存储 API Key。';

COMMENT ON COLUMN ai_runtime_config.config_key IS
'逻辑配置键。用于映射历史环境变量或逻辑入口，例如 THIRD_PARTY_MODEL、AI_EMBEDDING_MODEL、AI_ASR_MODEL。';

COMMENT ON COLUMN ai_runtime_config.domain IS
'能力域：chat、embedding、asr、tts。';

COMMENT ON COLUMN ai_runtime_config.scene IS
'业务场景：global、simulation、knowledgebase、voice、document。';

COMMENT ON COLUMN ai_runtime_config.provider_id IS
'Provider 标识。通常在职责域内保持稳定。';

COMMENT ON COLUMN ai_runtime_config.model_name IS
'主模型名。由解析层应用到聊天、Embedding、ASR、TTS 链路。';

COMMENT ON COLUMN ai_runtime_config.fallback_model_name IS
'回退模型名。仅对支持 fallback 的聊天链路生效。';

COMMENT ON COLUMN ai_runtime_config.enabled IS
'是否启用当前配置。';

COMMENT ON COLUMN ai_runtime_config.priority IS
'优先级，值越小优先级越高。';

COMMENT ON COLUMN ai_runtime_config.config_version IS
'配置版本号。用于缓存失效与快照比较。';

COMMENT ON COLUMN ai_runtime_config.remark IS
'业务说明，不允许填写密钥、账号、私有地址等敏感信息。';

COMMENT ON COLUMN ai_runtime_config.updated_by IS
'最后修改人。';

COMMENT ON COLUMN ai_runtime_config.updated_at IS
'最后修改时间。';

COMMENT ON COLUMN ai_runtime_config.created_at IS
'创建时间。';

CREATE TABLE IF NOT EXISTS ai_runtime_config_audit (
  id BIGSERIAL PRIMARY KEY,
  config_id BIGINT,
  action_type VARCHAR(32) NOT NULL,
  config_key VARCHAR(64),
  domain VARCHAR(32),
  scene VARCHAR(32),
  before_summary TEXT,
  after_summary TEXT,
  operator VARCHAR(64),
  operated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remark VARCHAR(500)
);

COMMENT ON TABLE ai_runtime_config_audit IS
'AI 运行时配置审计表。用于记录控制面变更，不记录敏感凭据。';

COMMENT ON COLUMN ai_runtime_config_audit.action_type IS
'操作类型，例如 CREATE、UPDATE、ENABLE、DISABLE、REFRESH。';

COMMENT ON COLUMN ai_runtime_config_audit.before_summary IS
'变更前摘要，不记录敏感信息。';

COMMENT ON COLUMN ai_runtime_config_audit.after_summary IS
'变更后摘要，不记录敏感信息。';

CREATE INDEX IF NOT EXISTS idx_ai_runtime_config_domain_scene_enabled
  ON ai_runtime_config (domain, scene, enabled);

CREATE INDEX IF NOT EXISTS idx_ai_runtime_config_key_enabled_priority
  ON ai_runtime_config (config_key, enabled, priority);

CREATE INDEX IF NOT EXISTS idx_ai_runtime_config_provider_domain_scene
  ON ai_runtime_config (provider_id, domain, scene);

CREATE INDEX IF NOT EXISTS idx_ai_runtime_config_audit_config_id
  ON ai_runtime_config_audit (config_id);

CREATE INDEX IF NOT EXISTS idx_ai_runtime_config_audit_operated_at
  ON ai_runtime_config_audit (operated_at);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'uk_ai_runtime_config_scope_priority'
      AND conrelid = 'public.ai_runtime_config'::regclass
  ) THEN
    ALTER TABLE ai_runtime_config
      ADD CONSTRAINT uk_ai_runtime_config_scope_priority
      UNIQUE (config_key, domain, scene, priority);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_ai_runtime_config_domain'
  ) THEN
    ALTER TABLE ai_runtime_config
      ADD CONSTRAINT chk_ai_runtime_config_domain
      CHECK (domain IN ('chat', 'embedding', 'asr', 'tts'));
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_ai_runtime_config_scene'
  ) THEN
    ALTER TABLE ai_runtime_config
      ADD CONSTRAINT chk_ai_runtime_config_scene
      CHECK (scene IN ('global', 'simulation', 'knowledgebase', 'voice', 'document'));
  END IF;
END $$;

INSERT INTO ai_runtime_config (
  config_key,
  domain,
  scene,
  provider_id,
  model_name,
  fallback_model_name,
  enabled,
  priority,
  config_version,
  remark,
  updated_by
) VALUES
  (
    'THIRD_PARTY_MODEL',
    'chat',
    'global',
    'third-party',
    'gpt-5.2',
    'qwen-plus',
    TRUE,
    10,
    1,
    '全局聊天默认主模型；作用于普通聊天、文档分析文本推理、情景模拟文本生成与知识库问答。',
    'system-seed'
  ),
  (
    'AI_EMBEDDING_MODEL',
    'embedding',
    'knowledgebase',
    'dashscope',
    'text-embedding-v3',
    NULL,
    TRUE,
    10,
    1,
    '知识库向量化默认 Embedding 模型；仅作用于向量生成与检索相关链路。',
    'system-seed'
  ),
  (
    'AI_ASR_MODEL',
    'asr',
    'voice',
    'dashscope',
    'qwen3-asr-flash-realtime',
    NULL,
    TRUE,
    10,
    1,
    '语音模块实时识别模型；仅作用于新建语音会话的 ASR 初始化。',
    'system-seed'
  ),
  (
    'AI_TTS_MODEL',
    'tts',
    'voice',
    'dashscope',
    'qwen-tts-realtime',
    NULL,
    TRUE,
    10,
    1,
    '语音模块默认 TTS 模型；仅作用于新建语音会话的语音合成初始化。',
    'system-seed'
  )
ON CONFLICT (config_key, domain, scene, priority) DO NOTHING;
