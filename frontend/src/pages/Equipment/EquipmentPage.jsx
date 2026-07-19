import React, { useState, useEffect } from 'react';
import EquipmentTable from './EquipmentTable';
import EquipmentForm from './EquipmentForm';
import Modal from '../../components/Modal';
import Button from '../../components/Button';

import { equipmentApi } from '../../api/equipment';
import { roomApi } from '../../api/rooms';

const EquipmentPage = () => {
  // Stările pentru date
  const [equipment, setEquipment] = useState([]);
  const [equipmentTypes, setEquipmentTypes] = useState([]);
  const [rooms, setRooms] = useState([]);
  
  // Stările pentru UI
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedEquipment, setSelectedEquipment] = useState(null);
  const [loading, setLoading] = useState(true);

  // Încărcăm datele la deschiderea paginii
  useEffect(() => {
    fetchData();
  }, []);

  // Funcția care aduce TOATE datele necesare în paralel
  const fetchData = async () => {
    try {
      setLoading(true);
      // Folosim Promise.all pentru a face cele 3 request-uri simultan (e mult mai rapid)
      const [eqData, typesData, roomsData] = await Promise.all([
        equipmentApi.getAll(),
        equipmentApi.getTypes(),
        roomApi.getAll()
      ]);
      
      setEquipment(eqData);
      setEquipmentTypes(typesData);
      setRooms(roomsData);
    } catch (error) {
      console.error("Eroare la încărcarea datelor:", error);
    } finally {
      setLoading(false);
    }
  };

  // Handler pentru butonul "Adaugă Echipament"
  const handleAdd = () => {
    setSelectedEquipment(null); // Curățăm selecția pentru a avea un formular gol
    setIsModalOpen(true);
  };

  // Handler pentru butonul de "Editează" din tabel
  const handleEdit = (item) => {
    setSelectedEquipment(item); // Punem datele echipamentului selectat în formular
    setIsModalOpen(true);
  };

  // Handler pentru trimiterea datelor către backend
  const handleSave = async (formData) => {
    try {
      const dataToSave = {
        ...formData,
        equipmentTypeId: formData.equipmentTypeId,
        roomId: formData.roomId || null
      };

      if (selectedEquipment) {
        // Dacă aveam un echipament selectat, facem UPDATE
        await equipmentApi.update(selectedEquipment.id, dataToSave);
      } else {
        // Dacă nu, facem CREATE
        await equipmentApi.create(dataToSave);
      }
      
      setIsModalOpen(false); // Închidem modalul
      fetchData();           // Reîncărcăm tabelul cu noile date
    } catch (error) {
      console.error("Eroare la salvarea echipamentului:", error);
    }
  };

  return (
    <div className="p-6 max-w-7xl mx-auto">
      {/* Header-ul paginii */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Administrare Echipamente</h1>
          <p className="text-gray-500 text-sm mt-1">Gestionează inventarul de echipamente și alocarea lor</p>
        </div>
        <Button onClick={handleAdd} variant="primary">
          + Adaugă Echipament
        </Button>
      </div>

      {/* Zona principală (Loading sau Tabel) */}
      {loading ? (
        <div className="text-center py-10 text-gray-500">
          <p>Încărcare date echipamente...</p>
        </div>
      ) : (
        <EquipmentTable 
          equipment={equipment} 
          equipmentTypes={equipmentTypes} 
          rooms={rooms} 
          onEdit={handleEdit} 
        />
      )}

      {/* Fereastra Modală cu Formularul */}
      <Modal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        title={selectedEquipment ? "Editează Echipament" : "Adaugă Echipament"}
      >
        <EquipmentForm 
          initialData={selectedEquipment} 
          equipmentTypes={equipmentTypes} 
          rooms={rooms} 
          onSave={handleSave} 
          onCancel={() => setIsModalOpen(false)} 
        />
      </Modal>
    </div>
  );
};

export default EquipmentPage;