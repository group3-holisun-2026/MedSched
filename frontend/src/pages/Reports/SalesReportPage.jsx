import React, { useState } from 'react';
import { useToast } from '../../context/ToastContext';
import { reportsApi } from '../../api/reports';

import ReportTabs from '../../components/report/ReportTabs';
import ReportFilters from '../../components/report/ReportFilters';
import ExportButtons from '../../components/report/ExportButtons';

export default function SalesReportPage() {
  // ToastContext expune showSuccess/showError, nu un `toast` generic — destructurarea
  // veche intorcea undefined si pagina crapa cu "toast is not a function" la prima cerere.
  const { showSuccess, showError } = useToast();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchReport = async (startDate, endDate) => {
    try {
      setLoading(true);
      const result = await reportsApi.getSalesReport(startDate, endDate);
      setData(result);
      showSuccess('Raportul a fost generat.');
    } catch (error) {
      showError(error.response?.data?.message ?? 'Eroare la generarea raportului.');
    } finally {
      setLoading(false);
    }
  };

  const handleExportCSV = () => {
    showSuccess('Export Excel în curs de implementare.');
  };

  const handleExportPDF = () => {
    showSuccess('Export PDF în curs de implementare.');
  };

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold text-gray-800">Rapoarte și Statistici</h1>
        <ExportButtons onExportCSV={handleExportCSV} onExportPDF={handleExportPDF} />
      </div>

      {/* TABS */}
      <ReportTabs />

      {/* FILTRE */}
      <ReportFilters onApplyFilters={fetchReport} isLoading={loading} />

      {/* CONȚINUT RAPORT */}
      {loading ? (
        <div className="text-center p-8 text-gray-500">Se încarcă datele...</div>
      ) : data ? (
        <div className="space-y-6">
          {/* Card Total Încasări */}
          <div className="bg-blue-50 border border-blue-100 p-6 rounded-lg text-center">
            <h2 className="text-lg font-semibold text-blue-800">Total Încasări</h2>
            <p className="text-3xl font-bold text-blue-600 mt-2">{data.totalRevenue || 0} RON</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Tabel pe Serviciu */}
            <div className="bg-white p-4 border border-gray-200 rounded-lg shadow-sm">
              <h3 className="font-bold text-gray-700 mb-3 border-b pb-2">Vânzări per Serviciu</h3>
              <ul className="space-y-2">
                {data.byService?.map((item, index) => (
                  <li key={index} className="flex justify-between text-sm">
                    <span>{item.serviceName}</span>
                    <span className="font-semibold">{item.revenue} RON</span>
                  </li>
                )) || <li className="text-gray-400 text-sm">Fără date</li>}
              </ul>
            </div>

            {/* Tabel pe Medic */}
            <div className="bg-white p-4 border border-gray-200 rounded-lg shadow-sm">
              <h3 className="font-bold text-gray-700 mb-3 border-b pb-2">Vânzări per Medic</h3>
              <ul className="space-y-2">
                {data.byDoctor?.map((item, index) => (
                  <li key={index} className="flex justify-between text-sm">
                    <span>{item.doctorName}</span>
                    <span className="font-semibold">{item.revenue} RON</span>
                  </li>
                )) || <li className="text-gray-400 text-sm">Fără date</li>}
              </ul>
            </div>
          </div>
        </div>
      ) : (
        <div className="text-center p-8 text-gray-400 bg-white rounded-lg border border-gray-200">
          Alegeți o perioadă și apăsați "Aplică Filtre" pentru a genera raportul de vânzări.
        </div>
      )}
    </div>
  );
}