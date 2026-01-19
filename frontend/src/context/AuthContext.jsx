import { createContext, useContext, useState, useEffect } from "react";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Initialize auth state from local storage
        const token = localStorage.getItem("token");
        let role = localStorage.getItem("role");

        if (token) {
            if (!role) {
                // Fallback: extract role from JWT payload if missing in localStorage
                try {
                    const payload = JSON.parse(atob(token.split(".")[1]));
                    role = payload.role;
                } catch (e) {
                    console.error("Failed to decode token", e);
                }
            }
            if (role) {
                setUser({ token, role });
            }
        }
        setLoading(false);
    }, []);

    const login = (userData) => {
        localStorage.setItem("token", userData.token);
        localStorage.setItem("role", userData.role);
        setUser(userData);
    };

    const logout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, loading, login, logout, isAuthenticated: !!user }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
};
