import apiClient from "../services/apiClient";

export async function getAuditLogRequest({ userId, from, to }) {
    // `user` e optional in backend: fara el, raspunsul acopera toti utilizatorii din interval.
    // Trimis gol ar fi insa un UUID invalid, deci il omitem cu totul.
    const params = { from, to };
    if (userId) params.user = userId;

    try {
        const response = await apiClient.get("/audit-log", { params });
        return response.data; // AuditLogResponse[]
    } catch (error) {
        if (error.response?.status === 403) {
            throw new Error("Jurnalul de audit este accesibil doar administratorilor");
        }
        throw new Error("Nu s-au putut obtine inregistrarile de audit");
    }
}
