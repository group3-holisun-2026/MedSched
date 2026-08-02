import React from 'react';
import RateBar from '../../components/report/RateBar';
import { formatRate } from '../../utils/reportFormat';

// WeekdayNoShowRow = { dayOfWeek, total, noShows, rate } (rate e fractie 0..1).
const DAY_TRANSLATIONS = {
  MONDAY: 'Luni',
  TUESDAY: 'Marți',
  WEDNESDAY: 'Miercuri',
  THURSDAY: 'Joi',
  FRIDAY: 'Vineri',
  SATURDAY: 'Sâmbătă',
  SUNDAY: 'Duminică',
};

export default function NoShowReportByWeekdayTable({
  weekdays = [],
}) {
  if (weekdays.length === 0) {
    return (
      <div className="text-center p-4 text-gray-500">
        Nicio programare în intervalul selectat.
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
              Zi
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
          {weekdays.map((day) => (
            <tr
              key={day.dayOfWeek}
              style={{ borderBottom: '1px solid #ddd' }}
            >
              <td style={{ padding: '12px' }}>
                {DAY_TRANSLATIONS[day.dayOfWeek] ?? day.dayOfWeek}
              </td>

              <td style={{ padding: '12px' }}>
                {day.total}
              </td>

              <td style={{ padding: '12px' }}>
                {day.noShows}
              </td>

              <td style={{ padding: '12px' }}>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                  }}
                >
                  <span>
                    {formatRate(day.rate, day.total > 0)}
                  </span>

                  <RateBar
                      percentage={(day.rate ?? 0) * 100}
                      size="sm"
                      showPercentageLabel={false}
                  />
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <p
        style={{
          fontSize: '0.8rem',
          color: '#888',
          marginTop: 8,
        }}
      >
        Programările anulate din timp nu sunt contorizate.
      </p>
    </div>
  );
}