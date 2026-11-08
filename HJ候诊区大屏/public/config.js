let baseURL = "/PDJH"; // Http API 走 vue.config.js 代理至 pdjh-server（见 VUE_APP_PROXY_TARGET）
// SockJS + STOMP：须与后端 server.servlet.context-path=/api 及 WebSocketConfig 中 /pdjh-ws 一致
let signalrURL = "http://127.0.0.1:8081/api/pdjh-ws";
export {
    baseURL, signalrURL
}
