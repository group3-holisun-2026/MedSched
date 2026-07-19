import apiClient from '../services/apiClient';
export const doctorApi = {
  getAll: async () => {
    const response = await apiClient.get('/doctors');
    return response.data;
  },
  
  getById: async (id) => {
    const response = await apiClient.get(`/doctors/${id}`);
    return response.data;
  },
  
  create: async (data) => {
    const response = await apiClient.post('/doctors', data);
    return response.data;
  },
  
  update: async (id, data) => {
    const response = await apiClient.put(`/doctors/${id}`, data);
    return response.data;
  },
  
  delete: async (id) => {
    const response = await apiClient.delete(`/doctors/${id}`);
    return response.data;
  }
};
