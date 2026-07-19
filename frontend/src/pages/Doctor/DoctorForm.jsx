import React, { useState, useEffect } from 'react';
import Input from '../../components/Input';
import Button from '../../components/Button';
const DAYS_OF_WEEK = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
const defaultSchedule = Array.from({ length: 7 }, (_, i) => ({
  dayOfWeek: i + 1,
  active: false,
  startTime: '08:00',
  endTime: '16:00'
}));
const DoctorForm = ({ initialData, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    userId: '',
    speciality: '',
    standardConsultationDurationMinutes: 30,
    schedule: defaultSchedule,
  });
  useEffect(() => {
    if (initialData) {
      // Merge initial schedule with default schedule
      const mergedSchedule = defaultSchedule.map(ds => {
        const existing = initialData.schedule?.find(s => s.dayOfWeek === ds.dayOfWeek);
        if (existing) {
          return {
            ...existing,
            active: true,
            // Format time properly (e.g., "08:00:00" to "08:00")
            startTime: existing.startTime.substring(0, 5),
            endTime: existing.endTime.substring(0, 5)
          };
        }
        return ds;
      });
      setFormData({
        ...initialData,
        userId: initialData.userId || '',
        speciality: initialData.speciality || '',
        standardConsultationDurationMinutes: initialData.standardConsultationDurationMinutes || 30,
        schedule: mergedSchedule
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
  const handleScheduleChange = (index, field, value) => {
    setFormData(prev => {
      const newSchedule = [...prev.schedule];
      newSchedule[index] = { ...newSchedule[index], [field]: value };
      return { ...prev, schedule: newSchedule };
    });
  };
  const handleSubmit = (e) => {
    e.preventDefault();
    
    // Prepare payload
    const payload = {
      userId: formData.userId,
      speciality: formData.speciality,
      standardConsultationDurationMinutes: parseInt(formData.standardConsultationDurationMinutes, 10),
      schedule: formData.schedule
        .filter(s => s.active)
        .map(s => ({
          dayOfWeek: s.dayOfWeek,
          startTime: s.startTime + ':00', // Backend might expect HH:MM:SS
          endTime: s.endTime + ':00'
        }))
    };
    onSave(payload);
  };
  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium mb-1">User ID (UUID)</label>
          <Input 
            name="userId" 
            value={formData.userId} 
            onChange={handleChange} 
            required 
            placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000"
            disabled={!!initialData} // Cannot change userId after creation
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Speciality</label>
          <Input 
            name="speciality" 
            value={formData.speciality} 
            onChange={handleChange} 
            required 
            placeholder="e.g. Cardiology"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Consultation Duration (min)</label>
          <Input 
            name="standardConsultationDurationMinutes" 
            type="number" 
            value={formData.standardConsultationDurationMinutes} 
            onChange={handleChange} 
            required 
            min="1"
          />
        </div>
      </div>
      <div>
        <h3 className="text-lg font-medium mb-4">Weekly Schedule Matrix</h3>
        <div className="space-y-3">
          {formData.schedule.map((day, index) => (
            <div key={day.dayOfWeek} className="flex items-center gap-4 bg-slate-50 p-3 rounded-md border">
              <div className="w-32 flex items-center gap-2">
                <input 
                  type="checkbox" 
                  checked={day.active} 
                  onChange={(e) => handleScheduleChange(index, 'active', e.target.checked)}
                  id={`day-${day.dayOfWeek}`}
                />
                <label htmlFor={`day-${day.dayOfWeek}`} className="text-sm font-medium">
                  {DAYS_OF_WEEK[index]}
                </label>
              </div>
              
              <div className="flex items-center gap-2 flex-1">
                <Input 
                  type="time" 
                  value={day.startTime} 
                  onChange={(e) => handleScheduleChange(index, 'startTime', e.target.value)}
                  disabled={!day.active}
                  className="w-32"
                />
                <span className="text-sm text-gray-500">to</span>
                <Input 
                  type="time" 
                  value={day.endTime} 
                  onChange={(e) => handleScheduleChange(index, 'endTime', e.target.value)}
                  disabled={!day.active}
                  className="w-32"
                />
              </div>
            </div>
          ))}
        </div>
      </div>
      <div className="flex justify-end gap-3 pt-4 border-t">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" variant="primary">
          Save Doctor
        </Button>
      </div>
    </form>
  );
};
export default DoctorForm;
