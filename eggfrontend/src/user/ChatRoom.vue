<template>
  <div class="chat-container">
    <!-- 左侧用户列表 -->
    <aside class="user-list">
      <div class="user-card">
        <h3>聊天列表</h3>
        <el-button style="float: right; margin-bottom: 10px;" type="primary" @click="$router.push('/userlist')">
          用户管理
        </el-button>
        <el-scrollbar class="user-scroll">
          <!-- 群聊 -->
          <div
              class="user-item"
              :class="{ active: selectedUserId === null }"
              @click="selectUser(null)"
          >
            群聊
            <span class="status-dot online"></span>
          </div>

          <!-- 用户列表 -->
          <div
              class="user-item"
              v-for="user in users"
              :key="user.id"
              :class="{ active: selectedUserId === user.id }"
              @click="selectUser(user.id)"
          >
            {{ user.username }}
            <span class="status-dot" :class="user.online ? 'online' : 'offline'"></span>
          </div>
        </el-scrollbar>
      </div>
    </aside>

    <!-- 右侧聊天区 -->
    <main class="chat-main">
      <div class="messages" ref="messageContainer">
        <div
            v-for="msg in filteredMessages"
            :key="msg.id || msg.tempId"
            :class="['message', msg.fromUserId === Number(userId) ? 'mine' : 'other']"
        >
          <span class="from">
            {{ msg.fromUserId === Number(userId) ? '我' : getUsername(msg.fromUserId) }}
          </span>
          <div class="bubble">
            <span class="content">{{ msg.content }}</span>
          </div>
          <span class="time">{{ formatTime(msg.createTime) }}</span>
        </div>
      </div>

      <!-- 输入框区域 -->
      <div class="input-area">
        <el-input
            v-model="inputMessage"
            placeholder="请输入消息"
            @keyup.enter="sendChatMessage"
            class="input-box"
            clearable
        ></el-input>
        <el-button type="primary" @click="sendChatMessage">发送</el-button>
      </div>
    </main>
  </div>
</template>

<script>
import { connect, sendMessage } from '@/utils/chat'
import egg from '@/utils/egg'
import { ElMessage } from 'element-plus'

export default {
  name: 'ChatRoom',
  data() {
    return {
      userId: null,
      users: [],
      selectedUserId: null,
      messages: [],
      inputMessage: ''
    }
  },
  computed: {
    filteredMessages() {
      return this.messages
          .filter(msg => {
            if (this.selectedUserId === null) return !msg.toUserId
            return (
                (msg.fromUserId === this.selectedUserId && msg.toUserId === Number(this.userId)) ||
                (msg.fromUserId === Number(this.userId) && msg.toUserId === this.selectedUserId)
            )
          })
          .sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
    }
  },
  async mounted() {
    this.userId = Number(sessionStorage.getItem('userId'))
    if (!this.userId) {
      ElMessage.error('请先登录')
      this.$router.push('/login')
      return
    }

    // 拉取用户列表和在线状态
    const usersRes = await egg.get('/user')
    this.users = usersRes.map(u => ({ ...u, online: false }))

    const onlineRes = await egg.get('/user/online')
    const onlineSet = new Set(onlineRes.map(id => Number(id)))
    this.users.forEach(u => {
      u.online = u.id === this.userId || onlineSet.has(u.id)
    })

    // 加载本地缓存（仅私聊消息）
    this.loadMessagesFromLocal()

    // 拉取群聊消息
    const groupRes = await egg.get('/chat/group-msg')
    this.mergeMessages(groupRes)

    // 拉取私聊离线消息
    const offlineRes = await egg.get('/user/offline-msg', { params: { userId: this.userId } })
    this.mergeMessages(offlineRes)

    // 保存私聊消息到 localStorage
    this.saveMessagesToLocal()

    // 连接 WebSocket
    connect(this.userId, this.onMessageReceived)

    this.scrollToBottom()
  },
  methods: {
    getStorageKey() {
      return `chat_messages_user_${this.userId}`
    },

    saveMessagesToLocal() {
      const grouped = {}
      this.messages.forEach(msg => {
        if (!msg.toUserId) return // 群聊不存
        const sessionId = msg.fromUserId === this.userId ? msg.toUserId : msg.fromUserId
        if (!grouped[sessionId]) grouped[sessionId] = []
        if (!grouped[sessionId].some(m => (m.id && m.id === msg.id) || (m.tempId && m.tempId === msg.tempId))) {
          grouped[sessionId].push(msg)
        }
      })
      localStorage.setItem(this.getStorageKey(), JSON.stringify(grouped))
    },

    loadMessagesFromLocal() {
      const key = this.getStorageKey()
      const grouped = JSON.parse(localStorage.getItem(key) || '{}')
      const loaded = []
      Object.values(grouped).forEach(arr => {
        arr.forEach(msg => {
          const exists = loaded.find(
              m => (m.id && m.id === msg.id) || (m.tempId && m.tempId === msg.tempId)
          )
          if (!exists) loaded.push(msg)
        })
      })
      this.messages = loaded
    },

    mergeMessages(newMsgs) {
      newMsgs.forEach(msg => {
        const key = msg.id
            ? `id_${msg.id}`
            : msg.tempId
                ? `temp_${msg.tempId}`
                : `key_${msg.fromUserId}_${msg.toUserId || 'group'}_${msg.content}`

        const exists = this.messages.find(m => {
          const mKey = m.id
              ? `id_${m.id}`
              : m.tempId
                  ? `temp_${m.tempId}`
                  : `key_${m.fromUserId}_${m.toUserId || 'group'}_${m.content}`
          return mKey === key
        })

        if (!exists) {
          this.messages.push(msg)
        } else if (!exists.id && msg.id) {
          const index = this.messages.indexOf(exists)
          this.$set(this.messages, index, { ...msg })
        }
      })
    },

    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messageContainer
        if (container) container.scrollTop = container.scrollHeight
      })
    },

    pushMessageLocally(msg) {
      const exists = this.messages.find(
          m => (m.tempId && m.tempId === msg.tempId) || (m.id && m.id === msg.id)
      )
      if (!exists) {
        this.messages.push(msg)
        if (msg.toUserId) this.saveMessagesToLocal() // 私聊才存
        this.scrollToBottom()
      }
    },

    onMessageReceived(msg) {
      const existing = this.messages.find(
          m => (m.tempId && m.tempId === msg.tempId) || (m.id && m.id === msg.id)
      )
      if (existing) {
        const index = this.messages.indexOf(existing)
        this.$set(this.messages, index, { ...existing, ...msg })
      } else {
        this.messages.push(msg)
      }
      if (msg.toUserId) this.saveMessagesToLocal()
      this.scrollToBottom()
    },

    selectUser(userId) {
      this.selectedUserId = userId
      this.scrollToBottom()
    },

    sendChatMessage() {
      const content = this.inputMessage.trim()
      if (!content) return

      const tempId = Date.now() + '_' + Math.random().toString(16).slice(2)
      const payload = {
        fromUserId: this.userId,
        toUserId: this.selectedUserId,
        content,
        tempId,
        createTime: new Date().toISOString()
      }

      const isSelfMessage = payload.toUserId === this.userId
      const isGroupMessage = payload.toUserId === null && payload.fromUserId === this.userId

      if (!isSelfMessage && !isGroupMessage) {
        this.pushMessageLocally(payload)
      }

      this.inputMessage = ''
      sendMessage(payload.fromUserId, payload.toUserId, payload.content, tempId)
    },

    formatTime(datetime) {
      return datetime ? new Date(datetime).toLocaleTimeString() : ''
    },

    getUsername(id) {
      const user = this.users.find(u => u.id === id)
      return user ? user.username : '未知'
    }
  }
}
</script>

<style scoped>
/* 样式保持原样 */
.chat-container {
  display: flex;
  height: 100vh;
  width: 100vw;
}
/* 左侧用户列表 */
.user-list { width: 20%; display: flex; flex-direction: column; padding: 10px; background-color: #fafafa; box-sizing: border-box; height: 100%; }
.user-card { display: flex; flex-direction: column; flex: 1; height: 100%; }
.user-scroll { flex: 1; overflow-y: auto; margin-top: 10px; }
.user-item { padding: 10px; border: 1px solid #ddd; border-radius: 6px; cursor: pointer; background-color: #fff; margin-bottom: 6px; display: flex; align-items: center; justify-content: space-between; }
.user-item.active { background-color: #bae7ff; border-color: #91d5ff; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.status-dot.online { background-color: #52c41a; }
.status-dot.offline { background-color: #f5222d; }
/* 右侧聊天区 */
.chat-main { width: 80%; display: flex; flex-direction: column; padding: 10px; box-sizing: border-box; background-color: #f0f2f5; height: 100%; }
.messages { flex: 1; overflow-y: auto; padding: 10px; background-color: #ffffff; border-radius: 8px; display: flex; flex-direction: column; gap: 10px; }
.input-area { display: flex; gap: 10px; padding-top: 10px; height: 50px; border-top: 1px solid #ddd; }
.input-box { flex: 1; }
.message { display: flex; flex-direction: column; }
.message.mine { align-items: flex-end; }
.message.other { align-items: flex-start; }
.bubble { max-width: 70%; padding: 10px 14px; border-radius: 16px; word-wrap: break-word; position: relative; background-color: #e6f7ff; }
.message.mine .bubble { background-color: #91d5ff; color: #000; }
.message.mine .bubble::after { content: ""; position: absolute; right: -8px; top: 10px; border-width: 8px 0 8px 8px; border-style: solid; border-color: transparent transparent transparent #91d5ff; }
.message.other .bubble::after { content: ""; position: absolute; left: -8px; top: 10px; border-width: 8px 8px 8px 0; border-style: solid; border-color: transparent #e6f7ff transparent transparent; }
.time { font-size: 12px; color: #999; margin-top: 2px; }
.message.mine .time { text-align: right; }
.message.other .time { text-align: left; }
</style>
