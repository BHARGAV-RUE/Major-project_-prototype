import React, { useEffect, useState } from 'react';
import { api } from '../services/api';
import {
  AdminTelemetry,
  AdminTransferSession,
  ContributorDashboard,
  TamperAlert,
} from '../types/transfer';

type TabType = 'overview' | 'transfers' | 'contributors' | 'alerts' | 'blockchain';

export const AdminControlTower: React.FC = () => {
  const [adminSecret, setAdminSecret] = useState<string>(
    () => sessionStorage.getItem('ct_admin_secret') || ''
  );
  const [inputPassword, setInputPassword] = useState('');
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(
    () => !!sessionStorage.getItem('ct_admin_secret')
  );

  const [activeTab, setActiveTab] = useState<TabType>('alerts');
  const [telemetry, setTelemetry] = useState<AdminTelemetry | null>(null);
  const [contributors, setContributors] = useState<ContributorDashboard[]>([]);
  const [transfers, setTransfers] = useState<AdminTransferSession[]>([]);
  const [alerts, setAlerts] = useState<TamperAlert[]>([]);
  const [selectedSession, setSelectedSession] = useState<AdminTransferSession | null>(null);
  const [targetSenderEmail, setTargetSenderEmail] = useState('');

  const [loading, setLoading] = useState(false);
  const [authenticating, setAuthenticating] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (isAuthenticated && adminSecret) {
      loadAllData(adminSecret);
    }
  }, [isAuthenticated, adminSecret]);

  const handleAdminLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputPassword.trim()) return;

    setAuthenticating(true);
    setErrorMsg(null);
    try {
      const result = await api.loginAdmin(inputPassword);
      if (result && result.adminToken) {
        sessionStorage.setItem('ct_admin_secret', result.adminToken);
        setAdminSecret(result.adminToken);
        setIsAuthenticated(true);
        loadAllData(result.adminToken);
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Invalid Master Admin Password.');
    } finally {
      setAuthenticating(false);
    }
  };

  const loadAllData = async (secretKey = adminSecret) => {
    if (!secretKey) return;
    setLoading(true);
    setErrorMsg(null);
    try {
      const [telemetryData, contributorsData, transfersData, alertsData] = await Promise.all([
        api.getAdminTelemetry(secretKey),
        api.getAdminContributors(secretKey),
        api.getAdminTransfers(secretKey),
        api.getAdminTamperAlerts(secretKey),
      ]);
      setTelemetry(telemetryData);
      setContributors(contributorsData);
      setTransfers(transfersData);
      setAlerts(alertsData);
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to load telemetry or unauthorized.');
      if (err.message?.includes('Unauthorized') || err.message?.includes('invalid')) {
        handleLogout();
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    sessionStorage.removeItem('ct_admin_secret');
    setAdminSecret('');
    setIsAuthenticated(false);
    setTelemetry(null);
  };

  const handleApproveContributor = async (id: string) => {
    try {
      await api.approveContributor(id, adminSecret);
      loadAllData(adminSecret);
      alert('Contributor node approved and activated!');
    } catch (err: any) {
      alert(err.message || 'Failed to approve contributor');
    }
  };

  const handleWarnTamper = async (id: string) => {
    try {
      const res = await api.warnContributorTamper(id, adminSecret);
      loadAllData(adminSecret);
      alert(`Tamper action recorded: ${res.message}`);
    } catch (err: any) {
      alert(err.message || 'Failed to warn contributor');
    }
  };

  const handleTerminateNode = async (id: string) => {
    if (!window.confirm('Are you sure you want to terminate and ban this contributor node?')) return;
    try {
      await api.deleteContributor(id, adminSecret);
      loadAllData(adminSecret);
      alert('Contributor node terminated successfully.');
    } catch (err: any) {
      alert(err.message || 'Failed to terminate contributor node');
    }
  };

  const handleDeleteSession = async (sessionId: string, filename: string) => {
    if (!window.confirm(`⚠️ Are you sure you want to purge all original and replica chunks for "${filename}"?`)) return;

    try {
      const res = await api.deleteAdminTransferSession(sessionId, adminSecret);
      alert(`✅ ${res.message}\nReclaimed: ${(res.reclaimedBytes / (1024 * 1024)).toFixed(2)} MB`);
      if (selectedSession?.id === sessionId) setSelectedSession(null);
      loadAllData(adminSecret);
    } catch (err: any) {
      alert(err.message || 'Failed to delete transfer session.');
    }
  };

  const handlePurgeSender = async () => {
    if (!targetSenderEmail.trim()) {
      alert('Please enter a sender email.');
      return;
    }
    if (!window.confirm(`⚠️ FORCE PURGE all file transfers sent by ${targetSenderEmail}?`)) return;

    try {
      const res = await api.purgeSenderData(targetSenderEmail.trim(), adminSecret);
      alert(`✅ ${res.message}\nSessions Purged: ${res.purgedSessions}\nChunks Deleted: ${res.deletedChunks}\nReclaimed: ${(res.reclaimedBytes / (1024 * 1024)).toFixed(2)} MB`);
      setTargetSenderEmail('');
      loadAllData(adminSecret);
    } catch (err: any) {
      alert(err.message || 'Failed to purge sender data.');
    }
  };

  const handleEmergencyKillSwitch = async () => {
    const confirmation = window.prompt('🚨 EMERGENCY KILL SWITCH!\nType "PURGE ALL" to confirm nuclear deletion of all chunks across all connected drive spaces:');
    if (confirmation !== 'PURGE ALL') {
      alert('Emergency purge cancelled.');
      return;
    }

    try {
      const res = await api.triggerEmergencyPurge(adminSecret);
      alert(`🚨 NUCLEAR PURGE COMPLETE!\nAffected Sessions: ${res.purgedSessionsCount}\nReclaimed: ${(res.totalReclaimedBytes / (1024 * 1024)).toFixed(2)} MB`);
      loadAllData(adminSecret);
    } catch (err: any) {
      alert(err.message || 'Failed to execute emergency purge.');
    }
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="max-w-md mx-auto my-24 p-8 bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex p-3 bg-cyan-500/10 text-cyan-400 rounded-xl mb-2">🔒</div>
          <h2 className="text-2xl font-extrabold text-white">ADMIN CONTROL TOWER</h2>
          <p className="text-xs text-slate-400">Enter master password for real-time telemetry access</p>
        </div>

        {errorMsg && (
          <div className="p-3 bg-rose-950/40 border border-rose-800 rounded-xl text-xs text-rose-300 text-center">
            ⚠️ {errorMsg}
          </div>
        )}

        <form onSubmit={handleAdminLogin} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-400 mb-1">ADMIN SECRET KEY</label>
            <input
              type="password"
              value={inputPassword}
              onChange={(e) => setInputPassword(e.target.value)}
              placeholder="Enter master admin password..."
              className="w-full px-4 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white text-sm focus:outline-none focus:border-cyan-500 transition"
              required
            />
          </div>
          <button
            type="submit"
            disabled={authenticating}
            className="w-full py-3 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold text-xs uppercase tracking-wider rounded-xl transition shadow-lg shadow-cyan-500/20 disabled:opacity-50"
          >
            {authenticating ? 'AUTHENTICATING...' : 'UNLOCK CONTROL TOWER'}
          </button>
        </form>
      </div>
    );
  }

  const capacityGb = telemetry ? (telemetry.totalCapacityBytes / (1024 * 1024 * 1024)).toFixed(1) : '0';
  const consumedGb = telemetry ? (telemetry.consumedStorageBytes / (1024 * 1024 * 1024)).toFixed(2) : '0';

  const pendingRegistrations = alerts.filter(a => a.tamperType === 'NEW_REGISTRATION_REQUEST');
  const tamperSecurityAlerts = alerts.filter(a => a.tamperType !== 'NEW_REGISTRATION_REQUEST');

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex">
      {/* SIDEBAR NAVIGATION */}
      <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between p-4 flex-shrink-0">
        <div className="space-y-6">
          <div className="flex items-center space-x-3 px-2">
            <div className="p-2 bg-cyan-500/20 text-cyan-400 rounded-lg text-lg font-bold">CT</div>
            <div>
              <h2 className="text-sm font-extrabold text-white tracking-wider">CONTROL TOWER</h2>
              <span className="text-[10px] text-emerald-400 font-mono">● LIVE AUDIT ON</span>
            </div>
          </div>

          <nav className="space-y-1">
            <button
              onClick={() => setActiveTab('overview')}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
                activeTab === 'overview'
                  ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/30'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <span>📊 System Telemetry</span>
            </button>

            <button
              onClick={() => setActiveTab('transfers')}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
                activeTab === 'transfers'
                  ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/30'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <span>📦 Transfer Sessions</span>
              <span className="px-1.5 py-0.5 bg-slate-800 text-slate-300 rounded text-[10px]">
                {transfers.length}
              </span>
            </button>

            <button
              onClick={() => setActiveTab('contributors')}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
                activeTab === 'contributors'
                  ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/30'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <span>👥 Storage Contributors</span>
              <span className="px-1.5 py-0.5 bg-slate-800 text-slate-300 rounded text-[10px]">
                {contributors.length}
              </span>
            </button>

            <button
              onClick={() => setActiveTab('alerts')}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
                activeTab === 'alerts'
                  ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <span>🔔 Admin Notifications</span>
              {alerts.length > 0 && (
                <span className="px-1.5 py-0.5 bg-amber-500 text-slate-950 font-bold rounded-full text-[10px] animate-pulse">
                  {alerts.length}
                </span>
              )}
            </button>

            <button
              onClick={() => setActiveTab('blockchain')}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition ${
                activeTab === 'blockchain'
                  ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/30'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <span>⛓️ Polygon Blockchain</span>
            </button>
          </nav>
        </div>

        <div className="space-y-2 pt-4 border-t border-slate-800">
          <button
            onClick={() => loadAllData(adminSecret)}
            className="w-full py-2 bg-slate-800 hover:bg-slate-700 text-cyan-400 text-xs font-mono rounded-lg transition"
          >
            🔄 REFRESH DATA
          </button>
          <button
            onClick={handleLogout}
            className="w-full py-2 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-mono rounded-lg border border-rose-500/30 transition"
          >
            🔒 LOCK TOWER
          </button>
        </div>
      </aside>

      {/* MAIN VIEW */}
      <main className="flex-1 p-8 overflow-y-auto space-y-6">
        {errorMsg && (
          <div className="p-4 bg-rose-950/40 border border-rose-800 rounded-xl text-xs text-rose-300">
            ⚠️ {errorMsg}
          </div>
        )}

        {/* --- 📦 TAB 2: TRANSFER SESSIONS (MOST RECENT FIRST) --- */}
        {activeTab === 'transfers' && (
          <div className="space-y-6">
            <div className="flex justify-between items-center">
              <h1 className="text-2xl font-extrabold text-white">TRANSFER SESSIONS (MOST RECENT FIRST)</h1>
            </div>

            {/* EMERGENCY PURGE & SENDER CLEANUP CONTROL */}
            <div className="bg-slate-900 border border-rose-900/40 p-5 rounded-2xl space-y-4">
              <h3 className="text-xs font-bold text-rose-400 uppercase tracking-wider">
                🚨 EMERGENCY PURGE & SENDER CLEANUP CONTROLS
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex gap-2">
                  <input
                    type="email"
                    value={targetSenderEmail}
                    onChange={(e) => setTargetSenderEmail(e.target.value)}
                    placeholder="sender@example.com"
                    className="flex-1 px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white outline-none focus:border-rose-500"
                  />
                  <button
                    onClick={handlePurgeSender}
                    className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs uppercase rounded-xl transition"
                  >
                    🗑️ PURGE SENDER
                  </button>
                </div>

                <button
                  onClick={handleEmergencyKillSwitch}
                  className="py-2 px-4 bg-gradient-to-r from-rose-600 to-red-700 hover:from-rose-500 hover:to-red-600 text-white font-black text-xs uppercase tracking-wider rounded-xl transition shadow-lg shadow-rose-950/50"
                >
                  💣 TRIGGER SYSTEM EMERGENCY PURGE
                </button>
              </div>
            </div>

            {/* SESSIONS TABLE */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950 text-slate-400 uppercase">
                  <tr>
                    <th className="p-3">SENDER</th>
                    <th className="p-3">FILE / SIZE</th>
                    <th className="p-3">CREATED AT</th>
                    <th className="p-3">EXPIRY TIMESTAMP</th>
                    <th className="p-3">STATUS</th>
                    <th className="p-3 text-right">ACTIONS</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {transfers.map((t) => (
                    <tr key={t.id} className="hover:bg-slate-800/40 transition">
                      <td className="p-3 font-bold text-white">
                        <div>{t.senderName || 'Anonymous'}</div>
                        <div className="text-slate-500 text-[10px]">{t.senderEmail}</div>
                      </td>
                      <td className="p-3">
                        <div className="font-semibold text-white">{t.originalFilename}</div>
                        <div className="text-slate-500 text-[10px]">
                          {(t.totalSizeBytes / (1024 * 1024)).toFixed(2)} MB ({t.totalChunks} Chunks)
                        </div>
                      </td>
                      <td className="p-3 font-mono text-cyan-300 text-[11px]">
                        {formatDate(t.createdAt)}
                      </td>
                      <td className="p-3 font-mono text-amber-300 text-[11px]">
                        {formatDate(t.expiryTimestamp)}
                      </td>
                      <td className="p-3">
                        <span
                          className={`px-2 py-0.5 rounded font-mono text-[10px] font-bold ${
                            t.status === 'ADMIN_DELETED' || t.status === 'PURGED_BY_ADMIN' || t.status === 'SYSTEM_EMERGENCY_PURGED'
                              ? 'bg-rose-500/20 text-rose-400'
                              : 'bg-emerald-500/20 text-emerald-400'
                          }`}
                        >
                          {t.status || 'ACTIVE'}
                        </span>
                      </td>
                      <td className="p-3 text-right space-x-2">
                        <button
                          onClick={() => setSelectedSession(t)}
                          className="px-2.5 py-1 bg-cyan-500/10 hover:bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 rounded text-[11px] font-semibold transition"
                        >
                          🔍 Matrix
                        </button>
                        {t.status !== 'ADMIN_DELETED' && t.status !== 'PURGED_BY_ADMIN' && t.status !== 'SYSTEM_EMERGENCY_PURGED' && (
                          <button
                            onClick={() => handleDeleteSession(t.id, t.originalFilename)}
                            className="px-2.5 py-1 bg-rose-600/20 hover:bg-rose-600 text-rose-300 hover:text-white border border-rose-500/40 rounded text-[11px] font-bold transition"
                          >
                            🗑️ DELETE CHUNKS
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {selectedSession && (
              <div className="p-6 bg-slate-900 border border-cyan-500/30 rounded-2xl space-y-4">
                <div className="flex justify-between items-center border-b border-slate-800 pb-3">
                  <h3 className="text-sm font-bold text-cyan-400">
                    CHUNK MATRIX: {selectedSession.originalFilename}
                  </h3>
                  <button onClick={() => setSelectedSession(null)} className="text-xs text-slate-400 hover:text-white">
                    ✕ CLOSE
                  </button>
                </div>
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-950 text-slate-400 uppercase">
                    <tr>
                      <th className="p-2">CHUNK #</th>
                      <th className="p-2">PRIMARY NODE</th>
                      <th className="p-2">REPLICA NODE</th>
                      <th className="p-2">STATUS</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {selectedSession.chunks.map((c) => (
                      <tr key={c.id}>
                        <td className="p-2 font-mono text-cyan-400">#{c.chunkIndex}</td>
                        <td className="p-2">{c.primaryContributorEmail} ({c.primaryProvider})</td>
                        <td className="p-2">{c.replicaContributorEmail} ({c.replicaProvider})</td>
                        <td className="p-2 text-emerald-400">● {c.status}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* --- 🔔 TAB 4: NOTIFICATIONS & ALERTS --- */}
        {activeTab === 'alerts' && (
          <div className="space-y-8">
            <h1 className="text-2xl font-extrabold text-white">ADMIN NOTIFICATIONS & ALERTS</h1>

            <div className="space-y-4">
              <h2 className="text-sm font-bold text-cyan-400 uppercase tracking-wider flex items-center gap-2">
                <span>🆕 NEW CONTRIBUTOR REGISTRATION REQUESTS</span>
                <span className="px-2 py-0.5 bg-cyan-500/20 text-cyan-300 text-xs rounded-full">
                  {pendingRegistrations.length}
                </span>
              </h2>

              {pendingRegistrations.length === 0 ? (
                <div className="p-4 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-400">
                  🟢 No pending contributor registration requests awaiting acceptance.
                </div>
              ) : (
                <div className="space-y-3">
                  {pendingRegistrations.map((req) => (
                    <div
                      key={req.alertId}
                      className="p-4 bg-slate-900 border border-cyan-500/30 rounded-xl flex items-center justify-between"
                    >
                      <div>
                        <div className="text-sm font-bold text-white">
                          Contributor Email: <span className="text-cyan-400">{req.contributorEmail}</span>
                        </div>
                        <div className="text-xs text-slate-400 mt-1">
                          Cloud Provider: <span className="text-white font-semibold">{req.cloudProvider}</span>
                        </div>
                      </div>
                      <button
                        onClick={() => handleApproveContributor(req.contributorId)}
                        className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs uppercase rounded-xl transition shadow-lg shadow-emerald-900/40"
                      >
                        ✅ ACCEPT & ACTIVATE NODE
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="space-y-4">
              <h2 className="text-sm font-bold text-rose-400 uppercase tracking-wider flex items-center gap-2">
                <span>🚨 DATA TAMPERING VIOLATIONS & WARNING FLAGS</span>
                <span className="px-2 py-0.5 bg-rose-500/20 text-rose-300 text-xs rounded-full">
                  {tamperSecurityAlerts.length}
                </span>
              </h2>

              {tamperSecurityAlerts.length === 0 ? (
                <div className="p-4 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-400">
                  🟢 No active tamper alerts or chunk deletion violations reported.
                </div>
              ) : (
                <div className="space-y-3">
                  {tamperSecurityAlerts.map((alertItem) => (
                    <div
                      key={alertItem.alertId}
                      className="p-4 bg-rose-950/30 border border-rose-800/80 rounded-xl flex items-center justify-between gap-4"
                    >
                      <div className="space-y-1">
                        <div className="text-sm font-bold text-white">
                          Offending Node: <span className="text-rose-300">{alertItem.contributorEmail}</span> ({alertItem.cloudProvider})
                        </div>
                        <div className="text-xs font-mono text-amber-400">
                          Warning Flags Issued: <span className="text-white font-bold">{alertItem.tamperFlags || 0} / 3</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => handleWarnTamper(alertItem.contributorId)}
                          className="px-3 py-2 bg-amber-600 hover:bg-amber-500 text-slate-950 font-extrabold text-xs uppercase rounded-xl transition shadow-lg shadow-amber-900/40"
                        >
                          ⚠️ ISSUE WARNING FLAG
                        </button>
                        <button
                          onClick={() => handleTerminateNode(alertItem.contributorId)}
                          className="px-3 py-2 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs uppercase rounded-xl transition shadow-lg shadow-rose-900/40"
                        >
                          🚫 BAN & PURGE NODE
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* --- 📊 TAB 1: SYSTEM OVERVIEW --- */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            <h1 className="text-2xl font-extrabold text-white">SYSTEM TELEMETRY & NETWORK HEALTH</h1>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl">
                <div className="text-xs font-semibold text-slate-400 uppercase">ACTIVE STORAGE NODES</div>
                <div className="text-2xl font-bold text-white mt-1">{contributors.length}</div>
              </div>
              <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl">
                <div className="text-xs font-semibold text-slate-400 uppercase">POOLED CAPACITY</div>
                <div className="text-2xl font-bold text-white mt-1">{capacityGb} GB</div>
              </div>
              <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl">
                <div className="text-xs font-semibold text-slate-400 uppercase">CONSUMED STORAGE</div>
                <div className="text-2xl font-bold text-white mt-1">{consumedGb} GB</div>
              </div>
              <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl">
                <div className="text-xs font-semibold text-slate-400 uppercase">NOTIFICATIONS</div>
                <div className="text-2xl font-bold text-amber-400 mt-1">{alerts.length} Pending</div>
              </div>
            </div>
          </div>
        )}

        {/* --- 👥 TAB 3: CONTRIBUTORS --- */}
        {activeTab === 'contributors' && (
          <div className="space-y-6">
            <h1 className="text-2xl font-extrabold text-white">NETWORK CONTRIBUTORS</h1>
            <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950 text-slate-400 uppercase">
                  <tr>
                    <th className="p-3">NAME / EMAIL</th>
                    <th className="p-3">PROVIDER</th>
                    <th className="p-3">WARNING FLAGS</th>
                    <th className="p-3">STATUS</th>
                    <th className="p-3">ACTIONS</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {contributors.map((c) => (
                    <tr key={c.id}>
                      <td className="p-3 font-medium text-white">{c.email}</td>
                      <td className="p-3">{c.cloudProvider}</td>
                      <td className="p-3 font-mono font-bold text-amber-400">{c.tamperFlags || 0} / 3</td>
                      <td className="p-3 font-semibold text-emerald-400">{c.status}</td>
                      <td className="p-3 space-x-2">
                        <button onClick={() => handleWarnTamper(c.id as string)} className="text-amber-400 hover:underline font-semibold">
                          WARN FLAG
                        </button>
                        <button onClick={() => handleTerminateNode(c.id as string)} className="text-rose-400 hover:underline font-semibold">
                          TERMINATE
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* --- ⛓️ TAB 5: BLOCKCHAIN AUDIT --- */}
        {activeTab === 'blockchain' && (
          <div className="space-y-6">
            <h1 className="text-2xl font-extrabold text-white">POLYGON BLOCKCHAIN AUDIT</h1>
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4">
              {transfers.map((t) => (
                <div key={t.id} className="p-4 bg-slate-950 border border-slate-800 rounded-xl space-y-1">
                  <div className="flex justify-between items-center text-xs font-bold text-cyan-400">
                    <span>Session: {t.id}</span>
                    <span className="text-emerald-400">{t.blockchainStatus}</span>
                  </div>
                  <div className="text-xs text-slate-400 font-mono break-all">Merkle Root: {t.merkleRoot || 'N/A'}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>
    </div>
  );
};