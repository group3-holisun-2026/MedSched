import apiClient from '../services/apiClient';

const DOCTOR_IDS_FORMAT = 'repeated'; 

export const appointmentApi = {
    create: async (payload) => {
        const response = await apiClient.post('/appointments', payload);
        return response.data;
    },

    update: async (id, payload) => {
        const response = await apiClient.put(`/appointments/${id}`, payload);
        return response.data;
    },

    cancel: async (id) => {
        const response = await apiClient.patch(`/appointments/${id}/cancel`);
        return response.data;
    },

    getById: async (id) => {
        const response = await apiClient.get(`/appointments/${id}`);
        return response.data;
    },

    getCalendarAppointments: async ({ from, to, doctorIds, roomId } = {}) => {
        const params = { from, to };

        if (roomId) {
        params.roomId = roomId;
        } else if (doctorIds && doctorIds.length > 0) {
        params.doctorIds = DOCTOR_IDS_FORMAT === 'csv' ? doctorIds.join(',') : doctorIds;
        }

        const response = await apiClient.get('/appointments/calendar', { params });
        return response.data; // CalendarAppointmentResponse[]
    },
};

const DOCTOR_IDS_FORMAT = 'repeated'; 

export default appointmentApi;
