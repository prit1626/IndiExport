import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const ProtectedRoute = ({ allowedRoles }) => {
    const { user, loading, isAuthenticated } = useAuth();

    if (loading) {
        return (
            <div className="container" style={{ textAlign: "center", marginTop: "50px" }}>
                <p>Loading...</p>
            </div>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/" replace />;
    }

    // Check if role is allowed
    // Some roles might be SELLER_BASIC or SELLER_ADVANCED, so we check for substring or exact match
    const userRole = user.role;
    const isAllowed = allowedRoles.some(role =>
        userRole === role || userRole.includes(role)
    );

    if (!isAllowed) {
        return <Navigate to="/" replace />;
    }

    return <Outlet />;
};

export default ProtectedRoute;
