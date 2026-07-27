import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
    const { login } = useAuth();
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const loggedInUser = await login(email, password);
            // Receptia intra direct pe Calendar - e ecranul cu care lucreaza cel mai mult,
            // restul rolurilor merg pe Dashboard ca inainte.
            if (loggedInUser?.role === 'RECEPTION') {
                navigate('/calendar');
            } else {
                navigate('/dashboard');
            }
        } catch (err) {
            setError('Email sau parola gresita.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                minHeight: '80vh',
                padding: '20px',
            }}
        >
            <div style={{ width: '100%', maxWidth: '400px' }}>
                <h1 style={{ textAlign: 'center' }}>Login MedSched</h1>

                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '12px' }}>
                        <label>Email</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            style={{ width: '100%', padding: '8px' }}
                        />
                    </div>

                    <div style={{ marginBottom: '12px' }}>
                        <label>Parola</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            style={{ width: '100%', padding: '8px' }}
                        />
                    </div>

                    {error && <p style={{ color: 'red' }}>{error}</p>}

                    <button type="submit" disabled={loading} style={{ width: '100%', padding: '10px' }}>
                        {loading ? 'Se conecteaza...' : 'Login'}
                    </button>
                </form>
            </div>
        </div>
    );
}