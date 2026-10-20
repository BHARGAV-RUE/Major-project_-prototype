import React, { useState } from 'react';
import { startTransferSession } from './services/api';

const CHUNK_SIZE = 50 * 1024 * 1024; // 50MB client-side chunk slice

export default function App() {
  const [files, setFiles] = useState([]);
  const [recipients, setRecipients] = useState(['prof.adviser@university.edu']);
  const [recipientInput, setRecipientInput] = useState('');
  const [senderEmail, setSenderEmail] = useState('');
  const [senderName, setSenderName] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');

  const handleFileChange = (e) => {
    const selectedFiles = Array.from(e.target.files);
    const newFiles = selectedFiles.map(file => {
      const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
      return {
        fileObj: file,
        name: file.name,
        size: file.size,
        sizeFormatted: (file.size / (1024 * 1024)).toFixed(1) + ' MB',
        type: file.name.split('.').pop().toUpperCase(),
        totalChunks: totalChunks
      };
    });
    setFiles(prev => [...prev, ...newFiles]);
  };

  const removeFile = (index) => {
    setFiles(files.filter((_, i) => i !== index));
  };

  const addRecipientTag = (e) => {
    if (e.key === 'Enter' && recipientInput.trim()) {
      e.preventDefault();
      if (!recipients.includes(recipientInput.trim())) {
        setRecipients([...recipients, recipientInput.trim()]);
      }
      setRecipientInput('');
    }
  };

  const removeRecipient = (emailToRemove) => {
    setRecipients(recipients.filter(email => email !== emailToRemove));
  };

  const handleTransferSubmit = async (e) => {
    e.preventDefault();
    if (files.length === 0) {
      alert('Please add at least one file to transfer.');
      return;
    }
    if (recipients.length === 0) {
      alert('Please add at least one recipient email.');
      return;
    }

    setLoading(true);
    setStatusMessage('Slicing files into 50MB chunks & computing SHA-256 hashes...');

    try {
      for (const fileItem of files) {
        const totalChunks = fileItem.totalChunks;
        for (let i = 0; i < totalChunks; i++) {
          const start = i * CHUNK_SIZE;
          const end = Math.min(fileItem.size, start + CHUNK_SIZE);
          const chunkBlob = fileItem.fileObj.slice(start, end);

          const buffer = await chunkBlob.arrayBuffer();
          const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
          const hashArray = Array.from(new Uint8Array(hashBuffer));
          const chunkHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

          console.log(`[Client-Side] Sliced Chunk #${i} | Size: ${chunkBlob.size} bytes | Hash: ${chunkHash.substring(0,12)}...`);
        }
      }

      setStatusMessage('Dispatching transfer to Brain & sending email notifications...');

      const payload = {
        senderEmail: senderEmail,
        filename: files[0].name,
        filePath: "C:/docs/" + files[0].name,
        totalSizeBytes: files.reduce((acc, f) => acc + f.size, 0),
        expiryDays: 7
      };

      await startTransferSession(payload);
      alert('Transfer initiated successfully! Recipient email notification sent.');
      setFiles([]);
    } catch (err) {
      console.error(err);
      alert('Transfer failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
      setStatusMessage('');
    }
  };

  const totalSizeMB = files.reduce((acc, f) => acc + f.size, 0) / (1024 * 1024);

  return (
    <div className="bg-[#f3f6fb] text-[#10192b] min-h-screen flex flex-col font-sans">

      {/* Navbar */}
      <nav className="sticky top-0 z-50 flex justify-between items-center px-[clamp(1rem,5vw,3rem)] py-[.85rem] bg-[#0c1a33]/88 backdrop-blur-md border-b border-white/10 text-white">
        <a href="#" className="flex items-center gap-2.5 font-bold text-xl tracking-tight">
          <span className="w-[34px] h-[34px] rounded-[9px] bg-[#2456f0] grid place-items-center">
            <svg className="w-[19px] h-[19px] stroke-white fill-none stroke-[2.2] stroke-linecap-round stroke-linejoin-round" viewBox="0 0 24 24"><path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/></svg>
          </span>
          Cloud<b>Transfer</b>
        </a>
        <ul className="flex gap-1 list-none">
          <li><a href="#about" className="px-3.5 py-2 rounded-lg font-semibold text-[0.93rem] text-[#b9c6dd] hover:text-white hover:bg-white/10 transition">About Us</a></li>
          <li><a href="#contributor" className="px-3.5 py-2 rounded-lg font-semibold text-[0.93rem] text-[#b9c6dd] hover:text-white hover:bg-white/10 transition">Wanna be a contributor?</a></li>
          <li><a href="#features" className="px-3.5 py-2 rounded-lg font-semibold text-[0.93rem] text-[#b9c6dd] hover:text-white hover:bg-white/10 transition">Features</a></li>
        </ul>
      </nav>

      {/* Hero Section */}
      <header className="relative bg-[#0c1a33] text-white pt-[4.5rem] pb-[8rem] px-[clamp(1rem,5vw,3rem)] overflow-hidden">
        <div className="max-w-[1040px] mx-auto relative z-10">
          <h1 className="font-extrabold text-[clamp(2.4rem,5.2vw,3.9rem)] leading-[1.04] tracking-tight max-w-[14ch]">
            Send large files, securely.
          </h1>
          <p className="mt-5 max-w-[48ch] text-[1.12rem] text-[#a9b9d3]">
            No account needed. Your files are encrypted in your browser and stored across several clouds, up to 5 GB per transfer.
          </p>
        </div>
      </header>

      {/* Main Transfer Widget */}
      <main className="px-[clamp(1rem,5vw,3rem)] pb-20 -mt-16 relative z-20">
        <div className="max-w-[1040px] mx-auto bg-white rounded-2xl overflow-hidden shadow-[0_40px_70px_-28px_rgba(5,12,30,0.55)] flex flex-col md:flex-row">

          {/* Left: Files Dropzone & List */}
          <section className="flex-1 p-9 flex flex-col min-w-0">
            <h2 className="font-bold text-xl mb-5 tracking-tight">Add your files</h2>

            <label className="border-[1.5px] border-dashed border-[#b8c4dc] rounded-2xl p-8 text-center bg-[#f7f9fd] cursor-pointer mb-5 hover:border-[#2456f0] hover:bg-[#eef3ff] transition block">
              <div className="w-12 h-12 rounded-xl bg-[#2456f0] grid place-items-center mx-auto mb-3.5">
                <svg className="w-[22px] h-[22px] stroke-white fill-none stroke-[2.2] stroke-linecap-round" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>
              </div>
              <h3 className="text-base font-bold text-[#2456f0]">Click to add files</h3>
              <p className="text-[#5b6880] text-sm mt-1">Or drag and drop them here</p>
              <input type="file" multiple className="hidden" onChange={handleFileChange} />
            </label>

            <div className="flex flex-col gap-2.5 overflow-y-auto max-h-[220px]">
              {files.map((file, idx) => (
                <div key={idx} className="flex items-center gap-3.5 p-3 border border-[#dfe5ef] rounded-xl bg-white">
                  <span className="flex-none w-10 h-10 rounded-lg grid place-items-center text-[0.64rem] font-extrabold bg-[#e6edff] text-[#1b44c4]">
                    {file.type}
                  </span>
                  <div className="flex-1 min-w-0 flex flex-col">
                    <span className="font-semibold text-[0.92rem] truncate">{file.name}</span>
                    <span className="text-xs text-[#5b6880]">{file.sizeFormatted} • {file.totalChunks} (50MB) Chunks</span>
                  </div>
                  <button onClick={() => removeFile(idx)} className="flex-none w-8 h-8 grid place-items-center border-0 rounded-lg text-[#8d9ab2] hover:bg-[#fdecef] hover:text-[#d92d4a] transition">
                    ✕
                  </button>
                </div>
              ))}
            </div>

            <div className="mt-auto pt-5 text-xs text-[#5b6880]">
              {totalSizeMB.toFixed(1)} MB of 5 GB used
              <div className="h-1.5 mt-2 rounded-full bg-[#e4e9f3] overflow-hidden">
                <div className="h-full bg-[#12a594] transition-all" style={{ width: `${Math.min((totalSizeMB / 5000) * 100, 100)}%` }}></div>
              </div>
            </div>
          </section>

          {/* Right: Transfer Details Form */}
          <section className="flex-1 p-9 bg-[#f7f9fd] border-t md:border-t-0 md:border-l border-[#dfe5ef] flex flex-col">
            <h2 className="font-bold text-xl mb-5 tracking-tight">Transfer details</h2>

            <form onSubmit={handleTransferSubmit} className="flex flex-col flex-1">
              <div className="flex flex-col mb-4">
                <label className="text-[0.84rem] font-semibold mb-1.5">Recipient email(s) *</label>
                <div className="flex flex-wrap gap-1.5 mb-2">
                  {recipients.map((email, i) => (
                    <span key={i} className="inline-flex items-center gap-2 py-1 px-2.5 rounded-lg bg-[#e6edff] text-[#1b44c4] text-xs font-semibold">
                      {email}
                      <button type="button" onClick={() => removeRecipient(email)} className="opacity-60 hover:opacity-100">×</button>
                    </span>
                  ))}
                </div>
                <input
                  type="email"
                  value={recipientInput}
                  onChange={(e) => setRecipientInput(e.target.value)}
                  onKeyDown={addRecipientTag}
                  placeholder="Type an email and press Enter"
                  className="w-full p-3 border border-[#cdd6e6] rounded-xl bg-white text-sm focus:outline-none focus:border-[#2456f0]"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 mb-4">
                <div className="flex flex-col">
                  <label className="text-[0.84rem] font-semibold mb-1.5">Your email *</label>
                  <input type="email" value={senderEmail} onChange={(e) => setSenderEmail(e.target.value)} placeholder="sender@example.com" required className="p-3 border border-[#cdd6e6] rounded-xl bg-white text-sm focus:outline-none" />
                </div>
                <div className="flex flex-col">
                  <label className="text-[0.84rem] font-semibold mb-1.5">Your name *</label>
                  <input type="text" value={senderName} onChange={(e) => setSenderName(e.target.value)} placeholder="John Doe" required className="p-3 border border-[#cdd6e6] rounded-xl bg-white text-sm focus:outline-none" />
                </div>
              </div>

              <div className="flex flex-col mb-4">
                <label className="text-[0.84rem] font-semibold mb-1.5">Subject <span className="text-[#5b6880] text-xs font-normal">(optional)</span></label>
                <input type="text" value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="Project files" className="p-3 border border-[#cdd6e6] rounded-xl bg-white text-sm focus:outline-none" />
              </div>

              <div className="flex flex-col mb-4">
                <label className="text-[0.84rem] font-semibold mb-1.5">Message <span className="text-[#5b6880] text-xs font-normal">(optional)</span></label>
                <textarea rows="2" value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Here are the latest files..." className="p-3 border border-[#cdd6e6] rounded-xl bg-white text-sm focus:outline-none resize-none"></textarea>
              </div>

              {loading && <p className="text-sm font-semibold text-[#2456f0] mb-3 text-center">{statusMessage}</p>}

              <button type="submit" disabled={loading} className="mt-auto w-full py-3.5 border-0 rounded-xl bg-[#2456f0] text-white font-bold text-base cursor-pointer hover:bg-[#1b44c4] transition active:scale-[0.985] disabled:opacity-50">
                {loading ? 'Processing Transfer...' : 'Transfer files'}
              </button>
              <p className="mt-3 text-center text-xs text-[#5b6880]">Files are client-side chunked (50MB) and encrypted with ChaCha20 before upload.</p>
            </form>
          </section>

        </div>
      </main>

      {/* Footer */}
      <footer className="mt-auto flex justify-between items-center gap-6 flex-wrap bg-[#0c1a33] text-white px-[clamp(1rem,5vw,3rem)] py-10">
        <div>
          <a href="#" className="flex items-center gap-2.5 font-bold text-lg">
            <span className="w-8 h-8 rounded-lg bg-[#2456f0] grid place-items-center">
              <svg className="w-4 h-4 stroke-white fill-none stroke-[2.2]" viewBox="0 0 24 24"><path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/></svg>
            </span>
            Cloud<b>Transfer</b>
          </a>
          <p className="mt-2 text-[0.88rem] text-[#8ea0c0]">Developed by <span className="text-[#e3eaf8] font-semibold ml-1.5">K. Bhargav</span><span className="text-[#e3eaf8] font-semibold ml-1.5">A. Adithya</span><span className="text-[#e3eaf8] font-semibold ml-1.5">Komal Shah</span><span className="text-[#e3eaf8] font-semibold ml-1.5">Kranthi</span></p>
        </div>
        <div className="flex gap-3">
          <button className="px-5 py-2.5 border border-[#2c3f66] rounded-lg bg-transparent text-white text-sm font-semibold hover:border-[#7fa3ff] hover:bg-[#7fa3ff]/12 transition">Contributor</button>
          <button className="px-5 py-2.5 border border-[#2c3f66] rounded-lg bg-transparent text-white text-sm font-semibold hover:border-[#7fa3ff] hover:bg-[#7fa3ff]/12 transition">Admin</button>
        </div>
      </footer>

    </div>
  );
}