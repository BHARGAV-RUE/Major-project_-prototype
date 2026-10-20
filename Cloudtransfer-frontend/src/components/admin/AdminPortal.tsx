import React, { useState, useEffect } from 'react';
import { adminApi } from '../../services/api';
import { SystemStatsDto, Contributor, SessionInitMessage } from '../../types/transfer';

export const AdminPortal: React.FC = () => {
  const [stats, setStats] = useState<SystemStatsDto>({
    activeSessions: 12,
    totalTransferredBytes: 485000000000,
    activeContributors: 8,
    storageNodesCount: 24,
  });

  const [contributors, setContributors] = useState<Contributor[]>([
    {
      id: 'node-01',
      name: 'Adithya Node Primary',
      email: 'adithya@cloudtransfer.io',
      nodeAddress: '0x71C...39A2',
      storageAllocatedGB: 500,
      status: 'ACTIVE',
      reputationScore: 98,
    },
    {
      id: 'node-02',
      name: 'Komal Storage Mesh',
      email: 'komal@cloudtransfer.io',
      nodeAddress: '0x32B...90F1',
      storageAllocatedGB: 1000,
      status: 'PENDING',
      reputationScore: 100,
    },
  ]);

  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'CONTRIBUTORS' | 'SESSIONS'>('OVERVIEW');

  useEffect(() => {
    // Fetch system statistics and node telemetry
    adminApi.getStats().then(setStats).catch(() => {});
  }, []);

  const approveNode = async (id: string) => {
    try {
      await adminApi.approveContributor(id);
      setContributors((prev) =>
        prev.map((c) => (c.id === id ? { ...c, status: 'ACTIVE' } : c))
      );
    } catch (e) {
      console.error('Failed to approve node', e);
    }
  };

  return (
    <div className="max-w-[1040px] mx-auto p-6 sm:p-9">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-8 gap-4">
        <div>
          <h1 className="font-display text-3xl font-bold tracking-tight text-bodytext">Admin Control Engine</h1>
          <p className="text-muted text-sm">Decentralized Multi-Cloud Mesh & Node Telemetry</p>
        </div>
        <div className="flex gap-2 bg-soft p-1 rounded-xl border border-line">
          <button
            onClick={() => setActiveTab('OVERVIEW')}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition-colors border-none cursor-pointer ${
              activeTab === 'OVERVIEW' ? 'bg-cobalt text-white' : 'bg-transparent text-muted hover:text-bodytext'
            }`}
          >
            Metrics
          </button>
          <button
            onClick={() => setActiveTab('CONTRIBUTORS')}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition-colors border-none cursor-pointer ${
              activeTab === 'CONTRIBUTORS' ? 'bg-cobalt text-white' : 'bg-transparent text-muted hover:text-bodytext'
            }`}
          >
            Contributors
          </button>
        </div>
      </div>

      {activeTab === 'OVERVIEW' && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
            <div className="p-5 border border-line rounded-2xl bg-white shadow-sm">
              <span className="text-xs font-semibold text-muted block mb-1">Active Transfers</span>
              <span className="font-display text-3xl font-extrabold text-cobalt">{stats.activeSessions}</span>
            </div>
            <div className="p-5 border border-line rounded-2xl bg-white shadow-sm">
              <span className="text-xs font-semibold text-muted block mb-1">Total Bandwidth</span>
              <span className="font-display text-3xl font-extrabold text-teal">
                {(stats.totalTransferredBytes / 1e9).toFixed(1)} GB
              </span>
            </div>
            <div className="p-5 border border-line rounded-2xl bg-white shadow-sm">
              <span className="text-xs font-semibold text-muted block mb-1">Storage Nodes</span>
              <span className="font-display text-3xl font-extrabold text-navy">{stats.storageNodesCount}</span>
            </div>
            <div className="p-5 border border-line rounded-2xl bg-white shadow-sm">
              <span className="text-xs font-semibold text-muted block mb-1">Active Contributors</span>
              <span className="font-display text-3xl font-extrabold text-cobalt-dark">{stats.activeContributors}</span>
            </div>
          </div>

          <div className="border border-line rounded-2xl bg-white p-6 mb-8">
            <h3 className="font-display text-lg font-bold mb-4">Multi-Cloud Storage Router Status</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-4 border border-line rounded-xl bg-soft">
                <div className="flex justify-between items-center mb-2">
                  <span className="font-semibold text-sm">Google Drive OAuth</span>
                  <span className="w-2.5 h-2.5 rounded-full bg-teal" />
                </div>
                <span className="text-xs text-muted">Primary Chunk Destination</span>
              </div>
              <div className="p-4 border border-line rounded-xl bg-soft">
                <div className="flex justify-between items-center mb-2">
                  <span className="font-semibold text-sm">Dropbox API</span>
                  <span className="w-2.5 h-2.5 rounded-full bg-teal" />
                </div>
                <span className="text-xs text-muted">Secondary Chunk Destination</span>
              </div>
              <div className="p-4 border border-line rounded-xl bg-soft">
                <div className="flex justify-between items-center mb-2">
                  <span className="font-semibold text-sm">RabbitMQ Engine</span>
                  <span className="w-2.5 h-2.5 rounded-full bg-teal" />
                </div>
                <span className="text-xs text-muted">Stream Queue Operational</span>
              </div>
            </div>
          </div>
        </>
      )}

      {activeTab === 'CONTRIBUTORS' && (
        <div className="border border-line rounded-2xl bg-white overflow-hidden shadow-sm">
          <div className="p-6 border-b border-line flex justify-between items-center">
            <h3 className="font-display text-lg font-bold">Node Approvals & Storage Allotment</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-soft border-b border-line text-xs font-bold text-muted">
                  <th className="p-4">Node Name</th>
                  <th className="p-4">Wallet Address</th>
                  <th className="p-4">Allocated Capacity</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line text-sm">
                {contributors.map((c) => (
                  <tr key={c.id} className="hover:bg-soft/50">
                    <td className="p-4 font-semibold text-bodytext">{c.name}</td>
                    <td className="p-4 font-mono text-xs text-muted">{c.nodeAddress}</td>
                    <td className="p-4 font-semibold">{c.storageAllocatedGB} GB</td>
                    <td className="p-4">
                      <span className={`px-2.5 py-1 rounded-full text-[0.7rem] font-extrabold ${
                        c.status === 'ACTIVE' ? 'bg-teal/10 text-teal' : 'bg-amber-100 text-amber-800'
                      }`}>
                        {c.status}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      {c.status === 'PENDING' && (
                        <button
                          onClick={() => approveNode(c.id)}
                          className="px-3 py-1.5 border-0 rounded-lg bg-cobalt text-white font-bold text-xs cursor-pointer hover:bg-cobalt-dark"
                        >
                          Approve Node
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};