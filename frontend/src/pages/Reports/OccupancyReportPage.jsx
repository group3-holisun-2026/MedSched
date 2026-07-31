import React, { useEffect, useMemo, useState } from 'react';
import Card from '../../components/Card';
import RateBar from '../../components/report/RateBar';

// --- MOCK DATA -------------------------------------------------------------
// Raportul nu depinde încă de backend (nu există endpoint /rapoarte/ocupare).
// Structura de mai jos e gândită să corespundă cu ce ar putea întoarce
// un viitor endpoint, ca înlocuirea cu fetch-ul real să fie directă:
//   const data = await reportApi.getOccupancy({ from, to });
// Minutele sunt calculate pe perioada de raportare (implicit: săptămâna curentă).
const MOCK_DOCTORS_OCCUPANCY = [
  { id: 'd1', name: 'Dr. Ana Popescu', speciality: 'Cardiologie', occupiedMinutes: 1320, totalMinutes: 2400 },
  { id: 'd2', name: 'Dr. Mihai Ionescu', speciality: 'Pediatrie', occupiedMinutes: 2040, totalMinutes: 2400 },
  { id: 'd3', name: 'Dr. Elena Dumitrescu', speciality: 'Dermatologie', occupiedMinutes: 720, totalMinutes: 2400 },
  { id: 'd4', name: 'Dr. Radu Constantin', speciality: 'Ortopedie', occupiedMinutes: 1080, totalMinutes: 1800 },
  { id: 'd5', name: 'Dr. Ioana Marinescu', speciality: 'Neurologie', occupiedMinutes: 300, totalMinutes: 1800 },
];

const MOCK_ROOMS_OCCUPANCY = [
  { id: 'r1', name: 'Cabinet 1 - Cardiologie', occupiedMinutes: 1560, totalMinutes: 2400 },
  { id: 'r2', name: 'Cabinet 2 - Pediatrie', occupiedMinutes: 2160, totalMinutes: 2400 },
  { id: 'r3', name: 'Cabinet 3 - Dermatologie', occupiedMinutes: 600, totalMinutes: 2400 },
  { id: 'r4', name: 'Sală Tratamente', occupiedMinutes: 900, totalMinutes: 1800 },
  { id: 'r5', name: 'Cabinet 4 - Ortopedie', occupiedMinutes: 1440, totalMinutes: 1800 },
];

// Simulează un apel asincron către backend, ca înlocuirea ulterioară
// cu apiClient să nu ceară modificări în restul componentei.
const fetchOccupancyReport = () =>
  new Promise((resolve) => {
    setTimeout(() => {
      resolve({ doctors: MOCK_DOCTORS_OCCUPANCY, rooms: MOCK_ROOMS_OCCUPANCY });
    }, 300);
  });

const withPercentage = (item) => ({
  ...item,
  percentage: item.totalMinutes > 0 ? (item.occupiedMinutes / item.totalMinutes) * 100 : 0,
});

const average = (items) =>
  items.length === 0 ? 0 : items.reduce((sum, item) => sum + item.percentage, 0) / items.length;

const OccupancyTable = ({ title, subtitle, rows, nameHeader }) => {
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
        {subtitle && <p className="text-sm text-gray-500 mt-0.5">{subtitle}</p>}
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ backgroundColor: '#f8fafc', textAlign: 'left' }}>
              <th style={{ padding: '12px', borderBottom: '1px solid #e2e8f0' }}>{nameHeader}</th>
              <th style={{ padding: '12px', borderBottom: '1px solid #e2e8f0' }}>Minute ocupate</th>
              <th style={{ padding: '12px', borderBottom: '1px solid #e2e8f0' }}>Minute disponibile</th>
              <th style={{ padding: '12px', borderBottom: '1px solid #e2e8f0', width: '30%' }}>Grad de ocupare</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '12px', fontWeight: 500, color: '#0f172a' }}>
                  {row.name}
                  {row.speciality && (
                    <span className="block text-xs text-gray-400 font-normal">{row.speciality}</span>
                  )}
                </td>
                <td style={{ padding: '12px', color: '#334155' }}>{row.occupiedMinutes} min</td>
                <td style={{ padding: '12px', color: '#334155' }}>
                  {Math.max(row.totalMinutes - row.occupiedMinutes, 0)} min
                </td>
                <td style={{ padding: '12px' }}>
                  <RateBar
                    percentage={row.percentage}
                    occupiedMinutes={row.occupiedMinutes}
                    totalMinutes={row.totalMinutes}
                    showPercentageLabel
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
};

const OccupancyReportPage = () => {
  const [doctors, setDoctors] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadReport = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await fetchOccupancyReport();
        setDoctors(data.doctors.map(withPercentage));
        setRooms(data.rooms.map(withPercentage));
      } catch (err) {
        console.error(err);
        setError('Nu am putut încărca raportul de ocupare.');
      } finally {
        setLoading(false);
      }
    };
    loadReport();
  }, []);

  const doctorsAvgOccupancy = useMemo(() => average(doctors), [doctors]);
  const roomsAvgOccupancy = useMemo(() => average(rooms), [rooms]);

  return (
    <div className="p-8 min-h-screen" style={{ backgroundColor: '#f8fafc' }}>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Raport Ocupare Resurse</h1>
        <p className="text-sm text-gray-500 mt-1">Grad de ocupare pentru medici și cabinete — săptămâna curentă</p>
      </div>

      {error && <p className="text-red-500 mb-4">{error}</p>}

      {loading ? (
        <p className="text-gray-500">Se încarcă raportul...</p>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Card>
              <p className="text-sm text-gray-500">Grad mediu de ocupare — Medici</p>
              <RateBar percentage={doctorsAvgOccupancy} className="mt-2" />
            </Card>
            <Card>
              <p className="text-sm text-gray-500">Grad mediu de ocupare — Cabinete</p>
              <RateBar percentage={roomsAvgOccupancy} className="mt-2" />
            </Card>
          </div>

          <OccupancyTable title="Medici" nameHeader="Medic" rows={doctors} />
          <OccupancyTable title="Cabinete" nameHeader="Cabinet" rows={rooms} />
        </>
      )}
    </div>
  );
};

export default OccupancyReportPage;
