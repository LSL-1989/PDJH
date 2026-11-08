/**
 * 标准化第三方 WebSocket 对接验证脚本
 * 运行方式 (需要 Node.js 环境)：
 * 1. npm init -y (如果尚无 package.json)
 * 2. npm install ws
 * 3. node test_websocket_integration.js
 */

const WebSocket = require('ws');

// 配置对接目标地址，附带厂商独立 AppKey
const wsUrl = 'ws://localhost:8081/api/ws/integration?appKey=vendor_test_v1';
const ws = new WebSocket(wsUrl);

console.log('正在连接到智能排队标准化事件总线...', wsUrl);

ws.on('open', function open() {
    console.log('✅ 已成功连接！等待事件下发...\n');
    
    // 第三方也可以选择主动向后端发心跳保活
    setInterval(() => {
        ws.send(JSON.stringify({ type: 'PING', timestamp: Date.now() }));
    }, 30000);
});

ws.on('message', function incoming(data) {
    try {
        const payload = JSON.parse(data);
        console.log(`\n🔔 收到推送事件: [${payload.eventType}]`);
        console.log(`- 事件ID: ${payload.eventId}`);
        console.log(`- 时间戳: ${payload.timestamp}`);
        console.log('- 业务数据:');
        console.log(JSON.stringify(payload.data, null, 2));
        
        // --- 可以在此接入各位厂家的真实业务分发 ---
        // if (payload.eventType === 'CALL_MSG') doTtsSpeech(payload.data)
        // if (payload.eventType === 'PATIENT_EXPIRED') cancelHisTicket(payload.data)

    } catch (err) {
        console.error('⚠️ 解析非标准消息头:', data.toString());
    }
});

ws.on('close', function close(code, reason) {
    console.log(`❌ 连接已断开 (code: ${code}): ${reason}`);
    // 在真实应用中，第三方厂家通常需要在此实现指数退避重连逻辑
});

ws.on('error', function error(err) {
    console.error('🔥 WebSocket 发生异常:', err.message);
});
