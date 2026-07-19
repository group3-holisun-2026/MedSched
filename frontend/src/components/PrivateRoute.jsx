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

    if (roles && roles.length > 0 && !roles.includes(user?.role)) {
        return <Navigate to="/dashboard" replace />;
    }

    return children;
}
