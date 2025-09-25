import { createRouter, createWebHistory } from 'vue-router'
import Register from '../user/Register.vue'
import Login from '../user/Login.vue'
import UserList from "@/user/UserList.vue";
import ChatRoom from "@/user/ChatRoom.vue";

const routes = [
    {
        path: '/',
        redirect: '/login' // 默认访问重定向到登录页
    },
    {
        path: '/register',
        name: 'UserRegister',
        component: Register
    },
    {
        path: '/login',
        name: 'UserLogin',
        component: Login
    },
    {
        path: '/userlist',
        name: 'UserList',
        component: UserList
    },
    {
        path: '/chatroom',
        name: 'ChatRoom',
        component: ChatRoom
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
