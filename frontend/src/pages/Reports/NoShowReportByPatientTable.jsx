import React from 'react';
import RateBar from '../../components/report/RateBar';
import { formatRate } from '../../utils/reportFormat';

// PatientNoShowRow = { patientId, patientName, total, noShows, rate } (rate e fractie 0..1).
const RED_THRESHOLD = 0.3;
// Un pacient cu 1 programare si 1 neprezentare are rata 100%, dar nu e un "pacient problema".
// Fara pragul asta raportul ar fi rosu peste tot si ar deveni inutil.
const MIN_APPOINTMENTS_FOR_RED = 3;

export default function NoShowReportByPatientTable({
  patients = [],
  showOnlyFrequent,
}) {
  const filteredPatients = showOnlyFrequent
    ? patients.filter((p) => p.noShows >= 2)
    : patients;

  if (filteredPatients.length === 0) {
    return (
      <div className="text-center p-4 text-gray-500">
        {showOnlyFrequent
          ? 'Niciun pacient cu cel puțin 2 neprezentări în acest interval.'
          : 'Nicio programare în intervalul selectat.'}
      </div>
    );
  }

  return (
    <div style={{ overflowX: 'auto' }}>
      <table
        style={{
          width: '100%',
          borderCollapse: 'collapse',
          marginTop: '16px',
        }}
      >
        <thead>
          <tr style={{ backgroundColor: '#f2f2f2', textAlign: 'left' }}>
            <th style={{ padding: '12px', borderBottom: '1px solid #ddd' }}>
              Pacient
            </th>
            <th style={{ padding: '12px', borderBottom: '1px solid #ddd' }}>
              Programări
            </th>
            <th style={{ padding: '12px', borderBottom: '1px solid #ddd' }}>
              Neprezentări
            </th>
            <th style={{ padding: '12px', borderBottom: '1px solid #ddd' }}>
              Rată
            </th>
          </tr>
        </thead>

        <tbody>
          {filteredPatients.map((patient) => {
            const isProblematic =
              patient.rate > RED_THRESHOLD &&
              patient.total >= MIN_APPOINTMENTS_FOR_RED;

            return (
              <tr
                key={patient.patientId ?? patient.patientName}
                style={{
                  borderBottom: '1px solid #ddd',
                  backgroundColor: isProblematic ? '#fff5f5' : undefined,
                }}
              >
                <td
                  style={{
                    padding: '12px',
                    fontWeight: isProblematic ? 600 : 400,
                  }}
                >
                  {patient.patientName}
                </td>

                <td style={{ padding: '12px' }}>
                  {patient.total}
                </td>

                <td
                  style={{
                    padding: '12px',
                    color: isProblematic ? '#c0392b' : undefined,
                    fontWeight: isProblematic ? 600 : 400,
                  }}
                >
                  {patient.noShows}
                </td>

                <td style={{ padding: '12px' }}>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                    }}
                  >
                    <span
                      style={{
                        color: isProblematic ? '#c0392b' : undefined,
                        fontWeight: isProblematic ? 600 : 400,
                      }}
                    >
                      {formatRate(patient.rate, patient.total > 0)}
                    </span>

                    <RateBar
                        percentage={(patient.rate ?? 0) * 100}
                        size="sm"
                        showPercentageLabel={false}
                    />
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}