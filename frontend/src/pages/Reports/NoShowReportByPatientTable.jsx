import React from 'react';
import RateBar from '../../components/report/RateBar'; // TODO: verify that this does exist, if not, then cry about it

const RED_THRESHOLD = 0.3;
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
              patient.noShowRate > RED_THRESHOLD &&
              patient.totalAppointments >= MIN_APPOINTMENTS_FOR_RED;

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
                  {patient.totalAppointments}
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
                      {(patient.noShowRate * 100).toFixed(1)}%
                    </span>

                    <RateBar
                      rate={patient.noShowRate}
                      maxWidth={80}
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