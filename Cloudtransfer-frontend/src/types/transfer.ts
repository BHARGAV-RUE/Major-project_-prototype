export interface FileMetadataItem {
  id: string;
  file: File;
  name: string;
  sizeBytes: number;
  extension: string;
}

export interface UploadRequestDto {
  senderEmail: string;
  senderName: string;
  recipients: string[];
  subject?: string;
  message?: string;
  totalSizeBytes: number;
  files: {
    filename: string;
    fileSizeBytes: number;
  }[];
}

export interface SessionInitMessage {
  sessionId: string;
  encryptionKeyHash: string;
  totalChunks: number;
  chunkSize: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
}

export interface BlueprintResponseDto {
  sessionId: string;
  filename: string;
  totalSize: number;
  encryptionKeyHash: string;
  chunks: ChunkAssignmentDto[];
}

export interface ChunkAssignmentDto {
  chunkIndex: number;
  chunkHash: string;
  provider: 'GOOGLE_DRIVE' | 'DROPBOX' | 'DECENTRALIZED_NODE';
  downloadUrl: string;
}

export interface SystemStatsDto {
  activeSessions: number;
  totalTransferredBytes: number;
  activeContributors: number;
  storageNodesCount: number;
}

export interface Contributor {
  id: string;
  name: string;
  email: string;
  nodeAddress: string;
  storageAllocatedGB: number;
  status: 'ACTIVE' | 'PENDING' | 'SUSPENDED';
  reputationScore: number;
}