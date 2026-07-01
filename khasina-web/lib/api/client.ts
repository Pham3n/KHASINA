import axios from "axios";

const BASE_PORTS = {
    AUTH: 8000,
    CHAT: 8001,
    GAME: 8002
};

// This assumes development is happening on localhost or a shared local IP
const getBaseUrl = (port: number) => `http://localhost:${port}`;

export const authApi = axios.create({
    baseURL: getBaseUrl(BASE_PORTS.AUTH),
    timeout: 10000
});

export const chatApi = axios.create({
    baseURL: getBaseUrl(BASE_PORTS.CHAT),
    timeout: 10000
});

export const gameApi = axios.create({
    baseURL: getBaseUrl(BASE_PORTS.GAME),
    timeout: 10000
});

// Interceptor for adding JWT
authApi.interceptors.request.use((config) => {
    const token = typeof window !== "undefined" ? localStorage.getItem("access_token") : null;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Repeat for others if they need auth
chatApi.interceptors.request.use((config) => {
    const token = typeof window !== "undefined" ? localStorage.getItem("access_token") : null;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

gameApi.interceptors.request.use((config) => {
    const token = typeof window !== "undefined" ? localStorage.getItem("access_token") : null;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
