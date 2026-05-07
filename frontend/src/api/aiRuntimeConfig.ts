import { request } from './request';

export interface AiRuntimeConfigListItem {
  id: number;
  configKey: string;
  domain: string;
  scene: string;
  providerId: string;
  modelName: string;
  fallbackModelName: string | null;
  enabled: boolean;
  priority: number;
  configVersion: number;
  remark: string | null;
  updatedBy: string;
  updatedAt: string;
}

export interface AiRuntimeConfigDetail {
  id: number;
  configKey: string;
  domain: string;
  scene: string;
  providerId: string;
  modelName: string;
  fallbackModelName: string | null;
  enabled: boolean;
  priority: number;
  configVersion: number;
  remark: string | null;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface SaveAiRuntimeConfigRequest {
  id?: number | null;
  configKey: string;
  domain: string;
  scene: string;
  providerId?: string;
  modelName: string;
  fallbackModelName?: string;
  enabled: boolean;
  priority?: number;
  remark?: string;
}

export interface RefreshAiRuntimeConfigResponse {
  message: string;
  latestConfigVersion: number;
}

/** domain 标签映射 */
export const DOMAIN_LABELS: Record<string, string> = {
  chat: '通用对话',
  embedding: '向量化',
  asr: '语音识别',
  tts: '语音合成',
};

/** scene 标签映射 */
export const SCENE_LABELS: Record<string, string> = {
  global: '全局（默认）',
  simulation: '情景模拟',
  knowledgebase: '知识库',
  voice: '语音',
  document: '文档分析',
};

/** 前端可选项 */
export const DOMAIN_OPTIONS = ['chat', 'embedding', 'asr', 'tts'] as const;
export const SCENE_OPTIONS = ['global', 'simulation', 'knowledgebase', 'voice', 'document'] as const;

export const aiRuntimeConfigApi = {
  /** 查询配置列表 */
  list(domain?: string, scene?: string, providerId?: string) {
    const params = new URLSearchParams();
    if (domain) params.set('domain', domain);
    if (scene) params.set('scene', scene);
    if (providerId) params.set('providerId', providerId);
    const qs = params.toString();
    return request.get<AiRuntimeConfigListItem[]>(`/api/ai-runtime-config${qs ? `?${qs}` : ''}`);
  },

  /** 获取配置详情 */
  detail(id: number) {
    return request.get<AiRuntimeConfigDetail>(`/api/ai-runtime-config/${id}`);
  },

  /** 获取最新版本号 */
  version() {
    return request.get<number>('/api/ai-runtime-config/version');
  },

  /** 保存（新增或更新） */
  save(data: SaveAiRuntimeConfigRequest, operator = 'admin') {
    return request.post<number>(`/api/ai-runtime-config?operator=${operator}`, data);
  },

  /** 启用或禁用 */
  toggleEnabled(id: number, enabled: boolean, operator = 'admin') {
    return request.patch<void>(`/api/ai-runtime-config/${id}/enabled?enabled=${enabled}&operator=${operator}`);
  },

  /** 刷新缓存 */
  refresh(operator = 'admin') {
    return request.post<RefreshAiRuntimeConfigResponse>(`/api/ai-runtime-config/refresh?operator=${operator}`);
  },
};
