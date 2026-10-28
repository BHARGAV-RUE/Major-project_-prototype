export interface FileChunkSlice {
  chunkIndex: number;
  totalChunks: number;
  blob: Blob;
  filename: string;
}

/**
 * Prepares files for transfer:
 * - 1 File: Returns raw file as-is with original name.
 * - 2+ Files: Packages files into a standard extractable .zip archive (CloudTransfer_Files.zip).
 */
export async function prepareFilesPayload(
  files: { file: File; name: string }[]
): Promise<{ blob: Blob; filename: string }> {
  if (files.length === 0) {
    throw new Error('No files provided.');
  }

  // Single file: Send directly as-is
  if (files.length === 1) {
    return {
      blob: files[0].file,
      filename: files[0].name,
    };
  }

  // Multiple files: Package into a standard ZIP archive
  const zipBlob = await createZipArchive(files);
  return {
    blob: zipBlob,
    filename: 'CloudTransfer_Files.zip',
  };
}

/**
 * Slices a file or bundled payload Blob into chunks strictly using Core Engine's chunkSizeBytes.
 * Guarantees unique sequential indices (0, 1, 2, ..., totalChunks - 1).
 */
export function slicePayloadIntoChunks(
  payloadBlob: Blob,
  chunkSizeBytes: number,
  filename: string
): FileChunkSlice[] {
  const chunks: FileChunkSlice[] = [];
  const totalChunks = Math.max(1, Math.ceil(payloadBlob.size / chunkSizeBytes));

  for (let i = 0; i < totalChunks; i++) {
    const start = i * chunkSizeBytes;
    const end = Math.min(payloadBlob.size, start + chunkSizeBytes);
    const chunkBlob = payloadBlob.slice(start, end, payloadBlob.type);

    chunks.push({
      chunkIndex: i,
      totalChunks,
      blob: chunkBlob,
      filename,
    });
  }

  return chunks;
}

/**
 * Pure JavaScript ZIP archive generator (STORED mode / Zero external dependencies).
 */
async function createZipArchive(files: { file: File; name: string }[]): Promise<Blob> {
  const encoder = new TextEncoder();
  const localHeaders: Uint8Array[] = [];
  const centralDirectories: Uint8Array[] = [];
  let offset = 0;

  for (const item of files) {
    const fileBytes = new Uint8Array(await item.file.arrayBuffer());
    const filenameBytes = encoder.encode(item.name);
    const crc = calculateCRC32(fileBytes);
    const size = fileBytes.length;

    // Local file header (30 bytes + filename)
    const localHeader = new Uint8Array(30 + filenameBytes.length);
    const view = new DataView(localHeader.buffer);
    view.setUint32(0, 0x04034b50, true);
    view.setUint16(4, 20, true);
    view.setUint16(6, 0, true);
    view.setUint16(8, 0, true); // STORED (no compression delay)
    view.setUint16(10, 0, true);
    view.setUint16(12, 0, true);
    view.setUint32(14, crc, true);
    view.setUint32(18, size, true);
    view.setUint32(22, size, true);
    view.setUint16(26, filenameBytes.length, true);
    view.setUint16(28, 0, true);
    localHeader.set(filenameBytes, 30);

    localHeaders.push(localHeader);
    localHeaders.push(fileBytes);

    // Central directory header (46 bytes + filename)
    const cdHeader = new Uint8Array(46 + filenameBytes.length);
    const cdView = new DataView(cdHeader.buffer);
    cdView.setUint32(0, 0x02014b50, true);
    cdView.setUint16(4, 20, true);
    cdView.setUint16(6, 20, true);
    cdView.setUint16(8, 0, true);
    cdView.setUint16(10, 0, true);
    cdView.setUint16(12, 0, true);
    cdView.setUint16(14, 0, true);
    cdView.setUint32(16, crc, true);
    cdView.setUint32(20, size, true);
    cdView.setUint32(24, size, true);
    cdView.setUint16(28, filenameBytes.length, true);
    cdView.setUint16(30, 0, true);
    cdView.setUint16(32, 0, true);
    cdView.setUint16(34, 0, true);
    cdView.setUint16(36, 0, true);
    cdView.setUint32(38, 0, true);
    cdView.setUint32(42, offset, true);
    cdHeader.set(filenameBytes, 46);

    centralDirectories.push(cdHeader);
    offset += localHeader.length + fileBytes.length;
  }

  const cdOffset = offset;
  let cdSize = 0;
  for (const cd of centralDirectories) cdSize += cd.length;

  // End of central directory record (22 bytes)
  const eocd = new Uint8Array(22);
  const eocdView = new DataView(eocd.buffer);
  eocdView.setUint32(0, 0x06054b50, true);
  eocdView.setUint16(4, 0, true);
  eocdView.setUint16(6, 0, true);
  eocdView.setUint16(8, files.length, true);
  eocdView.setUint16(10, files.length, true);
  eocdView.setUint32(12, cdSize, true);
  eocdView.setUint32(16, cdOffset, true);

  const allParts = [...localHeaders, ...centralDirectories, eocd];
  return new Blob(allParts, { type: 'application/zip' });
}

function calculateCRC32(bytes: Uint8Array): number {
  let crc = 0xFFFFFFFF;
  for (let i = 0; i < bytes.length; i++) {
    crc ^= bytes[i];
    for (let j = 0; j < 8; j++) {
      crc = (crc >>> 1) ^ ((crc & 1) ? 0xEDB88320 : 0);
    }
  }
  return (crc ^ 0xFFFFFFFF) >>> 0;
}