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
    const response = await apiClient.delete(`/appointments/${id}`);
    return response.data;
  },

  getById: async (id) => {
    const response = await apiClient.get(`/appointments/${id}`);
    return response.data;
  },

  getCalendarAppointments: async (params) => {
    const response = await apiClient.get('/appointments/calendar', { params });
    return response.data;
  }
};