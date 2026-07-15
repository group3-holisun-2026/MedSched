const BASE_URL = "http://localhost:8080/api";

// ---- AUTH ----

export async function loginRequest(email, password) {
    const response = await fetch(`${BASE_URL}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
        throw new Error("Login esuat");
    }

    return response.json(); // { accessToken, refreshToken, tokenType, expiresIn, user: { id, username, email, phone, city, role } }
}

export async function refreshTokenRequest(refreshToken) {
    const response = await fetch(`${BASE_URL}/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
        throw new Error("Refresh esuat");
    }

    return response.json();
}

export async function logoutRequest(accessToken) {
    await fetch(`${BASE_URL}/auth/logout`, {
        method: "POST",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });
}

export async function meRequest(accessToken) {
    const response = await fetch(`${BASE_URL}/auth/me`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error("Nu s-a putut obtine userul curent");
    }

    return response.json(); // UserResponse: { id, username, email, phone, city, role }
}