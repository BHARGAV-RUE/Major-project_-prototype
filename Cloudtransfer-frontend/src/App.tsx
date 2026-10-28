import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';
import { TransferHub } from './pages/TransferHub';
import { DownloadPage } from './pages/DownloadPage';
import { ContributorRegister } from './pages/ContributorRegister';
import { ContributorDashboard } from './pages/ContributorDashboard';
import { AdminControlTower } from './pages/AdminControlTower';

export const App: React.FC = () => {
  return (
    <Router>
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-cyan-500 selection:text-white">
        <Navbar />
        <main className="flex-1">
          <Routes>
            <Route path="/" element={<TransferHub />} />
            <Route path="/download/:sessionId" element={<DownloadPage />} />
            <Route path="/contributor/register" element={<ContributorRegister />} />
            <Route path="/contributor/dashboard" element={<ContributorDashboard />} />
            <Route path="/admin" element={<AdminControlTower />} />
          </Routes>
        </main>
        <Footer />
      </div>
    </Router>
  );
};

export default App;