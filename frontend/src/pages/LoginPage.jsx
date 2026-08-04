import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Button from '../components/Button';
import Input from '../components/Input';
import LogoMark from '../components/LogoMark';

// Iconitele sunt inline (nu dintr-un pachet) ca pagina de login sa nu adauge nicio
// dependinta noua — sunt singurele din aplicatie deocamdata.
function MailIcon(props) {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
            <rect x="2.5" y="4.5" width="19" height="15" rx="2.5" />
            <path d="m3 7 8.2 5.6a1.5 1.5 0 0 0 1.6 0L21 7" />
        </svg>
    );
}

function LockIcon(props) {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
            <rect x="4" y="10.5" width="16" height="10" rx="2.5" />
            <path d="M8 10.5V7a4 4 0 0 1 8 0v3.5" />
        </svg>
    );
}

function EyeIcon({ off = false, ...props }) {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
            <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" />
            <circle cx="12" cy="12" r="3" />
            {off && <path d="m4 20 16-16" />}
        </svg>
    );
}

function CheckIcon(props) {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.25"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
            <path d="m5 12.5 4.5 4.5L19 7" />
        </svg>
    );
}

const FEATURES = [
    'Calendar unificat pe medici, cabinete si echipamente',
    'Fise de consultatie si istoric complet al pacientilor',
    'Rapoarte de vanzari, ocupare si neprezentari',
];

export default function LoginPage() {
    const { login } = useAuth();
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            await login(email, password);
            // Toate rolurile intra pe Calendar: e ecranul de lucru al clinicii si singurul
            // accesibil tuturor (ADMIN, DOCTOR, RECEPTION). Inainte doar receptia venea aici,
            // restul mergeau pe Dashboard - pagina a fost stearsa.
            navigate('/calendar');
        } catch {
            setError('Email sau parola gresita.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="grid min-h-screen lg:grid-cols-2">
            {/* Panoul de brand. Ascuns sub lg ca pe telefon formularul sa fie primul lucru
                de pe ecran, fara scroll. */}
            <aside className="relative hidden overflow-hidden bg-primary text-primary-foreground lg:flex lg:flex-col lg:justify-between lg:p-12">
                <div
                    className="pointer-events-none absolute inset-0 opacity-70"
                    style={{
                        background:
                            'radial-gradient(60rem 40rem at 15% -10%, rgba(14,165,233,0.45), transparent 60%),' +
                            'radial-gradient(45rem 35rem at 110% 110%, rgba(14,165,233,0.30), transparent 55%)',
                    }}
                />

                <div className="relative flex items-center gap-3">
                    <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-white/15 ring-1 ring-white/25">
                        <LogoMark className="h-6 w-6" />
                    </span>
                    <span className="text-xl font-semibold tracking-tight">MedSched</span>
                </div>

                <div className="relative max-w-md">
                    <h2 className="text-4xl font-semibold leading-tight tracking-tight">
                        Programarile clinicii,
                        <br />
                        intr-un singur loc.
                    </h2>
                    <p className="mt-4 text-base leading-relaxed text-white/70">
                        Medici, cabinete si echipamente sincronizate, fara suprapuneri si fara
                        agenda pe hartie.
                    </p>

                    <ul className="mt-10 space-y-4">
                        {FEATURES.map((feature) => (
                            <li key={feature} className="flex items-start gap-3 text-sm text-white/85">
                                <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-accent/25 text-accent-foreground">
                                    <CheckIcon className="h-3 w-3" />
                                </span>
                                {feature}
                            </li>
                        ))}
                    </ul>
                </div>

                <p className="relative text-xs text-white/45">
                    &copy; {new Date().getFullYear()} MedSched
                </p>
            </aside>

            {/* Formularul */}
            <main className="flex items-center justify-center px-5 py-10 sm:px-8">
                <div className="w-full max-w-sm">
                    {/* Logo-ul apare deasupra formularului doar cat timp panoul din stanga e ascuns. */}
                    <div className="mb-8 flex items-center gap-3 lg:hidden">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-primary-foreground">
                            <LogoMark className="h-5 w-5" />
                        </span>
                        <span className="text-lg font-semibold tracking-tight">MedSched</span>
                    </div>

                    <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Bine ai venit</h1>
                    <p className="mt-2 text-sm text-muted-foreground">
                        Autentifica-te ca sa continui in contul clinicii.
                    </p>

                    <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
                        <div className="space-y-1.5">
                            <label htmlFor="email" className="block text-sm font-medium">
                                Email
                            </label>
                            <div className="relative">
                                <MailIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                                <Input
                                    id="email"
                                    name="email"
                                    type="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                    autoFocus
                                    autoComplete="email"
                                    placeholder="nume@medsched.ro"
                                    aria-invalid={error ? 'true' : undefined}
                                    className="h-11 border-slate-200 bg-slate-50 pl-10 transition-colors focus:bg-card"
                                />
                            </div>
                        </div>

                        <div className="space-y-1.5">
                            <label htmlFor="password" className="block text-sm font-medium">
                                Parola
                            </label>
                            <div className="relative">
                                <LockIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                                <Input
                                    id="password"
                                    name="password"
                                    type={showPassword ? 'text' : 'password'}
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                    autoComplete="current-password"
                                    placeholder="••••••••"
                                    aria-invalid={error ? 'true' : undefined}
                                    className="h-11 border-slate-200 bg-slate-50 pl-10 pr-11 transition-colors focus:bg-card"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword((visible) => !visible)}
                                    aria-label={showPassword ? 'Ascunde parola' : 'Arata parola'}
                                    aria-pressed={showPassword}
                                    className="absolute right-1 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground transition-colors hover:text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                                >
                                    <EyeIcon off={showPassword} className="h-4 w-4" />
                                </button>
                            </div>
                        </div>

                        {/* role="alert" ca cititoarele de ecran sa anunte esecul fara sa mute focusul. */}
                        {error && (
                            <p
                                role="alert"
                                className="flex items-center gap-2 rounded-md bg-destructive/10 px-3 py-2.5 text-sm text-destructive"
                            >
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                                     strokeLinecap="round" className="h-4 w-4 shrink-0" aria-hidden="true">
                                    <circle cx="12" cy="12" r="9" />
                                    <path d="M12 7.5v5M12 16h.01" />
                                </svg>
                                {error}
                            </p>
                        )}

                        <Button
                            type="submit"
                            disabled={loading}
                            className="h-11 w-full gap-2 text-base shadow-sm transition-opacity"
                        >
                            {loading && (
                                <svg viewBox="0 0 24 24" className="h-4 w-4 animate-spin" aria-hidden="true">
                                    <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="3"
                                            fill="none" opacity="0.3" />
                                    <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" strokeWidth="3"
                                          fill="none" strokeLinecap="round" />
                                </svg>
                            )}
                            {loading ? 'Se conecteaza...' : 'Autentificare'}
                        </Button>
                    </form>

                    <p className="mt-8 text-center text-xs text-muted-foreground">
                        Ai probleme la autentificare? Contacteaza administratorul clinicii.
                    </p>
                </div>
            </main>
        </div>
    );
}
