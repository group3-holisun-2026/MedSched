import React from 'react';
import Button from '../../components/Button';
const DoctorTable = ({ doctors, onEdit, onDelete }) => {
  if (!doctors || doctors.length === 0) {
    return <p>No doctors found.</p>;
  }
  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '20px' }}>
        <thead>
          <tr style={{ backgroundColor: '#f2f2f2', textAlign: 'left' }}>
            <th style={{ padding: '12px', borderBottom: '1px solid #ddd' }}>Name</th>
            <th style={{ padding: '12px', borderBottom: '1px solid #ddd' }}>Speciality</th>
            <th style={{ padding: '12px', borderBottom: '1px solid #ddd' }}>Consultation Duration (min)</th>
            <th style={{ padding: '12px', borderBottom: '1px solid #ddd' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {doctors.map((doctor) => (
            <tr key={doctor.id} style={{ borderBottom: '1px solid #ddd' }}>
              <td style={{ padding: '12px' }}>{doctor.fullName || 'N/A'}</td>
              <td style={{ padding: '12px' }}>{doctor.speciality}</td>
              <td style={{ padding: '12px' }}>{doctor.standardConsultationDurationMinutes}</td>
              <td style={{ padding: '12px' }}>
                <Button onClick={() => onEdit(doctor)} style={{ marginRight: '10px' }}>
                  Edit
                </Button>
                <Button onClick={() => onDelete(doctor.id)} style={{ backgroundColor: '#e74c3c' }}>
                  Delete
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
export default DoctorTable;
