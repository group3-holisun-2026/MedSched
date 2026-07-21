import apiClient from '../services/apiClient';

export const serviceApi = {
    getAll: async () => {
        const response = await apiClient.get('/services');
        return response.data;
    },

    getById: async (id) => {
        const response = await apiClient.get(`/services/${id}`);
        return response.data;
    }
};