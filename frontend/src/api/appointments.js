import apiClient from '../services/apiClient';

export async function getCalendarAppointments({ from, to, doctorIds, roomId } = {}) {
    const params = { from, to };
    if (doctorIds && doctorIds.length > 0) {
        params.doctorIds = doctorIds;
    }
    if (roomId) {
        params.roomId = roomId;
    }

    const response = await apiClient.get('/appointments/calendar', { params });
    return response.data; // CalendarAppointmentResponse[]
}

export async function getAppointmentById(id) {
    const response = await apiClient.get(`/appointments/${id}`);
    return response.data; // AppointmentResponse
}

export async function createAppointment({ patientId, doctorId, roomId, serviceId, startTime, notes }) {
    const response = await apiClient.post('/appointments', {
        patientId,
        doctorId,
        roomId,
        serviceId,
        startTime,
        notes,
    });
    return response.data; // AppointmentResponse (201)
}

export async function updateAppointment(id, { patientId, doctorId, roomId, serviceId, startTime, notes }) {
    const response = await apiClient.put(`/appointments/${id}`, {
        patientId,
        doctorId,
        roomId,
        serviceId,
        startTime,
        notes,
    });
    return response.data; // AppointmentResponse
}

export async function cancelAppointment(id) {
    const response = await apiClient.patch(`/appointments/${id}/cancel`);
    return response.data; // AppointmentResponse
}