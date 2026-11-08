/** 使用浏览器构建，避免 Vite ESM 下 sockjs-client 主入口缺少 global 导致白屏 */
import SockJS from 'sockjs-client/dist/sockjs'
import { Client } from '@stomp/stompjs'
import { signalrURL } from '@/config'

export function startConnection() {
  const stompClient = new Client({
    webSocketFactory: () => new SockJS(signalrURL),
    debug(str) {
      console.log(str)
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000
  })

  const proxy = {
    handlers: {},
    on(eventName, callback) {
      if (!this.handlers[eventName]) {
        this.handlers[eventName] = []
      }
      this.handlers[eventName].push(callback)
    },
    trigger(eventName, data) {
      if (this.handlers[eventName]) {
        this.handlers[eventName].forEach((cb) => cb(data))
      }
    }
  }

  stompClient.onConnect = function onConnect(frame) {
    console.log('STOMP connected', frame)
    stompClient.subscribe('/topic/hall', function onHall(msg) {
      proxy.trigger('addMessageToList', msg.body)
    })
  }

  stompClient.onStompError = function onStompError(frame) {
    console.error('STOMP Broker reported error: ' + frame.headers.message)
    console.error('Additional details: ' + frame.body)
  }

  stompClient.activate()
  return proxy
}
