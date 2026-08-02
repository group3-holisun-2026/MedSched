import apiClient from '../services/apiClient';

// ReportController leaga datele pe `from` / `to` (@DateTimeFormat ISO.DATE), nu pe
// startDate/endDate — cu numele vechi fiecare cerere se intorcea cu 400.
// Valorile vin din <input type="date">, deci sunt deja in format yyyy-MM-dd.
export const reportsApi = {
  getSalesReport: async (from, to) => {
    const response = await apiClient.get('/reports/sales', { params: { from, to } });
    return response.data;
  },

  getOccupancyReport: async (from, to) => {
    const response = await apiClient.get('/reports/occupancy', { params: { from, to } });
    return response.data;
  },

  getNoShowReport: async (from, to) => {
    const response = await apiClient.get('/reports/no-show', { params: { from, to } });
    return response.data;
  }
};