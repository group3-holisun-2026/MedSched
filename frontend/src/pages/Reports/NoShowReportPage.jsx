import React, { useState } from 'react';
import { useToast } from '../../context/ToastContext';
import { reportsApi } from '../../api/reports';
import ReportTabs from '../../components/report/ReportTabs';
import ReportFilters from '../../components/report/ReportFilters';
import ExportButtons from '../../components/report/ExportButtons';
import NoShowReportByPatientTable from './NoShowReportByPatientTable';
import NoShowReportByWeekdayTable from './NoShowReportByWeekdayTable';

export default function NoShowReportPage() {
  const { toast } = useToast();

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showOnlyFrequent, setShowOnlyFrequent] = useState(false);

  const fetchReport = async (startDate, endDate) => {
    try {
      setLoading(true);

      const result = await reportsApi.getNoShowReport(
        startDate,
        endDate
      );

      setData(result);

      toast({
        type: 'success',
        message: 'Raportul a fost generat.',
      });
    } catch (error) {
      toast({
        type: 'error',
        message: 'Eroare la generarea raportului.',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleExportCSV = () => {
    toast({
      type: 'info',
      message: 'Export CSV în curs de implementare...',
    });
  };

  const handleExportPDF = () => {
    toast({
      type: 'info',
      message: 'Export PDF în curs de implementare...',
    });
  };

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold text-gray-800">
          Raport Neprezentări
        </h1>

        <ExportButtons
          onExportCSV={handleExportCSV}
          onExportPDF={handleExportPDF}
        />
      </div>

      <ReportTabs />

      <ReportFilters
        onApplyFilters={fetchReport}
        isLoading={loading}
      />

      {loading ? (
        <div className="text-center p-8 text-gray-500">
          Se încarcă datele...
        </div>
      ) : data ? (
        <div className="space-y-6">

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

            <div className="bg-white border border-gray-200 rounded-lg p-6 text-center shadow-sm">
              <h2 className="text-sm text-gray-600 uppercase tracking-wide">
                Total Programări
              </h2>

              <p className="text-3xl font-bold text-gray-800 mt-2">
                {data.totalAppointments}
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-lg p-6 text-center shadow-sm">
              <h2 className="text-sm text-gray-600 uppercase tracking-wide">
                Total Neprezentări
              </h2>

              <p className="text-3xl font-bold text-red-600 mt-2">
                {data.totalNoShows}
              </p>
            </div>

            <div className="bg-red-50 border border-red-100 rounded-lg p-6 text-center">
              <h2 className="text-sm text-red-700 uppercase tracking-wide">
                Rata Generală
              </h2>

              <p className="text-4xl font-bold text-red-600 mt-2">
                {(data.overallRate * 100).toFixed(1)}%
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