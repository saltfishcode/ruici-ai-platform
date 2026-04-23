import { request } from './request';
import type {
  ResumeDetail,
  ResumeListItem,
  ResumeStats,
  UploadResponse,
} from '../types/document';

export const documentApi = {
  /**
   * 上传简历并获取分析结果
   */
  async uploadAndAnalyze(file: File): Promise<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return request.upload<UploadResponse>('/api/documents/upload', formData);
  },

  /**
   * 健康检查
   */
  async healthCheck(): Promise<{ status: string; service: string }> {
    return request.get('/api/documents/health');
  },

  async getDocuments(): Promise<ResumeListItem[]> {
    return request.get<ResumeListItem[]>('/api/documents');
  },

  async getDocumentDetail(id: number): Promise<ResumeDetail> {
    return request.get<ResumeDetail>(`/api/documents/${id}/detail`);
  },

  async deleteDocument(id: number): Promise<void> {
    return request.delete(`/api/documents/${id}`);
  },

  async reanalyze(id: number): Promise<void> {
    return request.post(`/api/documents/${id}/reanalyze`);
  },

  async exportPdf(id: number): Promise<Blob> {
    const response = await request.getInstance().get(`/api/documents/${id}/export`, {
      responseType: 'blob',
      skipResultTransform: true,
    } as never);
    return response.data;
  },

  async getStatistics(): Promise<ResumeStats> {
    return request.get<ResumeStats>('/api/documents/statistics');
  },
};
