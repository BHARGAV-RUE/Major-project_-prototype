import React, { useState } from 'react';
import JSZip from 'jszip';
import { api, FileAttachment } from '../services/api';

export const TransferHub: React.FC = () => {
  const [files, setFiles] = useState<FileAttachment[]>([]);
  const [senderName, setSenderName] = useState('');
  const [senderEmail, setSenderEmail] = useState('');
  const [recipientEmailInput, setRecipientEmailInput] = useState('');
  const [recipientEmails, setRecipientEmails] = useState<string[]>([]);
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [expiryDays, setExpiryDays] = useState(7);

  const [isTransferring, setIsTransferring] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [transferSuccess, setTransferSuccess] = useState(false);
  const [downloadUrl, setDownloadUrl] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const selectedFiles = Array.from(e.target.files).map((f) => ({
        id: Math.random().toString(36).substring(7),
        file: f,
        name: f.name,
        size: f.size,
      }));
      setFiles((prev) => [...prev, ...selectedFiles]);
    }
  };

  const removeFile = (id: string) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  const handleRecipientKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if ((e.key === 'Enter' || e.key === ',' || e.key === ' ') && recipientEmailInput.trim()) {
      e.preventDefault();
      const email = recipientEmailInput.trim().replace(/,/g, '');
      if (email && !recipientEmails.includes(email)) {
        setRecipientEmails([...recipientEmails, email]);
        setRecipientEmailInput('');
      }
    }
  };

  const removeRecipient = (emailToRemove: string) => {
    setRecipientEmails(recipientEmails.filter((email) => email !== emailToRemove));
  };

  const getTotalSizeBytes = () => files.reduce((acc, curr) => acc + curr.size, 0);

  const formatSize = (bytes: number) => {
    const mb = bytes / (1024 * 1024);
    if (mb < 1) return `${(bytes / 1024).toFixed(2)} KB`;
    if (mb > 1024) return `${(mb / 1024).toFixed(2)} GB`;
    return `${mb.toFixed(2)} MB`;
  };

  const handleSendTransfer = async (e: React.FormEvent) => {
    e.preventDefault();

    let finalRecipients = [...recipientEmails];
    if (recipientEmailInput.trim() && !finalRecipients.includes(recipientEmailInput.trim())) {
      finalRecipients.push(recipientEmailInput.trim());
      setRecipientEmails(finalRecipients);
      setRecipientEmailInput('');
    }

    if (files.length === 0) {
      setErrorMessage('Please attach at least one file.');
      return;
    }
    if (finalRecipients.length === 0) {
      setErrorMessage('Please add at least one recipient email address.');
      return;
    }

    setIsTransferring(true);
    setUploadProgress(0);
    setErrorMessage('');

    try {
      // Step 1: Zip all attached files into a single archive (lossless)
      const zip = new JSZip();
      files.forEach((f) => zip.file(f.name, f.file));

      const payloadBlob = await zip.generateAsync({ type: 'blob' });
      const payloadFilename = 'CloudTransfer_Files.zip';

      // Step 2: Initialize ONE session on Core Engine
      const startRes = await api.startTransfer({
        senderEmail,
        senderName,
        recipientEmails: finalRecipients,
        subject,
        message,
        filename: payloadFilename,
        totalSizeBytes: payloadBlob.size,
        expiryDays,
      });

      const { sessionId, tokens, totalChunks, chunkSizeBytes } = startRes;

      // Step 3: Slice blob into chunks using backend-calculated chunkSizeBytes and stream to Stream Engine
      for (let i = 0; i < totalChunks; i++) {
        const start = i * chunkSizeBytes;
        const end = Math.min(start + chunkSizeBytes, payloadBlob.size);
        const chunkBlob = payloadBlob.slice(start, end);

        await api.uploadSingleChunk(sessionId, i, chunkBlob, payloadFilename, tokens);
        setUploadProgress(Math.round(((i + 1) / totalChunks) * 100));
      }

      // Step 4: Mark session complete ONCE -> Triggers EXACTLY 1 notification email
      await api.completeTransfer(sessionId);

      setDownloadUrl(`${window.location.origin}/download/${sessionId}`);
      setTransferSuccess(true);
    } catch (err: any) {
      setErrorMessage(err.message || 'An error occurred during transfer.');
    } finally {
      setIsTransferring(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#050b14] text-white font-sans flex flex-col justify-between">
      {/* Main Container */}
      <div className="max-w-6xl mx-auto w-full px-6 py-10">
        <div className="bg-[#09101d] border border-[#132035] rounded-2xl p-8 shadow-2xl">
          {transferSuccess ? (
            <div className="bg-[#0d1d2b] border border-cyan-500/30 p-10 rounded-xl text-center max-w-2xl mx-auto my-12">
              <div className="w-16 h-16 bg-cyan-500/10 text-cyan-400 rounded-full flex items-center justify-center mx-auto mb-4 text-3xl">
                ✓
              </div>
              <h2 className="text-2xl font-bold text-white mb-2">Transfer Complete!</h2>
              <p className="text-slate-400 text-sm mb-6">
                Your archive has been chunked, encrypted with ChaCha20, and distributed across volunteer nodes.
                A single notification email has been sent.
              </p>
              <div className="bg-[#050b14] p-3.5 rounded-lg border border-[#1e2d42] font-mono text-xs text-cyan-300 select-all mb-6">
                {downloadUrl}
              </div>
              <button
                onClick={() => {
                  setTransferSuccess(false);
                  setFiles([]);
                  setRecipientEmails([]);
                }}
                className="bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-500 hover:to-cyan-400 text-white font-bold px-8 py-3 rounded-xl text-sm tracking-wide shadow-lg transition-all"
              >
                SEND ANOTHER FILE
              </button>
            </div>
          ) : (
            <form onSubmit={handleSendTransfer} className="grid grid-cols-1 lg:grid-cols-2 gap-10">
              {/* LEFT COLUMN: 1. ADD YOUR FILES */}
              <div className="flex flex-col justify-between space-y-6">
                <div>
                  <h2 className="text-sm font-bold tracking-wider text-cyan-400 uppercase mb-4">
                    1. ADD YOUR FILES
                  </h2>

                  {/* Drop Box Zone */}
                  <div className="border-2 border-dashed border-[#1a2942] hover:border-cyan-500/50 bg-[#050c18] rounded-2xl p-10 flex flex-col items-center justify-center text-center cursor-pointer relative group transition-colors min-h-[220px]">
                    <input
                      type="file"
                      multiple
                      onChange={handleFileChange}
                      className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
                    />
                    <div className="w-14 h-12 bg-amber-500/10 rounded-xl flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
                      <span className="text-amber-400 text-2xl">📁</span>
                    </div>
                    <p className="text-base font-semibold text-slate-200 mb-1">
                      Drop Files Here
                    </p>
                    <p className="text-xs text-slate-500">
                      or click to browse from computer
                    </p>
                  </div>

                  {/* Selected Files List */}
                  {files.length > 0 && (
                    <div className="mt-6 space-y-2.5 max-h-[260px] overflow-y-auto pr-1">
                      {files.map((f) => (
                        <div
                          key={f.id}
                          className="bg-[#0d1726] border border-[#18263c] rounded-lg px-4 py-3 flex items-center justify-between text-xs group"
                        >
                          <div className="flex items-center space-x-3 truncate mr-2">
                            <span className="text-slate-400">📄</span>
                            <span className="text-slate-200 font-medium truncate">{f.name}</span>
                          </div>
                          <div className="flex items-center space-x-3 shrink-0">
                            <span className="text-slate-400 font-mono">{formatSize(f.size)}</span>
                            <button
                              type="button"
                              onClick={() => removeFile(f.id)}
                              className="text-slate-500 hover:text-red-400 text-sm px-1"
                            >
                              ✕
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Total Size Footer */}
                <div className="pt-4 border-t border-[#132035] flex justify-between items-center text-xs text-slate-400">
                  <span className="font-semibold tracking-wider uppercase">TOTAL SIZE</span>
                  <span className="font-mono">
                    {(getTotalSizeBytes() / (1024 * 1024 * 1024)).toFixed(2)} GB of 5 GB Limit
                  </span>
                </div>
              </div>

              {/* RIGHT COLUMN: 2. TRANSFER DETAILS */}
              <div className="space-y-4">
                <h2 className="text-sm font-bold tracking-wider text-cyan-400 uppercase mb-4">
                  2. TRANSFER DETAILS
                </h2>

                {/* Send To (Email addresses) */}
                <div>
                  <label className="block text-xs text-slate-400 mb-1.5 font-medium">
                    Send to (Email addresses)
                  </label>
                  <div className="bg-[#050c18] border border-[#132035] focus-within:border-cyan-500 rounded-lg p-2 flex flex-wrap items-center gap-1.5 min-h-[42px]">
                    {recipientEmails.map((email) => (
                      <span
                        key={email}
                        className="bg-cyan-950/80 border border-cyan-500/40 text-cyan-300 text-xs px-2.5 py-1 rounded-md flex items-center space-x-1"
                      >
                        <span>{email}</span>
                        <button
                          type="button"
                          onClick={() => removeRecipient(email)}
                          className="hover:text-red-400 font-bold ml-1"
                        >
                          ×
                        </button>
                      </span>
                    ))}
                    <input
                      type="email"
                      value={recipientEmailInput}
                      onChange={(e) => setRecipientEmailInput(e.target.value)}
                      onKeyDown={handleRecipientKeyDown}
                      placeholder={recipientEmails.length === 0 ? "recipient@example.com (Press Enter)" : ""}
                      className="bg-transparent text-xs text-slate-200 focus:outline-none flex-1 min-w-[140px] px-1 py-0.5"
                    />
                  </div>
                </div>

                {/* Sender Information */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs text-slate-400 mb-1.5 font-medium">Your Name</label>
                    <input
                      type="text"
                      required
                      value={senderName}
                      onChange={(e) => setSenderName(e.target.value)}
                      className="w-full bg-[#050c18] border border-[#132035] focus:border-cyan-500 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-slate-400 mb-1.5 font-medium">Your Email</label>
                    <input
                      type="email"
                      required
                      value={senderEmail}
                      onChange={(e) => setSenderEmail(e.target.value)}
                      className="w-full bg-[#050c18] border border-[#132035] focus:border-cyan-500 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none"
                    />
                  </div>
                </div>

                {/* Subject */}
                <div>
                  <label className="block text-xs text-slate-400 mb-1.5 font-medium">Subject (Optional)</label>
                  <input
                    type="text"
                    value={subject}
                    onChange={(e) => setSubject(e.target.value)}
                    className="w-full bg-[#050c18] border border-[#132035] focus:border-cyan-500 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none"
                  />
                </div>

                {/* Message */}
                <div>
                  <label className="block text-xs text-slate-400 mb-1.5 font-medium">Message (Optional)</label>
                  <textarea
                    rows={3}
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    className="w-full bg-[#050c18] border border-[#132035] focus:border-cyan-500 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none resize-none"
                  />
                </div>

                {/* Link Expiry */}
                <div>
                  <label className="block text-xs text-slate-400 mb-1.5 font-medium">Link Expiry</label>
                  <select
                    value={expiryDays}
                    onChange={(e) => setExpiryDays(Number(e.target.value))}
                    className="w-full bg-[#050c18] border border-[#132035] focus:border-cyan-500 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none cursor-pointer"
                  >
                    <option value={7}>7 Days (Auto-deletes after expiry)</option>
                    <option value={3}>3 Days (Auto-deletes after expiry)</option>
                    <option value={1}>1 Day (Auto-deletes after expiry)</option>
                  </select>
                </div>

                {/* Error Banner */}
                {errorMessage && (
                  <p className="text-red-400 text-xs font-medium leading-relaxed bg-red-950/30 p-2.5 rounded-lg border border-red-900/50">
                    {errorMessage}
                  </p>
                )}

                {/* Progress Bar */}
                {isTransferring && (
                  <div className="space-y-1.5">
                    <div className="flex justify-between text-xs text-slate-400 font-mono">
                      <span>Chunking & Streaming Chunks...</span>
                      <span>{uploadProgress}%</span>
                    </div>
                    <div className="w-full bg-[#050c18] border border-[#132035] rounded-full h-2 overflow-hidden">
                      <div
                        className="bg-gradient-to-r from-blue-600 to-cyan-400 h-full transition-all duration-300 rounded-full"
                        style={{ width: `${uploadProgress}%` }}
                      />
                    </div>
                  </div>
                )}

                {/* Action Button */}
                <button
                  type="submit"
                  disabled={isTransferring}
                  className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 disabled:opacity-50 text-white font-bold py-3.5 rounded-xl text-xs tracking-wider uppercase transition-all shadow-lg shadow-blue-900/20 mt-2"
                >
                  {isTransferring ? 'SENDING FILES...' : 'SEND FILES SECURELY'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>

      <footer className="text-center py-4 text-xs text-slate-500 border-t border-[#0e1726] bg-[#03070e] flex items-center justify-center space-x-2">
        <span>🔒</span>
        <span>Your files are chunked and encrypted before being uploaded. No one else can read them.</span>
      </footer>
    </div>
  );
};