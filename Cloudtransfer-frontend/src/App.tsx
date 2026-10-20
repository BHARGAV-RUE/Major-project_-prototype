import React, { useState } from 'react';
import { Navbar } from './components/common/Navbar';
import { TransferWidget } from './components/transfer/TransferWidget';
import { AdminPortal } from './components/admin/AdminPortal';

export function App() {
  const [activeView, setActiveView] = useState<'user' | 'admin' | 'download'>('user');

  return (
    <div className="min-h-screen flex flex-col bg-paper text-bodytext">
      <Navbar activeView={activeView} onNavigate={setActiveView} />

      {activeView === 'user' && (
        <main className="flex-1">
          <header className="relative bg-navy text-white px-4 sm:px-8 pt-18 pb-32 overflow-hidden">
            <div className="relative max-w-[1040px] mx-auto">
              <h1 className="font-display font-bold text-4xl sm:text-6xl tracking-tight leading-[1.04] max-w-[14ch]">
                Send large files, securely.
              </h1>
              <p className="mt-5 max-w-[48ch] text-lg text-[#a9b9d3]">
                No account needed. Your files are encrypted in your browser and stored across several clouds, up to 5 GB per transfer.
              </p>
            </div>
          </header>

          <div className="px-4 sm:px-8 pb-20 -mt-20 relative">
            <TransferWidget />
          </div>

          <div className="bg-white border-t border-line py-20 px-4 sm:px-8">
            <section id="about" className="max-w-[1040px] mx-auto mb-20">
              <h2 className="font-display text-3xl font-bold tracking-tight mb-8">About Us</h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
                <div className="pt-5 border-t-3 border-cobalt">
                  <h3 className="text-lg font-bold mb-2.5">What is CloudTransfer?</h3>
                  <p className="text-muted text-sm leading-relaxed">
                    CloudTransfer is a decentralized file delivery network built to ensure your files reach their destination securely without relying on a single central server point of failure.
                  </p>
                </div>
                <div className="pt-5 border-t-3 border-cobalt">
                  <h3 className="text-lg font-bold mb-2.5">Why do we need it?</h3>
                  <p className="text-muted text-sm leading-relaxed">
                    Traditional file transfer services store your data in one place, making it vulnerable. We created this to give users privacy, resilience, and total control over their data.
                  </p>
                </div>
                <div className="pt-5 border-t-3 border-cobalt">
                  <h3 className="text-lg font-bold mb-2.5">How does it work?</h3>
                  <p className="text-muted text-sm leading-relaxed">
                    Files are chunked client-side, encrypted using ChaCha20, and distributed across multiple storage nodes simultaneously via RabbitMQ streams, creating a seamless multi-cloud mesh.
                  </p>
                </div>
              </div>
            </section>
          </div>
        </main>
      )}

      {activeView === 'admin' && (
        <main className="flex-1 py-8">
          <AdminPortal />
        </main>
      )}

      <footer className="mt-auto bg-navy text-white px-4 sm:px-8 py-10 flex flex-col md:flex-row justify-between items-center gap-6">
        <div>
          <div className="flex items-center gap-2.5 font-display text-lg font-medium">
            <span className="w-8 h-8 rounded-lg bg-cobalt grid place-items-center">
              <svg className="w-4.5 h-4.5 stroke-white fill-none stroke-[2.2] stroke-linecap-round stroke-linejoin-round" viewBox="0 0 24 24">
                <path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/>
              </svg>
            </span>
            <span>Cloud<b className="font-extrabold">Transfer</b></span>
          </div>
          <p className="mt-3 text-sm text-[#8ea0c0]">
            Developed by <span className="ml-2 font-semibold text-[#e3eaf8]">K. Bhargav</span> <span className="ml-2 font-semibold text-[#e3eaf8]">A. Adithya</span> <span className="ml-2 font-semibold text-[#e3eaf8]">Komal Shah</span> <span className="ml-2 font-semibold text-[#e3eaf8]">Kranthi</span>
          </p>
        </div>

        <div className="flex gap-3">
          <button onClick={() => setActiveView('user')} className="px-5 py-2.5 border border-[#2c3f66] rounded-xl bg-transparent text-white text-sm font-semibold cursor-pointer hover:border-[#7fa3ff] hover:bg-[#7fa3ff]/10 transition-colors">
            User View
          </button>
          <button onClick={() => setActiveView('admin')} className="px-5 py-2.5 border border-[#2c3f66] rounded-xl bg-transparent text-white text-sm font-semibold cursor-pointer hover:border-[#7fa3ff] hover:bg-[#7fa3ff]/10 transition-colors">
            Admin Portal
          </button>
        </div>
      </footer>
    </div>
  );
}