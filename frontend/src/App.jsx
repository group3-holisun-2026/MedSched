import { BrowserRouter, Routes, Route, Link, useNavigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
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

            {isAuthenticated && role === 'ADMINISTRATOR' && (
                <Link to="/admin" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>
                    Administrare
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
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;