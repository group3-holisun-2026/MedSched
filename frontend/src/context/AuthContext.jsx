import { createContext, useContext, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { loginRequest, refreshTokenRequest, logoutRequest } from "../api/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [accessToken, setAccessToken] = useState(() => localStorage.getItem("accessToken"));
    const [refreshToken, setRefreshToken] = useState(() => localStorage.getItem("refreshToken"));
    const [user, setUser] = useState(null);
    const navigate = useNavigate();

    const isAuthenticated = !!accessToken;

    const saveTokens = useCallback((newAccessToken, newRefreshToken) => {
        setAccessToken(newAccessToken);
        localStorage.setItem("accessToken", newAccessToken);

        if (newRefreshToken) {
            setRefreshToken(newRefreshToken);
            localStorage.setItem("refreshToken", newRefreshToken);
        }
    }, []);

    const clearTokens = useCallback(() => {
        setAccessToken(null);
        setRefreshToken(null);
        setUser(null);
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
    }, []);

    const login = useCallback(async (email, password) => {
        const data = await loginRequest(email, password);
        saveTokens(data.accessToken, data.refreshToken);
        setUser(data.user ?? null);
    }, [saveTokens]);

    const logout = useCallback(async () => {
        if (accessToken) {
            try {
                await logoutRequest(accessToken);
            } catch (err) {
                // ignoram eroarea - oricum stergem sesiunea local
            }
        }
        clearTokens();
        navigate("/login");
    }, [accessToken, clearTokens, navigate]);

    // Incearca sa reimprospateze token-ul; daca esueaza, delogheaza userul
    const tryRefresh = useCallback(async () => {
        if (!refreshToken) {
            logout();
            return null;
        }

        try {
            const data = await refreshTokenRequest(refreshToken);
            saveTokens(data.accessToken, data.refreshToken);
            return data.accessToken;
        } catch (err) {
            // refresh token-ul a expirat sau e invalid -> delogare
            logout();
            return null;
        }
    }, [refreshToken, saveTokens, logout]);

    // Wrapper de fetch care adauga automat token-ul si reincearca dupa refresh, la nevoie
    const authFetch = useCallback(async (url, options = {}) => {
        const doFetch = (token) =>
            fetch(url, {
                ...options,
                headers: {
                    ...options.headers,
                    Authorization: `Bearer ${token}`,
                },
            });

        let response = await doFetch(accessToken);

        if (response.status === 401) {
            const newToken = await tryRefresh();
            if (!newToken) {
                throw new Error("Sesiune expirata");
            }
            response = await doFetch(newToken);
        }

        return response;
    }, [accessToken, tryRefresh]);

    return (
        <AuthContext.Provider value={{ user, isAuthenticated, login, logout, authFetch }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth trebuie folosit in interiorul unui AuthProvider");
    }
    return context;
}