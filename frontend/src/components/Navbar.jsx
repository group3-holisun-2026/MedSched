import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

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
                <Link
                    to="/login"
                    style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}
                >
                    Login
                </Link>
            </nav>
        );
    }

    return (
        <nav style={{ padding: '15px', background: '#2c3e50', marginBottom: '20px' }}>
            <Link
                to="/dashboard"
                style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}
            >
                Dashboard
            </Link>

            <Link
                to="/patients"
                style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}
            >
                Pacienti
            </Link>

            {role === 'ADMIN' && (
                <>
                    <Link
                        to="/medici"
                        style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}
                    >
                        Administrare Medici
                    </Link>

                    <Link
                        to="/admin/rooms"
                        style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}
                    >
                        Administrare Cabinete
                    </Link>

                    <Link
                        to="/admin/equipment"
                        style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}
                    >
                        Administrare Echipamente
                    </Link>

                    <Link
                        to="/audit-log"
                        style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}
                    >
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

export default Navbar;