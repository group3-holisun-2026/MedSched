import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'sonner';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import PatientPage from './pages/Patient/PatientPage';
import CalendarPage from './pages/Calendar/CalendarPage';
import AuditLogPage from './pages/AuditLog/AuditLogPage';
import ConsultationRecordPage from './pages/Consultation/ConsultationRecordPage';
import RoomsPage from './pages/Rooms/RoomsPage'; // Doar Cabinete
import DoctorPage from './pages/Doctor/DoctorPage';
import EquipmentPage from './pages/Equipment/EquipmentPage';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Navbar from './components/Navbar';
// Importurile pentru rapoarte
import SalesReportPage from './pages/Reports/SalesReportPage';
import OccupancyReportPage from './pages/Reports/OccupancyReportPage';

function App() {
    return (
        <BrowserRouter>
            <Toaster richColors position="top-right" closeButton />
            <AuthProvider>
                <Navbar />

                {/* Rutele care schimbă ecranele */}
                <Routes>
                    <Route path="/" element={<LoginPage />} />
                    <Route path="/login" element={<LoginPage />} />

                    <Route
                        path="/dashboard"
                        element={
                            <PrivateRoute>
                                <DashboardPage />
                            </PrivateRoute>
                        }
                    />
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
                    {/* P3 (no-show) inca nu e pe main - ruta ramane temporar pe placeholder */}
                    <Route
                        path="/rapoarte/no-show"
                        element={
                            <PrivateRoute roles={['ADMIN']}>
                                <SalesReportPage />
                            </PrivateRoute>
                        }
                    />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;