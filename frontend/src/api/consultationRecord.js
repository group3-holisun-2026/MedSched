const BASE_URL = "http://localhost:8080/api";

export async function getConsultationRecordRequest(accessToken, appointmentId) {
    const response = await fetch(`${BASE_URL}/appointments/${appointmentId}/record`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error("Nu s-a putut obtine fisa de consultatie");
    }

    return response.json(); // ConsultationRecordResponse
}

export async function createConsultationRecordRequest(accessToken, appointmentId, recordData) {
    const response = await fetch(`${BASE_URL}/appointments/${appointmentId}/record`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(recordData),
    });

    if (!response.ok) {
        throw new Error("Nu s-a putut crea fisa de consultatie");
    }

    return response.json(); // ConsultationRecordResponse (201)
}

export async function updateConsultationRecordRequest(accessToken, appointmentId, recordData) {
    const response = await fetch(`${BASE_URL}/appointments/${appointmentId}/record`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(recordData),
    });

    if (!response.ok) {
        throw new Error("Nu s-a putut actualiza fisa de consultatie");
    }

    return response.json(); // ConsultationRecordResponse
}