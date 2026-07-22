import { BrowserRouter, Routes, Route, Link, useNavigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import PatientPage from './pages/Patient/PatientPage';
import AuditLogPage from './pages/AuditLog/AuditLogPage';
import ConsultationRecordPage from './pages/Consultation/ConsultationRecordPage';
import RoomsPage from './pages/Rooms/RoomsPage'; // Doar Cabinete
import DoctorPage from './pages/Doctor/DoctorPage';
import EquipmentPage from './pages/Equipment/EquipmentPage';
import { AuthProvider, useAuth } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';

function Navbar() {
    const { isAuthenticated, logout, user } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const role = user?.role;

    if (!isAuthenticated) {
        return (
            <nav style={{ padding: '15px', background: '#2c3e50', marginBottom: '20px' }}>
                <Link to="/login" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>Login</Link>
            </nav>
        );
    }

    return (
        <nav style={{ padding: '15px', background: '#2c3e50', marginBottom: '20px' }}>
            <Link to="/dashboard" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>Dashboard</Link>
            <Link to="/patients" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>Pacienti</Link>

            {role === 'ADMIN' && (
                <>
                    <Link to="/medici" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>
                        Administrare Medici
                    </Link>
                    <Link to="/admin/rooms" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>
                        Administrare Cabinete
                    </Link>
                    <Link to="/admin/equipment" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>
                        Administrare Echipamente
                    </Link>
                    <Link to="/audit-log" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>
                        Audit Log
                    </Link>
                </>
            )}

            <button onClick={handleLogout} style={{ marginLeft: '10px' }}>
                Logout
            </button>
        </nav>
    );
}

function App() {
    return (
        <BrowserRouter>
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
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;