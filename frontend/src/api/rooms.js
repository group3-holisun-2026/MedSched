import apiClient from '../services/apiClient';
export const roomApi = {
  getAll: async () => {
    const response = await apiClient.get('/rooms');
    return response.data;
  },
  
  getById: async (id) => {
    const response = await apiClient.get(`/rooms/${id}`);
    return response.data;
  },
  
  create: async (data) => {
    const response = await apiClient.post('/rooms', data);
    return response.data;
  },
  
  update: async (id, data) => {
    const response = await apiClient.put(`/rooms/${id}`, data);
    return response.data;
  },
  
  delete: async (id) => {
    const response = await apiClient.delete(`/rooms/${id}`);
    return response.data;
  }
};
