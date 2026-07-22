const BASE_URL = "http://localhost:8080/api";

// ---- PATIENTS ----

export async function getPatientsRequest(accessToken, search = "") {
    const params = search ? `?search=${encodeURIComponent(search)}` : "";
    const response = await fetch(`${BASE_URL}/patients${params}`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error("Nu s-au putut obtine pacientii");
    }

    return response.json(); // PatientResponse[]
}

export async function getPatientByIdRequest(accessToken, id) {
    const response = await fetch(`${BASE_URL}/patients/${id}`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error("Pacientul nu a fost gasit");
    }

    return response.json(); // PatientResponse
}

export async function quickCreatePatientRequest(accessToken, { firstName, lastName, phone }) {
    const response = await fetch(`${BASE_URL}/patients`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify({ firstName, lastName, phone }),
    });

    if (!response.ok) {
        throw new Error("Nu s-a putut crea pacientul");
    }

    return response.json(); // PatientResponse (201)
}

export async function updatePatientRequest(accessToken, id, patientData) {
    const response = await fetch(`${BASE_URL}/patients/${id}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(patientData),
    });

    if (!response.ok) {
        throw new Error("Nu s-a putut actualiza pacientul");
    }

    return response.json(); // PatientResponse
}

export async function getIncompletePatientsRequest(accessToken, { search = "", sort = "createdAt,asc" } = {}) {
    const params = new URLSearchParams();
    if (search) params.append("search", search);
    params.append("sort", sort);

    const response = await fetch(`${BASE_URL}/patients/incomplete?${params.toString()}`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error("Nu s-au putut obtine pacientii incompleti");
    }

    return response.json(); // Page<PatientResponse>
}

// ---- ADAPTOR pentru compatibilitate cu AppointmentForm.jsx (stil patientApi.getAll()/.create()) ----

export const patientApi = {
    getAll: async () => {
        const patients = await getPatientsRequest(localStorage.getItem('accessToken'));
        return patients.map((p) => ({ ...p, name: `${p.firstName} ${p.lastName}` }));
    },
    create: async ({ name }) => {
        const [firstName, ...rest] = name.trim().split(' ');
        const lastName = rest.join(' ') || firstName;
        const created = await quickCreatePatientRequest(localStorage.getItem('accessToken'), {
            firstName,
            lastName,
            phone: '',
        });
        return { ...created, name: `${created.firstName} ${created.lastName}` };
    },
};