<template>
  <div class="app-container">
    <el-card>
      <template #header><div class="clearfix">
        <h2 style="margin:0"><i class="el-icon-guide" /> 项目实施与工作量验收控制台</h2>
      </div></template>

      <!-- 顶部统计概览 -->
      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :span="8">
          <el-card shadow="hover" style="background:#f2f6fc">
            <div style="font-size:14px; color:#909399">整体实施进度</div>
            <el-progress :percentage="totalProgress" :status="totalProgress === 100 ? 'success' : null" :stroke-width="12" style="margin-top:10px;" />
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" style="background:#f2f6fc">
            <div style="font-size:14px; color:#909399">已验收任务数</div>
            <div style="font-size:24px; font-weight:bold; color:#67C23A; margin-top:5px;">{{ completedCount }} / {{ totalStepsCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" style="background:#f2f6fc">
            <div style="font-size:14px; color:#909399">已累计实施耗时</div>
            <div style="font-size:24px; font-weight:bold; color:#E6A23C; margin-top:5px;">{{ toFixedHours(totalSpentMiles) }} 小时</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 左侧大纲与右侧子任务明细 -->
      <el-row :gutter="40">
        <el-col :span="6">
          <el-steps :active="activePhaseIndex" direction="vertical" style="height: 600px;">
            <el-step v-for="(phase, index) in phases" :key="index" :title="phase.name" :description="phase.desc" />
          </el-steps>
        </el-col>

        <el-col :span="18">
          <el-tabs v-model="activePhaseName" type="border-card">
            <el-tab-pane v-for="(phase, index) in phases" :key="index" :label="phase.name" :name="phase.code">
              <h3>阶段核心实施目标</h3>
              <p style="color:#606266; font-size:14px; margin-bottom:20px;">{{ phase.desc }}</p>

              <el-table :data="phase.steps" style="width: 100%" border :row-key="row => row.stepCode">
                <el-table-column prop="stepName" label="核心推进子项" min-width="200" />
                <el-table-column label="操作捷径" width="120" align="center">
                  <template #default="scope">
                    <el-button v-if="scope.row.route" text icon="el-icon-link" @click="gotoRoute(scope.row.route)">去配置</el-button>
                    <span v-else style="color:#C0C4CC">-无-</span>
                  </template>
                </el-table-column>
                <el-table-column label="目前状态" width="120" align="center">
                  <template #default="scope">
                    <el-tag :type="getStatusType(scope.row.record.status)">{{ getStatusName(scope.row.record.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="耗时(分)" width="100" align="center">
                  <template #default="scope">
                    <span style="font-weight:bold; color:#E6A23C;">{{ scope.row.record.timeSpentMinutes || '--' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作实施验收控制台" width="220" align="center">
                  <template #default="scope">
                    <el-button v-if="scope.row.record.status === 0" type="primary" size="small" @click="doStartTask(phase.code, scope.row)">开始攻坚</el-button>

                    <el-button v-if="scope.row.record.status === 1" type="success" size="small" @click="prepareFinishTask(scope.row)">打卡验收提交</el-button>

                    <el-button v-if="scope.row.record.status === 2" type="warning" size="small" plain @click="doRevertTask(scope.row)">打回重做</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-col>
      </el-row>
    </el-card>

    <!-- 验收提交弹窗 -->
    <el-dialog title="节点验收报告单与工作量申报错" v-model="finishDialogVisible" width="500px">
      <el-form :model="finishForm" label-width="80px">
        <el-form-item label="实施人">
          <el-input v-model="finishForm.implementer" :disabled="true" />
        </el-form-item>
        <el-form-item label="补充笔记">
          <el-input v-model="finishForm.remark" type="textarea" :rows="3" placeholder="填写遇到的坑或特殊参数" />
        </el-form-item>
        <el-form-item label="耗时修正">
          <el-input-number v-model="finishForm.timeSpentMinutes" :min="1" :max="999" />
          <span style="margin-left: 10px; color:#909399; font-size:12px;">自动推算为 {{ autoDiffMins }} 分钟</span>
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button @click="finishDialogVisible = false">取消</el-button>
        <el-button type="success" @click="doFinishTask">确认验收归档</el-button>
      </div></template>
    </el-dialog>

  </div>
</template>

<script>
import { getAllTasks, startTask, finishTask, revertTask } from '@/api/implement'

export default {
  name: 'ImplementationWizard',
  data() {
    return {
      activePhaseName: 'PHASE1_SETUP',
      finishDialogVisible: false,
      finishForm: {
        stepCode: '',
        remark: '',
        timeSpentMinutes: 0,
        implementer: ''
      },
      autoDiffMins: 0,

      // 树状实施模型地图
      phases: [
        {
          code: 'PHASE1_SETUP',
          name: '1. 基础设施底座与连通',
          desc: '检验并连通项目运行所必须的中间件基座池，包含数据库拉通、网络互通断点检查',
          steps: [
            { stepCode: 'STEP1_REDIS', stepName: 'Redis 连通与分布式锁基座校验', route: '' },
            { stepCode: 'STEP1_DB', stepName: '主从业务数据库初始化与预热', route: '' }
          ]
        },
        {
          code: 'PHASE2_BUSINESS',
          name: '2. 院内业务树状模型建联',
          desc: '在系统中精准还原、刻画医院真实运行的物理分区、建筑、科室与医护账号',
          steps: [
            { stepCode: 'STEP2_DEPT', stepName: '构建物理区域与虚拟业务逻辑科室', route: '/hospitalInfo/DeptManages' },
            { stepCode: 'STEP2_WORKER', stepName: '导入或映射所有的本院医护职员', route: '/hospitalInfo/DoctorProfile' }
          ]
        },
        {
          code: 'PHASE3_API',
          name: '3. 内部 HIS/LIS/PACS 系统融合对接',
          desc: '实施并干预医院内数据，打通外部患者的队列池注入系统',
          steps: [
            { stepCode: 'STEP3_CALL_API', stepName: '队列池写入接口全域跑通', route: '' }
          ]
        },
        {
          code: 'PHASE4_HARDWARE',
          name: '4. 分流屏与硬件投放设备挂载',
          desc: '对所有的物理终端节点（显示器、报到机、一体机）进行IP认证绑定与心跳探活',
          steps: [
            { stepCode: 'STEP4_BIGSCREEN', stepName: '候诊区大屏（SCREEN）绑定与模板路由', route: '/deviceManage/ScreenManages?terminalType=SCREEN' },
            { stepCode: 'STEP4_SMALLSCREEN', stepName: '诊室门边屏（CLINIC）绑定与显示配置', route: '/deviceManage/ScreenManages?terminalType=CLINIC' },
            { stepCode: 'STEP4_CLINIC_CFG', stepName: '门边屏脱敏与轮询配置', route: '/clinicConfig/index' }
          ]
        },
        {
          code: 'PHASE5_LINKAGE',
          name: '5. 全管线规则配置调拨与联合体测',
          desc: '基于各条业务大盘，调整自动过号、复诊优先、TSP智能寻路等超高阶能力',
          steps: [
            { stepCode: 'STEP5_OP', stepName: '门诊流水线穿插与防呆规则', route: '/businessConfig/ruleCenter' },
            { stepCode: 'STEP5_PHARMACY', stepName: '药房子干预及抢占规则', route: '/businessConfig/ruleCenter' },
            { stepCode: 'STEP5_EXAM', stepName: '体检超大网络沙盘 TSP 路由网规则', route: '/businessConfig/ruleCenter' }
          ]
        }
      ],

      // 所有打卡快照
      taskRecords: []
    }
  },
  computed: {
    totalStepsCount() {
      return this.phases.reduce((sum, phase) => sum + phase.steps.length, 0)
    },
    completedCount() {
      return this.taskRecords.filter(t => t.status === 2).length
    },
    totalProgress() {
      if (this.totalStepsCount === 0) return 0
      return Math.floor((this.completedCount / this.totalStepsCount) * 100)
    },
    totalSpentMiles() {
      return this.taskRecords.reduce((sum, t) => sum + (t.timeSpentMinutes || 0), 0)
    },
    activePhaseIndex() {
      for (let i = 0; i < this.phases.length; i++) {
        if (this.phases[i].code === this.activePhaseName) return i
      }
      return 0
    }
  },
  created() {
    this.finishForm.implementer = this.$store.getters.name || '当前用户'
    this.refreshData()
  },
  methods: {
    toFixedHours(val) {
      return Number(val / 60).toFixed(1)
    },
    refreshData() {
      getAllTasks().then(res => {
        if (res.success || res.code === 200) {
          this.taskRecords = res.data || []
          this.bindRecordsToModel()
        }
      })
    },
    bindRecordsToModel() {
      this.phases.forEach(phase => {
        phase.steps.forEach(step => {
          const matched = this.taskRecords.find(r => r.stepCode === step.stepCode)
          if (matched) {
            step.record = matched
          } else {
            step.record = { status: 0, timeSpentMinutes: 0 }
          }
        })
      })
    },
    getStatusName(status) {
      if (status === 1) return '⚔️ 进行中/计费中'
      if (status === 2) return '✅ 已验收存档'
      return '未启动'
    },
    getStatusType(status) {
      if (status === 1) return 'warning'
      if (status === 2) return 'success'
      return 'info'
    },
    gotoRoute(path) {
      this.$router.push(path)
    },

    // 开始任务
    doStartTask(phaseCode, step) {
      const payload = {
        stepCode: step.stepCode,
        stepName: step.stepName,
        phaseCode: phaseCode,
        implementer: this.finishForm.implementer
      }
      startTask(payload).then(res => {
        this.$message.success(res.msg || '开启倒计时！')
        this.refreshData()
      })
    },

    // 准备验收
    prepareFinishTask(step) {
      this.finishForm.stepCode = step.stepCode
      this.finishForm.remark = step.record.remark || ''

      const st = new Date(step.record.startTime).getTime()
      const ed = new Date().getTime()
      const diffMins = Math.floor((ed - st) / (1000 * 60))
      this.autoDiffMins = diffMins > 0 ? diffMins : 1
      this.finishForm.timeSpentMinutes = this.autoDiffMins

      this.finishDialogVisible = true
    },

    doFinishTask() {
      finishTask(this.finishForm).then(res => {
        if (res.success || res.code === 200) {
          this.$message.success('已清算工时并打卡完成！')
          this.finishDialogVisible = false
          this.refreshData()
        }
      })
    },

    doRevertTask(step) {
      this.$confirm('确定要打回重做？这会重开计时。', '警告', { type: 'warning' }).then(() => {
        revertTask({ stepCode: step.stepCode }).then(res => {
          this.$message.success('打回重做成功')
          this.refreshData()
        })
      })
    }
  }
}
</script>

<style scoped>
.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}
.clearfix:after {
  clear: both
}
</style>
