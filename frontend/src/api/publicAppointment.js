import axios from 'axios';

/*
 * DECIZIE DE SPRINT (P4):
 * Am ales să folosesc o instanță axios separată, minimală, cu baseURL setat pe '/public'.
 * Motivul: Pacientul nu are token. Dacă am fi folosit `apiClient` existent, o eroare 401
 * ar fi declanșat interceptorul care face forceLogout() și redirectează abuziv spre /login.
 */
const publicApiClient = axios.create({
    baseURL: 'http://localhost:8080/public'
});

export const publicAppointmentApi = {
    getDetails: (token) => publicApiClient.get(`/appointments/${token}`).then(res => res.data),
    confirm: (token) => publicApiClient.patch(`/appointments/${token}/confirm`).then(res => res.data),
    cancel: (token) => publicApiClient.patch(`/appointments/${token}/cancel`).then(res => res.data)
};