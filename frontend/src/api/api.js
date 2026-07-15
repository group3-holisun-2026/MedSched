const BASE_URL = "http://localhost:8080/api"; // TODO: confirma cu Ianis portul/URL-ul real al backend-ului

// ---- AUTH ----

export async function loginRequest(email, password) {
    // TODO: endpoint-ul /api/auth/login nu e inca implementat pe backend (doar /api/auth/register exista acum)
    const response = await fetch(`${BASE_URL}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
        throw new Error("Login esuat");
    }

    return response.json(); // TODO: confirma formatul exact al raspunsului (accessToken, refreshToken, user)
}

export async function refreshTokenRequest(refreshToken) {
    // TODO: endpoint-ul /api/auth/refresh nu e inca implementat pe backend
    const response = await fetch(`${BASE_URL}/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
        throw new Error("Refresh esuat");
    }

    return response.json(); // TODO: confirma formatul exact (accessToken nou, posibil si refreshToken nou)
}

export async function logoutRequest() {
    // TODO: endpoint-ul /api/auth/logout nu e inca implementat (optional, poate fi doar client-side)
}