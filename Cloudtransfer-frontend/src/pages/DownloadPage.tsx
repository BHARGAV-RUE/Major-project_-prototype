import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../services/api';

export const DownloadPage: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();
  const [manifest, setManifest] = useState<any>(null);
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);
  const [statusMessage, setStatusMessage] = useState('Fetching transfer details...');
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (sessionId) {
      loadManifest();
    }
  }, [sessionId]);

  const loadManifest = async () => {
    setErrorMessage('');
    setStatusMessage('Fetching transfer details...');
    try {
      const data = await api.getDownloadManifest(sessionId!);
      setManifest(data);
      setStatusMessage('Ready for download');
    } catch (err: any) {
      const msg = err.message || 'Failed to load transfer details. Session may have expired or not exist.';
      setErrorMessage(msg);
      setStatusMessage('Unable to load transfer');
    }
  };

  const handleDownload = async () => {
    if (!sessionId || !manifest) return;

    setIsDownloading(true);
    setDownloadProgress(0);
    setErrorMessage('');

    try {
      const totalChunks = manifest.totalChunks || manifest.chunks?.length || 1;
      const chunkBlobs: Blob[] = [];

      for (let i = 0; i < totalChunks; i++) {
        setStatusMessage(`Downloading & decrypting chunk ${i + 1} of ${totalChunks}...`);

        const chunkBlob = await api.downloadChunk(sessionId, i);
        chunkBlobs.push(chunkBlob);

        setDownloadProgress(Math.round(((i + 1) / totalChunks) * 100));
      }

      setStatusMessage('Reassembling final ZIP package...');

      const completeFileBlob = new Blob(chunkBlobs, { type: 'application/zip' });

      const downloadUrl = window.URL.createObjectURL(completeFileBlob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = manifest.filename || 'CloudTransfer_Files.zip';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(downloadUrl);

      setStatusMessage('Download complete!');
    } catch (err: any) {
      setErrorMessage(err.message || 'An error occurred during download.');
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#050b14] text-white font-sans flex flex-col items-center justify-center p-6">
      <div className="bg-[#09101d] border border-[#132035] rounded-2xl p-8 max-w-lg w-full shadow-2xl">
        <h1 className="text-xl font-bold text-cyan-400 mb-2">Secure File Download</h1>

        {/* Error Banner */}
        {errorMessage && (
          <div className="bg-red-950/40 border border-red-900/60 p-4 rounded-xl my-4">
            <p className="text-red-400 text-xs font-semibold mb-1">DOWNLOAD ERROR</p>
            <p className="text-red-300 text-xs leading-relaxed">{errorMessage}</p>
            <button
              onClick={loadManifest}
              className="mt-3 bg-red-900/50 hover:bg-red-800/60 text-red-200 text-xs font-bold px-3 py-1.5 rounded-lg transition-colors"
            >
              RETRY FETCHING MANIFEST
            </button>
          </div>
        )}

        {manifest ? (
          <div className="space-y-4 my-4">
            <div className="bg-[#050c18] p-4 rounded-xl border border-[#132035]">
              <p className="text-xs text-slate-400 font-medium">FILE NAME</p>
              <p className="text-sm font-semibold text-slate-200 truncate">{manifest.filename}</p>
            </div>

            <div className="bg-[#050c18] p-4 rounded-xl border border-[#132035] grid grid-cols-2 gap-4">
              <div>
                <p className="text-xs text-slate-400 font-medium">TOTAL SIZE</p>
                <p className="text-sm font-semibold text-slate-200">
                  {manifest.totalSizeBytes ? (manifest.totalSizeBytes / (1024 * 1024)).toFixed(2) : '0.00'} MB
                </p>
              </div>
              <div>
                <p className="text-xs text-slate-400 font-medium">CHUNKS</p>
                <p className="text-sm font-semibold text-slate-200">
                  {manifest.totalChunks || manifest.chunks?.length || 1}
                </p>
              </div>
            </div>

            {isDownloading && (
              <div className="space-y-2">
                <div className="flex justify-between text-xs text-slate-400 font-mono">
                  <span>{statusMessage}</span>
                  <span>{downloadProgress}%</span>
                </div>
                <div className="w-full bg-[#050c18] border border-[#132035] rounded-full h-2 overflow-hidden">
                  <div
                    className="bg-gradient-to-r from-blue-600 to-cyan-400 h-full transition-all duration-300 rounded-full"
                    style={{ width: `${downloadProgress}%` }}
                  />
                </div>
              </div>
            )}

            <button
              onClick={handleDownload}
              disabled={isDownloading}
              className="w-full bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-500 hover:to-cyan-400 disabled:opacity-50 text-white font-bold py-3.5 rounded-xl text-xs tracking-wider uppercase transition-all shadow-lg mt-2"
            >
              {isDownloading ? 'DOWNLOADING & REASSEMBLING...' : 'DOWNLOAD FILE'}
            </button>
          </div>
        ) : (
          !errorMessage && <p className="text-xs text-slate-400 my-6">{statusMessage}</p>
        )}
      </div>
    </div>
  );
};