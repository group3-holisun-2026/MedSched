import React, { useState, useEffect, useMemo } from 'react';
import { toast } from 'sonner';
import Button from '../../components/Button';
import Input from '../../components/Input';

// Importurile catre API-uri
import { serviceApi } from '../../api/services';
import { patientApi } from '../../api/patients';
import { doctorApi } from '../../api/doctors';
import { roomApi } from '../../api/rooms';
import { appointmentApi } from '../../api/appointments';

const AppointmentForm = ({ initialData, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    patientId: '',
    doctorId: '',
    roomId: '',
    serviceId: '',
    startTime: '',
    notes: ''
  });

  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);

  const [patientSearch, setPatientSearch] = useState('');
  const [showNewPatientForm, setShowNewPatientForm] = useState(false);
  const [newPatientName, setNewPatientName] = useState('');

  useEffect(() => {
    const fetchAllData = async () => {
      try {
        setLoading(true);
        const [patientsRes, docsRes, roomsRes, servRes] = await Promise.all([
          patientApi.getAll().catch(() => [{id: 1, name: 'Ion Popescu', cnp: '1234567890123'}]),
          doctorApi.getAll().catch(() => [{id: 1, firstName: 'Andrei', lastName: 'Ionescu'}]),
          roomApi.getAll().catch(() => [{id: 1, name: 'Cabinet 1'}]),
          serviceApi.getAll().catch(() => [{id: 1, name: 'Consultație Generală', defaultDurationMinutes: 30}])
        ]);
        
        setPatients(patientsRes);
        setDoctors(docsRes);
        setRooms(roomsRes);
        setServices(servRes);

        if (initialData) {
          setFormData({
            patientId: initialData.patientId || '',
            doctorId: initialData.doctorId || '',
            roomId: initialData.roomId || '',
            serviceId: initialData.serviceId || '',
            startTime: initialData.startTime ? initialData.startTime.substring(0, 16) : '',
            notes: initialData.notes || ''
          });
        }
      } catch (error) {
        toast.error("A apărut o problemă la preluarea datelor. Vă rugăm să reîncercați.");
      } finally {
        setLoading(false);
      }
    };

    fetchAllData();
  }, [initialData]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handlePatientSelect = (e) => {
    const val = e.target.value;
    if (val === 'NEW') {
      setShowNewPatientForm(true);
      setFormData(prev => ({ ...prev, patientId: '' }));
    } else {
      setShowNewPatientForm(false);
      setFormData(prev => ({ ...prev, patientId: val }));
    }
  };

  const calculateEndTime = () => {
    if (!formData.startTime || !formData.serviceId) return '';
    
    const service = services.find(s => s.id.toString() === formData.serviceId.toString());
    if (!service || !service.defaultDurationMinutes) return '';
    
    const start = new Date(formData.startTime);
    const end = new Date(start.getTime() + service.defaultDurationMinutes * 60000);
    
    return end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      let finalPatientId = formData.patientId;

      if (showNewPatientForm) {
        const newPatient = await patientApi.create({ name: newPatientName }); 
        finalPatientId = newPatient.id;
      }

      const payload = {
        patientId: finalPatientId, 
        doctorId: formData.doctorId,
        roomId: formData.roomId,
        serviceId: formData.serviceId,
        startTime: new Date(formData.startTime).toISOString(),
        notes: formData.notes
      };

      if (initialData) {
        await appointmentApi.update(initialData.id, payload);
        toast.success("Programarea a fost actualizată cu succes.");
      } else {
        await appointmentApi.create(payload);
        toast.success("Programarea a fost înregistrată cu succes în sistem.");
      }
      
      onSave && onSave();
    } catch (error) {
      if (error.response?.status === 409) {
        toast.error(error.response.data?.message || "Conflict de programare: Medicul sau cabinetul selectat este indisponibil în acest interval.");
      } else {
        toast.error("Înregistrarea programării a eșuat. Vă rugăm să verificați datele și să reîncercați.");
      }
    }
  };

  const filteredPatients = useMemo(() => {
    return patients.filter(p => p.name?.toLowerCase().includes(patientSearch.toLowerCase()) || p.cnp?.includes(patientSearch));
  }, [patients, patientSearch]);

  if (loading) return <div className="p-8 text-center text-gray-500 font-medium">Se preiau informațiile din sistem...</div>;

  return (
    <form onSubmit={handleSubmit} className="space-y-4 p-2">
      {/* 1. PACIENT */}
      <div className="bg-gray-50 p-4 rounded-lg border border-gray-100">
        <label className="block text-sm font-semibold text-gray-700 mb-1">
          Pacient <span className="text-red-500">*</span>
        </label>
        
        {!showNewPatientForm ? (
          <div className="space-y-2">
            <input 
              type="text" 
              placeholder="Introduceți numele sau CNP-ul pentru filtrare..." 
              value={patientSearch}
              onChange={(e) => setPatientSearch(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            />
            <select
              required
              value={formData.patientId}
              onChange={handlePatientSelect}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            >
              <option value="" disabled>-- Vă rugăm să selectați pacientul --</option>
              {filteredPatients.map(p => (
                <option key={p.id} value={p.id}>{p.name} {p.cnp ? `(${p.cnp})` : ''}</option>
              ))}
              <option value="NEW" className="font-bold text-blue-600">+ Înregistrare pacient nou</option>
            </select>
          </div>
        ) : (
          <div className="space-y-2">
            <input 
              type="text" 
              required
              placeholder="Numele complet al pacientului" 
              value={newPatientName}
              onChange={(e) => setNewPatientName(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            />
            <button type="button" onClick={() => setShowNewPatientForm(false)} className="text-sm text-blue-600 hover:text-blue-800 hover:underline transition-colors">
              Anulare înregistrare și revenire la căutare
            </button>
          </div>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4">
        {/* 2. MEDIC */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">
            Medic <span className="text-red-500">*</span>
          </label>
          <select
            name="doctorId"
            value={formData.doctorId}
            onChange={handleChange}
            required
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
          >
            <option value="" disabled>-- Selectați medicul curant --</option>
            {doctors.map(d => (
              <option key={d.id} value={d.id}>Dr. {d.firstName} {d.lastName}</option>
            ))}
          </select>
        </div>

        {/* 3. CABINET */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">
            Cabinet <span className="text-red-500">*</span>
          </label>
          <select
            name="roomId"
            value={formData.roomId}
            onChange={handleChange}
            required
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
          >
            <option value="" disabled>-- Selectați cabinetul --</option>
            {rooms.map(r => (
              <option key={r.id} value={r.id}>{r.name}</option>
            ))}
          </select>
        </div>
      </div>

      {/* 4. SERVICIU */}
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-1">
          Serviciu <span className="text-red-500">*</span>
        </label>
        <select
          name="serviceId"
          value={formData.serviceId}
          onChange={handleChange}
          required
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
        >
          <option value="" disabled>-- Selectați serviciul medical --</option>
          {services.map(s => (
            <option key={s.id} value={s.id}>{s.name} ({s.defaultDurationMinutes} min)</option>
          ))}
        </select>
      </div>

      {/* 5. DATA SI ORA START */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">
            Dată și Oră Start <span className="text-red-500">*</span>
          </label>
          <input
            type="datetime-local"
            name="startTime"
            value={formData.startTime}
            onChange={handleChange}
            required
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
          />
        </div>
        
        {/* PREVIEW ORA SFARSIT */}
        <div>
          <label className="block text-sm font-semibold text-gray-500 mb-1">
            Ora estimată a finalizării
          </label>
          <input
            type="text"
            readOnly
            value={calculateEndTime()}
            placeholder="Calculată automat de sistem"
            className="w-full rounded-md border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-500 cursor-not-allowed select-none"
          />
        </div>
      </div>

      {/* 6. NOTITE */}
      <div>
        <label className="block text-sm font-semibold text-gray-700 mb-1">
          Observații suplimentare <span className="text-gray-400 font-normal">(Opțional)</span>
        </label>
        <textarea
          name="notes"
          value={formData.notes}
          onChange={handleChange}
          rows="3"
          placeholder="Introduceți orice informații relevante pentru această programare..."
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div className="flex justify-end gap-3 pt-4 border-t border-gray-100 mt-6">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            Renunță
          </Button>
        )}
        <Button type="submit" variant="primary">
          Salvare Programare
        </Button>
      </div>
    </form>
  );
};

export default AppointmentForm;