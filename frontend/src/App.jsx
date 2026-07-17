import { BrowserRouter, Routes, Route, Link, useNavigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import PatientPage from './pages/Patient/PatientPage';
import AuditLogPage from './pages/AuditLog/AuditLogPage';
import ConsultationRecordPage from './pages/Consultation/ConsultationRecordPage';
import { AuthProvider, useAuth } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';

function Navbar() {
    const { isAuthenticated, logout, user } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    // TODO: confirma cu Ianis numele exact al campului de rol (user.role?) si valorile posibile (ADMIN, MEDIC, RECEPTIONIST?)
    const role = user?.role;

    return (
        <nav style={{ padding: '15px', background: '#2c3e50', marginBottom: '20px' }}>
            <Link to="/login" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>Login</Link>
            <Link to="/dashboard" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>Dashboard</Link>
            <Link to="/patients" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>Pacienti</Link>

            {isAuthenticated && (role === 'ADMINISTRATOR' || role === 'ADMIN') && (
                <Link to="/admin" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>
                    Administrare
                </Link>
            )}

            {/* TODO: bug cunoscut — rolul real de backend e "ADMIN" (@PreAuthorize hasRole('ADMIN')),
                dar aici verificam si "ADMINISTRATOR" ca sa nu pierdem accesul pana se rezolva inconsistenta.
                De curatat cand PR-ul de fix pentru roluri e mergeuit. */}
            {isAuthenticated && (role === 'ADMINISTRATOR' || role === 'ADMIN') && (
                <Link to="/audit-log" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>
                    Audit Log
                </Link>
            )}

            {isAuthenticated && role === 'MEDIC' && (
                <Link to="/doctor" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>
                    Program Medic
                </Link>
            )}

            {isAuthenticated && role === 'RECEPTIONIST' && (
                <Link to="/receptie" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>
                    Receptie
                </Link>
            )}

            {isAuthenticated && (
                <button onClick={handleLogout} style={{ marginLeft: '10px' }}>
                    Logout
                </button>
            )}
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
                            <PrivateRoute>
                                <PatientPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/audit-log"
                        element={
                            <PrivateRoute>
                                <AuditLogPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/appointments/:appointmentId/record"
                        element={
                            <PrivateRoute>
                                <ConsultationRecordPage />
                            </PrivateRoute>
                        }
                    />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;