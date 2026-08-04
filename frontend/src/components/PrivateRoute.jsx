import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function PrivateRoute({ children, roles }) {
    const { isAuthenticated, user, userLoading } = useAuth();

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    if (userLoading) {
        return null;
    }

    // Cine nu are voie pe ruta ceruta ajunge pe Calendar. E accesibil tuturor celor trei roluri,
    // deci redirectul nu se poate transforma in bucla.
    if (roles && roles.length > 0 && !roles.includes(user?.role)) {
        return <Navigate to="/calendar" replace />;
    }

    return children;
}
