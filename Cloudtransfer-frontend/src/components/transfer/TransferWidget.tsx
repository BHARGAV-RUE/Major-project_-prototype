import React, { useState, useRef } from 'react';
import { FileMetadataItem } from '../../types/transfer';
import { transferApi } from '../../services/api';

const MAX_TOTAL_BYTES = 5 * 1024 * 1024 * 1024; // 5 GB

export const TransferWidget: React.FC = () => {
  const [files, setFiles] = useState<FileMetadataItem[]>([]);
  const [recipients, setRecipients] = useState<string[]>(['prof.adviser@university.edu']);
  const [recipientInput, setRecipientInput] = useState('');
  const [senderEmail, setSenderEmail] = useState('');
  const [senderName, setSenderName] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');

  const [uploadStatus, setUploadStatus] = useState<'IDLE' | 'INIT' | 'UPLOADING' | 'SUCCESS' | 'ERROR'>('IDLE');
  const [uploadProgress, setUploadProgress] = useState(0);
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const totalUsedBytes = files.reduce((acc, curr) => acc + curr.sizeBytes, 0);
  const usedPercentage = Math.min(100, (totalUsedBytes / MAX_TOTAL_BYTES) * 100);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files) return;
    const selectedFiles = Array.from(e.target.files);

    const mapped: FileMetadataItem[] = selectedFiles.map((f) => ({
      id: `${f.name}-${Date.now()}-${Math.random()}`,
      file: f,
      name: f.name,
      sizeBytes: f.size,
      extension: f.name.split('.').pop()?.toUpperCase() || 'FILE',
    }));

    setFiles((prev) => [...prev, ...mapped]);
  };

  const removeFile = (id: string) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  const handleAddRecipient = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && recipientInput.trim()) {
      e.preventDefault();
      if (!recipients.includes(recipientInput.trim())) {
        setRecipients([...recipients, recipientInput.trim()]);
      }
      setRecipientInput('');
    }
  };

  const removeRecipient = (email: string) => {
    setRecipients(recipients.filter((r) => r !== email));
  };

  const formatSize = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  const executeTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (files.length === 0 || recipients.length === 0 || !senderEmail || !senderName) {
      alert('Please fill out all required fields and add at least one file.');
      return;
    }

    try {
      setUploadStatus('INIT');
      const sessionInit = await transferApi.createSession({
        senderEmail,
        senderName,
        recipients,
        subject,
        message,
        totalSizeBytes: totalUsedBytes,
        files: files.map((f) => ({ filename: f.name, fileSizeBytes: f.sizeBytes })),
      });

      setActiveSessionId(sessionInit.sessionId);
      setUploadStatus('UPLOADING');

      const formData = new FormData();
      files.forEach((f) => formData.append('files', f.file));

      await transferApi.streamFilePayload(sessionInit.sessionId, formData, (pct) => {
        setUploadProgress(pct);
      });

      setUploadStatus('SUCCESS');
    } catch (err) {
      console.error(err);
      setUploadStatus('ERROR');
    }
  };

  return (
    <div className="w-full max-w-[1040px] mx-auto bg-white rounded-2xl overflow-hidden shadow-widget border border-navy/5 animate-rise flex flex-col md:flex-row">
      {/* File Upload Column */}
      <section className="flex-1 p-6 sm:p-9 flex flex-col min-w-0">
        <h2 className="font-display text-xl font-bold tracking-tight mb-5 text-bodytext">Add your files</h2>

        <div
          onClick={() => fileInputRef.current?.click()}
          className="border-[1.5px] border-dashed border-[#b8c4dc] rounded-2xl p-8 text-center bg-soft cursor-pointer mb-5 hover:border-cobalt hover:bg-[#eef3ff] transition-colors"
        >
          <div className="w-12 h-12 rounded-xl bg-cobalt grid place-items-center mx-auto mb-3.5">
            <svg className="w-5.5 h-5.5 stroke-white fill-none stroke-[2.2] stroke-linecap-round" viewBox="0 0 24 24">
              <path d="M12 5v14M5 12h14"/>
            </svg>
          </div>
          <h3 className="text-base font-bold text-cobalt">Click to add files</h3>
          <p className="text-muted text-sm">Or drag and drop them here</p>
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            multiple
            className="hidden"
          />
        </div>

        {/* Selected File List */}
        <div className="flex flex-col gap-2.5 overflow-y-auto max-h-[220px] mb-4">
          {files.map((file) => (
            <div key={file.id} className="flex items-center gap-3 p-3 border border-line rounded-xl bg-white">
              <span className={`flex-none w-10 h-10 rounded-lg grid place-items-center text-[0.64rem] font-extrabold ${
                file.extension === 'PPTX' || file.extension === 'PPT'
                  ? 'bg-[#fdeadb] text-[#b9560f]'
                  : 'bg-[#e6edff] text-cobalt-dark'
              }`}>
                {file.extension}
              </span>
              <div className="flex-1 min-w-0 flex flex-col">
                <span className="font-semibold text-sm truncate text-bodytext">{file.name}</span>
                <span className="text-xs text-muted">{formatSize(file.sizeBytes)}</span>
              </div>
              <button
                type="button"
                onClick={() => removeFile(file.id)}
                className="flex-none w-8 h-8 grid place-items-center border-0 rounded-lg bg-transparent text-[#8d9ab2] cursor-pointer hover:bg-[#fdecef] hover:text-danger transition-colors"
              >
                <svg className="w-4 h-4 stroke-current fill-none stroke-[2.2] stroke-linecap-round" viewBox="0 0 24 24">
                  <path d="M18 6 6 18M6 6l12 12"/>
                </svg>
              </button>
            </div>
          ))}
        </div>

        {/* Storage Capacity Bar */}
        <div className="mt-auto pt-5 text-xs text-muted">
          {formatSize(totalUsedBytes)} of 5 GB used
          <div className="h-1.5 mt-2 rounded-full bg-[#e4e9f3] overflow-hidden">
            <span
              className="block h-full bg-teal transition-all duration-300"
              style={{ width: `${usedPercentage}%` }}
            />
          </div>
        </div>
      </section>

      {/* Form Details Column */}
      <section className="flex-1 p-6 sm:p-9 flex flex-col bg-soft border-t md:border-t-0 md:border-l border-line">
        <h2 className="font-display text-xl font-bold tracking-tight mb-5 text-bodytext">Transfer details</h2>

        <form onSubmit={executeTransfer} className="flex flex-col flex-1">
          {/* Recipients */}
          <div className="flex flex-col mb-4">
            <label className="text-xs font-semibold mb-1.5 text-bodytext">Recipient email(s) *</label>
            <div className="flex flex-wrap gap-1.5 mb-2">
              {recipients.map((email) => (
                <span key={email} className="inline-flex items-center gap-2 px-2.5 py-1 rounded-md bg-[#e6edff] text-cobalt-dark text-xs font-semibold">
                  {email}
                  <button type="button" onClick={() => removeRecipient(email)} className="border-0 bg-transparent text-current opacity-60 hover:opacity-100 cursor-pointer">
                    ×
                  </button>
                </span>
              ))}
            </div>
            <input
              type="text"
              value={recipientInput}
              onChange={(e) => setRecipientInput(e.target.value)}
              onKeyDown={handleAddRecipient}
              placeholder="Type an email and press Enter"
              className="w-full px-3.5 py-2.5 border border-[#cdd6e6] rounded-xl bg-white text-sm text-bodytext placeholder-[#9aa6bd] focus:outline-none focus:border-cobalt focus:ring-4 focus:ring-cobalt/15"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 mb-4">
            <div className="flex flex-col">
              <label className="text-xs font-semibold mb-1.5 text-bodytext">Your email *</label>
              <input
                type="email"
                required
                value={senderEmail}
                onChange={(e) => setSenderEmail(e.target.value)}
                placeholder="sender@example.com"
                className="w-full px-3.5 py-2.5 border border-[#cdd6e6] rounded-xl bg-white text-sm text-bodytext focus:outline-none focus:border-cobalt focus:ring-4 focus:ring-cobalt/15"
              />
            </div>
            <div className="flex flex-col">
              <label className="text-xs font-semibold mb-1.5 text-bodytext">Your name *</label>
              <input
                type="text"
                required
                value={senderName}
                onChange={(e) => setSenderName(e.target.value)}
                placeholder="John Doe"
                className="w-full px-3.5 py-2.5 border border-[#cdd6e6] rounded-xl bg-white text-sm text-bodytext focus:outline-none focus:border-cobalt focus:ring-4 focus:ring-cobalt/15"
              />
            </div>
          </div>

          <div className="flex flex-col mb-4">
            <label className="text-xs font-semibold mb-1.5 text-bodytext">
              Subject <span className="font-normal text-muted text-xs">(optional)</span>
            </label>
            <input
              type="text"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              placeholder="Project files"
              className="w-full px-3.5 py-2.5 border border-[#cdd6e6] rounded-xl bg-white text-sm text-bodytext focus:outline-none focus:border-cobalt focus:ring-4 focus:ring-cobalt/15"
            />
          </div>

          <div className="flex flex-col mb-4">
            <label className="text-xs font-semibold mb-1.5 text-bodytext">
              Message <span className="font-normal text-muted text-xs">(optional)</span>
            </label>
            <textarea
              rows={2}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Here are the latest files..."
              className="w-full px-3.5 py-2.5 border border-[#cdd6e6] rounded-xl bg-white text-sm text-bodytext resize-none focus:outline-none focus:border-cobalt focus:ring-4 focus:ring-cobalt/15"
            />
          </div>

          {/* Progress Overlay */}
          {uploadStatus !== 'IDLE' && (
            <div className="mb-4 p-3 border border-line rounded-xl bg-white">
              <div className="flex justify-between text-xs font-semibold mb-1">
                <span>{uploadStatus === 'INIT' ? 'Initializing Encryption Session...' : 'Streaming ChaCha20 Chunks...'}</span>
                <span>{uploadProgress}%</span>
              </div>
              <div className="w-full bg-[#e4e9f3] h-2 rounded-full overflow-hidden">
                <div className="bg-cobalt h-full transition-all duration-200" style={{ width: `${uploadProgress}%` }} />
              </div>
              {activeSessionId && (
                <span className="block text-[0.65rem] text-muted font-mono mt-1">Session ID: {activeSessionId}</span>
              )}
            </div>
          )}

          <button
            type="submit"
            disabled={uploadStatus === 'INIT' || uploadStatus === 'UPLOADING'}
            className="mt-auto w-full py-3.5 border-0 rounded-xl bg-cobalt text-white font-bold text-base cursor-pointer hover:bg-cobalt-dark active:scale-[0.985] transition-all disabled:bg-gray-400"
          >
            {uploadStatus === 'SUCCESS' ? 'Transfer Completed!' : 'Transfer files'}
          </button>

          <p className="mt-3 text-center text-xs text-muted">
            Files are encrypted before they leave your browser.
          </p>
        </form>
      </section>
    </div>
  );
};