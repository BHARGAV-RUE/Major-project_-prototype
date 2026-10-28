import React from 'react';
import { Link, useLocation } from 'react-router-dom';

export const Navbar: React.FC = () => {
  const location = useLocation();

  const isActive = (path: string) =>
    location.pathname === path
      ? 'text-cyan-400 font-semibold border-b-2 border-cyan-400 pb-1'
      : 'text-slate-300 hover:text-white transition';

  return (
    <nav className="bg-slate-900/80 backdrop-blur-md border-b border-slate-800 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-cyan-500 to-indigo-600 flex items-center justify-center font-bold text-white shadow-lg shadow-cyan-500/20">
            CT
          </div>
          <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-white via-slate-200 to-slate-400 bg-clip-text text-transparent">
            CloudTransfer
          </span>
        </Link>

        <div className="flex items-center gap-6 text-sm">
          <Link to="/" className={isActive('/')}>
            Send Files
          </Link>
          <Link to="/contributor/register" className={isActive('/contributor/register')}>
            Become a Contributor
          </Link>
          <Link to="/contributor/dashboard" className={isActive('/contributor/dashboard')}>
            Dashboard
          </Link>
          <Link
            to="/admin"
            className="px-3 py-1.5 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
          >
            Admin Access
          </Link>
        </div>
      </div>
    </nav>
  );
};