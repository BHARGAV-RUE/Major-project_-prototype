import { api } from './api';

export class ChunkUploadPipeline {
  async uploadSingleChunk(
    sessionId: string,
    chunkIndex: number,
    blob: Blob,
    filename: string
  ): Promise<void> {
    await api.uploadSingleChunk(sessionId, chunkIndex, blob, filename);
  }
}