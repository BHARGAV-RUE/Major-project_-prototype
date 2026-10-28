export interface FileAttachment {
  id: string;
  file: File;
  name: string;
  size: number;
}

export interface StartTransferRequest {
  senderEmail: string;
  senderName: string;
  recipientEmails: string[];
  subject: string;
  message: string;
  filename: string;
  totalSizeBytes: number;
  expiryDays: number;
}

export interface StartTransferResponse {
  id: string;
  sessionId: string;
  originalFilename?: string;
  totalSizeBytes?: number;
  totalChunks: number;
  chunkSizeBytes: number;
  status: string;
  expiryTimestamp: string;
  merkleRoot?: string;
  tokens?: any[];
}

export interface TransferManifest {
  sessionId: string;
  filename: string;
  originalFilename?: string;
  totalSizeBytes: number;
  totalChunks: number;
  senderName?: string;
  senderEmail?: string;
  subject?: string;
  message?: string;
  status?: string;
  createdAt?: string;
  expiryTimestamp?: string;
  merkleRoot?: string;
}

export interface ContributorRegistration {
  name: string;
  email: string;
  password?: string;
  cloudProvider: 'GOOGLE_DRIVE' | 'DROPBOX';
  pledgedStorageBytes: number;
  refreshToken?: string;
}

export interface ContributorNode {
  id: string;
  name?: string;
  email?: string;
  cloudProvider: string;
  pledgedStorageBytes: number;
  usedStorageBytes: number;
  refreshToken?: string;
  status: string;
  tamperFlags?: number;
  createdAt?: string;
}

export interface ContributorDashboard {
  id?: string;
  name: string;
  email: string;
  cloudProvider?: string;
  pledgedStorageBytes: number;
  usedStorageBytes: number;
  status: string;
  tamperFlags?: number;
  createdAt?: string;
  googleDriveNode?: ContributorNode | null;
  dropboxNode?: ContributorNode | null;
  nodes?: ContributorNode[];
}

export interface ContributorLoginRequest {
  email: string;
  password: string;
}

export interface AdminTelemetry {
  activeContributors: number;
  totalCapacityBytes: number;
  consumedStorageBytes: number;
  activeTransfersCount: number;
  activeAlertsCount: number;
}

export interface AdminChunkDetail {
  id: string;
  chunkIndex: number;
  chunkSizeBytes: number;
  sha256Hash: string;
  status: string;
  primaryContributorId?: string;
  primaryContributorEmail: string;
  primaryProvider: string;
  replicaContributorId?: string;
  replicaContributorEmail: string;
  replicaProvider: string;
}

export interface AdminTransferSession {
  id: string;
  senderEmail: string;
  senderName: string;
  subject: string;
  message: string;
  originalFilename: string;
  totalSizeBytes: number;
  totalChunks: number;
  expiryDays: number;
  createdAt: string;
  expiryTimestamp: string;
  status: string;
  merkleRoot: string;
  recipientEmails: string[];
  chunks: AdminChunkDetail[];
  blockchainStatus: 'ANCHORED' | 'ON_CHAIN_MISMATCH';
}

export interface TamperAlert {
  alertId: string;
  sessionId?: string;
  chunkIndex?: number;
  contributorId: string;
  contributorEmail: string;
  cloudProvider: string;
  tamperType: string;
  tamperFlags?: number;
  timestamp: string;
  scheduledTerminationAt?: string;
  status: string;
}