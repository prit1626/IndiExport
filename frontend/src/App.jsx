import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import Unauthorized from "./pages/Unauthorized";
import Login from "./pages/Login";
import Register from "./pages/Register";
import BuyerDashboard from "./pages/BuyerDashboard";
import BuyerProfile from "./pages/BuyerProfile";
import ProductBrowse from "./pages/ProductBrowse";
import ProductDetail from "./pages/ProductDetail";
import BuyerInquiries from "./pages/BuyerInquiries";
import BuyerRFQs from "./pages/BuyerRFQs";
import SellerDashboard from "./pages/SellerDashboard";
import SellerOnboarding from "./pages/SellerOnboarding";
import ProductManagement from "./pages/ProductManagement";
import InquiryInbox from "./pages/InquiryInbox";
import InquiryDetail from "./pages/InquiryDetail";
import RFQListing from "./pages/RFQListing";
import RFQDetail from "./pages/RFQDetail";
import InvoiceManagement from "./pages/InvoiceManagement";
import AdminDashboard from "./pages/AdminDashboard";

const App = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Buyer Routes */}
          <Route element={<ProtectedRoute allowedRoles={["BUYER"]} />}>
            <Route path="/buyer" element={<BuyerDashboard />} />
            <Route path="/buyer/profile" element={<BuyerProfile />} />
            <Route path="/products/browse" element={<ProductBrowse />} />
            <Route path="/products/:id" element={<ProductDetail />} />
            <Route path="/buyer/inquiries" element={<BuyerInquiries />} />
            <Route path="/buyer/rfqs" element={<BuyerRFQs />} />
          </Route>

          {/* Seller Routes */}
          <Route element={<ProtectedRoute allowedRoles={["SELLER"]} />}>
            <Route path="/seller" element={<SellerDashboard />} />
            <Route path="/seller/products" element={<ProductManagement />} />
            <Route path="/seller/inquiries" element={<InquiryInbox />} />
            <Route path="/seller/inquiries/:id" element={<InquiryDetail />} />
            <Route path="/seller/rfqs" element={<RFQListing />} />
            <Route path="/seller/rfqs/:id" element={<RFQDetail />} />
            <Route path="/seller/invoices" element={<InvoiceManagement />} />
            <Route path="/seller/onboard" element={<SellerOnboarding />} />
          </Route>

          {/* Admin Routes */}
          <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
            <Route path="/admin" element={<AdminDashboard />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
