import React, { useMemo, useState } from 'react';
import Card from '../../components/Card';
import RateBar from '../../components/report/RateBar';
import ReportTabs from '../../components/report/ReportTabs';
import ReportFilters from '../../components/report/ReportFilters';
import ExportButtons from '../../components/report/ExportButtons';
import { reportsApi } from '../../api/reports';
import { useToast } from '../../context/ToastContext';
import { formatMinutes, formatRate } from '../../utils/reportFormat';

// ResourceOccupancyRow = { resourceId, name, bookedMinutes, availableMinutes, occupancyRate }
// occupancyRate e fractie 0..1; RateBar primeste procente, deci se inmulteste cu 100.

// Medie ponderata, nu media procentelor: un medic cu 8 ore de program si unul cu 1 ora
// nu cantaresc la fel in gradul de ocupare al clinicii.
const weightedRate = (rows) => {
  const booked = rows.reduce((sum, row) => sum + (row.bookedMinutes ?? 0), 0);
  const available = rows.reduce((sum, row) => sum + (row.availableMinutes ?? 0), 0);
  return available > 0 ? booked / available : 0;
};

const OccupancyTable = ({ title, nameHeader, rows }) => {
  if (!rows || rows.length === 0) {
    return (
      <div className="text-center py-10 bg-white rounded-xl border border-gray-100 shadow-sm mt-4">
        <p className="text-gray-500">Nu există date pentru {title.toLowerCase()}.</p>
      </div>
    );
  }

  return (
    <Card className="mt-4">
      <div className="mb-4">
        <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
      </div>

      {/* NFR-3: 4 coloane sparg layout-ul la 768px fara wrapper-ul de scroll. */}
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ backgroundColor: '#f8fafc', textAlign: 'left' }}>
              <th style={thStyle}>{nameHeader}</th>
              <th style={thStyle}>Minute ocupate</th>
              <th style={thStyle}>Minute disponibile</th>
              <th style={{ ...thStyle, width: '30%' }}>Grad de ocupare</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => {
              // Medic fara orar definit in interval: numitor 0. Nu ascundem randul —
              // exact asta vrea adminul sa vada (resursa neconfigurata).
              const hasSchedule = (row.availableMinutes ?? 0) > 0;

              return (
                <tr key={row.resourceId ?? row.name} style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ ...tdStyle, fontWeight: 500, color: '#0f172a' }}>{row.name}</td>
                  <td style={tdStyle}>{formatMinutes(row.bookedMinutes)}</td>
                  <td style={tdStyle}>
                    {hasSchedule ? (
                      formatMinutes(row.availableMinutes)
                    ) : (
                      <span title="Resursa nu are program definit în acest interval.">—</span>
                    )}
                  </td>
                  <td style={tdStyle}>
                    {hasSchedule ? (
                      <RateBar percentage={(row.occupancyRate ?? 0) * 100} showPercentageLabel />
                    ) : (
                      <span
                        className="text-gray-400"
                        title="Resursa nu are program definit în acest interval."
                      >
                        {formatRate(row.occupancyRate, false)}
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </Card>
  );
};

export default function OccupancyReportPage() {
  const { showSuccess, showError } = useToast();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [appliedRange, setAppliedRange] = useState({ from: '', to: '' });

  const fetchReport = async (from, to) => {
    try {
      setLoading(true);
      const result = await reportsApi.getOccupancyReport(from, to);
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

  // Backend-ul trimite deja randurile sortate; sortam local doar descrescator dupa ocupare,
  // ca resursele subutilizate sa iasa in evidenta la coada tabelului.
  const doctors = useMemo(
    () => [...(data?.doctors ?? [])].sort((a, b) => (b.occupancyRate ?? 0) - (a.occupancyRate ?? 0)),
    [data]
  );
  const rooms = useMemo(
    () => [...(data?.rooms ?? [])].sort((a, b) => (b.occupancyRate ?? 0) - (a.occupancyRate ?? 0)),
    [data]
  );

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex flex-wrap justify-between items-center gap-4 mb-4">
        <h1 className="text-2xl font-bold text-gray-800">Raport Ocupare Resurse</h1>
        <ExportButtons
          type="occupancy"
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
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Card>
              <p className="text-sm text-gray-500">Grad mediu de ocupare — Medici</p>
              <RateBar percentage={weightedRate(doctors) * 100} className="mt-2" />
            </Card>
            <Card>
              <p className="text-sm text-gray-500">Grad mediu de ocupare — Cabinete</p>
              <RateBar percentage={weightedRate(rooms) * 100} className="mt-2" />
            </Card>
          </div>

          <OccupancyTable title="Medici" nameHeader="Medic" rows={doctors} />
          <OccupancyTable title="Cabinete" nameHeader="Cabinet" rows={rooms} />

          <p className="text-sm text-gray-500 mt-4">
            Programările anulate și neprezentările nu ocupă resursa. Un „—” la gradul de ocupare
            înseamnă că resursa nu are program definit în intervalul selectat.
          </p>
        </>
      ) : (
        <div className="text-center p-8 text-gray-400 bg-white rounded-lg border border-gray-200">
          Alegeți o perioadă și apăsați <strong>Generează</strong> pentru raportul de ocupare.
        </div>
      )}
    </div>
  );
}

const thStyle = { padding: '12px', borderBottom: '1px solid #e2e8f0', whiteSpace: 'nowrap' };
const tdStyle = { padding: '12px', color: '#334155' };
