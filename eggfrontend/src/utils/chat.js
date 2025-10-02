import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let stompClient = null
let isConnected = false
let reconnectTimeout = null
let pendingMessages = [] // 掉线期间未发送的消息缓存

/**
 * 连接 STOMP WebSocket
 * @param {string} username 用户名
 * @param {function} onMessageReceived 收到消息回调
 */
export function connect(username, onMessageReceived) {
    console.log('[chat.js] 开始连接 STOMP，username=', username)

    const socket = new SockJS(`http://${window.location.hostname}:8090/ws-chat`)
    const token = localStorage.getItem('token')  // 从本地取 token

    stompClient = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
            username: username,       // header 名称改为 username
            Authorization: token ? 'Bearer ' + token : ''
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
        stompClient.subscribe(`/topic/private-${username}`, msg => {
            const message = JSON.parse(msg.body)
            console.log('[STOMP] 收到私聊消息:', message)
            onMessageReceived(message)
        })

        // ✅ 订阅在线用户
        stompClient.subscribe('/topic/online-users', msg => {
            const onlineList = JSON.parse(msg.body)  // array of usernames
            console.log('[STOMP] 在线用户更新:', onlineList)
            if (typeof window.updateOnlineUsers === 'function') {
                window.updateOnlineUsers(onlineList)
            }
        })

        console.log('[chat.js] 已订阅: /topic/group 和 /topic/private-' + username)
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
            connect(username, onMessageReceived)
        }, 3000)
    }

    stompClient.activate()
}

/**
 * 发送消息
 * @param {string} fromUsername 发送者用户名
 * @param {string} toUsername 接收者用户名
 * @param {string} content 消息内容
 * @param {string} tempId 临时 ID
 */
export function sendMessage(fromUsername, toUsername, content, tempId) {
    const payload = { fromUsername, toUsername, content, tempId }
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

/**
 * 发送缓存消息
 */
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

/**
 * 断开连接
 */
export function disconnect() {
    if (stompClient) {
        stompClient.deactivate()
        console.log('[chat.js] STOMP disconnected')
        isConnected = false
    }
}
