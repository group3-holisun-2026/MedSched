import apiClient from '../services/apiClient';

export const consultationRecordApi = {
  getRecord: async (id) => {
    const response = await apiClient.get(`/consultation-records/${id}`);
    return response.data;
  },

  saveRecord: async (id, data) => {
    const response = await apiClient.put(`/consultation-records/${id}`, data);
    return response.data;
  }
};