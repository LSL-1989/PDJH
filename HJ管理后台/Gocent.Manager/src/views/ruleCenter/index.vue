<template>
  <div class="rule-center">

    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><i class="el-icon-setting" /></div>
        <div>
          <h2>叫号规则配置中心</h2>
          <span class="header-sub">统一管理门诊 · 药房 · 医技检查 · 体检各业务线的排队调度规则参数</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="warning" plain size="small" icon="el-icon-document" @click="exportConfig">导出配置快照</el-button>
        <el-button plain size="small" icon="el-icon-upload2" @click="triggerImport">导入配置快照</el-button>
        <el-button plain size="small" icon="el-icon-time" @click="openHistory">变更历史</el-button>
        <el-button type="primary" size="small" icon="el-icon-refresh" :loading="loading" @click="loadAllRules">重新加载</el-button>
        <input ref="importFile" type="file" accept="application/json,.json" style="display:none" @change="onImportFile">
      </div>
    </div>

    <!-- 顶部状态提示 -->
    <el-alert
      v-if="saveSuccess"
      title="规则已实时生效，后端调度引擎将在下一个扫描周期（≤2分钟）内读取新配置。"
      type="success"
      :closable="true"
      show-icon
      style="margin-bottom:16px;"
      @close="saveSuccess=false"
    />

    <!-- Tab 分类配置 -->
    <el-tabs v-model="activeTab" type="card" class="rule-tabs">

      <!-- ==================== 门诊规则 ==================== -->
      <el-tab-pane name="outpatient" label="🏥 门诊排队规则">
        <div class="tab-desc">
          <i class="el-icon-info" />
          配置将以 Key=<code>RULE_OUTPATIENT</code> 存入系统配置表，由门诊排队调度引擎实时读取。
        </div>
        <div class="rule-grid">
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-time" style="color:#1A73E8;" /> 超时与过号控制</div>
            <el-form :model="outpatient" label-width="200px" size="small">
              <el-form-item label="呼叫等待超时（分钟）">
                <el-input-number v-model="outpatient.callTimeoutMin" :min="1" :max="60" style="width:150px;" />
                <span class="tip">患者被呼叫后，超过此时间无响应则执行过号（1–60 分钟）</span>
              </el-form-item>
              <el-form-item label="最大连续过号次数">
                <el-input-number v-model="outpatient.maxSkipCount" :min="1" :max="10" style="width:150px;" />
                <span class="tip">连续过号超过此次数，患者移出可叫池（1–10）</span>
              </el-form-item>
              <el-form-item label="呼叫前预叫人数">
                <el-input-number v-model="outpatient.preCallCount" :min="1" :max="10" style="width:150px;" />
                <span class="tip">提前广播N位患者准备就绪，减少等候焦虑</span>
              </el-form-item>
              <el-form-item label="过号后重新排队">
                <el-switch v-model="outpatient.allowReQueue" active-text="允许" inactive-text="禁止" />
                <span class="tip">患者过号后是否允许在同一天重新取号</span>
              </el-form-item>
            </el-form>
          </div>
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-s-flag" style="color:#EA4335;" /> 优先级与分流策略</div>
            <el-form :model="outpatient" label-width="200px" size="small">
              <el-form-item label="复诊患者优先权重">
                <el-slider v-model="outpatient.revisitPriorityWeight" :min="1" :max="10" show-input style="width:300px;" />
                <span class="tip">权重越高，复诊患者越靠前（标准患者权重=1）</span>
              </el-form-item>
              <el-form-item label="老年人（≥65岁）优先">
                <el-switch v-model="outpatient.elderPriority" active-text="开启" inactive-text="关闭" />
              </el-form-item>
              <el-form-item label="急诊患者即插即叫">
                <el-switch v-model="outpatient.emergencyInsert" active-text="开启" inactive-text="关闭" />
                <span class="tip" style="color:#EA4335;">开启后，标记为急诊的患者将立即插队到队头</span>
              </el-form-item>
              <el-form-item label="跨诊室自动分流">
                <el-switch v-model="outpatient.autoSplitEnabled" active-text="开启" inactive-text="关闭" />
                <span class="tip">单诊室队列超过阈值时，自动将溢出患者分流到同科其他诊室</span>
              </el-form-item>
              <el-form-item v-if="outpatient.autoSplitEnabled" label="分流触发阈值（人）">
                <el-input-number v-model="outpatient.splitThreshold" :min="5" :max="50" style="width:150px;" />
              </el-form-item>
            </el-form>
          </div>
          <div class="rule-section full-width">
            <div class="section-title"><i class="el-icon-bell" style="color:#FBBC04;" /> 患者通知设置</div>
            <el-form :model="outpatient" label-width="200px" size="small">
              <el-form-item label="短信通知开关">
                <el-switch v-model="outpatient.smsEnabled" active-text="开启" inactive-text="关闭" />
              </el-form-item>
              <el-form-item label="提前N位提醒患者就位">
                <el-input-number v-model="outpatient.smsAdvanceCount" :min="1" :max="10" style="width:150px;" :disabled="!outpatient.smsEnabled" />
                <span class="tip">当前排队第N位时，提前发送「即将就诊」短信</span>
              </el-form-item>
              <el-form-item label="过号通知">
                <el-switch v-model="outpatient.smsOnSkip" active-text="发送" inactive-text="不发送" :disabled="!outpatient.smsEnabled" />
              </el-form-item>
            </el-form>
          </div>
        </div>
        <div class="save-bar">
          <el-button type="primary" icon="el-icon-check" :loading="saving" @click="saveRule('RULE_OUTPATIENT', outpatient)">保存门诊规则</el-button>
          <el-button plain @click="resetRule('outpatient')">恢复默认值</el-button>
          <span v-if="lastSaveTime.outpatient" class="last-save">最后保存：{{ lastSaveTime.outpatient }}</span>
        </div>
      </el-tab-pane>

      <!-- ==================== 药房规则 ==================== -->
      <el-tab-pane name="pharmacy" label="💊 药房发药规则">
        <div class="tab-desc">
          <i class="el-icon-info" />
          配置将以 Key=<code>RULE_PHARMACY</code> 存入系统配置表，由后端 <code>QueueManager.scanPharmacyTimeout()</code> 定时任务实时读取（每2分钟执行一次）。
        </div>
        <div class="rule-grid">
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-alarm-clock" style="color:#EA4335;" /> 超时与滞留控制</div>
            <el-form :model="pharmacy" label-width="200px" size="small">
              <el-form-item label="取药等待超时（分钟）">
                <el-input-number v-model="pharmacy.timeoutMinutes" :min="5" :max="120" :step="5" style="width:150px;" />
                <span class="tip" style="color:#EA4335;">⚡ 此值直接影响后端定时任务，修改保存即生效</span>
              </el-form-item>
              <el-form-item label="超时告警阈值（分钟）">
                <el-input-number v-model="pharmacy.alertMinutes" :min="5" :max="60" style="width:150px;" />
                <span class="tip">超出此时长时，在药房主任工作台显示红色预警</span>
              </el-form-item>
              <el-form-item label="最大滞留处方数">
                <el-input-number v-model="pharmacy.maxPendingPerWindow" :min="5" :max="50" style="width:150px;" />
                <span class="tip">单窗口超过此数量时，触发拥堵告警</span>
              </el-form-item>
            </el-form>
          </div>
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-s-shop" style="color:#34A853;" /> 窗口分流策略</div>
            <el-form :model="pharmacy" label-width="200px" size="small">
              <el-form-item label="分流算法">
                <el-select v-model="pharmacy.splitAlgorithm" style="width:200px;">
                  <el-option label="最短队列优先 (推荐)" value="SHORTEST_QUEUE" />
                  <el-option label="轮询分配" value="ROUND_ROBIN" />
                  <el-option label="随机分配" value="RANDOM" />
                  <el-option label="固定分配（按处方类型）" value="FIXED_TYPE" />
                </el-select>
              </el-form-item>
              <el-form-item label="急诊处方专用窗口">
                <el-switch v-model="pharmacy.emergencyWindowEnabled" active-text="启用" inactive-text="不启用" />
                <span class="tip">启用后，急诊处方始终分配至急诊药房窗口</span>
              </el-form-item>
              <el-form-item label="中西药分离">
                <el-switch v-model="pharmacy.separateChinaWestern" active-text="分离" inactive-text="混合" />
                <span class="tip">中药/西药处方分配到对应类型窗口</span>
              </el-form-item>
            </el-form>
          </div>
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-message" style="color:#1A73E8;" /> 取药通知与催促</div>
            <el-form :model="pharmacy" label-width="200px" size="small">
              <el-form-item label="自动催取药提醒">
                <el-switch v-model="pharmacy.autoReminderEnabled" active-text="开启" inactive-text="关闭" />
              </el-form-item>
              <el-form-item label="首次提醒（分钟后）">
                <el-input-number v-model="pharmacy.firstReminderMin" :min="5" :max="30" style="width:150px;" :disabled="!pharmacy.autoReminderEnabled" />
              </el-form-item>
              <el-form-item label="重复提醒间隔（分钟）">
                <el-input-number v-model="pharmacy.repeatReminderMin" :min="5" :max="30" style="width:150px;" :disabled="!pharmacy.autoReminderEnabled" />
              </el-form-item>
            </el-form>
          </div>
        </div>
        <div class="save-bar">
          <el-button type="primary" icon="el-icon-check" :loading="saving" @click="saveRule('RULE_PHARMACY', pharmacy)">保存药房规则</el-button>
          <el-button plain @click="resetRule('pharmacy')">恢复默认值</el-button>
          <span v-if="lastSaveTime.pharmacy" class="last-save">最后保存：{{ lastSaveTime.pharmacy }}</span>
        </div>
      </el-tab-pane>

      <!-- ==================== 体检规则 ==================== -->
      <el-tab-pane name="exam" label="🔬 体检路由规则">
        <div class="tab-desc">
          <i class="el-icon-info" />
          配置将以 Key=<code>RULE_EXAM</code> 存入系统配置表，由 <code>ExamRoutingService</code> 的 TSP 路由引擎读取。
        </div>
        <div class="rule-grid">
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-map-location" style="color:#9C27B0;" /> TSP 智能路由算法</div>
            <el-form :model="exam" label-width="200px" size="small">
              <el-form-item label="路由算法模式">
                <el-radio-group v-model="exam.triageAlgorithm">
                  <el-radio-button label="FASTING_FIRST">早高峰空腹优先</el-radio-button>
                  <el-radio-button label="LEAST_WAIT">最少排队优先 (负载均衡)</el-radio-button>
                  <el-radio-button label="TSP_GREEDY">TSP贪心最短路径</el-radio-button>
                  <el-radio-button label="NONE">人工调度分配</el-radio-button>
                </el-radio-group>
                <div class="tip" style="margin-left:0;">
                  <b>贪心</b>：总是去最近/最快的检查室，计算快；
                  <b>动态规划</b>：枚举所有路径求最短总等候时长，精确但大套餐时稍慢；
                  <b>负载均衡</b>：优先去当前队列最短的检查室
                </div>
              </el-form-item>
              <el-form-item label="分时段自动切换算法" style="margin-top:20px;">
                <el-switch v-model="exam.timeScheduleEnabled" active-text="开启分时计划" inactive-text="关闭" />
                <div class="tip" style="margin-left:0;">开启后，系统将按时间段自动覆盖上方设定的基础算法（如：早8点优先空腹，10点后实时负载均衡）。</div>
                <template v-if="exam.timeScheduleEnabled">
                  <el-table :data="exam.timeSchedules" border size="small" style="margin-top:10px;width:550px">
                    <el-table-column label="起始时间" width="120">
                      <template #default="scope">
                        <el-time-select v-model="scope.row.startTime" start="07:00" step="00:30" end="18:00" placeholder="开始" style="width:100%" />
                      </template>
                    </el-table-column>
                    <el-table-column label="结束时间" width="120">
                      <template #default="scope">
                        <el-time-select v-model="scope.row.endTime" start="07:00" step="00:30" end="18:00" :min-time="scope.row.startTime" placeholder="结束" style="width:100%" />
                      </template>
                    </el-table-column>
                    <el-table-column label="应用算法">
                      <template #default="scope">
                        <el-select v-model="scope.row.algorithm" size="small" style="width:100%">
                          <el-option label="空腹优先 (推荐早高峰)" value="FASTING_FIRST" />
                          <el-option label="最少排队优先 (推荐平峰)" value="LEAST_WAIT" />
                          <el-option label="贪心最近优先 (TSP_GREEDY)" value="TSP_GREEDY" />
                          <el-option label="人工调度分配 (NONE)" value="NONE" />
                        </el-select>
                      </template>
                    </el-table-column>
                    <table-action-column :count="1">
                      <template #default="{ $index }">
                        <table-action-icon
                          icon="el-icon-delete"
                          tip="删除时段"
                          type="danger"
                          @click="exam.timeSchedules.splice($index, 1)"
                        />
                      </template>
                    </table-action-column>
                  </el-table>
                  <el-button type="dashed" size="small" icon="el-icon-plus" style="width:550px;margin-top:5px" @click="exam.timeSchedules.push({startTime:'', endTime:'', algorithm:'FASTING_FIRST'})">新增时段</el-button>
                </template>
              </el-form-item>
              <el-form-item label="最大等待时间权重">
                <el-slider v-model="exam.waitTimeWeight" :min="1" :max="10" show-input style="width:300px;" />
                <span class="tip">在路由计算时，等待时长的权重（越高越优先规避拥堵）</span>
              </el-form-item>
              <el-form-item label="距离权重">
                <el-slider v-model="exam.distanceWeight" :min="1" :max="10" show-input style="width:300px;" />
                <span class="tip">检查室间物理距离的路由权重</span>
              </el-form-item>
            </el-form>
          </div>
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-warning-outline" style="color:#EA4335;" /> 体检进度与干预</div>
            <el-form :model="exam" label-width="200px" size="small">
              <el-form-item label="单项超时告警（分钟）">
                <el-input-number v-model="exam.itemTimeoutMin" :min="10" :max="120" :step="5" style="width:150px;" />
                <span class="tip">单个检查项等待超过此时间，触发主任工作台告警</span>
              </el-form-item>
              <el-form-item label="全套预计完成时长（小时）">
                <el-input-number v-model="exam.expectedTotalHours" :min="1" :max="8" :step="0.5" style="width:150px;" :precision="1" />
                <span class="tip">全套体检的预估时长，用于向患者展示预计完成时间</span>
              </el-form-item>
              <el-form-item label="检查科室容量上限">
                <el-input-number v-model="exam.roomCapacityLimit" :min="3" :max="30" style="width:150px;" />
                <span class="tip">单个检查科室同时接待患者上限，超出则路由到备用室</span>
              </el-form-item>
              <el-form-item label="路由实时重算">
                <el-switch v-model="exam.dynamicReroute" active-text="开启" inactive-text="关闭" />
                <span class="tip">开启后，当检查室队列变化时自动为等候患者重新规划路线</span>
              </el-form-item>
            </el-form>
          </div>
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-star-on" style="color:#FBBC04;" /> 特殊人群政策</div>
            <el-form :model="exam" label-width="200px" size="small">
              <el-form-item label="VIP客户专属通道">
                <el-switch v-model="exam.vipFastTrack" active-text="启用" inactive-text="关闭" />
                <span class="tip">VIP/团检客户优先进入队列（不影响紧急项目）</span>
              </el-form-item>
              <el-form-item label="空腹项目优先安排">
                <el-switch v-model="exam.fastingItemFirst" active-text="优先" inactive-text="正常顺序" />
                <span class="tip">自动将血液检查、腹部彩超等空腹项目安排到体检路线起点</span>
              </el-form-item>
              <el-form-item label="老年人路线简化">
                <el-switch v-model="exam.elderSimplifyRoute" active-text="启用" inactive-text="不启用" />
                <span class="tip">老年患者减少步行距离，优先就近安排</span>
              </el-form-item>
            </el-form>
          </div>
        </div>
        <div class="save-bar">
          <el-button type="primary" icon="el-icon-check" :loading="saving" @click="saveRule('RULE_EXAM', exam)">保存体检规则</el-button>
          <el-button plain @click="resetRule('exam')">恢复默认值</el-button>
          <span v-if="lastSaveTime.exam" class="last-save">最后保存：{{ lastSaveTime.exam }}</span>
        </div>
      </el-tab-pane>

      <!-- ==================== 医技科室规则 ==================== -->
      <el-tab-pane name="medtech" label="🏨 医技科室规则">
        <div class="tab-desc" style="border-left-color:#0097A7;background:#E0F7FA;color:#00838F;">
          <i class="el-icon-info" />
          各医技科室（检验科/超声科/放射科/心电图室）的独立叫号规则，以 Key=<code>RULE_MEDICAL_TECH</code> 统一存储，各科室独立配置互不干扰。
        </div>

        <!-- 科室Tab切换 -->
        <el-tabs v-model="medtechTab" type="border-card" class="inner-tabs">
          <!-- ===== 医技全科室共性参数 ===== -->
          <el-tab-pane name="global" label="🌐 全局协同参数">
            <div class="rule-grid">
              <div class="rule-section" style="width:100%">
                <div class="section-title"><i class="el-icon-connection" style="color:#FB8C00;" /> 跨系统联动</div>
                <el-form :model="medtech.global" label-width="210px" size="small">
                  <el-form-item label="HIS缴费联动延迟（秒）">
                    <el-input-number v-model="medtech.global.paymentSyncDelaySec" :min="0" :max="60" style="width:150px;" />
                    <span class="tip">患者完成缴费后，延迟多久将数据写入排队系统（防HIS还没生效就去取号导致空数据）。设置为 0 代表即时推送。</span>
                  </el-form-item>
                  <el-form-item label="多科室提醒聚合推送">
                    <el-switch v-model="medtech.global.multiDeptMsgEnabled" active-text="合并推送" inactive-text="分开推送" />
                    <span class="tip">当患者同时开具3个科室检查单时，开启合并推送只发一条"您有3项检查待做"的消息，防骚扰。</span>
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-tab-pane>

          <!-- ===== 检验科 ===== -->
          <el-tab-pane name="lab" label="🩸 检验科（抽血）">
            <div class="medtech-tag">调度模型：多窗口批量服务 · 快进快出 · 峰值明显（早8-10点）</div>
            <div class="rule-grid">
              <div class="rule-section">
                <div class="section-title"><i class="el-icon-office-building" style="color:#1A73E8;" /> 窗口配置</div>
                <el-form :model="medtech.lab" label-width="210px" size="small">
                  <el-form-item label="采血窗口总数">
                    <el-input-number v-model="medtech.lab.windowCount" :min="1" :max="30" style="width:140px;" />
                    <span class="tip">系统并联叫号的最大窗口数量</span>
                  </el-form-item>
                  <el-form-item label="单窗口积压告警阈值（人）">
                    <el-input-number v-model="medtech.lab.maxPendingPerWindow" :min="3" :max="30" style="width:140px;" />
                    <span class="tip">超过此人数时，主任工作台显示拥堵告警</span>
                  </el-form-item>
                  <el-form-item label="叫号超时（分钟）">
                    <el-input-number v-model="medtech.lab.callTimeoutMin" :min="2" :max="20" style="width:140px;" />
                    <span class="tip">被叫到后超时未到采血窗口，执行过号</span>
                  </el-form-item>
                  <el-form-item label="窗口分流算法">
                    <el-select v-model="medtech.lab.splitAlgorithm" style="width:200px;">
                      <el-option label="最短队列优先（推荐）" value="SHORTEST_QUEUE" />
                      <el-option label="轮询分配" value="ROUND_ROBIN" />
                      <el-option label="随机分配" value="RANDOM" />
                    </el-select>
                  </el-form-item>
                </el-form>
              </div>
              <div class="rule-section">
                <div class="section-title"><i class="el-icon-s-flag" style="color:#EA4335;" /> 优先级与特殊规则</div>
                <el-form :model="medtech.lab" label-width="210px" size="small">
                  <el-form-item label="空腹项目早高峰优先">
                    <el-switch v-model="medtech.lab.fastingPriorityEnabled" active-text="启用" inactive-text="关闭" />
                    <span class="tip">早上空腹患者优先叫号，避免空腹时间超窗（建议截止11:00）</span>
                  </el-form-item>
                  <el-form-item v-if="medtech.lab.fastingPriorityEnabled" label="空腹优先截止时间">
                    <el-time-select v-model="medtech.lab.fastingCutoffTime" start="07:00" step="00:30" end="12:00" placeholder="选择截止时间" style="width:150px;" />
                  </el-form-item>
                  <el-form-item label="急诊标本优先叫号">
                    <el-switch v-model="medtech.lab.emergencyPriority" active-text="启用" inactive-text="关闭" />
                    <span class="tip" style="color:#EA4335;">急诊采血单立即插队到当前叫号窗口队首</span>
                  </el-form-item>
                  <el-form-item label="同患者多管合并叫号">
                    <el-switch v-model="medtech.lab.mergeMultiTube" active-text="合并" inactive-text="拆分" />
                    <span class="tip">同一患者多个检验项目合并为一次采血叫号</span>
                  </el-form-item>
                  <el-form-item label="老年人优先">
                    <el-switch v-model="medtech.lab.elderPriority" active-text="启用" inactive-text="关闭" />
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-tab-pane>

          <!-- ===== 超声科 ===== -->
          <el-tab-pane name="ultrasound" label="🔊 超声科（B超/彩超）">
            <div class="medtech-tag" style="background:#FFF3E0;color:#E65100;border-color:#E65100;">调度模型：多检查室分流 · 检查时间长（10-30分钟）· 支持预约+当日混合模式</div>
            <div class="rule-grid">
              <div class="rule-section">
                <div class="section-title"><i class="el-icon-s-home" style="color:#E65100;" /> 检查室配置</div>
                <el-form :model="medtech.ultrasound" label-width="210px" size="small">
                  <el-form-item label="检查室数量">
                    <el-input-number v-model="medtech.ultrasound.roomCount" :min="1" :max="20" style="width:140px;" />
                    <span class="tip">超声科当前开放的检查室总数（影响并联叫号上限）</span>
                  </el-form-item>
                  <el-form-item label="单室积压告警阈值（人）">
                    <el-input-number v-model="medtech.ultrasound.maxPendingPerRoom" :min="3" :max="20" style="width:140px;" />
                  </el-form-item>
                  <el-form-item label="叫号超时（分钟）">
                    <el-input-number v-model="medtech.ultrasound.callTimeoutMin" :min="5" :max="30" style="width:140px;" />
                  </el-form-item>
                </el-form>
              </div>
              <div class="rule-section">
                <div class="section-title"><i class="el-icon-date" style="color:#E65100;" /> 预约模式</div>
                <el-form :model="medtech.ultrasound" label-width="210px" size="small">
                  <el-form-item label="启用预约制">
                    <el-switch v-model="medtech.ultrasound.appointmentEnabled" active-text="启用" inactive-text="纯当日" />
                    <span class="tip">启用后，患者可提前预约时间段，减少现场等待</span>
                  </el-form-item>
                  <el-form-item v-if="medtech.ultrasound.appointmentEnabled" label="预约患者优先权重">
                    <el-slider v-model="medtech.ultrasound.appointmentWeight" :min="1" :max="10" show-input style="width:280px;" />
                    <span class="tip">相对当日取号患者的优先级权重（当日患者=1）</span>
                  </el-form-item>
                  <el-form-item label="按超声类型分检查室">
                    <el-switch v-model="medtech.ultrasound.splitByType" active-text="分类" inactive-text="混合" />
                    <span class="tip">腹部超声/心脏超声/血管超声分配到对应专室</span>
                  </el-form-item>
                  <el-form-item label="空腹患者早上优先">
                    <el-switch v-model="medtech.ultrasound.fastingPriorityEnabled" active-text="启用" inactive-text="关闭" />
                    <span class="tip">腹部超声等需空腹的检查，早上优先叫号</span>
                  </el-form-item>
                  <el-form-item label="急诊患者优先">
                    <el-switch v-model="medtech.ultrasound.emergencyPriority" active-text="启用" inactive-text="关闭" />
                  </el-form-item>
                </el-form>
              </div>
              <div class="rule-section full-width">
                <div class="section-title"><i class="el-icon-time" style="color:#E65100;" /> 等候时间预估配置</div>
                <el-form :model="medtech.ultrasound" label-width="210px" size="small">
                  <el-form-item label="腹部超声平均耗时（分钟）">
                    <el-input-number v-model="medtech.ultrasound.avgTimeAbdomen" :min="5" :max="60" style="width:140px;" />
                    <span class="tip">用于大屏预估等候时间计算</span>
                  </el-form-item>
                  <el-form-item label="心脏超声平均耗时（分钟）">
                    <el-input-number v-model="medtech.ultrasound.avgTimeHeart" :min="5" :max="60" style="width:140px;" />
                  </el-form-item>
                  <el-form-item label="血管/其他超声平均耗时（分钟）">
                    <el-input-number v-model="medtech.ultrasound.avgTimeOther" :min="5" :max="60" style="width:140px;" />
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-tab-pane>

          <!-- ===== 放射科 ===== -->
          <el-tab-pane name="radiology" label="☢️ 放射科（CT/X光）">
            <div class="medtech-tag" style="background:#F3E5F5;color:#6A1B9A;border-color:#6A1B9A;">调度模型：强制预约制（CT）· 设备分线叫号 · 时段配额精控</div>
            <div class="rule-grid">
              <div class="rule-section">
                <div class="section-title"><i class="el-icon-cpu" style="color:#6A1B9A;" /> 设备与配额</div>
                <el-form :model="medtech.radiology" label-width="210px" size="small">
                  <el-form-item label="CT设备数量">
                    <el-input-number v-model="medtech.radiology.ctMachineCount" :min="1" :max="10" style="width:140px;" />
                    <span class="tip">每台CT独立叫号队列</span>
                  </el-form-item>
                  <el-form-item label="MRI设备数量">
                    <el-input-number v-model="medtech.radiology.mriMachineCount" :min="0" :max="10" style="width:140px;" />
                  </el-form-item>
                  <el-form-item label="CT每时段配额（人）">
                    <el-input-number v-model="medtech.radiology.ctSlotCapacity" :min="1" :max="10" style="width:140px;" />
                    <span class="tip">每15分钟时间段内可预约的CT检查人数上限</span>
                  </el-form-item>
                  <el-form-item label="X光是否采用预约制">
                    <el-switch v-model="medtech.radiology.xrayAppointmentEnabled" active-text="预约制" inactive-text="自助取号" />
                    <span class="tip">X光检查快（5分钟），通常无需预约</span>
                  </el-form-item>
                </el-form>
              </div>
              <div class="rule-section">
                <div class="section-title"><i class="el-icon-warning-outline" style="color:#6A1B9A;" /> 特殊流程配置</div>
                <el-form :model="medtech.radiology" label-width="210px" size="small">
                  <el-form-item label="造影剂等待专属区">
                    <el-switch v-model="medtech.radiology.contrastWaitArea" active-text="启用" inactive-text="关闭" />
                    <span class="tip">需注射造影剂的CT，设独立等候区，注射后等候30-60min再叫号</span>
                  </el-form-item>
                  <el-form-item v-if="medtech.radiology.contrastWaitArea" label="造影等待时长（分钟）">
                    <el-input-number v-model="medtech.radiology.contrastWaitMin" :min="15" :max="90" :step="5" style="width:140px;" />
                  </el-form-item>
                  <el-form-item label="报告出具预估（CT，小时）">
                    <el-input-number v-model="medtech.radiology.ctReportHours" :min="0.5" :max="6" :step="0.5" :precision="1" style="width:140px;" />
                    <span class="tip">报告出具后自动推送通知给患者</span>
                  </el-form-item>
                  <el-form-item label="危急值加急通知">
                    <el-switch v-model="medtech.radiology.criticalValueAlert" active-text="启用" inactive-text="关闭" />
                    <span class="tip" style="color:#EA4335;">影像发现危急值时，立即通知患者和开单医生</span>
                  </el-form-item>
                  <el-form-item label="急诊CT绿色通道">
                    <el-switch v-model="medtech.radiology.emergencyCtFastTrack" active-text="启用" inactive-text="关闭" />
                    <span class="tip">急诊CT单直接跳过预约队列，当前设备完成当前检查后立即安排</span>
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-tab-pane>

          <!-- ===== 心电图室 ===== -->
          <el-tab-pane name="ecg" label="❤️ 心电图室">
            <div class="medtech-tag" style="background:#FCE4EC;color:#B71C1C;border-color:#C62828;">调度模型：快速轮转 · 多检查床并联 · 急性胸痛10分钟目标</div>
            <div class="rule-grid">
              <div class="rule-section">
                <div class="section-title"><i class="el-icon-monitor" style="color:#C62828;" /> 检查床配置</div>
                <el-form :model="medtech.ecg" label-width="210px" size="small">
                  <el-form-item label="普通心电检查床数">
                    <el-input-number v-model="medtech.ecg.bedCount" :min="1" :max="20" style="width:140px;" />
                    <span class="tip">每张检查床独立叫号，系统自动分配最空闲的床</span>
                  </el-form-item>
                  <el-form-item label="Holter（动态心电）设备数">
                    <el-input-number v-model="medtech.ecg.holterDeviceCount" :min="0" :max="20" style="width:140px;" />
                    <span class="tip">Holter流程独立（需佩戴24小时），与普通心电分开叫号</span>
                  </el-form-item>
                  <el-form-item label="叫号超时（分钟）">
                    <el-input-number v-model="medtech.ecg.callTimeoutMin" :min="2" :max="20" style="width:140px;" />
                  </el-form-item>
                  <el-form-item label="检查平均耗时（分钟）">
                    <el-input-number v-model="medtech.ecg.avgExamMin" :min="3" :max="20" style="width:140px;" />
                    <span class="tip">用于大屏等候时间预估计算（普通心电约5-8分钟）</span>
                  </el-form-item>
                </el-form>
              </div>
              <div class="rule-section">
                <div class="section-title"><i class="el-icon-alarm-clock" style="color:#C62828;" /> 急诊绿色通道</div>
                <el-form :model="medtech.ecg" label-width="210px" size="small">
                  <el-form-item label="急诊心电10分钟目标">
                    <el-switch v-model="medtech.ecg.emergencyGreenChannel" active-text="启用" inactive-text="关闭" />
                    <span class="tip" style="color:#C62828;">⚠️ 医疗安全要求：急性胸痛患者的心电图必须在10分钟内完成，启用后急诊单插队队首</span>
                  </el-form-item>
                  <el-form-item v-if="medtech.ecg.emergencyGreenChannel" label="目标完成时限（分钟）">
                    <el-input-number v-model="medtech.ecg.emergencyTargetMin" :min="5" :max="30" style="width:140px;" />
                    <el-tag type="danger" size="small" style="margin-left:8px;">超过此时限触发医疗安全告警</el-tag>
                  </el-form-item>
                  <el-form-item label="老年人优先">
                    <el-switch v-model="medtech.ecg.elderPriority" active-text="启用" inactive-text="关闭" />
                  </el-form-item>
                  <el-form-item label="报告出具通知">
                    <el-switch v-model="medtech.ecg.reportNotify" active-text="启用" inactive-text="关闭" />
                    <span class="tip">心电图报告即时出具后，推送通知给患者和开单医生</span>
                  </el-form-item>
                </el-form>
              </div>
              <div class="rule-section full-width">
                <div class="section-title"><i class="el-icon-info" style="color:#C62828;" /> 跨场景区分（门诊检查 vs 体检导检）</div>
                <div style="padding:12px;background:#fff5f5;border-radius:8px;border:1px solid #ffcdd2;font-size:13px;line-height:1.8;color:#4a4a4a;">
                  <p style="margin:0 0 8px;"><b>🔴 门诊检查患者</b>（由医生开单）：患者自行前来取号，进入普通心电等候队列，按先后顺序叫号。系统<b>不主动推送</b>，患者自主掌握节奏。</p>
                  <p style="margin:0;"><b>🟣 体检导检患者</b>（由 ExamRoutingService 派单）：系统主动将患者推入心电图室排队，完成后自动派发体检下一站。两类患者需在叫号大屏上做视觉区分（如体检患者加「体检」标签）。</p>
                </div>
              </div>
            </div>
          </el-tab-pane>

        </el-tabs>

        <div class="save-bar">
          <el-button type="primary" icon="el-icon-check" :loading="saving" @click="saveRule('RULE_MEDICAL_TECH', medtech)">保存医技规则</el-button>
          <el-button plain @click="resetRule('medtech')">恢复默认值</el-button>
          <el-tag type="warning" size="small" style="margin-left:8px;">各科室配置统一存储，保存一次全部生效</el-tag>
          <span v-if="lastSaveTime.medtech" class="last-save">最后保存：{{ lastSaveTime.medtech }}</span>
        </div>
      </el-tab-pane>

      <!-- ==================== 全局通知规则 ==================== -->
      <el-tab-pane name="notification" label="📢 全局通知规则">
        <div class="tab-desc">
          <i class="el-icon-info" />
          配置将以 Key=<code>RULE_NOTIFICATION</code> 存入，由 <code>MessageNotificationService</code> 读取，控制短信/微信推送的触发时机。
        </div>
        <div class="rule-grid">
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-bell" style="color:#1A73E8;" /> 通知渠道开关</div>
            <el-form :model="notification" label-width="200px" size="small">
              <el-form-item label="短信通知（SMS）">
                <el-switch v-model="notification.smsEnabled" active-text="开启" inactive-text="关闭" />
              </el-form-item>
              <el-form-item label="微信公众号推送">
                <el-switch v-model="notification.wechatEnabled" active-text="开启" inactive-text="关闭" />
              </el-form-item>
              <el-form-item label="院内广播语音">
                <el-switch v-model="notification.voiceEnabled" active-text="开启" inactive-text="关闭" />
              </el-form-item>
            </el-form>
          </div>
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-chat-dot-square" style="color:#34A853;" /> 触发事件配置</div>
            <el-form :model="notification" label-width="200px" size="small">
              <el-form-item label="取号成功通知">
                <el-checkbox-group v-model="notification.eventsTrigger">
                  <el-checkbox label="ON_QUEUE">入队时通知</el-checkbox>
                  <el-checkbox label="ON_CALLED">叫号时通知</el-checkbox>
                  <el-checkbox label="ON_SKIP">过号时通知</el-checkbox>
                  <el-checkbox label="ON_DONE">就诊完成通知</el-checkbox>
                  <el-checkbox label="ON_ADVANCE">提前N位预通知</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="叫号重复次数">
                <el-input-number v-model="notification.callRepeatCount" :min="1" :max="5" style="width:150px;" />
                <span class="tip">语音叫号重复播报几遍</span>
              </el-form-item>
              <el-form-item label="叫号间隔（秒）">
                <el-input-number v-model="notification.callIntervalSec" :min="3" :max="30" style="width:150px;" />
              </el-form-item>
            </el-form>
          </div>
          <div class="rule-section">
            <div class="section-title"><i class="el-icon-edit-outline" style="color:#9C27B0;" /> 短信模板快速设置</div>
            <el-form :model="notification" label-width="100px" size="small">
              <el-form-item label="叫号模板">
                <el-input
                  v-model="notification.smsTemplateCall"
                  type="textarea"
                  :rows="2"
                  style="font-size:13px;"
                  placeholder="例如：尊敬的{姓名}，您的{科室}就诊号 {号码} 现在叫到，请及时就诊。"
                />
              </el-form-item>
              <el-form-item label="过号模板">
                <el-input
                  v-model="notification.smsTemplateSkip"
                  type="textarea"
                  :rows="2"
                  style="font-size:13px;"
                  placeholder="例如：您的{科室}就诊号 {号码} 已过号，如需就诊请重新取号。"
                />
              </el-form-item>
              <el-form-item label="预提醒模板">
                <el-input
                  v-model="notification.smsTemplateAdvance"
                  type="textarea"
                  :rows="2"
                  style="font-size:13px;"
                  placeholder="例如：您的{科室}就诊号 {号码} 即将叫到，请做好就诊准备。"
                />
              </el-form-item>
            </el-form>
          </div>
        </div>
        <div class="save-bar">
          <el-button type="primary" icon="el-icon-check" :loading="saving" @click="saveRule('RULE_NOTIFICATION', notification)">保存通知规则</el-button>
          <el-button plain @click="resetRule('notification')">恢复默认值</el-button>
          <span v-if="lastSaveTime.notification" class="last-save">最后保存：{{ lastSaveTime.notification }}</span>
        </div>
      </el-tab-pane>

      <!-- ==================== 高级/原始配置 ==================== -->
      <el-tab-pane name="advanced" label="⚙️ 高级原始配置">
        <div class="tab-desc" style="border-left-color:#EA4335;">
          <i class="el-icon-warning" style="color:#EA4335;" />
          此处直接展示 sys_config 表中的所有原始 KEY-VALUE，供高级运维人员或实施工程师使用。请谨慎操作。
        </div>
        <div style="display:flex;justify-content:flex-end;margin-bottom:12px;">
          <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAddRaw">新增配置项</el-button>
        </div>
        <el-table v-loading="rawLoading" :data="rawConfigs" size="small" border stripe style="width:100%;">
          <el-table-column prop="configKey" label="配置键 (Key)" width="220" show-overflow-tooltip>
            <template #default="scope">
              <code style="background:#F1F3F4;padding:2px 6px;border-radius:4px;font-size:12px;">{{ scope.row.configKey }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="configValue" label="配置值 (Value)" show-overflow-tooltip />
          <el-table-column prop="description" label="说明" width="180" show-overflow-tooltip />
          <table-action-column :count="2">
            <template #default="{ row }">
              <table-action-icon icon="el-icon-edit" tip="编辑" @click="handleEditRaw(row)" />
              <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="handleDeleteRaw(row)" />
            </template>
          </table-action-column>
        </el-table>

        <form-drawer :title="rawDialogMode === 'create' ? '新增配置项' : '编辑配置项'" v-model="rawDialogVisible" size="520px">
          <el-form :model="rawTemp" label-width="100px" size="small">
            <el-form-item label="配置键 (Key)" required>
              <el-input v-model="rawTemp.configKey" :disabled="rawDialogMode === 'update'" placeholder="例如: RULE_PHARMACY" />
            </el-form-item>
            <el-form-item label="配置说明">
              <el-input v-model="rawTemp.description" />
            </el-form-item>
            <el-form-item label="配置值">
              <el-input v-model="rawTemp.configValue" type="textarea" :rows="6" style="font-family:monospace;font-size:12px;" placeholder="JSON格式或纯文本值" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="rawDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="saveRaw">保存</el-button>
          </template>
        </form-drawer>
      </el-tab-pane>

    </el-tabs>

    <form-drawer title="规则变更历史" v-model="historyVisible" size="640px">
      <div class="history-filter">
        <el-select v-model="historyQuery.keyword" placeholder="规则键" clearable style="width:220px;" @change="loadHistory">
          <el-option label="全部 RULE_*" value="" />
          <el-option label="门诊 RULE_OUTPATIENT" value="RULE_OUTPATIENT" />
          <el-option label="药房 RULE_PHARMACY" value="RULE_PHARMACY" />
          <el-option label="体检 RULE_EXAM" value="RULE_EXAM" />
          <el-option label="医技 RULE_MEDICAL_TECH" value="RULE_MEDICAL_TECH" />
          <el-option label="通知 RULE_NOTIFICATION" value="RULE_NOTIFICATION" />
        </el-select>
      </div>
      <el-table v-loading="historyLoading" :data="historyList" size="small" border stripe>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatLogTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" show-overflow-tooltip />
        <el-table-column prop="actionDesc" label="动作" min-width="140" show-overflow-tooltip />
        <el-table-column label="快照" width="80" align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="viewHistoryPayload(row)">对比</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="historyTotal > historyQuery.pageSize"
        style="margin-top:12px;"
        background
        layout="prev, pager, next"
        :total="historyTotal"
        :page-size="historyQuery.pageSize"
        :current-page="historyQuery.pageIndex"
        @current-change="onHistoryPage"
      />
      <pre v-if="historyPayload" class="history-payload">{{ historyPayload }}</pre>
      <template #footer>
        <el-button @click="historyVisible = false">关闭</el-button>
      </template>
    </form-drawer>
  </div>
</template>

<script>
import { getAllConfigs, saveConfig, deleteConfig, queryActionLogs } from '@/api/systemConfig'

const DEFAULT_OUTPATIENT = {
  callTimeoutMin: 5,
  maxSkipCount: 3,
  preCallCount: 3,
  allowReQueue: true,
  revisitPriorityWeight: 2,
  elderPriority: true,
  emergencyInsert: true,
  autoSplitEnabled: false,
  splitThreshold: 15,
  smsEnabled: false,
  smsAdvanceCount: 3,
  smsOnSkip: true
}

const DEFAULT_LAB = {
  interleaveRatio: 3,
  missedPenalty: 3,
  requireConfirmToFreeze: true,
  tatWarningMin: 45
}

const DEFAULT_PHARMACY = {
  timeoutMinutes: 30,
  alertMinutes: 15,
  maxPendingPerWindow: 20,
  splitAlgorithm: 'SHORTEST_QUEUE',
  emergencyWindowEnabled: true,
  separateChinaWestern: true,
  autoReminderEnabled: true,
  firstReminderMin: 10,
  repeatReminderMin: 10
}

const DEFAULT_EXAM = {
  triageAlgorithm: 'FASTING_FIRST',
  waitTimeWeight: 6,
  distanceWeight: 4,
  itemTimeoutMin: 60,
  expectedTotalHours: 3.0,
  roomCapacityLimit: 8,
  dynamicReroute: true,
  vipFastTrack: false,
  fastingItemFirst: true,
  elderSimplifyRoute: true,
  timeScheduleEnabled: false,
  timeSchedules: [
    { startTime: '08:00', endTime: '10:00', algorithm: 'FASTING_FIRST' },
    { startTime: '10:00', endTime: '12:00', algorithm: 'LEAST_WAIT' }
  ]
}

const DEFAULT_MEDTECH = {
  global: {
    paymentSyncDelaySec: 0,
    multiDeptMsgEnabled: true
  },
  lab: {
    windowCount: 6,
    maxPendingPerWindow: 10,
    callTimeoutMin: 5,
    splitAlgorithm: 'SHORTEST_QUEUE',
    fastingPriorityEnabled: true,
    fastingCutoffTime: '11:00',
    emergencyPriority: true,
    mergeMultiTube: true,
    elderPriority: true
  },
  ultrasound: {
    roomCount: 4,
    maxPendingPerRoom: 8,
    callTimeoutMin: 10,
    appointmentEnabled: false,
    appointmentWeight: 7,
    splitByType: false,
    fastingPriorityEnabled: true,
    emergencyPriority: true,
    avgTimeAbdomen: 20,
    avgTimeHeart: 25,
    avgTimeOther: 15
  },
  radiology: {
    ctMachineCount: 2,
    mriMachineCount: 1,
    ctSlotCapacity: 2,
    xrayAppointmentEnabled: false,
    contrastWaitArea: true,
    contrastWaitMin: 30,
    ctReportHours: 1.5,
    criticalValueAlert: true,
    emergencyCtFastTrack: true
  },
  ecg: {
    bedCount: 3,
    holterDeviceCount: 2,
    callTimeoutMin: 5,
    avgExamMin: 6,
    emergencyGreenChannel: true,
    emergencyTargetMin: 10,
    elderPriority: true,
    reportNotify: true
  }
}

const DEFAULT_NOTIFICATION = {
  smsEnabled: false,
  wechatEnabled: false,
  voiceEnabled: true,
  eventsTrigger: ['ON_CALLED', 'ON_ADVANCE'],
  callRepeatCount: 3,
  callIntervalSec: 5,
  smsTemplateCall: '尊敬的{姓名}，您的{科室}就诊号 {号码} 现在叫到，请及时就诊。',
  smsTemplateSkip: '您的{科室}就诊号 {号码} 已过号，如需就诊请重新取号。',
  smsTemplateAdvance: '您的{科室}就诊号 {号码} 即将叫到，请做好就诊准备。'
}

export default {
  name: 'RuleCenter',
  data() {
    return {
      activeTab: 'outpatient',
      medtechTab: 'lab',
      loading: false,
      saving: false,
      saveSuccess: false,
      rawLoading: false,
      rawDialogVisible: false,
      rawDialogMode: 'create',
      rawTemp: { configKey: '', configValue: '', description: '' },
      rawConfigs: [],
      lastSaveTime: { outpatient: '', pharmacy: '', exam: '', medtech: '', notification: '' },
      historyVisible: false,
      historyLoading: false,
      historyList: [],
      historyTotal: 0,
      historyPayload: '',
      historyQuery: { keyword: '', pageIndex: 1, pageSize: 10 },
      outpatient: { ...DEFAULT_OUTPATIENT },
      lab: { ...DEFAULT_LAB },
      pharmacy: { ...DEFAULT_PHARMACY },
      exam: { ...DEFAULT_EXAM },
      medtech: JSON.parse(JSON.stringify(DEFAULT_MEDTECH)),
      notification: { ...DEFAULT_NOTIFICATION }
    }
  },
  created() { this.loadAllRules() },
  methods: {
    loadAllRules() {
      this.loading = true
      this.rawLoading = true
      getAllConfigs().then(res => {
        const list = res.data || []
        this.rawConfigs = list
        // 解析每个规则
        const parse = (key, target, defaults) => {
          const item = list.find(c => (c.configKey || c.ConfigKey) === key)
          if (item && item.configValue) {
            try {
              const parsed = JSON.parse(item.configValue)
              Object.assign(target, { ...defaults, ...parsed })
            } catch (e) { Object.assign(target, defaults) }
          } else {
            Object.assign(target, defaults)
          }
        }
        parse('RULE_OUTPATIENT', this.outpatient, DEFAULT_OUTPATIENT)
        parse('RULE_PHARMACY', this.pharmacy, DEFAULT_PHARMACY)
        parse('RULE_EXAM', this.exam, DEFAULT_EXAM)
        parse('RULE_LAB', this.lab, DEFAULT_LAB)
        // 医技科室规则：嵌套对象需深度合并
        const medtechItem = list.find(c => (c.configKey || c.ConfigKey) === 'RULE_MEDICAL_TECH')
        if (medtechItem && medtechItem.configValue) {
          try {
            const parsed = JSON.parse(medtechItem.configValue)
            this.medtech = this.$_.merge ? this.$_.merge({}, JSON.parse(JSON.stringify(DEFAULT_MEDTECH)), parsed)
              : Object.assign(JSON.parse(JSON.stringify(DEFAULT_MEDTECH)), parsed)
          } catch (e) { this.medtech = JSON.parse(JSON.stringify(DEFAULT_MEDTECH)) }
        } else {
          this.medtech = JSON.parse(JSON.stringify(DEFAULT_MEDTECH))
        }
        parse('RULE_NOTIFICATION', this.notification, DEFAULT_NOTIFICATION)
      }).catch(e => {
        this.$message.warning('加载配置失败，已使用默认值展示，保存后将写入数据库。')
      }).finally(() => {
        this.loading = false
        this.rawLoading = false
      })
    },
    saveRule(key, ruleObj) {
      const localErr = this.validateRule(key, ruleObj)
      if (localErr) {
        this.$message.error(localErr)
        return
      }
      this.saving = true
      const payload = {
        configKey: key,
        configValue: JSON.stringify(ruleObj),
        description: this.getRuleDescription(key),
        operator: this.$store.getters.name || ''
      }

      if (key === 'RULE_MEDICAL_TECH') {
        if (!ruleObj.global) ruleObj.global = { ...DEFAULT_MEDTECH.global }
      }
      if (key === 'RULE_EXAM') {
        if (ruleObj.timeScheduleEnabled === undefined) ruleObj.timeScheduleEnabled = false
        if (!ruleObj.timeSchedules) ruleObj.timeSchedules = []
      }

      saveConfig(payload).then((res) => {
        if (res.code !== 200 || res.success === false) {
          this.$message.error(res.msg || '保存失败')
          return
        }
        const tabKey = { RULE_OUTPATIENT: 'outpatient', RULE_PHARMACY: 'pharmacy', RULE_EXAM: 'exam', RULE_MEDICAL_TECH: 'medtech', RULE_NOTIFICATION: 'notification' }[key]
        const now = new Date().toLocaleTimeString('zh-CN')
        if (tabKey) this.lastSaveTime[tabKey] = now
        this.saveSuccess = true
        this.$message.success('规则已保存，调度引擎将在下次扫描时读取新配置')
        this.loadAllRules()
      }).catch(e => {
        this.$message.error(e.message || '保存失败，请检查网络连接和后端服务状态')
      }).finally(() => { this.saving = false })
    },
    /** 与后端 RuleConfigValidator 对齐的本地校验；失败返回文案，通过返回空 */
    validateRule(key, obj) {
      const inRange = (v, min, max, name) => {
        if (v == null || v === '') return null
        const n = Number(v)
        if (Number.isNaN(n) || n < min || n > max) return name + ' 须在 ' + min + '–' + max + ' 之间'
        return null
      }
      if (key === 'RULE_OUTPATIENT') {
        return inRange(obj.callTimeoutMin, 1, 60, 'callTimeoutMin')
          || inRange(obj.maxSkipCount, 1, 10, 'maxSkipCount')
          || inRange(obj.preCallCount, 1, 10, 'preCallCount')
      }
      if (key === 'RULE_PHARMACY') {
        return inRange(obj.timeoutMinutes, 5, 120, 'timeoutMinutes')
          || inRange(obj.alertMinutes, 5, 60, 'alertMinutes')
      }
      if (key === 'RULE_NOTIFICATION') {
        const placeholders = ['{姓名}', '{科室}', '{号码}']
        for (const field of ['smsTemplateCall', 'smsTemplateSkip', 'smsTemplateAdvance']) {
          const t = obj[field]
          if (!t) continue
          for (const p of placeholders) {
            if (!t.includes(p)) return field + ' 缺少占位符 ' + p
          }
        }
      }
      return null
    },
    getRuleDescription(key) {
      return {
        RULE_OUTPATIENT: '门诊排队调度规则（过号/优先级/分流/通知）',
        RULE_PHARMACY: '药房发药队列规则（超时/分流/告警/提醒）',
        RULE_EXAM: '体检TSP智能路由规则（算法选型/权重/项目特殊政策）',
        RULE_MEDICAL_TECH: '医技科室叫号规则（检验科/超声科/放射科/心电图室各科室独立参数）',
        RULE_NOTIFICATION: '全局消息通知规则（渠道/触发事件/短信模板）'
      }[key] || '系统规则配置'
    },
    resetRule(type) {
      const map = {
        outpatient: { key: 'RULE_OUTPATIENT', apply: () => { this.outpatient = { ...DEFAULT_OUTPATIENT } }, obj: () => this.outpatient },
        pharmacy: { key: 'RULE_PHARMACY', apply: () => { this.pharmacy = { ...DEFAULT_PHARMACY } }, obj: () => this.pharmacy },
        exam: { key: 'RULE_EXAM', apply: () => { this.exam = { ...DEFAULT_EXAM } }, obj: () => this.exam },
        medtech: { key: 'RULE_MEDICAL_TECH', apply: () => { this.medtech = JSON.parse(JSON.stringify(DEFAULT_MEDTECH)) }, obj: () => this.medtech },
        notification: { key: 'RULE_NOTIFICATION', apply: () => { this.notification = { ...DEFAULT_NOTIFICATION } }, obj: () => this.notification }
      }
      const item = map[type]
      if (!item) return
      this.$confirm('确认恢复内置默认值并立即保存？当前未保存的修改将丢失。', '确认重置', { type: 'warning' }).then(() => {
        item.apply()
        this.saveRule(item.key, item.obj())
      }).catch(() => {})
    },
    exportConfig() {
      const snapshot = {
        exportTime: new Date().toISOString(),
        RULE_OUTPATIENT: this.outpatient,
        RULE_LAB: this.lab,
        RULE_PHARMACY: this.pharmacy,
        RULE_EXAM: this.exam,
        RULE_MEDICAL_TECH: this.medtech,
        RULE_NOTIFICATION: this.notification
      }
      const blob = new Blob([JSON.stringify(snapshot, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `叫号规则配置_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}.json`
      a.click()
      URL.revokeObjectURL(url)
      this.$message.success('配置快照已导出')
    },
    triggerImport() {
      this.$refs.importFile && this.$refs.importFile.click()
    },
    onImportFile(ev) {
      const file = ev.target.files && ev.target.files[0]
      ev.target.value = ''
      if (!file) return
      const reader = new FileReader()
      reader.onload = () => {
        let parsed
        try {
          parsed = JSON.parse(reader.result)
        } catch (e) {
          this.$message.error('快照不是合法 JSON')
          return
        }
        const keys = Object.keys(parsed).filter(k => k.startsWith('RULE_'))
        if (!keys.length) {
          this.$message.error('快照中没有 RULE_* 键')
          return
        }
        this.$confirm('将覆盖当前作用域的 ' + keys.length + ' 条规则并写入审计，是否继续？', '导入快照', { type: 'warning' }).then(async() => {
          for (const key of keys) {
            const val = parsed[key]
            if (val == null || typeof val !== 'object') {
              this.$message.error(key + ' 不是 JSON 对象，已跳过后续导入')
              return
            }
            const res = await saveConfig({
              configKey: key,
              configValue: JSON.stringify(val),
              description: this.getRuleDescription(key),
              operator: this.$store.getters.name || ''
            })
            if (res.code !== 200 || res.success === false) {
              this.$message.error((res.msg || '导入失败') + '（' + key + '）')
              return
            }
          }
          this.$message.success('快照已导入')
          this.loadAllRules()
        }).catch(() => {})
      }
      reader.readAsText(file, 'utf-8')
    },
    openHistory() {
      const tabToKey = { outpatient: 'RULE_OUTPATIENT', pharmacy: 'RULE_PHARMACY', exam: 'RULE_EXAM', medtech: 'RULE_MEDICAL_TECH', notification: 'RULE_NOTIFICATION' }
      this.historyQuery.keyword = tabToKey[this.activeTab] || ''
      this.historyQuery.pageIndex = 1
      this.historyPayload = ''
      this.historyVisible = true
      this.loadHistory()
    },
    onHistoryPage(p) {
      this.historyQuery.pageIndex = p
      this.loadHistory()
    },
    loadHistory() {
      this.historyLoading = true
      queryActionLogs({
        logType: 'RULE_CHANGE',
        keyword: this.historyQuery.keyword || null,
        pageIndex: this.historyQuery.pageIndex,
        pageSize: this.historyQuery.pageSize
      }).then(res => {
        const data = (res.data && res.data.data) || []
        this.historyList = data
        this.historyTotal = (res.data && (res.data.TotalCount || res.data.totalCount)) || 0
      }).catch(() => {
        this.historyList = []
        this.historyTotal = 0
      }).finally(() => { this.historyLoading = false })
    },
    viewHistoryPayload(row) {
      if (!row || !row.payload) {
        this.historyPayload = '无快照'
        return
      }
      try {
        this.historyPayload = JSON.stringify(JSON.parse(row.payload), null, 2)
      } catch (e) {
        this.historyPayload = row.payload
      }
    },
    formatLogTime(t) {
      if (!t) return '—'
      if (typeof t === 'string') return t.replace('T', ' ').slice(0, 19)
      return t
    },
    handleAddRaw() {
      this.rawTemp = { configKey: '', configValue: '', description: '' }
      this.rawDialogMode = 'create'
      this.rawDialogVisible = true
    },
    handleEditRaw(row) {
      this.rawTemp = { configKey: row.configKey || row.ConfigKey, configValue: row.configValue || row.ConfigValue, description: row.description || row.Description || '' }
      this.rawDialogMode = 'update'
      this.rawDialogVisible = true
    },
    saveRaw() {
      if (!this.rawTemp.configKey) { this.$message.warning('配置键不能为空'); return }
      saveConfig({
        configKey: this.rawTemp.configKey,
        configValue: this.rawTemp.configValue,
        description: this.rawTemp.description,
        operator: this.$store.getters.name || ''
      }).then((res) => {
        if (res.code !== 200 || res.success === false) {
          this.$message.error(res.msg || '保存失败')
          return
        }
        this.$message.success('保存成功')
        this.rawDialogVisible = false
        this.loadAllRules()
      }).catch(e => {
        this.$message.error(e.message || '保存失败')
      })
    },
    handleDeleteRaw(row) {
      const key = row.configKey || row.ConfigKey
      this.$confirm(`确认删除配置项 "${key}"？删除后不可恢复。`, '确认删除', { type: 'warning' }).then(() => {
        deleteConfig({ ConfigKey: key }).then(() => {
          this.$message.success('已删除')
          this.loadAllRules()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.rule-center { padding: 0; }

.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  .header-left { display: flex; align-items: center; gap: 16px; }
  .header-icon {
    width: 52px; height: 52px; background: linear-gradient(135deg, #1A73E8, #1557B0);
    border-radius: 14px; display: flex; justify-content: center; align-items: center;
    i { font-size: 24px; color: white; }
  }
  h2 { margin: 0; font-size: 22px; font-weight: 700; color: #202124; }
  .header-sub { font-size: 13px; color: #80868B; margin-top: 3px; display: block; }
  .header-actions { display: flex; gap: 10px; }
}

.rule-tabs {
  ::v-deep .el-tabs__header { margin-bottom: 0; }
  ::v-deep .el-tabs__content { background: #FFF; border: 1px solid #E4E7ED; border-top: none; border-radius: 0 0 8px 8px; padding: 24px; }
}

.tab-desc {
  background: #E8F0FE; border-left: 4px solid #1A73E8; padding: 10px 16px;
  border-radius: 0 8px 8px 0; margin-bottom: 20px; font-size: 13px; color: #1A73E8;
  code { background: rgba(26,115,232,0.15); padding: 1px 5px; border-radius: 3px; font-size: 12px; }
}

.rule-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 20px;
  .full-width { grid-column: 1 / -1; }
}

.rule-section {
  background: #F8F9FA; border-radius: 12px; padding: 18px 20px;
  border: 1px solid #E8EAED;
  .section-title {
    font-size: 14px; font-weight: 600; color: #202124;
    margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #E8EAED;
    i { margin-right: 6px; }
  }
}

.tip {
  display: block; font-size: 12px; color: #80868B; margin-top: 4px; line-height: 1.5;
}

.save-bar {
  margin-top: 20px; padding: 16px 20px; background: #F8F9FA;
  border-radius: 8px; display: flex; align-items: center; gap: 12px;
  border: 1px dashed #DADCE0;
  .last-save { font-size: 12px; color: #80868B; margin-left: auto; }
}

::v-deep .el-form-item {
  margin-bottom: 14px;
  .el-form-item__label { font-size: 13px; color: #3C4043; font-weight: 500; }
}

@media (max-width: 1200px) {
  .rule-grid { grid-template-columns: 1fr; }
}

.inner-tabs {
  border-radius: 8px;
  ::v-deep .el-tabs__header {
    background: #F8F9FA;
    border-radius: 8px 8px 0 0;
  }
  ::v-deep .el-tabs__content {
    padding: 20px;
    background: #fff;
  }
  ::v-deep .el-tabs__item {
    font-size: 13px;
    font-weight: 500;
  }
  ::v-deep .el-tabs__item.is-active {
    color: #0097A7;
    font-weight: 600;
  }
}

.medtech-tag {
  display: inline-block;
  background: #E0F7FA;
  color: #00838F;
  border: 1px solid #00838F;
  border-radius: 20px;
  padding: 4px 14px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 16px;
  letter-spacing: 0.3px;
}

.history-filter { margin-bottom: 12px; }
.history-payload {
  margin-top: 12px;
  background: #F8F9FA;
  border: 1px solid #E8EAED;
  border-radius: 8px;
  padding: 12px;
  font-size: 12px;
  line-height: 1.45;
  max-height: 280px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
