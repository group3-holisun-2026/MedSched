import React, { useState, useEffect } from 'react';
import Input from '../../components/Input';
import Button from '../../components/Button';
const RoomForm = ({ initialData, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
  });
  useEffect(() => {
    if (initialData) {
      setFormData({
        name: initialData.name || '',
        description: initialData.description || '',
      });
    } else {
      setFormData({
        name: '',
        description: '',
      });
    }
  }, [initialData]);
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
          Nume Cabinet <span className="text-red-500">*</span>
        </label>
        <Input 
          name="name" 
          value={formData.name} 
          onChange={handleChange} 
          required 
          placeholder="ex: Cabinet 1, Sala Tratamente..."
          className="w-full"
        />
      </div>
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-1">
          Descriere
        </label>
        <textarea
          name="description"
          value={formData.description}
          onChange={handleChange}
          rows={4}
          placeholder="Adaugă detalii opționale..."
          className="w-full rounded-md border border-input bg-input-background px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
        />
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
export default RoomForm;
