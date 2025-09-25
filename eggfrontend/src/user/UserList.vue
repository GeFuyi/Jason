<template>
  <div class="user-list">
    <el-card shadow="hover">

      <h2>用户管理</h2>
      <el-button style="float: left; margin-bottom: 10px;" type="primary" @click="$router.push('/chatroom')">
        去聊天
      </el-button>

      <!-- 条件筛选 -->
      <div class="filters">
        <el-select class="filter-item" v-model="criteria.username" placeholder="全部用户名" clearable>
          <el-option v-for="u in usernameList" :key="u" :label="u" :value="u"></el-option>
        </el-select>

        <el-select class="filter-item" v-model="criteria.email" placeholder="全部邮箱" clearable>
          <el-option v-for="e in emailList" :key="e" :label="e" :value="e"></el-option>
        </el-select>

        <el-select class="filter-item" v-model="criteria.age" placeholder="全部年龄" clearable>
          <el-option v-for="a in ageList" :key="a" :label="a" :value="a"></el-option>
        </el-select>

        <el-select class="filter-item" v-model="criteria.gender" placeholder="全部性别" clearable>
          <el-option label="男" :value="1"></el-option>
          <el-option label="女" :value="0"></el-option>
        </el-select>

        <el-button class="filter-item" type="primary" @click="onQuery">查询</el-button>
        <el-button class="filter-item" type="warning" @click="onReset">重置</el-button>
        <el-button class="filter-item" type="success" @click="openDialog('add')">新增用户</el-button>
      </div>

      <!-- 用户表格 -->
      <el-table :data="pagedUsers" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="60"/>
        <el-table-column prop="username" label="用户名"/>
        <el-table-column prop="email" label="邮箱"/>
        <el-table-column prop="password" label="密码"/>
        <el-table-column prop="age" label="年龄" width="80"/>
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="scope">
            {{ scope.row.gender === 1 ? '男' : '女' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <el-button type="primary" size="mini" @click="openDialog('edit', scope.row)">编辑</el-button>
            <el-button type="danger" size="mini" @click="confirmDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-info">
        当前页: {{ page }} / {{ totalPages }} ，总条数: {{ total }}
      </div>
      <el-pagination
          background
          layout="sizes, prev, pager, next"
          :page-size="pageSize"
          :current-page="page"
          :total="total"
          :page-sizes="pageSizes"
          @current-change="changePage"
          @size-change="handleSizeChange"
      ></el-pagination>
    </el-card>

    <!-- 新增/编辑用户弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible">
      <el-form :model="formUser" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="formUser.username"></el-input>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formUser.email"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="formUser.password" type="text"></el-input>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input v-model.number="formUser.age" type="number"></el-input>
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="formUser.gender">
            <el-option label="男" :value="1"></el-option>
            <el-option label="女" :value="0"></el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {ref, reactive, onMounted} from "vue";
import egg from '@/utils/egg'
import {ElMessageBox} from "element-plus";

export default {
  name: "UserList",
  setup() {
    const criteria = reactive({username: null, email: null, age: null, gender: null});
    const usernameList = ref([]);
    const emailList = ref([]);
    const ageList = ref([]);

    const pagedUsers = ref([]);
    const page = ref(1);
    const pageSize = ref(10);
    const total = ref(0);
    const totalPages = ref(1);
    const pageSizes = ref([5, 10, 20]);

    const dialogVisible = ref(false);
    const dialogTitle = ref("新增用户");
    const formUser = reactive({id: null, username: "", email: "", password: "", age: null, gender: 1});

    const buildQueryParams = () => {
      const params = {page: page.value, pageSize: pageSize.value};
      if (criteria.username) params.username = criteria.username;
      if (criteria.email) params.email = criteria.email;
      if (criteria.age) params.age = criteria.age;
      if (criteria.gender !== null && criteria.gender !== "") params.gender = criteria.gender;
      return params;
    };

    const fetchOptions = async () => {
      try {
        const [uRes, eRes, aRes] = await Promise.all([
          egg.get(`/user/options/username`),
          egg.get(`/user/options/email`),
          egg.get(`/user/options/age`),
        ]);
        usernameList.value = Array.isArray(uRes) ? uRes : [];
        emailList.value = Array.isArray(eRes) ? eRes : [];
        ageList.value = Array.isArray(aRes) ? aRes : [];
      } catch (err) {
        console.error("获取下拉选项失败:", err.message);
      }
    };

    const fetchUsers = async () => {
      try {
        const res = await egg.get(`/user/query`, {params: buildQueryParams()});
        pagedUsers.value = Array.isArray(res.data) ? res.data : [];
        total.value = res.total || 0;
        totalPages.value = Math.max(1, Math.ceil(total.value / pageSize.value));
      } catch (err) {
        console.error("获取用户列表失败:", err.message);
      }
    };

    const onQuery = async () => {
      page.value = 1;
      await fetchUsers();
    };

    const onReset = async () => {
      criteria.username = null;
      criteria.email = null;
      criteria.age = null;
      criteria.gender = null;
      page.value = 1;
      await fetchUsers();
    };

    const changePage = async (p) => {
      page.value = p;
      await fetchUsers();
    };

    const handleSizeChange = async (newSize) => {
      pageSize.value = newSize;
      page.value = 1;
      await fetchUsers();
    };

    const openDialog = (type, user = null) => {
      dialogTitle.value = type === "add" ? "新增用户" : "编辑用户";
      if (user) {
        Object.assign(formUser, user);
      } else {
        formUser.id = null;
        formUser.username = "";
        formUser.email = "";
        formUser.password = "";
        formUser.age = null;
        formUser.gender = 1;
      }
      dialogVisible.value = true;
    };

    const saveUser = async () => {
      try {
        if (formUser.id) {
          await egg.put(`/user/${formUser.id}`, {...formUser});
        } else {
          await egg.post(`/user`, {...formUser});
        }
        dialogVisible.value = false;
        await fetchOptions();
        await fetchUsers();
      } catch (err) {
        console.error("保存用户失败:", err.message);
      }
    };

    const confirmDelete = async (id) => {
      try {
        await ElMessageBox.confirm("确定要删除该用户吗？", "删除确认", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        });
        await egg.delete(`/user/${id}`);
        await fetchOptions();
        await fetchUsers();
      } catch (err) {
        console.error("删除用户失败:", err.message);
      }
    };

    onMounted(async () => {
      await fetchOptions();
      await fetchUsers();
    });

    return {
      criteria,
      usernameList,
      emailList,
      ageList,
      pagedUsers,
      page,
      pageSize,
      total,
      totalPages,
      pageSizes,
      dialogVisible,
      dialogTitle,
      formUser,
      onQuery,
      onReset,
      changePage,
      handleSizeChange,
      openDialog,
      saveUser,
      confirmDelete,
    };
  },
};
</script>

<style scoped>
.user-list {
  padding: 20px;
}

.filters {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.filter-item {
  flex: 1 1 180px;
  max-width: 150px;
}

.el-select,
.el-input {
  width: 100% !important;
  height: 40px;
}

.el-table th, .el-table td {
  text-align: center;
}

.pagination-info {
  margin: 10px 0;
  text-align: right;
  font-weight: bold;
}
</style>
