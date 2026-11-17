import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
  timeout: 10000,
});

api.interceptors.request.use((config) => {
  const saved = sessionStorage.getItem("shopnexa-session");
  const requestPath = new URL(config.url ?? "", config.baseURL).pathname;
  const isPublicCatalogRead = config.method?.toLowerCase() === "get"
    && /^\/api\/(products|categories)(\/|$)/.test(requestPath);

  if (saved && !isPublicCatalogRead) {
    try {
      const { accessToken } = JSON.parse(saved);
      if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
    } catch {
      sessionStorage.removeItem("shopnexa-session");
    }
  }
  return config;
});

export function getErrorMessage(error) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message
      ?? error.response?.data?.detail
      ?? "Could not connect to ShopNexa. Check that the backend is running.";
  }
  return "Something went wrong. Please try again.";
}

export default api;
