import { onMounted, onUnmounted } from 'vue'
import { Client, type IFrame, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

const proxyBase = import.meta.env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8081'
const defaultWs = import.meta.env.VITE_WS_URL || `${proxyBase.replace(/\/$/, '')}/api/pdjh-ws`

export interface HallMessage {
  type?: string
  code?: string
  callText?: string
  office?: string
  callListId?: number
  state?: number
}

export function useHallWebSocket(
  shouldHandle: (msg: HallMessage) => boolean,
  onRefresh: () => void
) {
  let client: Client | null = null

  const connect = () => {
    try {
      client = new Client({
        webSocketFactory: () => new SockJS(defaultWs),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: (_frame: IFrame) => {
          client?.subscribe('/topic/hall', (message: IMessage) => {
            if (!message.body) return
            try {
              const parsed = JSON.parse(message.body) as HallMessage
              const type = parsed.type
              if (type !== 'CALL_MSG' && type !== 'STATE_UPDATE') return
              if (shouldHandle(parsed)) {
                onRefresh()
              }
            } catch {
              /* silent */
            }
          })
        },
        onStompError: () => {
          /* silent degrade to polling */
        }
      })
      client.activate()
    } catch {
      /* silent */
    }
  }

  const disconnect = () => {
    client?.deactivate()
    client = null
  }

  onMounted(connect)
  onUnmounted(disconnect)

  return { disconnect }
}
