import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { GoogleLogin } from "@react-oauth/google";
import { login, googleLogin } from "../services/authService";
import { useAuth } from "../context/AuthContext";

const Login = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const { login: authLogin } = useAuth();

  const redirect = (role) => {
    if (role === "BUYER") navigate("/buyer");
    else if (role.includes("SELLER")) navigate("/seller");
    else navigate("/admin");
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    try {
      const res = await login(email, password);
      authLogin({ token: res.data.token, role: res.data.role });
      redirect(res.data.role);
    } catch {
      setError("Invalid credentials");
    }
  };

  const handleGoogleSuccess = async (res) => {
    try {
      const r = await googleLogin(res.credential);
      authLogin({ token: r.data.token, role: r.data.role });
      redirect(r.data.role);
    } catch {
      setError("Google login failed");
    }
  };

  return (
    <div className="container">
      <h2>IndiExport Login</h2>
      {error && <p className="error">{error}</p>}

      <form onSubmit={handleLogin}>
        <input placeholder="Email" value={email}
          onChange={(e) => setEmail(e.target.value)} required />
        <input type="password" placeholder="Password"
          value={password} onChange={(e) => setPassword(e.target.value)} required />
        <button>Login</button>
      </form>

      {/* <GoogleLogin
        onSuccess={handleGoogleSuccess}
        onError={() => setError("Google login failed")}
      /> */}

      <div className="link">
        <Link to="/register">Create account</Link>
      </div>
    </div>
  );
};

export default Login;
