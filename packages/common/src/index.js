import axios from 'axios';
import { Client } from '@stomp/stompjs';

export * from './terminalBinding.js';
export { installAndroidNativeMock } from './androidNative.mock.js';
export { desensitizeName } from './desensitize.js';

// 统一 Axios 拦截器配置
export const createHttpClient = (baseURL) => {
    const client = axios.create({
        baseURL,
        timeout: 10000
    });

    client.interceptors.request.use(config => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    });

    client.interceptors.response.use(
        response => response.data,
        error => Promise.reject(error)
    );

    return client;
};

// 统一 STOMP 客户端配置
export const createStompClient = (brokerURL, onConnect) => {
    const client = new Client({
        brokerURL,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
    });

    client.onConnect = onConnect;
    
    client.onStompError = (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
    };

    return client;
};
