import React, { useState } from 'react';
import { api } from '../services/api';
import { ContributorDashboard as ContributorDashboardType, ContributorNode } from '../types/transfer';

export const ContributorDashboard: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [dashboardData, setDashboardData] = useState<ContributorDashboardType | null>(null);

  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Self-forfeiture modal state
  const [forfeitingNode, setForfeitingNode] = useState<ContributorNode | null>(null);
  const [confirmPassword, setConfirmPassword] = useState('');
  const [forfeitLoading, setForfeitLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password.trim()) return;

    setLoading(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    setDashboardData(null);

    try {
      const data = await api.loginContributorDashboard({
        email: email.trim(),
        password: password.trim(),
      });
      setDashboardData(data);
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to authenticate. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleConnectGoogle = async () => {
    try {
      const url = await api.getGoogleAuthUrl(email);
      window.location.href = url;
    } catch (err: any) {
      alert(err.message || 'Failed to generate Google OAuth URL.');
    }
  };

  const handleConnectDropbox = async () => {
    try {
      const url = await api.getDropboxAuthUrl(email);
      window.location.href = url;
    } catch (err: any) {
      alert(err.message || 'Failed to generate Dropbox OAuth URL.');
    }
  };

  const handleForfeitSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!forfeitingNode || !confirmPassword.trim()) return;

    setForfeitLoading(true);
    setErrorMsg(null);

    try {
      await api.forfeitContributorNode(forfeitingNode.id, confirmPassword.trim());
      setSuccessMsg(`Node (${forfeitingNode.cloudProvider}) successfully forfeited and pledged storage released.`);
      setForfeitingNode(null);
      setConfirmPassword('');

      const updatedData = await api.loginContributorDashboard({ email, password });
      setDashboardData(updatedData);
    } catch (err: any) {
      alert(err.message || 'Failed to forfeit node.');
    } finally {
      setForfeitLoading(false);
    }
  };

  const totalPledgedGb = dashboardData ? (dashboardData.pledgedStorageBytes / (1024 * 1024 * 1024)).toFixed(1) : '0';
  const totalUsedGb = dashboardData ? (dashboardData.usedStorageBytes / (1024 * 1024 * 1024)).toFixed(2) : '0';
  const usagePercentage = dashboardData && dashboardData.pledgedStorageBytes > 0
    ? Math.min(100, Math.round((dashboardData.usedStorageBytes / dashboardData.pledgedStorageBytes) * 100))
    : 0;

  const connectedNodes: ContributorNode[] = dashboardData?.nodes || [];
  const googleDriveNode = dashboardData?.googleDriveNode || connectedNodes.find(n => n.cloudProvider === 'GOOGLE_DRIVE');
  const dropboxNode = dashboardData?.dropboxNode || connectedNodes.find(n => n.cloudProvider === 'DROPBOX');

  return (
    <div className="max-w-4xl mx-auto py-12 px-6 space-y-8">
      <div className="text-center">
        <h1 className="text-3xl font-extrabold text-white tracking-tight">CONTRIBUTOR DASHBOARD</h1>
        <p className="text-slate-400 mt-2 text-sm">Monitor your pledged storage nodes and manage vault contributions.</p>
      </div>

      {/* LOGIN FORM */}
      {!dashboardData && (
        <div className="max-w-md mx-auto bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
          <h2 className="text-sm font-bold text-cyan-400 uppercase tracking-wider text-center">AUTHENTICATE CONTRIBUTOR DASHBOARD</h2>

          {errorMsg && (
            <div className="p-3 bg-rose-950/50 border border-rose-800 rounded-xl text-xs text-rose-300 text-center">
              ⚠️ {errorMsg}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1">EMAIL ADDRESS</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Enter contributor email..."
                className="w-full p-3 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white outline-none focus:border-cyan-500 transition"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1">PASSWORD</label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter account password..."
                className="w-full p-3 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white outline-none focus:border-cyan-500 transition"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-slate-950 font-bold text-xs uppercase rounded-xl shadow-lg transition disabled:opacity-50"
            >
              {loading ? 'AUTHENTICATING...' : 'ACCESS DASHBOARD'}
            </button>
          </form>
        </div>
      )}

      {/* DASHBOARD CONTENT */}
      {dashboardData && (
        <div className="space-y-6">
          <div className="flex justify-between items-center bg-slate-900 border border-slate-800 p-4 rounded-xl">
            <div>
              <h2 className="text-lg font-bold text-white">{dashboardData.name || dashboardData.email}</h2>
              <p className="text-xs text-slate-400">{dashboardData.email}</p>
            </div>
            <button
              onClick={() => setDashboardData(null)}
              className="px-3 py-1 bg-slate-800 hover:bg-slate-700 text-xs text-rose-400 rounded-lg transition"
            >
              🔒 Logout
            </button>
          </div>

          {successMsg && (
            <div className="p-4 bg-emerald-950/40 border border-emerald-800 rounded-xl text-xs text-emerald-300">
              {successMsg}
            </div>
          )}

          {/* Top Summary Stats */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
              <h3 className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2">
                TOTAL POOLED CONTRIBUTION
              </h3>
              <div className="text-2xl font-extrabold text-white mt-1">
                {totalUsedGb} GB Used of {totalPledgedGb} GB
              </div>
              <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden mt-4">
                <div className="bg-cyan-500 h-full transition-all duration-300" style={{ width: `${usagePercentage}%` }} />
              </div>
              <div className="text-[11px] text-slate-400 mt-2">{usagePercentage}% of your total shared space is in use across nodes.</div>
            </div>

            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
              <h3 className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2">
                NETWORK ACTIVITY & STATUS
              </h3>
              <div className="space-y-2 text-xs text-slate-300 mt-3">
                <div className="flex items-center gap-2">
                  <span className="text-emerald-400">●</span> Status: <span className="text-white font-semibold">{dashboardData.status || 'ACTIVE'}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-cyan-400">●</span> Active Nodes: <span className="text-white font-semibold">{connectedNodes.length} Storage Vault(s)</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-indigo-400">●</span> Multi-Cloud Sync: <span className="text-emerald-400">Operational</span>
                </div>
              </div>
            </div>
          </div>

          {/* SIDE-BY-SIDE INTEGRATION CARDS */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* GOOGLE DRIVE PANEL */}
            <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl space-y-4 flex flex-col justify-between">
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-bold text-white flex items-center gap-2">
                    🌐 GOOGLE DRIVE NODE
                  </span>
                  <span
                    className={`px-2.5 py-0.5 text-[10px] font-mono font-bold rounded ${
                      googleDriveNode?.refreshToken
                        ? 'bg-emerald-500/20 text-emerald-400'
                        : 'bg-slate-800 text-slate-400'
                    }`}
                  >
                    {googleDriveNode?.refreshToken ? (googleDriveNode.status || 'ACTIVE') : 'UNAUTHENTICATED'}
                  </span>
                </div>

                {googleDriveNode ? (
                  <div className="space-y-2 text-xs text-slate-300">
                    <div className="flex justify-between">
                      <span>Pledged Capacity:</span>
                      <span className="font-mono text-white">
                        {(googleDriveNode.pledgedStorageBytes / (1024 * 1024 * 1024)).toFixed(1)} GB
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span>Consumed Space:</span>
                      <span className="font-mono text-cyan-400">
                        {(googleDriveNode.usedStorageBytes / (1024 * 1024)).toFixed(2)} MB
                      </span>
                    </div>
                  </div>
                ) : (
                  <p className="text-xs text-slate-400">
                    Connect Google Drive to start sharing volunteer storage and earn contributor metrics.
                  </p>
                )}
              </div>

              <button
                onClick={handleConnectGoogle}
                className={`w-full py-2.5 font-bold text-xs uppercase rounded-xl transition ${
                  googleDriveNode?.refreshToken
                    ? 'bg-slate-800 hover:bg-slate-700 text-cyan-400 border border-cyan-500/30'
                    : 'bg-cyan-500 hover:bg-cyan-400 text-slate-950 shadow-lg shadow-cyan-500/20'
                }`}
              >
                {googleDriveNode?.refreshToken ? '🔄 RE-AUTHENTICATE GOOGLE DRIVE' : '🔗 CONNECT GOOGLE DRIVE'}
              </button>
            </div>

            {/* DROPBOX PANEL */}
            <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl space-y-4 flex flex-col justify-between">
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-bold text-white flex items-center gap-2">
                    📦 DROPBOX NODE
                  </span>
                  <span
                    className={`px-2.5 py-0.5 text-[10px] font-mono font-bold rounded ${
                      dropboxNode?.refreshToken
                        ? 'bg-emerald-500/20 text-emerald-400'
                        : 'bg-slate-800 text-slate-400'
                    }`}
                  >
                    {dropboxNode?.refreshToken ? (dropboxNode.status || 'ACTIVE') : 'UNAUTHENTICATED'}
                  </span>
                </div>

                {dropboxNode ? (
                  <div className="space-y-2 text-xs text-slate-300">
                    <div className="flex justify-between">
                      <span>Pledged Capacity:</span>
                      <span className="font-mono text-white">
                        {(dropboxNode.pledgedStorageBytes / (1024 * 1024 * 1024)).toFixed(1)} GB
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span>Consumed Space:</span>
                      <span className="font-mono text-cyan-400">
                        {(dropboxNode.usedStorageBytes / (1024 * 1024)).toFixed(2)} MB
                      </span>
                    </div>
                  </div>
                ) : (
                  <p className="text-xs text-slate-400">
                    Connect Dropbox to add multi-cloud redundancy to your volunteer profile.
                  </p>
                )}
              </div>

              <button
                onClick={handleConnectDropbox}
                className={`w-full py-2.5 font-bold text-xs uppercase rounded-xl transition ${
                  dropboxNode?.refreshToken
                    ? 'bg-slate-800 hover:bg-slate-700 text-cyan-400 border border-cyan-500/30'
                    : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-900/40'
                }`}
              >
                {dropboxNode?.refreshToken ? '🔄 RE-AUTHENTICATE DROPBOX' : '🔗 CONNECT DROPBOX'}
              </button>
            </div>
          </div>

          {/* TABLE OF NODES & FORFEIT OPTION */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <h3 className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-4">
              CONNECTED STORAGE NODES ({connectedNodes.length})
            </h3>
            {connectedNodes.length === 0 ? (
              <div className="text-center py-6 text-xs text-slate-500">No active storage nodes attached to this account.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-950 text-slate-400 uppercase">
                    <tr>
                      <th className="p-3">PROVIDER</th>
                      <th className="p-3">PLEDGED SPACE</th>
                      <th className="p-3">USED SPACE</th>
                      <th className="p-3">STATUS</th>
                      <th className="p-3">ACTIONS</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {connectedNodes.map((node) => (
                      <tr key={node.id}>
                        <td className="p-3 font-semibold text-white">
                          {node.cloudProvider === 'GOOGLE_DRIVE' ? '🌐 Google Drive' : '📦 Dropbox'}
                        </td>
                        <td className="p-3">{(node.pledgedStorageBytes / (1024 * 1024 * 1024)).toFixed(0)} GB</td>
                        <td className="p-3">{(node.usedStorageBytes / (1024 * 1024 * 1024)).toFixed(2)} GB</td>
                        <td className="p-3 text-emerald-400">● {node.status}</td>
                        <td className="p-3">
                          <button
                            onClick={() => setForfeitingNode(node)}
                            className="px-3 py-1 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 rounded text-xs transition font-semibold"
                          >
                            Forfeit Node
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* SELF-FORFEIT CONFIRMATION MODAL */}
      {forfeitingNode && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-rose-800 rounded-2xl p-6 max-w-md w-full space-y-4">
            <div className="flex justify-between items-center">
              <h3 className="text-base font-bold text-rose-400">FORFEIT STORAGE NODE</h3>
              <button onClick={() => setForfeitingNode(null)} className="text-slate-400 hover:text-white">✕</button>
            </div>
            <p className="text-xs text-slate-300">
              Are you sure you want to decommission your <span className="text-white font-bold">{forfeitingNode.cloudProvider}</span> node? Pledged storage will be unlinked from the network pool.
            </p>

            <form onSubmit={handleForfeitSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">ENTER DASHBOARD PASSWORD TO CONFIRM</label>
                <input
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Enter password..."
                  className="w-full p-2.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white outline-none focus:border-rose-500"
                />
              </div>

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setForfeitingNode(null)}
                  className="flex-1 py-2 bg-slate-800 text-slate-300 text-xs rounded-xl font-semibold hover:bg-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={forfeitLoading}
                  className="flex-1 py-2 bg-rose-600 hover:bg-rose-500 text-white text-xs rounded-xl font-bold uppercase transition disabled:opacity-50"
                >
                  {forfeitLoading ? 'FORFEITING...' : 'CONFIRM FORFEIT'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};