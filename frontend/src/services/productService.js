import api from "./api";

export const addProduct = (productData) => {
    return api.post("/products", productData);
};

export const getMyProducts = () => {
    return api.get("/products/my");
};

export const updateProduct = (id, productData) => {
    return api.put(`/products/${id}`, productData);
};

export const deleteProduct = (id) => {
    return api.delete(`/products/${id}`);
};

export const getAllCountries = () => {
    return api.get("/products/countries");
};

// Buyer endpoints
export const browseProducts = (category = null, search = null) => {
    const params = new URLSearchParams();
    if (category) params.append("category", category);
    if (search) params.append("search", search);
    const queryString = params.toString();
    console.log("queryString", queryString)
    return api.get(`/products/browse${queryString}`);
};

export const getProductDetails = (id) => {
    return api.get(`/products/${id}`);
};