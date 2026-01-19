import { useNavigate } from "react-router-dom";

const Unauthorized = () => {
    const navigate = useNavigate();

    return (
        <div className="container" style={{ textAlign: "center", marginTop: "100px" }}>
            <h1 style={{ color: "#dc3545" }}>Access Denied</h1>
            <p>You do not have permission to access this page.</p>
            <div style={{ marginTop: "20px" }}>
                <button onClick={() => navigate(-1)} style={{ marginRight: "10px" }}>Go Back</button>
                <button onClick={() => navigate("/")}>Go to Home</button>
            </div>
        </div>
    );
};

export default Unauthorized;
