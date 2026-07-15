import { createContext, useContext, useState, useCallback } from "react";
import ToastContainer from "../components/Toast/ToastContainer";

const ToastContext = createContext(null);

let idCounter = 0;

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);

    const removeToast = useCallback((id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    const addToast = useCallback((message, type = "success") => {
        const id = ++idCounter;
        setToasts((prev) => [...prev, { id, message, type }]);

        setTimeout(() => {
            removeToast(id);
        }, 4000); // auto-dismiss după 4 secunde (în intervalul 3-5s cerut)
    }, [removeToast]);

    const showSuccess = useCallback((msg) => addToast(msg, "success"), [addToast]);
    const showError = useCallback((msg) => addToast(msg, "error"), [addToast]);

    return (
        <ToastContext.Provider value={{ showSuccess, showError }}>
            {children}
            <ToastContainer toasts={toasts} onClose={removeToast} />
        </ToastContext.Provider>
    );
}

export function useToast() {
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error("useToast trebuie folosit în interiorul unui ToastProvider");
    }
    return context;
}