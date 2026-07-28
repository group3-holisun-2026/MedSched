import React, { useState } from 'react';
import Button from '../Button'; // Ajustează calea dacă e diferită

export default function ReportFilters({ onApplyFilters, isLoading }) {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    onApplyFilters(startDate, endDate);
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-4 bg-white p-4 rounded-lg shadow-sm border border-gray-100 mb-6">
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-1">De la (Data)</label>
        <input
          type="date"
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
          required
          className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-1">Până la (Data)</label>
        <input
          type="date"
          value={endDate}
          onChange={(e) => setEndDate(e.target.value)}
          required
          className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <Button type="submit" variant="primary" disabled={isLoading}>
        {isLoading ? 'Se aplică...' : 'Aplică Filtre'}
      </Button>
    </form>
  );
}