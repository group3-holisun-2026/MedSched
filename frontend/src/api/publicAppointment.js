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

// PublicAppointmentController mapeaza confirm/cancel pe POST, nu pe PATCH — cu PATCH ambele
// actiuni se intorceau cu 405 Method Not Allowed.
export const publicAppointmentApi = {
    getDetails: (token) => publicApiClient.get(`/appointments/${token}`).then(res => res.data),
    confirm: (token) => publicApiClient.post(`/appointments/${token}/confirm`).then(res => res.data),
    cancel: (token) => publicApiClient.post(`/appointments/${token}/cancel`).then(res => res.data)
};