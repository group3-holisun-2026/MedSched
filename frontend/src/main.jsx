import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { ToastProvider } from './context/ToastContext'
import { Toaster } from 'sonner'

// CalendarPage/AppointmentForm/ConsultationRecordPage emit prin `sonner`; fara <Toaster />
// montat aici, toate acele toast-uri erau inghitite in tacere (verificat: nu era montat nicaieri).
createRoot(document.getElementById('root')).render(
    <StrictMode>
        <ToastProvider>
            <App />
            <Toaster position="top-right" richColors />
        </ToastProvider>
    </StrictMode>,
)