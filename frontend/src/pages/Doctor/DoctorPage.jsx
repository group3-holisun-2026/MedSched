import React, { useState, useEffect } from 'react';
import DoctorTable from './DoctorTable';
import DoctorForm from './DoctorForm';
import { doctorApi } from '../../api/doctors';
import Modal from '../../components/Modal';
import Button from '../../components/Button';
const DoctorPage = () => {
  const [doctors, setDoctors] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingDoctor, setEditingDoctor] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const fetchDoctors = async () => {
    try {
      setLoading(true);
      const data = await doctorApi.getAll();
      setDoctors(data);
    } catch (err) {
      console.error(err);
      setError('Failed to load doctors.');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    fetchDoctors();
  }, []);
  const handleAddClick = () => {
    setEditingDoctor(null);
    setIsModalOpen(true);
  };
  const handleEditClick = (doctor) => {
    setEditingDoctor(doctor);
    setIsModalOpen(true);
  };
  const handleDeleteClick = async (id) => {
    if (window.confirm('Are you sure you want to delete this doctor?')) {
      try {
        await doctorApi.delete(id);
        fetchDoctors();
      } catch (err) {
        console.error(err);
        alert('Failed to delete doctor.');
      }
    }
  };
  const handleSave = async (doctorData) => {
    try {
      if (editingDoctor) {
        await doctorApi.update(editingDoctor.id, doctorData);
      } else {
        await doctorApi.create(doctorData);
      }
      setIsModalOpen(false);
      fetchDoctors();
    } catch (err) {
      console.error(err);
      alert('Failed to save doctor.');
    }
  };
  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Doctor Management</h1>
        <Button onClick={handleAddClick} variant="primary">
          Add New Doctor
        </Button>
      </div>
      {error && <p className="text-red-500 mb-4">{error}</p>}
      {loading ? (
        <p>Loading doctors...</p>
      ) : (
        <DoctorTable 
          doctors={doctors} 
          onEdit={handleEditClick} 
          onDelete={handleDeleteClick} 
        />
      )}
      <Modal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        title={editingDoctor ? 'Edit Doctor' : 'Add New Doctor'}
      >
        <DoctorForm 
          initialData={editingDoctor} 
          onSave={handleSave} 
          onCancel={() => setIsModalOpen(false)}
        />
      </Modal>
    </div>
  );
};
export default DoctorPage;
