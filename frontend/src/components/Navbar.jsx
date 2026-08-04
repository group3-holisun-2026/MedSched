import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LogoMark from './LogoMark';

// Etichetele celor trei linkuri de administrare erau "Administrare Medici/Cabinete/Echipamente".
// Cu banda centrata nu mai incapeau: cele opt linkuri de admin depaseau latimea ecranului si
// impingeau butonul de cont afara. Prefixul "Administrare" e oricum redundant — linkurile apar
// doar in navbar-ul de ADMIN.
const ADMIN_LINKS = [
    { to: '/medici', label: 'Medici' },
    { to: '/admin/rooms', label: 'Cabinete' },
    { to: '/admin/equipment', label: 'Echipamente' },
    { to: '/audit-log', label: 'Audit Log' },
    // Un singur link pentru rapoarte; navigarea intre ele se face din banda de tab-uri
    // (ReportTabs) din capul fiecarei pagini. `match` marcheaza linkul activ pe tot /rapoarte/*.
    { to: '/rapoarte/vanzari', label: 'Rapoarte', match: '/rapoarte' },
    { to: '/admin/notificari', label: 'Notificări' },
];

const BASE_LINKS = [
    { to: '/patients', label: 'Pacienti' },
    { to: '/calendar', label: 'Calendar' },
];

function Navbar() {
    const { isAuthenticated, logout, user } = useAuth();
    const navigate = useNavigate();
    const { pathname } = useLocation();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const role = user?.role;

    // Ascunde complet Navbar-ul pentru rutele publice (ex: link-ul din notificare) si pentru
    // login, care e ecran pe toata inaltimea si isi poarta singur logo-ul — o bara cu un singur
    // link "Login" deasupra paginii de login nu ducea nicaieri.
    // Iesirea din componenta se face dupa toate hook-urile, altfel ordinea lor difera
    // intre aceste rute si restul, si React arunca "rendered fewer hooks than expected".
    if (pathname.startsWith('/c/') || pathname === '/' || pathname === '/login') {
        return null;
    }

    const isActive = (link) => pathname.startsWith(link.match ?? link.to);

    const linkClass = (link) =>
        'rounded-md px-3 py-2 text-sm font-medium whitespace-nowrap transition-colors ' +
        (isActive(link)
            ? 'bg-white/15 text-white'
            : 'text-white/75 hover:bg-white/10 hover:text-white');

    // Logo-ul e doar semn de brand, nu link: nu duce nicaieri si nu raspunde la click.
    const brand = (
        <div className="flex select-none items-center gap-2.5 text-white">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/15 ring-1 ring-white/25">
                <LogoMark className="h-4.5 w-4.5" />
            </span>
            <span className="text-base font-semibold tracking-tight">MedSched</span>
        </div>
    );

    // Butonul de cont, mereu in dreapta. Login-ul e umplut cu accent ca sa se vada de la distanta
    // pe fundalul bleumarin; logout-ul e conturat, ca sa nu concureze vizual cu navigarea.
    const accountButton = isAuthenticated ? (
        <button
            onClick={handleLogout}
            className="rounded-md px-4 py-2 text-sm font-semibold whitespace-nowrap text-white ring-1 ring-white/40 transition-colors hover:bg-white/15 focus:outline-none focus:ring-2 focus:ring-white"
        >
            Logout
        </button>
    ) : (
        <Link
            to="/login"
            className="rounded-md bg-accent px-4 py-2 text-sm font-semibold whitespace-nowrap text-accent-foreground shadow-sm transition-opacity hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-white"
        >
            Login
        </Link>
    );

    // Grila 1fr / auto / 1fr tine banda din mijloc centrata pe ecran indiferent cat de late sunt
    // marginile — cu flex + justify-between centrul s-ar deplasa cand se schimba rolul.
    const shell = (middle) => (
        <nav className="mb-5 bg-primary text-primary-foreground shadow-sm">
            <div className="mx-auto grid max-w-7xl grid-cols-[1fr_auto_1fr] items-center gap-4 px-4 py-3">
                <div className="flex justify-start">{brand}</div>
                <div className="flex justify-center">{middle}</div>
                <div className="flex justify-end">{accountButton}</div>
            </div>
        </nav>
    );

    if (!isAuthenticated) {
        return shell(null);
    }

    // Medicii si receptia vad doar Pacienti si Calendar, direct in banda centrala. Medicii aveau
    // inainte un meniu burger — cu doua intrari ascundea mai mult decat economisea, mai ales pe
    // tableta, unde un tap in plus inseamna ceva.
    const links = role === 'ADMIN' ? [...BASE_LINKS, ...ADMIN_LINKS] : BASE_LINKS;

    return shell(
        <div className="flex flex-wrap items-center justify-center gap-1">
            {links.map((link) => (
                <Link key={link.to} to={link.to} className={linkClass(link)}>
                    {link.label}
                </Link>
            ))}
        </div>
    );
}

export default Navbar;
