import { useState, useEffect } from "react";
import { getSellerProfile, updateSellerProfile, getSellerAnalytics } from "../services/sellerService";
import { logout } from "../services/authService";
import { Link, useNavigate } from "react-router-dom";

const SellerDashboard = () => {
    const [profile, setProfile] = useState(null);
    const [analytics, setAnalytics] = useState(null);
    const [loading, setLoading] = useState(true);
    const [editing, setEditing] = useState(false);
    const [formData, setFormData] = useState({});
    const [error, setError] = useState("");
    const [activeTab, setActiveTab] = useState("profile"); // "profile" or "analytics"
    const navigate = useNavigate();

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const [profileRes, analyticsRes] = await Promise.all([
                getSellerProfile().catch(() => ({ data: null })),
                getSellerAnalytics().catch(() => ({ data: null }))
            ]);
            setProfile(profileRes.data);
            setFormData(profileRes.data || {});
            setAnalytics(analyticsRes.data);
        } catch (err) {
            console.error(err);
            setError("Failed to load dashboard");
        } finally {
            setLoading(false);
        }
    };

    const handleEdit = () => {
        setEditing(true);
    };

    const handleCancel = () => {
        setEditing(false);
        setFormData(profile);
    };

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSave = async () => {
        try {
            const res = await updateSellerProfile(formData);
            setProfile(res.data);
            setEditing(false);
        } catch (err) {
            setError("Failed to update profile");
        }
    };

    const handleLogout = async () => {
        try {
            await logout();
            localStorage.removeItem("token");
            localStorage.removeItem("role");
            navigate("/");
        } catch (err) {
            console.error("Logout error:", err);
            localStorage.removeItem("token");
            localStorage.removeItem("role");
            navigate("/");
        }
    };

    const renderBarChart = (data, maxValue) => {
        if (!data || data.length === 0) return null;
        return (
            <div style={{ display: "flex", alignItems: "flex-end", gap: "10px", height: "200px", marginTop: "20px" }}>
                {data.map((item, idx) => {
                    const height = maxValue > 0 ? (item.count / maxValue) * 100 : 0;
                    return (
                        <div key={idx} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center" }}>
                            <div style={{
                                width: "100%",
                                backgroundColor: "#1976d2",
                                height: `${height}%`,
                                minHeight: "4px",
                                borderRadius: "4px 4px 0 0",
                                transition: "height 0.3s"
                            }} />
                            <div style={{ fontSize: "10px", marginTop: "5px", color: "#64748b", textAlign: "center" }}>
                                {item.month ? item.month.split("-")[1] : idx + 1}
                            </div>
                        </div>
                    );
                })}
            </div>
        );
    };

    const renderFunnel = (funnel) => {
        if (!funnel) return null;
        const stages = [
            { label: "Product Views", value: funnel.productViews, color: "#3b82f6" },
            { label: "Inquiries", value: funnel.inquiries, color: "#8b5cf6", rate: funnel.viewToInquiryRate },
            { label: "Chats", value: funnel.chats, color: "#ec4899", rate: funnel.inquiryToChatRate },
            { label: "Invoices", value: funnel.invoices, color: "#10b981", rate: funnel.chatToInvoiceRate }
        ];
        const maxValue = Math.max(...stages.map(s => s.value), 1);

        return (
            <div style={{ display: "flex", flexDirection: "column", gap: "15px", marginTop: "20px" }}>
                {stages.map((stage, idx) => {
                    const width = maxValue > 0 ? (stage.value / maxValue) * 100 : 0;
                    return (
                        <div key={idx}>
                            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "5px" }}>
                                <span style={{ fontWeight: "600", color: "#1e293b" }}>{stage.label}</span>
                                <span style={{ color: "#64748b" }}>{stage.value}</span>
                            </div>
                            <div style={{
                                width: "100%",
                                height: "40px",
                                backgroundColor: "#f1f5f9",
                                borderRadius: "6px",
                                overflow: "hidden",
                                position: "relative"
                            }}>
                                <div style={{
                                    width: `${width}%`,
                                    height: "100%",
                                    backgroundColor: stage.color,
                                    transition: "width 0.3s",
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "center",
                                    color: "#fff",
                                    fontWeight: "600",
                                    fontSize: "14px"
                                }}>
                                    {stage.value > 0 && stage.value}
                                </div>
                            </div>
                            {stage.rate !== undefined && (
                                <div style={{ fontSize: "12px", color: "#64748b", marginTop: "3px" }}>
                                    Conversion: {stage.rate.toFixed(1)}%
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        );
    };

    if (loading) return <div style={{ padding: "20px", textAlign: "center", color: "#666" }}>Loading Dashboard...</div>;

    const isAdvanced = profile?.sellerMode === "ADVANCED";

    return (
        <div className="dashboard-container" style={{ display: "flex", minHeight: "100vh" }}>
            <div className="sidebar" style={{ width: "250px", backgroundColor: "#fff", padding: "20px", borderRight: "1px solid #ddd", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
                <div>
                    <h3 style={{ color: "#1976d2", marginBottom: "20px" }}>IndiExport Seller</h3>
                    <ul style={{ listStyle: "none", padding: 0 }}>
                        <li
                            onClick={() => setActiveTab("profile")}
                            style={{ padding: "10px", backgroundColor: activeTab === "profile" ? "#e3f2fd" : "transparent", borderRadius: "6px", color: activeTab === "profile" ? "#1976d2" : "#666", cursor: "pointer", fontWeight: activeTab === "profile" ? "600" : "400" }}
                        >
                            Profile
                        </li>
                        <li
                            onClick={() => setActiveTab("analytics")}
                            style={{ padding: "10px", backgroundColor: activeTab === "analytics" ? "#e3f2fd" : "transparent", borderRadius: "6px", color: activeTab === "analytics" ? "#1976d2" : "#666", cursor: "pointer", fontWeight: activeTab === "analytics" ? "600" : "400", marginTop: "5px" }}
                        >
                            Analytics
                        </li>
                        <Link to="/seller/products" style={{ textDecoration: "none" }}>
                            <li style={{ padding: "10px", color: "#666", cursor: "pointer" }}>Products</li>
                        </Link>
                        <Link to="/seller/inquiries" style={{ textDecoration: "none" }}>
                            <li style={{ padding: "10px", color: "#666", cursor: "pointer" }}>Inquiries</li>
                        </Link>
                        <Link to="/seller/rfqs" style={{ textDecoration: "none" }}>
                            <li style={{ padding: "10px", color: "#666", cursor: "pointer" }}>RFQs</li>
                        </Link>
                        <Link to="/seller/invoices" style={{ textDecoration: "none" }}>
                            <li style={{ padding: "10px", color: "#666", cursor: "pointer" }}>Invoices</li>
                        </Link>
                    </ul>
                </div>
                <button
                    onClick={handleLogout}
                    style={{ padding: "10px 16px", backgroundColor: "#d32f2f", color: "#fff", border: "none", borderRadius: "6px", cursor: "pointer", fontWeight: "600", marginTop: "20px" }}
                >
                    Logout
                </button>
            </div>

            <div className="main-content" style={{ flex: 1, padding: "30px", backgroundColor: "#f4f6f8" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "30px" }}>
                    <h2 style={{ color: "#333", margin: 0 }}>Seller Dashboard</h2>
                    <div className="badges" style={{ display: "flex", gap: "10px" }}>
                        <span style={{
                            padding: "6px 12px", borderRadius: "20px", fontSize: "12px", fontWeight: "bold",
                            backgroundColor: isAdvanced ? "#ffd700" : "#e0e0e0",
                            color: isAdvanced ? "#333" : "#555"
                        }}>
                            {profile?.sellerMode || "BASIC"} SELLER
                        </span>
                        <span style={{
                            padding: "6px 12px", borderRadius: "20px", fontSize: "12px", fontWeight: "bold",
                            backgroundColor: profile?.isVerified ? "#c8e6c9" : "#ffcdd2",
                            color: profile?.isVerified ? "#2e7d32" : "#c62828"
                        }}>
                            {profile?.isVerified ? "VERIFIED" : "UNVERIFIED"}
                        </span>
                    </div>
                </div>

                {activeTab === "profile" ? (
                    <div className="card" style={{ backgroundColor: "#fff", padding: "30px", borderRadius: "12px", boxShadow: "0 4px 20px rgba(0,0,0,0.05)" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "20px" }}>
                            <h3 style={{ margin: 0, color: "#444" }}>Company Profile</h3>
                            {!editing && (
                                <button onClick={handleEdit} style={{ padding: "8px 16px", backgroundColor: "#1976d2", color: "#fff", border: "none", borderRadius: "6px", cursor: "pointer" }}>
                                    Edit Profile
                                </button>
                            )}
                            {editing && (
                                <div style={{ display: "flex", gap: "10px" }}>
                                    <button onClick={handleCancel} style={{ padding: "8px 16px", backgroundColor: "#e0e0e0", color: "#333", border: "none", borderRadius: "6px", cursor: "pointer" }}>Cancel</button>
                                    <button onClick={handleSave} style={{ padding: "8px 16px", backgroundColor: "#2e7d32", color: "#fff", border: "none", borderRadius: "6px", cursor: "pointer" }}>Save Changes</button>
                                </div>
                            )}
                        </div>
                        {error && <p style={{ color: "red" }}>{error}</p>}
                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px" }}>
                            <Field label="Company Name" value={formData?.businessName} name="businessName" editing={editing} onChange={handleChange} />
                            <Field label="GST Number" value={formData?.gstNumber} name="gstNumber" editing={editing} onChange={handleChange} />
                            <Field label="Business Type" value={formData?.businessType} name="businessType" editing={editing} onChange={handleChange} type="select" options={["Manufacturer", "Wholesaler", "Trader", "Exporter"]} />
                            <div className="full-width" style={{ gridColumn: "1 / -1" }}>
                                <Field label="Address" value={formData?.address} name="address" editing={editing} onChange={handleChange} type="textarea" />
                            </div>
                            <Field label="City" value={formData?.city} name="city" editing={editing} onChange={handleChange} />
                            <Field label="State" value={formData?.state} name="state" editing={editing} onChange={handleChange} />
                            <Field label="Pincode" value={formData?.pincode} name="pincode" editing={editing} onChange={handleChange} />
                            <div className="field-group">
                                <label style={{ display: "block", fontSize: "12px", color: "#888", marginBottom: "4px" }}>Country</label>
                                <input value="India" disabled style={{ width: "100%", padding: "10px", borderRadius: "6px", border: "1px solid #eee", fontSize: "14px", backgroundColor: "#f9f9f9", color: "#555", cursor: "not-allowed", fontWeight: "600" }} />
                            </div>
                        </div>
                    </div>
                ) : (
                    <div>
                        {/* Summary Cards */}
                        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "20px", marginBottom: "30px" }}>
                            <MetricCard title="Product Views" value={analytics?.totalProductViews || 0} />
                            <MetricCard title="Inquiries" value={analytics?.totalInquiries || 0} />
                            <MetricCard title="RFQs Participated" value={analytics?.totalRFQsParticipated || 0} />
                            <MetricCard title="Active Chats" value={analytics?.totalChatsInitiated || 0} />
                            <MetricCard title="Invoices Generated" value={analytics?.totalInvoicesGenerated || 0} />
                        </div>

                        {isAdvanced && analytics ? (
                            <>
                                {/* Inquiry Growth Chart */}
                                {analytics.inquiryGrowth && analytics.inquiryGrowth.length > 0 && (
                                    <div className="card" style={{ backgroundColor: "#fff", padding: "30px", borderRadius: "12px", boxShadow: "0 4px 20px rgba(0,0,0,0.05)", marginBottom: "30px" }}>
                                        <h3 style={{ marginTop: 0, marginBottom: "20px" }}>Inquiry Growth</h3>
                                        {renderBarChart(analytics.inquiryGrowth, Math.max(...analytics.inquiryGrowth.map(d => d.count), 1))}
                                    </div>
                                )}

                                {/* Conversion Funnel */}
                                {analytics.conversionFunnel && (
                                    <div className="card" style={{ backgroundColor: "#fff", padding: "30px", borderRadius: "12px", boxShadow: "0 4px 20px rgba(0,0,0,0.05)", marginBottom: "30px" }}>
                                        <h3 style={{ marginTop: 0, marginBottom: "20px" }}>Conversion Funnel</h3>
                                        {renderFunnel(analytics.conversionFunnel)}
                                    </div>
                                )}

                                {/* Top Products */}
                                {analytics.topProducts && analytics.topProducts.length > 0 && (
                                    <div className="card" style={{ backgroundColor: "#fff", padding: "30px", borderRadius: "12px", boxShadow: "0 4px 20px rgba(0,0,0,0.05)", marginBottom: "30px" }}>
                                        <h3 style={{ marginTop: 0, marginBottom: "20px" }}>Top Performing Products</h3>
                                        <div style={{ display: "flex", flexDirection: "column", gap: "15px" }}>
                                            {analytics.topProducts.map((product, idx) => (
                                                <div key={idx} style={{ padding: "15px", border: "1px solid #e2e8f0", borderRadius: "8px" }}>
                                                    <div style={{ fontWeight: "600", marginBottom: "10px" }}>{product.productName}</div>
                                                    <div style={{ display: "flex", gap: "20px", fontSize: "14px", color: "#64748b" }}>
                                                        <span>Inquiries: {product.inquiryCount}</span>
                                                        <span>Chats: {product.chatCount}</span>
                                                        <span>Invoices: {product.invoiceCount}</span>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {/* Suggestions */}
                                {analytics.suggestions && analytics.suggestions.length > 0 && (
                                    <div className="card" style={{ backgroundColor: "#fff", padding: "30px", borderRadius: "12px", boxShadow: "0 4px 20px rgba(0,0,0,0.05)" }}>
                                        <h3 style={{ marginTop: 0, marginBottom: "20px" }}>Actionable Suggestions</h3>
                                        <ul style={{ paddingLeft: "20px" }}>
                                            {analytics.suggestions.map((suggestion, idx) => (
                                                <li key={idx} style={{ marginBottom: "10px", color: "#64748b", lineHeight: "1.6" }}>
                                                    {suggestion}
                                                </li>
                                            ))}
                                        </ul>
                                    </div>
                                )}
                            </>
                        ) : (
                            <div className="card" style={{ backgroundColor: "#fff", padding: "30px", borderRadius: "12px", boxShadow: "0 4px 20px rgba(0,0,0,0.05)" }}>
                                <p style={{ color: "#64748b", textAlign: "center" }}>
                                    Upgrade to ADVANCED seller to view detailed analytics, charts, and actionable suggestions.
                                </p>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

const MetricCard = ({ title, value }) => (
    <div style={{ backgroundColor: "#fff", padding: "20px", borderRadius: "12px", boxShadow: "0 4px 20px rgba(0,0,0,0.05)" }}>
        <div style={{ fontSize: "12px", color: "#64748b", marginBottom: "8px", fontWeight: "600" }}>{title}</div>
        <div style={{ fontSize: "32px", fontWeight: "700", color: "#1e293b" }}>{value}</div>
    </div>
);

const Field = ({ label, value, name, editing, onChange, type = "text", options = [] }) => (
    <div className="field-group">
        <label style={{ display: "block", fontSize: "12px", color: "#888", marginBottom: "4px" }}>{label}</label>
        {editing ? (
            type === "textarea" ? (
                <textarea name={name} value={value} onChange={onChange} rows="2" style={inputStyle} />
            ) : type === "select" ? (
                <select name={name} value={value} onChange={onChange} style={inputStyle}>
                    {options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                </select>
            ) : (
                <input type={type} name={name} value={value} onChange={onChange} style={inputStyle} />
            )
        ) : (
            <div style={{ padding: "10px", fontSize: "14px", color: "#333", borderBottom: "1px solid #eee" }}>{value || "-"}</div>
        )}
    </div>
);

const inputStyle = {
    width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #ccc", fontSize: "14px"
};

export default SellerDashboard;
