import React, { useState } from 'react';
import { useToast } from '../../context/ToastContext';
import { reportsApi } from '../../api/reports';
import { formatRate } from '../../utils/reportFormat';
import ReportTabs from '../../components/report/ReportTabs';
import ReportFilters from '../../components/report/ReportFilters';
import ExportButtons from '../../components/report/ExportButtons';
import NoShowReportByPatientTable from './NoShowReportByPatientTable';
import NoShowReportByWeekdayTable from './NoShowReportByWeekdayTable';

export default function NoShowReportPage() {
  const { showSuccess, showError } = useToast();

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showOnlyFrequent, setShowOnlyFrequent] = useState(false);
  // Intervalul efectiv aplicat — exportul scoate exact ce e pe ecran.
  const [appliedRange, setAppliedRange] = useState({ from: '', to: '' });

  const fetchReport = async (from, to) => {
    try {
      setLoading(true);

      const result = await reportsApi.getNoShowReport(from, to);

      setData(result);
      setAppliedRange({ from, to });
      showSuccess('Raportul a fost generat.');
    } catch (error) {
      showError(
        error.response?.data?.message ??
        'Eroare la generarea raportului.'
      );
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex flex-wrap justify-between items-center gap-4 mb-4">
        <h1 className="text-2xl font-bold text-gray-800">
          Raport Neprezentări
        </h1>

        <ExportButtons
          type="no-show"
          from={appliedRange.from}
          to={appliedRange.to}
          onError={showError}
        />
      </div>

      <ReportTabs />

      <ReportFilters
        onApplyFilters={fetchReport}
        onInvalid={showError}
        isLoading={loading}
      />

      {loading ? (
        <div className="text-center p-8 text-gray-500">
          Se generează raportul...
        </div>
      ) : data ? (
        <div className="space-y-6">

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

            <div className="bg-white border border-gray-200 rounded-lg p-6 text-center shadow-sm">
              <h2 className="text-sm text-gray-600 uppercase tracking-wide">
                Total Programări
              </h2>

              <p className="text-3xl font-bold text-gray-800 mt-2">
                {data.total}
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-lg p-6 text-center shadow-sm">
              <h2 className="text-sm text-gray-600 uppercase tracking-wide">
                Total Neprezentări
              </h2>

              <p className="text-3xl font-bold text-red-600 mt-2">
                {data.noShows}
              </p>
            </div>

            <div className="bg-red-50 border border-red-100 rounded-lg p-6 text-center">
              <h2 className="text-sm text-red-700 uppercase tracking-wide">
                Rata Generală
              </h2>

              <p className="text-4xl font-bold text-red-600 mt-2">
                {formatRate(data.rate, data.total > 0)}
              </p>
            </div>

          </div>

          <div className="bg-white border border-gray-200 rounded-lg shadow-sm p-4">

            <div className="flex justify-between items-center mb-4">

              <h3 className="font-bold text-gray-700">
                Neprezentări pe pacient
              </h3>

              <label className="flex items-center gap-2 text-sm">

                <input
                  type="checkbox"
                  checked={showOnlyFrequent}
                  onChange={(e) =>
                    setShowOnlyFrequent(e.target.checked)
                  }
                />

                Doar pacienți cu ≥ 2 neprezentări

              </label>

            </div>

            <NoShowReportByPatientTable
              patients={data.byPatient}
              showOnlyFrequent={showOnlyFrequent}
            />

          </div>

          <div className="bg-white border border-gray-200 rounded-lg shadow-sm p-4">

            <h3 className="font-bold text-gray-700 mb-4">
              Neprezentări pe zi a săptămânii
            </h3>

            <NoShowReportByWeekdayTable
              weekdays={data.byWeekday}
            />

          </div>

        </div>
      ) : (
        <div className="text-center p-8 text-gray-400 bg-white rounded-lg border border-gray-200">
          Alegeți o perioadă și apăsați

          <strong>Aplică Filtre</strong>

          pentru a genera raportul.
        </div>
      )}
    </div>
  );
}