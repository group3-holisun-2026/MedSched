import React, { useState, useEffect } from 'react';
import RoomTable from './RoomTable';
import RoomForm from './RoomForm';
import { roomApi } from '../../api/rooms';
import Modal from '../../components/Modal';
import Button from '../../components/Button';
const RoomsPage = () => {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState(null);
  const fetchRooms = async () => {
    try {
      setLoading(true);
      const data = await roomApi.getAll();
      setRooms(data);
    } catch (err) {
      console.error('Failed to load rooms', err);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    fetchRooms();
  }, []);
  const handleAddRoom = () => {
    setEditingRoom(null);
    setIsModalOpen(true);
  };
  const handleEditRoom = (room) => {
    setEditingRoom(room);
    setIsModalOpen(true);
  };
  const handleSaveRoom = async (roomData) => {
    try {
      if (editingRoom) {
        await roomApi.update(editingRoom.id, roomData);
      } else {
        await roomApi.create(roomData);
      }
      setIsModalOpen(false);
      fetchRooms();
    } catch (err) {
      console.error(err);
      alert('Failed to save room.');
    }
  };
  return (
    <div className="p-8 min-h-screen" style={{ backgroundColor: '#f8fafc' }}>
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Cabinete</h1>
          <p className="text-sm text-gray-500 mt-1">Gestiune cabinete și săli de tratament</p>
        </div>
        
        <Button onClick={handleAddRoom} variant="primary" style={{ backgroundColor: '#0ea5e9' }}>
          + Adaugă Cabinet
        </Button>
      </div>
      {loading ? (
        <p className="text-gray-500">Se încarcă datele...</p>
      ) : (
        <RoomTable rooms={rooms} onEdit={handleEditRoom} />
      )}
      <Modal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        title={editingRoom ? 'Editare Cabinet' : 'Cabinet Nou'}
      >
        <RoomForm 
          initialData={editingRoom} 
          onSave={handleSaveRoom} 
          onCancel={() => setIsModalOpen(false)}
        />
      </Modal>
    </div>
  );
};
export default RoomsPage;
