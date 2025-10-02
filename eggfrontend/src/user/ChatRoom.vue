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
              :class="{ active: selectedUsername === null }"
              @click="selectUser(null)"
          >
            群聊
            <span class="status-dot online"></span>
          </div>

          <!-- 用户列表 -->
          <div
              class="user-item"
              v-for="user in users"
              :key="user.username"
              :class="{ active: selectedUsername === user.username }"
              @click="selectUser(user.username)"
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
            :class="['message', msg.fromUsername === this.username ? 'mine' : 'other']"
        >
          <span class="from">
            {{ msg.fromUsername === this.username ? '我' : msg.fromUsername }}
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
      username: null,
      users: [],
      selectedUsername: null,
      messages: [],
      inputMessage: ''
    }
  },
  computed: {
    filteredMessages() {
      return this.messages
          .filter(msg => {
            if (this.selectedUsername === null) return !msg.toUsername
            return (
                (msg.fromUsername === this.selectedUsername && msg.toUsername === this.username) ||
                (msg.fromUsername === this.username && msg.toUsername === this.selectedUsername)
            )
          })
          .sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
    }
  },
  async mounted() {
    this.username = sessionStorage.getItem('username')
    if (!this.username) {
      ElMessage.error('请先登录')
      this.$router.push('/login')
      return
    }

    // 1️⃣ 拉取用户列表
    const usersRes = await egg.get('/user')
    this.users = usersRes.map(u => ({ ...u, online: false }))

    // 2️⃣ 拉取当前在线用户列表，初始化 online 状态
    const onlineRes = await egg.get('/user/online') // 返回 ["user1", "user2", ...]
    const onlineSet = new Set(onlineRes)
    this.users.forEach(u => {
      u.online = onlineSet.has(u.username)
    })

    // 3️⃣ 保证自己永远 online
    const self = this.users.find(u => u.username === this.username)
    if (self) self.online = true

    // 4️⃣ 注册全局回调，WebSocket 推送更新在线列表
    window.updateOnlineUsers = this.updateOnlineUsersList

    // 5️⃣ 拉取历史消息
    this.loadMessagesFromLocal()
    const groupRes = await egg.get('/chat/group-msg')
    this.mergeMessages(groupRes)
    const offlineRes = await egg.get('/user/offline-msg', { params: { username: this.username } })
    this.mergeMessages(offlineRes)
    this.saveMessagesToLocal()

    // 6️⃣ 连接 WebSocket
    connect(this.username, this.onMessageReceived)

    // 7️⃣ 滚动到底部
    this.scrollToBottom()
  },


  methods: {

    updateOnlineUsersList(onlineList) {
      const onlineSet = new Set(onlineList.map(u => String(u)))

      this.users.forEach(u => {
        if (u.username === this.username) return // 自己永远在线
        u.online = onlineSet.has(String(u.username))
      })
    },

    getStorageKey() {
      return `chat_messages_user_${this.username}`
    },

    saveMessagesToLocal() {
      const grouped = {}
      this.messages.forEach(msg => {
        if (!msg.toUsername) return // 群聊不存
        const sessionId = msg.fromUsername === this.username ? msg.toUsername : msg.fromUsername
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
                : `key_${msg.fromUsername}_${msg.toUsername || 'group'}_${msg.content}`

        const exists = this.messages.find(m => {
          const mKey = m.id
              ? `id_${m.id}`
              : m.tempId
                  ? `temp_${m.tempId}`
                  : `key_${m.fromUsername}_${m.toUsername || 'group'}_${m.content}`
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
        if (msg.toUsername) this.saveMessagesToLocal() // 私聊才存
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
      if (msg.toUsername) this.saveMessagesToLocal()
      this.scrollToBottom()
    },

    selectUser(username) {
      this.selectedUsername = username
      this.scrollToBottom()
    },

    sendChatMessage() {
      const content = this.inputMessage.trim()
      if (!content) return

      const tempId = Date.now() + '_' + Math.random().toString(16).slice(2)
      const payload = {
        fromUsername: this.username,
        toUsername: this.selectedUsername,
        content,
        tempId,
        createTime: new Date().toISOString()
      }

      const isSelfMessage = payload.toUsername === this.username
      const isGroupMessage = payload.toUsername === null && payload.fromUsername === this.username

      if (!isSelfMessage && !isGroupMessage) {
        this.pushMessageLocally(payload)
      }

      this.inputMessage = ''
      sendMessage(payload.fromUsername, payload.toUsername, payload.content, tempId)
    },

    formatTime(datetime) {
      return datetime ? new Date(datetime).toLocaleTimeString() : ''
    }
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  width: 100vw;
}
.user-list { width: 20%; display: flex; flex-direction: column; padding: 10px; background-color: #fafafa; box-sizing: border-box; height: 100%; }
.user-card { display: flex; flex-direction: column; flex: 1; height: 100%; }
.user-scroll { flex: 1; overflow-y: auto; margin-top: 10px; }
.user-item { padding: 10px; border: 1px solid #ddd; border-radius: 6px; cursor: pointer; background-color: #fff; margin-bottom: 6px; display: flex; align-items: center; justify-content: space-between; }
.user-item.active { background-color: #bae7ff; border-color: #91d5ff; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.status-dot.online { background-color: #52c41a; }
.status-dot.offline { background-color: #f5222d; }
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
