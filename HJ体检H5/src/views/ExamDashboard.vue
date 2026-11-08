<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'

const route = useRoute()
const patientCode = ref(route.query.patientCode as string || '')

const pendingItems = ref<any[]>([])
const completedItems = ref<any[]>([])
const currentItem = ref<any>(null)
const loading = ref(false)
const adjusting = ref(false)
const activeTab = ref(0)

const fetchExamData = async () => {
  if (!patientCode.value) {
    showToast('缺少患者识别码')
    return
  }
  
  loading.value = true
  try {
    const res = await axios.post('/api/exam/getPatientTodoList', { patientCode: patientCode.value })
    const body = res.data || {}
    pendingItems.value = body.pending || []
    completedItems.value = body.completed || []
    currentItem.value = body.current || null
  } catch (error) {
    console.error(error)
    showToast('获取体检数据失败')
  } finally {
    loading.value = false
  }
}

/** 调用已有 POST /exam/adjustQueue，重新计算下一站 */
const handleAdjustQueue = async () => {
  if (!patientCode.value || adjusting.value) return
  adjusting.value = true
  try {
    const res = await axios.post('/api/exam/adjustQueue', { patientCode: patientCode.value })
    if (res.data === true) {
      showToast('已重新安排下一站')
      await fetchExamData()
    } else {
      showToast('调整失败，请联系导检台')
    }
  } catch (error) {
    console.error(error)
    showToast('调整失败，请稍后重试')
  } finally {
    adjusting.value = false
  }
}

onMounted(() => {
  fetchExamData()
})

</script>

<template>
  <div class="exam-dashboard">
    <van-nav-bar title="智能导检大厅" />
    
    <div class="header-card" v-if="currentItem">
        <div class="title">当前进行项目</div>
        <div class="value">{{ currentItem.itemName }}</div>
        <div class="status">目前状态：前方有 {{ currentItem.waitingCount }} 人排队</div>
    </div>
    
    <van-tabs v-model:active="activeTab" sticky>
      <van-tab title="未做项目">
        <van-pull-refresh v-model="loading" @refresh="fetchExamData">
            <van-list v-if="pendingItems.length > 0">
                <van-cell-group inset class="mt-4">
                    <van-cell v-for="item in pendingItems" :key="item.id" :title="item.itemName" :value="item.isFasting ? '需空腹' : '常规'">
                        <template #label>
                            建议科室：{{ item.suggestedDeptName || '待分配' }}
                        </template>
                    </van-cell>
                </van-cell-group>
            </van-list>
            <van-empty v-else description="所有项目已完成" />
        </van-pull-refresh>
        
        <div class="action-bar" v-if="pendingItems.length > 0">
           <van-button type="primary" block round :loading="adjusting" @click="handleAdjustQueue">我想调整当前进度</van-button>
        </div>
      </van-tab>
      
      <van-tab title="已做项目">
         <van-list v-if="completedItems.length > 0">
            <van-cell-group inset class="mt-4">
                <van-cell v-for="item in completedItems" :key="item.id" :title="item.itemName" value="已完成" />
            </van-cell-group>
        </van-list>
         <van-empty v-else description="暂无已完成项目" />
      </van-tab>
    </van-tabs>
  </div>
</template>

<style scoped>
.exam-dashboard {
  min-height: 100vh;
  padding-bottom: 80px;
}
.header-card {
    margin: 16px;
    padding: 20px;
    background: linear-gradient(135deg, #1989fa, #0570db);
    border-radius: 12px;
    color: white;
    box-shadow: 0 4px 12px rgba(25, 137, 250, 0.3);
}
.header-card .title {
    font-size: 14px;
    opacity: 0.9;
}
.header-card .value {
    font-size: 24px;
    font-weight: bold;
    margin: 10px 0;
}
.header-card .status {
    font-size: 13px;
    opacity: 0.8;
}
.mt-4 {
    margin-top: 16px;
}
.action-bar {
    padding: 20px 16px;
}
</style>
