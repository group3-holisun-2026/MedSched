import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const linkStyle = { marginRight: '20px', color: 'white', textDecoration: 'none' };

function Navbar() {
    const { isAuthenticated, logout, user } = useAuth();
    const navigate = useNavigate();
    const { pathname } = useLocation();

    // Ascunde complet Navbar-ul pentru rutele publice (ex: link-ul din SMS)
    if (pathname.startsWith('/c/')) {
        return null;
    }

    const [menuOpen, setMenuOpen] = useState(false);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const role = user?.role;

    if (!isAuthenticated) {
        return (
            <nav style={{ padding: '15px', background: '#2c3e50', marginBottom: '20px' }}>
                <Link to="/login" style={linkStyle}>
                    Login
                </Link>
            </nav>
        );
    }

    // Medicii au un navbar restrans (burger)
    if (role === 'DOCTOR') {
        return (
            <nav style={{ padding: '15px', background: '#2c3e50', marginBottom: '20px', position: 'relative' }}>
                <button
                    onClick={() => setMenuOpen((open) => !open)}
                    aria-label="Deschide meniul"
                    style={{
                        background: 'none',
                        border: '1px solid white',
                        color: 'white',
                        fontSize: '1.2rem',
                        lineHeight: 1,
                        padding: '6px 14px',
                        cursor: 'pointer',
                        borderRadius: '4px',
                    }}
                >
                    ☰
                </button>

                {menuOpen && (
                    <div
                        style={{
                            position: 'absolute',
                            top: 'calc(100% + 4px)',
                            left: '15px',
                            background: '#2c3e50',
                            border: '1px solid #445868',
                            borderRadius: '4px',
                            padding: '12px',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '10px',
                            zIndex: 10,
                        }}
                    >
                        <Link to="/patients" style={linkStyle} onClick={() => setMenuOpen(false)}>
                            Pacienti
                        </Link>
                        <Link to="/calendar" style={linkStyle} onClick={() => setMenuOpen(false)}>
                            Calendar
                        </Link>
                        <button onClick={handleLogout} style={{ alignSelf: 'flex-start' }}>
                            Logout
                        </button>
                    </div>
                )}
            </nav>
        );
    }

    return (
        <nav style={{ padding: '15px', background: '#2c3e50', marginBottom: '20px' }}>
            <Link to="/patients" style={linkStyle}>
                Pacienti
            </Link>

            <Link to="/calendar" style={linkStyle}>
                Calendar
            </Link>

            {role === 'ADMIN' && (
                <>
                    <Link to="/medici" style={linkStyle}>
                        Administrare Medici
                    </Link>

                    <Link to="/admin/rooms" style={linkStyle}>
                        Administrare Cabinete
                    </Link>

                    <Link to="/admin/equipment" style={linkStyle}>
                        Administrare Echipamente
                    </Link>

                    <Link to="/audit-log" style={linkStyle}>
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