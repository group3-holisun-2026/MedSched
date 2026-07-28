import apiClient from '../services/apiClient';

export const reportsApi = {
  getSalesReport: async (startDate, endDate) => {
    // Trimitem datele ca parametri de query (?startDate=...&endDate=...)
    const response = await apiClient.get('/reports/sales', {
      params: { startDate, endDate }
    });
    return response.data;
  },
  
  getOccupancyReport: async (startDate, endDate) => {
    const response = await apiClient.get('/reports/occupancy', { params: { startDate, endDate } });
    return response.data;
  },
  
  getNoShowReport: async (startDate, endDate) => {
    const response = await apiClient.get('/reports/no-show', { params: { startDate, endDate } });
    return response.data;
  }
};