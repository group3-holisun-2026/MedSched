import React, { useState } from 'react';
import { useToast } from '../../context/ToastContext';
import { reportsApi } from '../../api/reports';
import { formatCurrency } from '../../utils/reportFormat';

import ReportTabs from '../../components/report/ReportTabs';
import ReportFilters from '../../components/report/ReportFilters';
import ExportButtons from '../../components/report/ExportButtons';

// SalesRow = { id, name, appointments, total } — vezi module5_6_endpoints.md.
function SalesTable({ title, rows }) {
  return (
    <div className="bg-white p-4 border border-gray-200 rounded-lg shadow-sm">
      <h3 className="font-bold text-gray-700 mb-3 border-b pb-2">{title}</h3>

      {rows.length === 0 ? (
        <p className="text-gray-400 text-sm">Nicio programare finalizată în intervalul selectat.</p>
      ) : (
        // NFR-3: pe tableta un tabel de 3 coloane cu nume lungi sparge layout-ul fara wrapper.
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ textAlign: 'left', backgroundColor: '#f2f2f2' }}>
                <th style={thStyle}>Nume</th>
                <th style={thStyle}>Nr. programări</th>
                <th style={{ ...thStyle, textAlign: 'right' }}>Total</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id ?? row.name} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={tdStyle}>{row.name}</td>
                  <td style={tdStyle}>{row.appointments}</td>
                  <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 600 }}>
                    {formatCurrency(row.total)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default function SalesReportPage() {
  // ToastContext expune showSuccess/showError, nu un `toast` generic.
  const { showSuccess, showError } = useToast();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  // Intervalul efectiv aplicat, nu cel din campuri — exportul trebuie sa scoata exact
  // ce se vede pe ecran, nu ce a apucat utilizatorul sa tasteze intre timp.
  const [appliedRange, setAppliedRange] = useState({ from: '', to: '' });

  const fetchReport = async (from, to) => {
    try {
      setLoading(true);
      const result = await reportsApi.getSalesReport(from, to);
      setData(result);
      setAppliedRange({ from, to });
      showSuccess('Raportul a fost generat.');
    } catch (error) {
      showError(error.response?.data?.message ?? 'Eroare la generarea raportului.');
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex flex-wrap justify-between items-center gap-4 mb-4">
        <h1 className="text-2xl font-bold text-gray-800">Raport Vânzări</h1>
        <ExportButtons
          type="sales"
          from={appliedRange.from}
          to={appliedRange.to}
          onError={showError}
        />
      </div>

      <ReportTabs />

      <ReportFilters onApplyFilters={fetchReport} onInvalid={showError} isLoading={loading} />

      {loading ? (
        <div className="text-center p-8 text-gray-500">Se generează raportul...</div>
      ) : data ? (
        <div className="space-y-6">
          <div className="bg-blue-50 border border-blue-100 p-6 rounded-lg text-center">
            <h2 className="text-lg font-semibold text-blue-800">Total Încasări</h2>
            <p className="text-3xl font-bold text-blue-600 mt-2">
              {formatCurrency(data.grandTotal)}
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <SalesTable title="Vânzări per Serviciu" rows={data.byService ?? []} />
            <SalesTable title="Vânzări per Medic" rows={data.byDoctor ?? []} />
          </div>

          <p className="text-sm text-gray-500">
            Se contorizează doar consultațiile finalizate.
          </p>
        </div>
      ) : (
        <div className="text-center p-8 text-gray-400 bg-white rounded-lg border border-gray-200">
          Alegeți o perioadă și apăsați <strong>Generează</strong> pentru raportul de vânzări.
        </div>
      )}
    </div>
  );
}

const thStyle = { padding: '12px', borderBottom: '1px solid #ddd', whiteSpace: 'nowrap' };
const tdStyle = { padding: '12px' };
