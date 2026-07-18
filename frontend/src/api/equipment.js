import apiClient from '../services/apiClient';
export const equipmentApi = {
  getAll: async () => {
    const response = await apiClient.get('/equipment');
    return response.data;
  },
  
  getById: async (id) => {
    const response = await apiClient.get(`/equipment/${id}`);
    return response.data;
  },
  
  create: async (data) => {
    const response = await apiClient.post('/equipment', data);
    return response.data;
  },
  
  update: async (id, data) => {
    const response = await apiClient.put(`/equipment/${id}`, data);
    return response.data;
  },
  
  delete: async (id) => {
    const response = await apiClient.delete(`/equipment/${id}`);
    return response.data;
  },
  getTypes: async () => {
    const response = await apiClient.get('/equipment-types');
    return response.data;
  }
};
