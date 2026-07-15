import Toast from "./Toast";
import "./Toast.css";

export default function ToastContainer({ toasts, onClose }) {
    return (
        <div className="toast-container">
            {toasts.map((toast) => (
                <Toast
                    key={toast.id}
                    message={toast.message}
                    type={toast.type}
                    onClose={() => onClose(toast.id)}
                />
            ))}
        </div>
    );
}