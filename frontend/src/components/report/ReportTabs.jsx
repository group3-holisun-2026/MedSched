import React from 'react';
import { NavLink } from 'react-router-dom';

export default function ReportTabs() {
  const tabClass = ({ isActive }) =>
    `px-4 py-2 font-medium text-sm rounded-t-lg transition-colors ${
      isActive
        ? 'bg-white text-blue-600 border-t border-l border-r border-gray-200'
        : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
    }`;

  return (
    <div className="flex space-x-1 border-b border-gray-200 mb-6 px-4 pt-2 bg-gray-50">
      <NavLink to="/rapoarte/vanzari" className={tabClass}>Vânzări</NavLink>
      <NavLink to="/rapoarte/ocupare" className={tabClass}>Grad Ocupare</NavLink>
      <NavLink to="/rapoarte/no-show" className={tabClass}>No-Show</NavLink>
    </div>
  );
}