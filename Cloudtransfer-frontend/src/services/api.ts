import axios from 'axios';

export const coreApi = axios.create({
  baseURL: import.meta.env.VITE_CORE_API_URL || 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

export const streamApi = axios.create({
  baseURL: import.meta.env.VITE_STREAM_API_URL || 'http://localhost:8081/api/v1',
  timeout: 0,
});

export const transferApi = {
  createSession: async (payload: UploadRequestDto) => {
    const res = await coreApi.post<SessionInitMessage>('/transfers/init', payload);
    return res.data;
  },

  streamFilePayload: async (sessionId: string, formData: FormData, onProgress: (pct: number) => void) => {
    return streamApi.post(`/stream/upload/${sessionId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (evt) => {
        if (evt.total) {
          onProgress(Math.round((evt.loaded * 100) / evt.total));
        }
      },
    });
  },

  getBlueprint: async (sessionId: string, downloadToken?: string) => {
    const res = await coreApi.get<BlueprintResponseDto>(`/download/blueprint/${sessionId}`, {
      params: { token: downloadToken },
    });
    return res.data;
  },
};

export const adminApi = {
  getStats: async () => {
    const res = await coreApi.get<SystemStatsDto>('/admin/stats');
    return res.data;
  },

  getSessions: async () => {
    const res = await coreApi.get<SessionInitMessage[]>('/admin/sessions');
    return res.data;
  },

  getContributors: async () => {
    const res = await coreApi.get<Contributor[]>('/admin/contributors');
    return res.data;
  },

  approveContributor: async (id: string) => {
    const res = await coreApi.post(`/admin/contributors/${id}/approve`);
    return res.data;
  },
};