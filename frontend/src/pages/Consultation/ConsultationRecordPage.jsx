import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext'; 
import apiClient from '../../services/apiClient';
import { consultationRecordApi } from '../../api/consultationRecord';
import Button from '../../components/Button';

export default function ConsultationRecordPage() {
  const { id } = useParams(); 
  const navigate = useNavigate();
  const { toast } = useToast();
  const { user } = useAuth(); 

  const [record, setRecord] = useState(null);
  const [appointment, setAppointment] = useState(null);
  const [formData, setFormData] = useState({ notes: '' }); 
  
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [currentTime, setCurrentTime] = useState(new Date());

  // 1. fetch dublu 
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [recordRes, appointmentRes] = await Promise.all([
          consultationRecordApi.getRecord(id).catch(() => null),
          apiClient.get(`/appointments/${id}`).then(res => res.data).catch(() => null)
        ]);

        if (recordRes) {
          setRecord(recordRes);
          setFormData(recordRes); 
        }
        
        if (appointmentRes) {
          setAppointment(appointmentRes);
        }
      } catch (error) {
        toast({ type: 'error', message: 'Eroare la încărcarea datelor.' });
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id, toast]);

  // 2. actualizare timp banner 
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentTime(new Date());
    }, 60000);
    return () => clearInterval(interval);
  }, []);


  const isDoctorAssignedOrAdmin = user?.role === 'ADMIN' || user?.doctorId === appointment?.doctorId;
  const isLocked = record?.locked === true;
  const showGraceBanner = appointment?.status === 'COMPLETED' && !isLocked;

  const getGraceLimitTime = () => {
    if (!appointment?.completedAt) return '';
    const limitDate = new Date(new Date(appointment.completedAt).getTime() + 30 * 60000);
    return limitDate.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' });
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // 3. salvare fisa
  const handleSaveRecord = async () => {
    try {
      setIsSaving(true);
      const response = await consultationRecordApi.saveRecord(id, formData);
      setRecord(response);
      toast({ type: 'success', message: 'Fișa a fost salvată.' });
      return true; 
    } catch (error) {
      if (error.response?.status === 403) {
        toast({ type: 'error', message: 'Nu aveți drepturi asupra acestei fișe.' });
      } else {
        toast({ type: 'error', message: 'Eroare la salvarea fișei.' });
      }
      return false;
    } finally {
      setIsSaving(false);
    }
  };

  // 4. finalizare consulatie
  const handleFinalize = async () => {
    const isSaved = await handleSaveRecord();
    if (!isSaved) return;

    try {
      setIsSaving(true);
      await apiClient.patch(`/appointments/${id}/complete`);
      toast({ type: 'success', message: 'Consultația a fost finalizată!' });
      
      setAppointment(prev => ({
        ...prev,
        status: 'COMPLETED',
        completedAt: new Date().toISOString()
      }));
    } catch (error) {
      if (error.response?.status === 403) {
        toast({ type: 'error', message: 'Nu aveți drepturi pentru a finaliza.' });
      } else {
        toast({ type: 'error', message: 'Eroare la finalizare.' });
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (loading) return <div className="p-8 text-center">Se încarcă...</div>;

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      <div className="flex justify-between items-center border-b pb-4">
        <h1 className="text-2xl font-bold text-gray-800">Fișă Consultație</h1>
        <Button variant="outline" onClick={() => navigate(-1)}>Înapoi</Button>
      </div>

      {/* BANNER UNIC */}
      {isLocked ? (
        <div className="bg-red-50 border-l-4 border-red-500 p-4">
          <p className="text-sm text-red-700 font-medium">Fișa este blocată și nu mai poate fi editată.</p>
        </div>
      ) : showGraceBanner ? (
        <div className="bg-blue-50 border-l-4 border-blue-500 p-4">
          <p className="text-sm text-blue-700 font-medium">
            Consultația e finalizată. Fișa rămâne editabilă până la ora {getGraceLimitTime()}.
          </p>
        </div>
      ) : null}

      {/* FORMULAR FIȘĂ */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Notițe</label>
          <textarea
            name="notes"
            value={formData.notes || ''}
            onChange={handleInputChange}
            disabled={isLocked || isSaving}
            rows="6"
            className="w-full rounded-md border border-gray-300 px-3 py-2"
          />
        </div>
      </div>

      {/* BUTOANE */}
      <div className="flex justify-end gap-4 pt-4">
        <Button onClick={handleSaveRecord} disabled={isLocked || isSaving} variant="outline">
          Salvează Modificări
        </Button>

        {appointment?.status === 'IN_PROGRESS' && isDoctorAssignedOrAdmin && (
          <Button onClick={handleFinalize} disabled={isSaving} variant="primary">
            Finalizează consultația
          </Button>
        )}
      </div>
    </div>
  );
}