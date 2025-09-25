import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let stompClient = null
let isConnected = false
let reconnectTimeout = null
let pendingMessages = [] // 掉线期间未发送的消息缓存

export function connect(userId, onMessageReceived) {
    console.log('[chat.js] 开始连接 STOMP，userId=', userId)

    const socket = new SockJS('http://localhost:8090/ws-chat')

    const token = localStorage.getItem('token')  // 从本地取 token

    stompClient = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
            userId: String(userId),
            Authorization: token ? 'Bearer ' + token : ''   // ✅ 加上 JWT
        },
        debug: str => console.log('[STOMP DEBUG]', str),
        reconnectDelay: 0
    })

    stompClient.onConnect = () => {
        console.log('[chat.js] STOMP connected')
        isConnected = true

        // 订阅群聊
        stompClient.subscribe('/topic/group', msg => {
            const message = JSON.parse(msg.body)
            console.log('[STOMP] 收到群聊消息:', message)
            onMessageReceived(message)
        })

        // 订阅私聊
        stompClient.subscribe(`/topic/private-${userId}`, msg => {
            const message = JSON.parse(msg.body)
            console.log('[STOMP] 收到私聊消息:', message)
            onMessageReceived(message)
        })

        console.log('[chat.js] 已订阅: /topic/group 和 /topic/private-' + userId)

        flushPendingMessages()
    }

    stompClient.onStompError = frame => {
        console.error('[STOMP ERROR] broker error: ' + frame.headers['message'])
        console.error('[STOMP ERROR] details: ' + frame.body)
    }

    stompClient.onWebSocketError = ev => console.error('[STOMP ERROR] WebSocket error', ev)

    stompClient.onWebSocketClose = ev => {
        console.warn('[STOMP] WebSocket closed', ev)
        isConnected = false
        if (reconnectTimeout) clearTimeout(reconnectTimeout)
        reconnectTimeout = setTimeout(() => {
            console.log('[STOMP] 尝试重连...')
            connect(userId, onMessageReceived)
        }, 3000)
    }

    stompClient.activate()
}

export function sendMessage(fromUserId, toUserId, content, tempId) {
    const payload = { fromUserId, toUserId, content, tempId }
    console.log('[chat.js] 准备发送消息 payload:', payload)

    if (!stompClient || !isConnected) {
        console.warn('[chat.js] STOMP 未连接，缓存消息')
        pendingMessages.push(payload)
        return
    }

    stompClient.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(payload)
    })
    console.log('[chat.js] 消息已发送:', payload)
}

function flushPendingMessages() {
    if (!pendingMessages.length || !stompClient || !isConnected) return
    console.log('[chat.js] 发送缓存消息', pendingMessages.length)
    pendingMessages.forEach(msg => {
        stompClient.publish({
            destination: '/app/chat.send',
            body: JSON.stringify(msg)
        })
        console.log('[chat.js] 已发送缓存消息:', msg)
    })
    pendingMessages = []
}

export function isStompConnected() { return isConnected }

export function disconnect() {
    if (stompClient) {
        stompClient.deactivate()
        console.log('[chat.js] STOMP disconnected')
        isConnected = false
    }
}
