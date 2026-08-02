import React, { useState } from 'react';
import Button from '../Button';
import { currentMonthRange } from '../../utils/reportFormat';

/**
 * Filtrul comun celor trei rapoarte (P1.1).
 *
 * @param onApplyFilters (from, to) — ambele 'yyyy-MM-dd'
 * @param onInvalid      mesaj de eroare cand intervalul e inversat (toast-ul difera pe pagini)
 * @param isLoading      dezactiveaza butonul cat timp raportul se incarca
 */
export default function ReportFilters({ onApplyFilters, onInvalid, isLoading }) {
  // Valori implicite: luna curenta. Adminul deschide pagina si vede ceva imediat,
  // nu un ecran gol cu doua campuri.
  const [range] = useState(currentMonthRange);
  const [from, setFrom] = useState(range.from);
  const [to, setTo] = useState(range.to);

  const handleSubmit = (e) => {
    e.preventDefault();

    if (from && to && from > to) {
      onInvalid?.('Data de început trebuie să fie înaintea celei de sfârșit.');
      return;
    }

    onApplyFilters(from, to);
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-wrap items-end gap-4 bg-white p-4 rounded-lg shadow-sm border border-gray-100 mb-6"
    >
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-1" htmlFor="report-from">
          De la (Data)
        </label>
        <input
          id="report-from"
          type="date"
          value={from}
          onChange={(e) => setFrom(e.target.value)}
          required
          className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-1" htmlFor="report-to">
          Până la (Data)
        </label>
        <input
          id="report-to"
          type="date"
          value={to}
          onChange={(e) => setTo(e.target.value)}
          required
          className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <Button type="submit" variant="primary" disabled={isLoading}>
        {isLoading ? 'Se generează...' : 'Generează'}
      </Button>
    </form>
  );
}
