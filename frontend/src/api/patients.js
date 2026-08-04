import apiClient from "../services/apiClient";

// Token-ul e pus de interceptorul de request din apiClient, care il reimprospateaza singur
// pe 401 — de aceea niciuna dintre functiile de aici nu mai primeste accessToken.

// ---- PATIENTS ----

export async function getPatientsRequest(search = "") {
    try {
        const response = await apiClient.get("/patients", {
            params: search ? { search } : undefined,
        });
        return response.data; // PatientResponse[]
    } catch {
        throw new Error("Nu s-au putut obtine pacientii");
    }
}

export async function getPatientByIdRequest(id) {
    try {
        const response = await apiClient.get(`/patients/${id}`);
        return response.data; // PatientResponse
    } catch {
        throw new Error("Pacientul nu a fost gasit");
    }
}

export async function quickCreatePatientRequest({ firstName, lastName, phone, email }) {
    try {
        // Emailul e optional, dar `""` pica pe @Email in backend — trimitem campul doar daca a fost completat.
        const response = await apiClient.post("/patients", {
            firstName,
            lastName,
            phone,
            email: email || undefined,
        });
        return response.data; // PatientResponse (201)
    } catch {
        throw new Error("Nu s-a putut crea pacientul");
    }
}

export async function updatePatientRequest(id, patientData) {
    try {
        const response = await apiClient.put(`/patients/${id}`, patientData);
        return response.data; // PatientResponse
    } catch {
        throw new Error("Nu s-a putut actualiza pacientul");
    }
}

export async function getIncompletePatientsRequest({ search = "", sort = "createdAt,asc" } = {}) {
    try {
        const params = { sort };
        if (search) params.search = search;

        const response = await apiClient.get("/patients/incomplete", { params });
        return response.data; // Page<PatientResponse>
    } catch {
        throw new Error("Nu s-au putut obtine pacientii incompleti");
    }
}

// ---- FISE DE CONSULTATIE (istoricul clinic al pacientului) ----

/**
 * Id-urile pacientilor care au cel putin o fisa. Endpoint-ul e DOCTOR/ADMIN (F-202), deci
 * pentru RECEPTION intoarce 403 — apelantul trateaza asta ca "niciun buton de fise", nu ca eroare.
 */
export async function getPatientsWithRecordsRequest() {
    const response = await apiClient.get("/patients/with-records");
    return response.data; // UUID[]
}

export async function getPatientRecordsRequest(patientId) {
    try {
        const response = await apiClient.get(`/patients/${patientId}/records`);
        return response.data; // PatientRecordSummary[]
    } catch {
        throw new Error("Nu s-au putut obtine fisele pacientului");
    }
}

// ---- ADAPTOR pentru compatibilitate cu AppointmentForm.jsx (stil patientApi.getAll()/.create()) ----

export const patientApi = {
    getAll: async () => {
        const patients = await getPatientsRequest();
        return patients.map((p) => ({ ...p, name: `${p.firstName} ${p.lastName}` }));
    },
    create: async ({ name, phone, email }) => {
        const [firstName, ...rest] = name.trim().split(' ');
        const lastName = rest.join(' ') || firstName;
        const created = await quickCreatePatientRequest({
            firstName,
            lastName,
            phone,
            email,
        });
        return { ...created, name: `${created.firstName} ${created.lastName}` };
    },
};
