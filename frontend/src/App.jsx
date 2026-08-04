import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'sonner';
import LoginPage from './pages/LoginPage';
import PatientPage from './pages/Patient/PatientPage';
import PatientRecordsPage from './pages/Patient/PatientRecordsPage';
import CalendarPage from './pages/Calendar/CalendarPage';
import AuditLogPage from './pages/AuditLog/AuditLogPage';
import ConsultationRecordPage from './pages/Consultation/ConsultationRecordPage';
import RoomsPage from './pages/Rooms/RoomsPage'; // Doar Cabinete
import DoctorPage from './pages/Doctor/DoctorPage';
import EquipmentPage from './pages/Equipment/EquipmentPage';
import NotificationsPage from './pages/Notifications/NotificationsPage';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Navbar from './components/Navbar';
import ScrollToTop from './components/ScrollToTop';
// Importurile pentru rapoarte
import SalesReportPage from './pages/Reports/SalesReportPage';
import OccupancyReportPage from './pages/Reports/OccupancyReportPage';
import NoShowReportPage from './pages/Reports/NoShowReportPage';
// Pagina publica deschisa din linkul de confirmare/anulare
import AppointmentConfirmPage from './pages/Public/AppointmentConfirmPage';

function App() {
    return (
        <BrowserRouter>
            <Toaster richColors position="top-right" closeButton />
            <AuthProvider>
                <ScrollToTop />
                <Navbar />

                {/* Rutele care schimbă ecranele */}
                <Routes>
                    <Route path="/" element={<LoginPage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/c/:token" element={<AppointmentConfirmPage />} />

                    <Route
                        path="/patients"
                        element={
                            <PrivateRoute roles={['ADMIN', 'DOCTOR', 'RECEPTION']}>
                                <PatientPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/calendar"
                        element={
                            <PrivateRoute roles={['ADMIN', 'DOCTOR', 'RECEPTION']}>
                                <CalendarPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/audit-log"
                        element={
                            <PrivateRoute roles={['ADMIN']}>
                                <AuditLogPage />
                            </PrivateRoute>
                        }
                    />
                    {/* Fisele sunt continut clinic (F-202): RECEPTION nu are acces, la fel ca
                        pe /appointments/:id/record. */}
                    <Route
                        path="/patients/:patientId/records"
                        element={
                            <PrivateRoute roles={['ADMIN', 'DOCTOR']}>
                                <PatientRecordsPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/appointments/:appointmentId/record"
                        element={
                            <PrivateRoute roles={['ADMIN', 'DOCTOR']}>
                                <ConsultationRecordPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/medici"
                        element={
                            <PrivateRoute roles={['ADMIN']}>
                                <DoctorPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/admin/rooms"
                        element={
                            <PrivateRoute roles={['ADMIN']}>
                                <RoomsPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/admin/equipment"
                        element={
                            <PrivateRoute roles={['ADMIN']}>
                                <EquipmentPage />
                            </PrivateRoute>
                        }
                    />

                    {/* Rute Noi: Infrastructura de Rapoarte (P1, P2, P3) */}
                    <Route
                        path="/rapoarte/vanzari"
                        element={
                            <PrivateRoute roles={['ADMIN']}>
                                <SalesReportPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/rapoarte/ocupare"
                        element={
                            <PrivateRoute roles={['ADMIN']}>
                                <OccupancyReportPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/rapoarte/no-show"
                        element={
                            <PrivateRoute roles={['ADMIN']}>
                                <NoShowReportPage />
                            </PrivateRoute>
                        }
                    />

                    {/* Administrarea cozii de notificari (P5) */}
                    <Route
                        path="/admin/notificari"
                        element={
                            <PrivateRoute roles={['ADMIN']}>
                                <NotificationsPage />
                            </PrivateRoute>
                        }
                    />

                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;