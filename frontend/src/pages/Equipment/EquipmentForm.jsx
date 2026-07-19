import React, { useState, useEffect } from 'react';
import Input from '../../components/Input';
import Button from '../../components/Button';
const EquipmentForm = ({ initialData, equipmentTypes, rooms, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    name: '',
    equipmentTypeId: '',
    roomId: '',
  });
  useEffect(() => {
    if (initialData) {
      setFormData({
        name: initialData.name || '',
        equipmentTypeId: initialData.equipmentTypeId || '',
        roomId: initialData.roomId || '',
      });
    } else {
      setFormData({
        name: '',
        equipmentTypeId: equipmentTypes.length > 0 ? equipmentTypes[0].id : '',
        roomId: '',
      });
    }
  }, [initialData, equipmentTypes]);
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };
  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
  };
  return (
    <form onSubmit={handleSubmit} className="space-y-5 p-1">
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-1">
          Nume Echipament <span className="text-red-500">*</span>
        </label>
        <Input 
          name="name" 
          value={formData.name} 
          onChange={handleChange} 
          required 
          placeholder="ex: Ecograf Philips..."
          className="w-full"
        />
      </div>
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-1">
          Tip Echipament <span className="text-red-500">*</span>
        </label>
        <select
          name="equipmentTypeId"
          value={formData.equipmentTypeId}
          onChange={handleChange}
          required
          className="w-full rounded-md border border-input bg-input-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="" disabled>Selectează un tip</option>
          {equipmentTypes.map(type => (
            <option key={type.id} value={type.id}>{type.name}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-1">
          Cabinet Asignat (Opțional)
        </label>
        <select
          name="roomId"
          value={formData.roomId}
          onChange={handleChange}
          className="w-full rounded-md border border-input bg-input-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="">Nespecificat / Echipament Mobil</option>
          {rooms.map(room => (
            <option key={room.id} value={room.id}>{room.name}</option>
          ))}
        </select>
      </div>
      <div className="flex justify-end gap-3 pt-4 mt-2">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            Anulează
          </Button>
        )}
        <Button type="submit" variant="primary">
          Salvează
        </Button>
      </div>
    </form>
  );
};
export default EquipmentForm;