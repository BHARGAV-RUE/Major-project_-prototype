import React from 'react';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-slate-950 border-t border-slate-900 py-6 text-center text-xs text-slate-500">
      <div className="max-w-7xl mx-auto px-6 flex flex-col md:flex-row items-center justify-between gap-4">
        <div>
          🔒 End-to-End Encrypted File Transfer Engine • Multi-Cloud Storage Network
        </div>
        <div>
          Decentralized Vault Verification &bull; All transfers auto-expire
        </div>
      </div>
    </footer>
  );
};