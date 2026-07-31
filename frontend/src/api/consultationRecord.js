import apiClient from '../services/apiClient';

// Contract real (ConsultationController): /api/appointments/{appointmentId}/record
// — GET / POST (creare) / PUT (actualizare). Nu exista /consultation-records.
export const consultationRecordApi = {
    getByAppointmentId: async (appointmentId) => {
        const response = await apiClient.get(`/appointments/${appointmentId}/record`);
        return response.data;
    },

    create: async (appointmentId, data) => {
        const response = await apiClient.post(`/appointments/${appointmentId}/record`, data);
        return response.data;
    },

    update: async (appointmentId, data) => {
        const response = await apiClient.put(`/appointments/${appointmentId}/record`, data);
        return response.data;
    },
};
