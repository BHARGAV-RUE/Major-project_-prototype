import React from 'react';

interface NavbarProps {
  activeView: 'user' | 'admin' | 'download';
  onNavigate: (view: 'user' | 'admin' | 'download') => void;
}

export const Navbar: React.FC<NavbarProps> = ({ activeView, onNavigate }) => {
  return (
    <nav className="sticky top-0 z-50 flex items-center justify-between px-4 sm:px-8 py-3.5 bg-navy/90 backdrop-blur-md border-b border-white/10">
      <button
        onClick={() => onNavigate('user')}
        className="flex items-center gap-2.5 text-white font-display text-xl font-medium tracking-tight border-none bg-transparent cursor-pointer"
      >
        <span className="w-8.5 h-8.5 rounded-lg bg-cobalt grid place-items-center">
          <svg className="w-5 h-5 stroke-white fill-none stroke-[2.2] stroke-linecap-round stroke-linejoin-round" viewBox="0 0 24 24">
            <path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/>
          </svg>
        </span>
        <span>Cloud<b className="font-extrabold">Transfer</b></span>
      </button>

      <ul className="hidden md:flex gap-1 list-none m-0 p-0">
        <li><a href="#about" className="px-3.5 py-2 rounded-lg font-semibold text-sm text-[#b9c6dd] hover:text-white hover:bg-white/10 transition-colors">About Us</a></li>
        <li><a href="#contributor" className="px-3.5 py-2 rounded-lg font-semibold text-sm text-[#b9c6dd] hover:text-white hover:bg-white/10 transition-colors">Wanna be a contributor?</a></li>
        <li><a href="#features" className="px-3.5 py-2 rounded-lg font-semibold text-sm text-[#b9c6dd] hover:text-white hover:bg-white/10 transition-colors">Features</a></li>
      </ul>

      <div className="flex gap-2">
        <button
          onClick={() => onNavigate('admin')}
          className={`px-3.5 py-1.5 border rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
            activeView === 'admin'
              ? 'bg-cobalt border-cobalt text-white'
              : 'border-[#2c3f66] text-white hover:border-[#7fa3ff] hover:bg-[#7fa3ff]/10'
          }`}
        >
          Admin Portal
        </button>
      </div>
    </nav>
  );
};