const BASE_URL = "http://localhost:8080/api";

export async function getAuditLogRequest(accessToken, { userId, from, to }) {
    const params = new URLSearchParams({ user: userId, from, to });

    const response = await fetch(`${BASE_URL}/audit-log?${params.toString()}`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error("Nu s-au putut obtine inregistrarile de audit");
    }

    return response.json(); // AuditLogResponse[]
}