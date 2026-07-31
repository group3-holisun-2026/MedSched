import apiClient from '../services/apiClient';

export const notificationsApi = {
    list: ({ status, appointmentId, page = 0, size = 20 } = {}) => {
        const params = { page, size };
        if (status) params.status = status;
        if (appointmentId) params.appointmentId = appointmentId;

        return apiClient.get('/notifications', { params }).then((r) => r.data);
    },

    retry: (id) => apiClient.post(`/notifications/${id}/retry`).then((r) => r.data),
};