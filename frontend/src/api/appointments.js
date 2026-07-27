import apiClient from '../services/apiClient';

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

    getCalendarAppointments: async (params) => {
        const response = await apiClient.get('/appointments/calendar', { params });
        return response.data;
    },

    confirm: async (id) => {
        const response = await apiClient.patch(`/appointments/${id}/confirm`);
        return response.data;
    },

    checkIn: async (id) => {
        const response = await apiClient.patch(`/appointments/${id}/check-in`);
        return response.data;
    },

    noShow: async (id) => {
        const response = await apiClient.patch(`/appointments/${id}/no-show`);
        return response.data;
    },

    complete: async (id) => {
        const response = await apiClient.patch(`/appointments/${id}/complete`);
        return response.data;
    },
};