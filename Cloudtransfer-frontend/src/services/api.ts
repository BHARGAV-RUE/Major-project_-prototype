import {
  AdminTelemetry,
  AdminTransferSession,
  ContributorDashboard,
  ContributorLoginRequest,
  ContributorRegistration,
  StartTransferRequest,
  StartTransferResponse,
  TamperAlert,
  TransferManifest,
} from '../types/transfer';

const CORE_API = 'http://localhost:8080/api/v1';
const STREAM_API = 'http://localhost:8081/api/v1';

export const api = {
  // --- 1. TRANSFER OPERATIONS ---
  async startTransfer(payload: StartTransferRequest): Promise<StartTransferResponse> {
    const res = await fetch(`${CORE_API}/transfers/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Server error (${res.status}) starting transfer.`);
    }
    return res.json();
  },

  async uploadSingleChunk(
    sessionId: string,
    chunkIndex: number,
    chunkBlob: Blob,
    filename: string
  ): Promise<any> {
    const formData = new FormData();
    formData.append('sessionId', sessionId);
    formData.append('chunkIndex', chunkIndex.toString());
    formData.append('files', chunkBlob, filename);

    const res = await fetch(`${STREAM_API}/stream/upload`, {
      method: 'POST',
      body: formData,
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Chunk #${chunkIndex} upload failed (${res.status}).`);
    }
    return res.json();
  },

  async completeTransfer(sessionId: string): Promise<any> {
    const res = await fetch(`${CORE_API}/transfers/${sessionId}/complete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Failed to complete transfer (${res.status}).`);
    }
    return res.json();
  },

  async getTransferManifest(sessionId: string): Promise<TransferManifest> {
    const res = await fetch(`${CORE_API}/transfers/download/${sessionId}`);
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      if (res.status === 410 || data.message?.includes('Admin deleted your files')) {
        throw new Error('Admin deleted your files.');
      }
      throw new Error(data.message || 'Transfer link expired or invalid');
    }
    return data;
  },

  async getDownloadManifest(sessionId: string): Promise<TransferManifest> {
    return this.getTransferManifest(sessionId);
  },

  getDownloadChunkUrl(sessionId: string, chunkIndex: number = 0, filename?: string): string {
    let url = `${STREAM_API}/stream/download-chunk?sessionId=${sessionId}&chunkIndex=${chunkIndex}`;
    if (filename) url += `&filename=${encodeURIComponent(filename)}`;
    return url;
  },

  async downloadChunk(sessionId: string, chunkIndex: number = 0): Promise<Blob> {
    const res = await fetch(`${STREAM_API}/stream/download-chunk?sessionId=${sessionId}&chunkIndex=${chunkIndex}`);
    if (!res.ok) {
      throw new Error(`Failed to download chunk #${chunkIndex} from Stream Engine.`);
    }
    return res.blob();
  },

  // --- 2. CONTRIBUTOR OPERATIONS ---
  async registerContributor(payload: ContributorRegistration): Promise<any> {
    const res = await fetch(`${CORE_API}/contributors/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      if (res.status === 409) {
        return { message: 'Contributor exists, proceed to OAuth authorization.' };
      }
      throw new Error(err.message || err.error || 'Registration failed');
    }
    return res.json();
  },

  async getGoogleAuthUrl(email?: string): Promise<string> {
    const url = email
      ? `${CORE_API}/contributors/oauth/google/url?email=${encodeURIComponent(email)}`
      : `${CORE_API}/contributors/oauth/google/url`;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Failed to fetch Google OAuth authorization URL.');
    return res.text();
  },

  async getDropboxAuthUrl(email?: string): Promise<string> {
    const url = email
      ? `${CORE_API}/contributors/oauth/dropbox/url?email=${encodeURIComponent(email)}`
      : `${CORE_API}/contributors/oauth/dropbox/url`;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Failed to fetch Dropbox OAuth authorization URL.');
    return res.text();
  },

  async getContributorDashboard(email: string): Promise<ContributorDashboard> {
    const res = await fetch(`${CORE_API}/contributors/dashboard/${encodeURIComponent(email)}`);
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'No contributor account found for this email.');
    }
    return res.json();
  },

  async loginContributorDashboard(credentials: ContributorLoginRequest): Promise<ContributorDashboard> {
    const res = await fetch(`${CORE_API}/contributors/dashboard/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Authentication failed');
    }
    return res.json();
  },

  async forfeitContributorNode(nodeId: string, password: string): Promise<any> {
    const res = await fetch(`${CORE_API}/contributors/forfeit/${nodeId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ password }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to forfeit node');
    }
    return res.json();
  },

  // --- 3. ADMIN CONTROL TOWER OPERATIONS ---
  async loginAdmin(password: string): Promise<{ status: string; adminToken: string }> {
    const res = await fetch(`${CORE_API}/admin/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ password }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Invalid admin credentials');
    }
    return res.json();
  },

  async getAdminTelemetry(adminSecret: string): Promise<AdminTelemetry> {
    const res = await fetch(`${CORE_API}/admin/telemetry`, {
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Secret': adminSecret,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to load telemetry stats');
    }
    return res.json();
  },

  async getAdminContributors(adminSecret: string): Promise<ContributorDashboard[]> {
    const res = await fetch(`${CORE_API}/admin/contributors`, {
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Secret': adminSecret,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to fetch contributors list');
    }
    return res.json();
  },

  async approveContributor(id: string, adminSecret: string): Promise<any> {
    const res = await fetch(`${CORE_API}/admin/contributors/${id}/approve`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Secret': adminSecret,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to approve contributor');
    }
    return res.json();
  },

  async warnContributorTamper(id: string, adminSecret: string): Promise<any> {
    const res = await fetch(`${CORE_API}/admin/contributors/${id}/warn-tamper`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Secret': adminSecret,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to issue tamper warning flag.');
    }
    return res.json();
  },

  async deleteContributor(id: string, adminSecret: string): Promise<any> {
    const res = await fetch(`${CORE_API}/admin/contributors/${id}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Secret': adminSecret,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to terminate contributor');
    }
    return res.json();
  },

  async getAdminTransfers(adminSecret: string): Promise<AdminTransferSession[]> {
    const res = await fetch(`${CORE_API}/admin/transfers`, {
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Secret': adminSecret,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to fetch transfer sessions history');
    }
    return res.json();
  },

  async deleteAdminTransferSession(sessionId: string, adminSecret: string): Promise<any> {
    const res = await fetch(`${CORE_API}/admin/transfers/${sessionId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Secret': adminSecret,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to delete transfer session.');
    }
    return res.json();
  },

  async purgeSenderData(senderEmail: string, adminSecret: string): Promise<any> {
    const res = await fetch(`${CORE_API}/admin/transfers/purge-sender?email=${encodeURIComponent(senderEmail)}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Secret': adminSecret,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to purge sender data.');
    }
    return res.json();
  },

  async triggerEmergencyPurge(adminSecret: string): Promise<any> {
    const res = await fetch(`${CORE_API}/admin/transfers/emergency-purge`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Secret': adminSecret,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to execute emergency purge.');
    }
    return res.json();
  },

  async getAdminTamperAlerts(adminSecret: string): Promise<TamperAlert[]> {
    const res = await fetch(`${CORE_API}/admin/tamper-alerts`, {
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Secret': adminSecret,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Failed to fetch tamper security alerts');
    }
    return res.json();
  },
};