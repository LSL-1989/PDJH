import { Client, IMessage } from '@stomp/stompjs'
// @ts-ignore
import SockJS from 'sockjs-client/dist/sockjs'

const wsUrl = import.meta.env.VITE_WS_URL || '/api/pdjh-ws'

class WebSocketService {
  private client: Client | null = null
  private callback: ((msg: Record<string, unknown>) => void) | null = null

  connect(onMessage: (msg: Record<string, unknown>) => void) {
    this.callback = onMessage
    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.client?.subscribe('/topic/hall', (message: IMessage) => {
          if (message.body && this.callback) {
            try {
              this.callback(JSON.parse(message.body))
            } catch {
              /* ignore */
            }
          }
        })
      }
    })
    this.client.activate()
  }

  disconnect() {
    this.client?.deactivate()
  }
}

export const wsService = new WebSocketService()
