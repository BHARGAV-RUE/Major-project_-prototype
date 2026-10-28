import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../services/api';

export const ContributorRegister: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [cloudProvider, setCloudProvider] = useState<'GOOGLE_DRIVE' | 'DROPBOX'>('GOOGLE_DRIVE');
  const [pledgedGb, setPledgedGb] = useState<number>(10);

  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  useEffect(() => {
    const status = searchParams.get('status');
    const provider = searchParams.get('provider');
    const message = searchParams.get('message');

    if (status === 'success') {
      setSuccessMsg(`🎉 Node connected via ${provider}! Your status is initially INACTIVE and will be approved by an Admin or auto-activated in 4 days.`);
    } else if (status === 'error') {
      setErrorMsg(`⚠️ OAuth authorization error: ${message || 'Failed to authorize storage provider.'}`);
    }
  }, [searchParams]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password.trim()) return;

    if (password.length < 6) {
      setErrorMsg('Password must be at least 6 characters.');
      return;
    }

    if (!pledgedGb || pledgedGb < 4) {
      setErrorMsg('Pledged storage capacity must be at least 4 GB.');
      return;
    }

    setLoading(true);
    setErrorMsg(null);
    setSuccessMsg(null);

    try {
      const pledgedBytes = pledgedGb * 1024 * 1024 * 1024;

      // 1. Register node credentials in database (Status set to INACTIVE)
      await api.registerContributor({
        name: name.trim() || email.trim(),
        email: email.trim(),
        password: password.trim(),
        cloudProvider,
        pledgedStorageBytes: pledgedBytes,
      });

      // 2. Redirect contributor for OAuth consent
      let authUrl = '';
      if (cloudProvider === 'GOOGLE_DRIVE') {
        authUrl = await api.getGoogleAuthUrl(email.trim());
      } else {
        authUrl = await api.getDropboxAuthUrl(email.trim());
      }

      window.location.href = authUrl;
    } catch (err: any) {
      setErrorMsg(err.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto py-12 px-6">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-2xl space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex p-3 bg-cyan-500/10 text-cyan-400 rounded-xl mb-2">
            🌐
          </div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">BECOME A STORAGE CONTRIBUTOR</h1>
          <p className="text-slate-400 text-xs">Pledge unused Google Drive or Dropbox space. Newly registered nodes remain INACTIVE until accepted by Admin or auto-activated after 4 days.</p>
        </div>

        {errorMsg && (
          <div className="p-4 bg-rose-950/40 border border-rose-800 rounded-xl text-xs text-rose-300 text-center">
            {errorMsg}
          </div>
        )}

        {successMsg && (
          <div className="p-4 bg-emerald-950/40 border border-emerald-800 rounded-xl text-xs text-emerald-300 text-center">
            {successMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-400 mb-1">FULL NAME / ALIAS</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Bhargav Kallepally"
              className="w-full px-4 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white text-xs outline-none focus:border-cyan-500 transition"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 mb-1">EMAIL ADDRESS</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="contributor@example.com"
              className="w-full px-4 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white text-xs outline-none focus:border-cyan-500 transition"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 mb-1">DASHBOARD PASSWORD</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Set account password (min. 6 characters)"
              className="w-full px-4 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white text-xs outline-none focus:border-cyan-500 transition"
              required
            />
            <p className="text-[10px] text-slate-500 mt-1">This password protects your contributor dashboard.</p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 mb-1">SELECT CLOUD PROVIDER</label>
            <div className="grid grid-cols-2 gap-4">
              <button
                type="button"
                onClick={() => setCloudProvider('GOOGLE_DRIVE')}
                className={`p-3 border rounded-xl flex items-center justify-center space-x-2 text-xs font-semibold transition ${
                  cloudProvider === 'GOOGLE_DRIVE'
                    ? 'bg-cyan-500/10 border-cyan-500 text-cyan-400'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                }`}
              >
                <span>🌐 Google Drive</span>
              </button>

              <button
                type="button"
                onClick={() => setCloudProvider('DROPBOX')}
                className={`p-3 border rounded-xl flex items-center justify-center space-x-2 text-xs font-semibold transition ${
                  cloudProvider === 'DROPBOX'
                    ? 'bg-cyan-500/10 border-cyan-500 text-cyan-400'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                }`}
              >
                <span>📦 Dropbox</span>
              </button>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 mb-1">PLEDGED STORAGE CAPACITY (GB)</label>
            <input
              type="number"
              min={4}
              value={pledgedGb}
              onChange={(e) => setPledgedGb(Number(e.target.value))}
              placeholder="e.g. 10"
              className="w-full px-4 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white text-xs outline-none focus:border-cyan-500 transition"
              required
            />
            <p className="text-[10px] text-slate-500 mt-1">Minimum pledged storage capacity is 4 GB.</p>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-slate-950 font-bold text-xs uppercase tracking-wider rounded-xl transition shadow-lg shadow-cyan-500/20 disabled:opacity-50"
          >
            {loading ? 'REGISTERING NODE...' : 'AUTHORIZE STORAGE & CONNECT'}
          </button>
        </form>
      </div>
    </div>
  );
};