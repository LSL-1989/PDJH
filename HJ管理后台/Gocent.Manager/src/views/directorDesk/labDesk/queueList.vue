<template>
  <div class="app-container">
    <el-card style="margin-bottom: 20px;">
      <template #header><div class="clearfix">
        <span style="font-weight: bold;"><i class="el-icon-s-operation" /> 门诊专属业务规则配置</span>
        <el-button style="float: right; padding: 3px 0" type="primary" @click="saveRules">提交流水线规则生效</el-button>
      </div></template>
      <el-form ref="ruleForm" :model="ruleForm" label-position="top" size="small">
        <el-row :gutter="40">
          <el-col :span="8">
            <el-form-item label="初复诊穿插比例设定">
              <el-select v-model="ruleForm.interleaveRatio" placeholder="选定穿插比例" style="width: 100%;">
                <el-option label="不干预 (排队先到先得)" :value="0" />
                <el-option label="3 : 1 (激进保障初诊)" :value="3" />
                <el-option label="1 : 1 (平衡处理)" :value="1" />
                <el-option label="0 : 1 (复诊无限优先)" :value="-1" />
              </el-select>
              <div style="font-size:12px;color:#909399;margin-top:5px;line-height:1.2;">建议设为 3:1，有效疏解门诊积压。</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="迟到重报到惩罚机制">
              <el-select v-model="ruleForm.missedPenalty" placeholder="迟到后顺移叫号位" style="width: 100%;">
                <el-option label="延后 1 位 (最轻微)" :value="1" />
                <el-option label="延后 3 位 (推荐)" :value="3" />
                <el-option label="延后 5 位 (高峰期施压)" :value="5" />
                <el-option label="直接作废弃号 (全额不退)" :value="999" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="坐席端跳号拦截防呆">
              <el-switch v-model="ruleForm.requireConfirmToFreeze" active-text="开启操作告警" inactive-text="关闭" />
              <div style="font-size:12px;color:#909399;margin-top:5px;line-height:1.2;">医生点击冻结或过号是否触发强警示</div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card>
      <!-- 条件查询区 -->
      <el-row>
        <el-form :model="QueryInput">
          <el-col :span="5" :xs="6" :sm="4" :md="4" :lg="4" :xl="4">
            <el-form-item label="患者编码">
              <el-input
                v-model="QueryInput.PatientCode"
                style="max-width: 70%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="5" :xs="6" :sm="4" :md="4" :lg="4" :xl="4">
            <el-form-item label="患者姓名">
              <el-input
                v-model="QueryInput.PatientName"
                style="max-width: 70%"
                placeholder="支持模糊查询"
              />
            </el-form-item>
          </el-col>
          <!--  -->
          <el-col :span="5" :xs="6" :sm="4" :md="4" :lg="4" :xl="4">
            <el-form-item label="诊室">
              <el-cascader
                v-model="QueryInput.DeptID"
                :options="DepartmentArry"
                :show-all-levels="false"
                clearable
                @change="EditorDeptChange"
              />
            </el-form-item>
          </el-col>

          <!-- <el-col :span="5">
            <el-form-item label="医生">
              <el-select v-model="QueryInput.Doctors" placeholder="请选择医生">
                <el-option
                  v-for="(item, index) in DoctorArry"
                  :key="index"
                  :label="item.UserName"
                  :value="item.UserName"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col> -->

          <el-col :span="6" :xs="6" :sm="4" :md="4" :lg="4" :xl="4">
            <el-form-item label="排队时间">
              <el-date-picker
                v-model="QueryInput.CallTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                :shortcuts="pickerOptions.shortcuts"
              />
            </el-form-item>
          </el-col>

          <el-col :span="2" style="text-align: center">
            <el-button
              type="primary"
              icon="el-icon-search"
              circle
              @click="GetQueryCallList"
            />
            <!-- <el-button type="success" @click="GetQueryCallList">查询</el-button> -->
          </el-col>

          <el-col :span="3">
            <el-button
              type="primary"
              icon="el-icon-edit-outline"
              @click="dialogRegsiterVisible = true"
            >登记排队</el-button>
          </el-col>

          <el-col :span="3">
            <el-button
              type="danger"
              icon="el-icon-delete"
              @click="dialogDeleteVisible = true"
            >批量删除</el-button>
          </el-col>
        </el-form>
      </el-row>

      <!-- 内容区 -->
      <el-row>
        <el-table
          v-loading="loadingBool"
          :data="CallList"
          style="width: 100%; border-radius: 8px;"
          :stripe="true"
        >
          <el-table-column prop="PatientCode" label="患者编码" width="120">
            <template #default="scope">
              <span style="font-family: monospace; color:#5F6368">{{ scope.row.PatientCode || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="PatientName" label="患者姓名" width="130">
            <template #default="scope">
              <span style="font-weight:600; color:#202124; font-size:15px">{{ scope.row.PatientName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sflbbigtype" label="队列类别" width="150" show-overflow-tooltip />
          <el-table-column prop="CallTime" label="排队时间" width="170">
            <template #default="scope">
              <i class="el-icon-time" style="color:#9AA0A6; margin-right:4px;" />
              <span style="color:#5F6368;">{{ scope.row.CallTime }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="xh" label="排队序号" width="90" align="center">
            <template #default="scope">
              <span style="font-size:16px; font-weight:700; color:#1A73E8">{{ scope.row.xh }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="xhlb" label="排号属性" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.xhlb === 'A' ? 'danger' : (scope.row.xhlb === 'B' ? 'warning' : 'info')" effect="light" size="small">
                {{ scope.row.xhlb === 'A' ? '急诊' : (scope.row.xhlb === 'B' ? '优先' : '普通') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="OutPatientOffice" label="诊室名称" width="160" show-overflow-tooltip>
            <template #default="scope">
              <span><i class="el-icon-location-information" style="color:#1A73E8;" /> {{ scope.row.OutPatientOffice }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="Doctors" label="医生" width="120" />
          <el-table-column prop="sflbmc" label="患者备注" min-width="150" show-overflow-tooltip />
          <el-table-column prop="state" label="当前状态" width="120" align="center">
            <template #default="scope">
              <el-tag
                :type="scope.row.state == 0 ? 'info' : (scope.row.state == 1 ? 'warning' : (scope.row.state == 2 ? 'success' : 'danger'))"
                effect="dark"
               
                round
                style="padding: 0 15px; font-weight: bold; letter-spacing: 1px;"
              >
                {{ scope.row.state == 0 ? '未呼' : (scope.row.state == 1 ? '呼叫中' : (scope.row.state == 2 ? '已完成' : '已过号')) }}
              </el-tag>
            </template>
          </el-table-column>

          <table-action-column label="操作与控制" :count="3">
            <template #default="{ row }">
              <table-action-icon icon="el-icon-edit" tip="编辑" @click="EditorShow(row)" />
              <table-action-icon icon="el-icon-sort" tip="调号" type="success" @click="SortCall(row)" />
              <table-action-icon icon="el-icon-delete" tip="作废" type="danger" @click="RemoveCall(row)" />
            </template>
          </table-action-column>
        </el-table>
        <!-- 分页 -->
        <el-pagination
          background
          :current-page="1"
          :page-sizes="[5, 10, 20, 30]"
          :page-size="PageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="TotalCount"
          @size-change="SizeChange"
          @prev-click="PagePrev"
          @current-change="PageChange"
          @next-click="PageNext"
        />
      </el-row>
    </el-card>

    <!-- 登记排队弹窗 -->
    <el-dialog title="登记排队" v-model="dialogRegsiterVisible" class="minimal-dialog" width="60%">
      <el-row>
        <!-- 通过卡号查数据 -->
        <el-form
          ref="PatientCardForm"
          :model="Patient"
          label-position="right"
          label-width="80px"
          size="small"
          :rules="PatientCardNoRules"
        >
          <el-row>
            <el-col :span="16">
              <el-form-item label="卡号" prop="PatientCardNo">
                <el-input
                  v-model="Patient.PatientCardNo"
                  @keyup.enter="GetPatientByCardeNo('PatientCardForm')"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8" style="padding-left: 20px">
              <el-button
                type="primary"
                :loading="QueryCardNoiSdisabled"
                size="small"
                @click="GetPatientByCardeNo('PatientCardForm')"
              >查询卡号</el-button>
            </el-col>
          </el-row>
        </el-form>

        <el-form
          ref="RegisterForm"
          :model="Call"
          label-position="right"
          label-width="80px"
          size="small"
          :rules="RegisterRules"
        >
          <el-row>
            <el-col :span="12">
              <el-form-item label="患者编码" prop="PatientCode">
                <el-input
                  v-model="Call.PatientCode"
                  v-loading="loading"
                  element-loading-text="拼命加载中"
                  element-loading-spinner="el-icon-loading"
                  element-loading-background="rgba(0, 0, 0, 0.8)"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="患者姓名" prop="PatientName">
                <el-input
                  v-model="Call.PatientName"
                  v-loading="loading"
                  element-loading-text="拼命加载中"
                  element-loading-spinner="el-icon-loading"
                  element-loading-background="rgba(0, 0, 0, 0.8)"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row>
            <el-col :span="12">
              <el-form-item label="排队时间" prop="CallTime">
                <el-date-picker
                  v-model="Call.CallTime"
                  type="datetime"
                  placeholder="选择排队时间"
                  align="right"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :shortcuts="pickerOptions.shortcuts"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="序号类别" prop="xhlb">
                <el-select v-model="Call.xhlb" placeholder="请选择序号类别">
                  <el-option label="急诊" value="A" />
                  <el-option label="优先" value="B" />
                  <el-option label="普通" value="C" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row>
            <el-col :span="12">
              <el-form-item label="诊 室" prop="DeptID">
                <el-cascader
                  v-model="Call.DeptID"
                  :options="DepartmentArry"
                  :show-all-levels="false"
                  clearable
                  @change="EditorDeptChange2"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="医 生" prop="Doctors">
                <el-select v-model="Call.Doctors" placeholder="请选择医生">
                  <el-option
                    v-for="(item, index) in DoctorArry2"
                    :key="index"
                    :label="item.UserName"
                    :value="item.UserName"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-row>
      <template #footer><div class="dialog-footer">
        <el-button @click="dialogRegsiterVisible = false">取 消</el-button>
        <el-button
          type="primary"
          :disabled="RegistersiSdisabled"
          @click="RegisterCall('RegisterForm')"
        >确 定</el-button>
      </div></template>
    </el-dialog>

    <!-- 批量删除弹窗 -->
    <el-dialog title="批量删除" v-model="dialogDeleteVisible">
      <el-form
        :model="BatchRemove"
        label-position="right"
        label-width="80px"
        size="small"
      >
        <el-form-item label="诊 室">
          <el-cascader
            v-model="BatchRemove.DeptID"
            :options="DepartmentArry"
            :show-all-levels="false"
            clearable
            @change="EditorDeptChange4"
          />
        </el-form-item>

        <el-form-item label="医 生">
          <el-select
            v-model="BatchRemove.Doctors"
            clearable
            placeholder="请选择医生"
          >
            <el-option
              v-for="(item, index) in DoctorArry4"
              :key="index"
              :label="item.UserName"
              :value="item.UserName"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="排队时间">
          <el-date-picker
            v-model="BatchRemove.CallTime"
            type="datetimerange"
            :shortcuts="pickerOptionsByRemove.shortcuts"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            align="right"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-checkbox
            v-model="BatchRemove.state0"
            label="未呼"
            border
           
          />
          <el-checkbox
            v-model="BatchRemove.state1"
            label="呼叫"
            border
           
          />
          <el-checkbox
            v-model="BatchRemove.state2"
            label="完成"
            border
           
          />
          <el-checkbox
            v-model="BatchRemove.state3"
            label="过号"
            border
           
          />
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button @click="dialogDeleteVisible = false">取 消</el-button>
        <el-button type="danger" @click="OnClickClear()">一键清空</el-button>
        <el-button type="primary" @click="BatchRemoves()">确 定</el-button>
      </div></template>
    </el-dialog>

    <!-- 修改队列弹窗 -->
    <el-dialog
      title="修改信息"
      v-model="dialogEditorVisible"
      :close-on-click-modal="false"
      class="minimal-dialog"
      width="60%"
    >
      <el-row>
        <el-form
          ref="EditorForm"
          :model="EditorCall"
          label-position="right"
          label-width="80px"
          size="small"
        >
          <el-row>
            <el-col :span="12">
              <el-form-item label="患者编码" prop="PatientCode">
                <el-input v-model="EditorCall.PatientCode" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="患者姓名" prop="PatientName">
                <el-input v-model="EditorCall.PatientName" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row>
            <el-col :span="16">
              <el-form-item label="排队时间" prop="CallTime">
                <el-date-picker
                  v-model="EditorCall.CallTime"
                  type="datetime"
                  placeholder="选择排队时间"
                  align="right"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :shortcuts="pickerOptions.shortcuts"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row>
            <el-col :span="24">
              <el-form-item label="患者备注">
                <el-input
                  v-model="EditorCall.sflbmc"
                  type="textarea"
                  :rows="2"
                  placeholder="请输入内容"
                />
              </el-form-item>
            </el-col>

            <!-- <el-col :span="7">
              <el-form-item label="序号" prop="xh">
                <el-input v-model="EditorCall.xh"></el-input>
              </el-form-item>
            </el-col>

            <el-col :span="7">
              <el-form-item label="序号类别" prop="xhlb">
                <el-select
                  v-model="EditorCall.xhlb"
                  placeholder="请选择序号类别"
                >
                  <el-option label="急诊" value="A"></el-option>
                  <el-option label="优先" value="B"></el-option>
                  <el-option label="普通" value="C"></el-option>
                </el-select>
              </el-form-item>
            </el-col> -->
          </el-row>

          <el-row>
            <el-col :span="12">
              <el-form-item label="诊 室" prop="DeptID">
                <el-cascader
                  v-model="EditorCall.DeptID"
                  :options="DepartmentArry"
                  :show-all-levels="false"
                  clearable
                  @change="EditorDeptChange3"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="医 生" prop="Doctors">
                <el-select
                  v-model="EditorCall.Doctors"
                  placeholder="请选择医生"
                >
                  <el-option
                    v-for="(item, index) in DoctorArry3"
                    :key="index"
                    :label="item.UserName"
                    :value="item.UserName"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row>
            <el-col :span="12">
              <el-form-item label="状态备注">
                <el-input v-model="EditorCall.remark" />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="状 态">
                <el-select v-model="EditorCall.state" placeholder="请选择医生">
                  <el-option label="未呼叫" value="0" />
                  <el-option label="呼叫" value="1" />
                  <el-option label="完成" value="2" />
                  <el-option label="过号" value="3" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row>
            <el-col :span="24">
              <el-form-item label="拼音码">
                <el-input v-model="EditorCall.pym" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row>
            <el-col :span="12">
              <el-form-item label="呼叫时间">
                <el-date-picker
                  v-model="EditorCall.CallDate"
                  type="datetime"
                  placeholder="选择呼叫时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="完成时间">
                <el-date-picker
                  v-model="EditorCall.EndDate"
                  type="datetime"
                  placeholder="选择完成时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-row>
      <template #footer><div class="dialog-footer">
        <el-button @click="dialogEditorVisible = false">取 消</el-button>
        <el-button
          type="primary"
          @click="EditorCalls('EditorForm')"
        >确 定</el-button>
      </div></template>
    </el-dialog>

    <!-- 调整队列弹窗    -->
    <el-dialog title="调整队列" v-model="dialogSortVisible" width="70%">
      <el-row type="flex" justify="center">
        <el-col :span="6">
          <h3 style="margin-top: 0px; margin-bottom: 30px">
            目前一共有{{ TotalCount2 }}位患者
          </h3>
        </el-col>
      </el-row>
      <el-row style="">
        <el-col :span="6" style="margin: 80px 40px 0px 0px">
          <el-form
            ref="SortForm"
            :model="Sort"
            :rules="SortRules"
            label-width="100px"
            size="small"
          >
            <el-form-item label="当前排队序号" prop="xh">
              <el-input v-model="Sort.xh" />
            </el-form-item>
            <el-form-item label="当前序号类别" prop="xhlb">
              <el-select v-model="Sort.xhlb">
                <el-option label="急诊" value="A" />
                <el-option label="优先" value="B" />
                <el-option label="普通" value="C" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-col>

        <el-col :span="17">
          <el-table
            ref="soltTable"
            v-loading="loadingBool"
            :data="CallList"
            style="width: 100%"
            border
            height="250px"
            size="small"
            highlight-current-row
          >
            <el-table-column prop="xh" label="排队序号" width="100" />
            <el-table-column prop="xhlb" label="序号类别" width="100" />
            <el-table-column prop="PatientCode" label="患者编码" width="180" />
            <el-table-column prop="PatientName" label="患者姓名" width="180" />
            <el-table-column prop="sflbbigtype" label="队列类别" width="180" />
            <el-table-column prop="CallTime" label="排队时间" width="200" />
            <el-table-column prop="sflbmc" label="患者备注" width="180" />
            <el-table-column prop="DeptID" label="诊室ID" width="180" />
            <el-table-column
              prop="OutPatientOffice"
              label="诊室名称"
              width="180"
            />
            <el-table-column prop="Doctors" label="医生" width="180" />
            <el-table-column prop="remark" label="状态备注" width="180" />
            <el-table-column prop="state" label="状态" width="180" />
            <el-table-column prop="state" label="状态" width="180" />
            <el-table-column prop="pym" label="拼音码" width="180" />
            <el-table-column prop="CallDate" label="呼叫时间" width="180" />
            <el-table-column prop="EndDate" label="完成时间" width="180" />
          </el-table>
        </el-col>
      </el-row>
      <template #footer><div class="dialog-footer">
        <el-button @click="dialogSortVisible = false">取 消</el-button>
        <el-button
          type="primary"
          @click="SortCalls('SortForm')"
        >确 定</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage as Message } from 'element-plus'
import { getAllConfigs, saveConfig } from '@/api/systemConfig'

export default {
  name: 'OutpatientCall',
  data() {
    var validatePass = (rule, value, callback) => {
      if (value.length <= 0) {
        callback(new Error('此项不能为空'))
      } else {
        callback()
      }
    }
    var checkxh = (rule, value, callback) => {
      if (!value) {
        return callback(new Error('此项不能为空'))
      }

      if (!Number.isInteger(parseInt(value))) {
        callback(new Error('请输入数字值'))
      } else {
        if (value < 0) {
          callback(new Error('序号不能为负数'))
        } else {
          callback()
        }
      }
    }
    return {
      CONFIG_KEY: 'RULE_LAB',
      ruleForm: {
        interleaveRatio: 3,
        missedPenalty: 3,
        requireConfirmToFreeze: true
      },
      loading: false,
      RegistersiSdisabled: false,
      QueryCardNoiSdisabled: false,
      Patients: [],
      Patient: {
        PatientCardNo: '' // 卡号
      },
      PatientCardNoRules: {
        PatientCardNo: [{ validator: validatePass, trigger: 'blur' }]
      },
      loadingBool: false,
      dialogRegsiterVisible: false,
      dialogEditorVisible: false,
      dialogDeleteVisible: false,
      dialogSortVisible: false,
      Sort: {
        id: '',
        xh: '',
        xhlb: ''
      },
      SortRules: {
        xh: [{ validator: checkxh, trigger: 'change' }],
        xhlb: [{ validator: validatePass, trigger: 'change' }]
      },
      BatchRemove: {
        // 批量删除
        DeptID: '',
        Doctors: '',
        CallTime: '',
        state0: false,
        state1: false,
        state2: false,
        state3: false
      },
      pickerOptions: {
        shortcuts: [
          {
            text: '今天',
            value() {
              return new Date()
            }
          }
        ]
      },
      Call: {
        PatientCode: '',
        PatientName: '',
        CallTime: '',
        xhlb: '',
        DeptID: '',
        Doctors: ''
      },
      EditorCall: {
        id: '',
        PatientCode: '',
        PatientName: '',
        sflbbigtype: '',
        CallTime: '',
        sflbmc: '',
        xh: '',
        xhlb: '',
        DeptID: '',
        Doctors: '',
        remark: '',
        state: '',
        pym: '',
        CallDate: '',
        EndDate: ''
      },
      RegisterRules: {
        PatientCode: [{ validator: validatePass, trigger: 'change' }],
        PatientName: [{ validator: validatePass, trigger: 'change' }],
        CallTime: [{ validator: validatePass, trigger: 'blur' }],
        xhlb: [{ validator: validatePass, trigger: 'blur' }],
        DeptID: [{ validator: validatePass, trigger: 'blur' }],
        Doctors: [{ validator: validatePass, trigger: 'blur' }]
      },
      QueryInput: {
        PatientCode: '',
        PatientName: '',
        DeptID: '',
        Doctors: '',
        CallTime: '',
        PageIndex: 1,
        PageSize: 5,
        businessType: 'LAB'
      },
      DepartmentArry: [], // 诊室信息
      PageIndex: 1,
      PageSize: 5,
      DoctorArry: [], // 医生信息
      DoctorArry2: [], // 登记
      DoctorArry3: [], // 修改
      DoctorArry4: [], // 批量删
      TotalCount: 0,
      TotalCount2: 0,
      // 队列数据
      CallList: [],
      pickerOptionsByRemove: {
        shortcuts: [
          {
            text: '最近一周',
            value() {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
              return [start, end]
            }
          },
          {
            text: '最近一个月',
            value() {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
              return [start, end]
            }
          },
          {
            text: '最近三个月',
            value() {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
              return [start, end]
            }
          }
        ]
      }
    }
  },

  mounted() {
    this.fetchRules()
    this.GetQueryCallList()
    this.InitDept()
    this.$Bus.$on('RefreshDept', (data) => {
      this.DepartmentArry = data
    })
  },
  methods: {
    // 排序
    SortCalls(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.$store
            .dispatch('callList/sortCall', this.Sort)
            .then((response) => {
              if (response == true) {
                this.$message({
                  type: 'success',
                  message: '调整成功！'
                })
                this.dialogSortVisible = false
                this.GetQueryCallList()
              } else {
                this.$message.error('无法调整该患者！')
              }
            })
            .catch((error) => {
              this.$message.error('服务器错误，无法调整该患者！')
            })
        } else {
          this.$message.error('请填入完整的信息！')
          return false
        }
      })
    },
    // 根据卡号获取患者数据
    GetPatientByCardeNo(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.loading = true
          this.RegistersiSdisabled = true
          this.QueryCardNoiSdisabled = true
          this.$store
            .dispatch('user/getPatientByCardNo', this.Patient.PatientCardNo)
            .then((response) => {
              if (response.length > 0) {
                this.Patients = response
                this.Call.PatientCode = response[0].PatientCode
                this.Call.PatientName = response[0].PatientName
                this.Call.CallTime = new Date()
                this.Call.xhlb = 'C'
              } else {
                this.$message('没有该卡号的患者信息')
              }
              this.loading = false
              this.RegistersiSdisabled = false
              this.QueryCardNoiSdisabled = false
            })
            .catch((error) => {
              this.loading = false
              this.RegistersiSdisabled = false
              this.QueryCardNoiSdisabled = false
              this.$message.error('服务器错误，未能找到该卡号患者')
            })
        } else {
          this.$message.error('请填入完整的信息！')
          return false
        }
      })
    },
    // 调整队列信息(排序)
    SortCall(row) {
      console.log(row)
      this.Sort.id = row.id
      this.Sort.xh = row.xh
      this.Sort.xhlb = row.xhlb
      this.$nextTick(() => {
        if (this.CallList.length > 0) {
          this.$refs.soltTable.setCurrentRow(row)
        }
      })

      this.dialogSortVisible = true
    },
    // 修改队列信息
    EditorCalls() {
      console.log('this.EditorCall', this.EditorCall)

      this.$store
        .dispatch('callList/editorCall', this.EditorCall)
        .then((response) => {
          if (response.data == true) {
            this.$message({
              type: 'success',
              message: '修改信息成功！'
            })
          }
          this.GetQueryCallList()
          this.dialogEditorVisible = false
        })
        .catch((error) => {
          this.$message.error('修改队列信息失败！')
        })
    },
    // 弹窗修改队列信息
    EditorShow(row) {
      // 赋默认值
      this.EditorCall.id = row.id
      this.EditorCall.PatientCode = row.PatientCode
      this.EditorCall.PatientName = row.PatientName
      this.EditorCall.sflbbigtype = row.sflbbigtype
      this.EditorCall.CallTime = row.CallTime
      this.EditorCall.sflbmc = row.sflbmc
      this.EditorCall.xh = row.xh
      this.EditorCall.xhlb = row.xhlb
      this.EditorCall.DeptID = row.DeptID
      this.EditorCall.Doctors = row.Doctors
      this.EditorCall.remark = row.remark
      this.EditorCall.state = `${row.state}`
      this.EditorCall.pym = row.pym
      this.EditorCall.CallDate = row.CallDate || ''
      this.EditorCall.EndDate = row.EndDate || ''

      this.dialogEditorVisible = true
    },
    // 批量删除
    BatchRemoves() {
      this.$store
        .dispatch('callList/batchRemove', this.BatchRemove)
        .then((response) => {
          this.$message({
            type: 'success',
            message: `总计删除${response}条数据`
          })
          this.dialogDeleteVisible = false
          if (response > 0) {
            this.GetQueryCallList()
          }
          this.BatchRemove.DeptID = ''
          this.BatchRemove.Doctors = ''
          this.BatchRemove.CallTime = ''
          this.BatchRemove.state0 = false
          this.BatchRemove.state1 = false
          this.BatchRemove.state2 = false
          this.BatchRemove.state3 = false
        })
        .catch((error) => {
          this.$message.error('删除失败!')
        })
    },
    // 一键清除
    OnClickClear() {
      this.$confirm('您确定要一键清除所有队列数据吗, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store.dispatch('callList/oneclickClear').then((response) => {
            if (response == true) {
              this.$message({
                type: 'success',
                message: '清除成功!'
              })
              this.GetQueryCallList()
              this.dialogDeleteVisible = false
            } else {
              this.$message.error('清除失败!')
            }
          })
        })
        .catch(() => {
          this.$message({
            type: 'info',
            message: '已取消清除'
          })
        })
    },
    // 登记排队
    RegisterCall(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.$store
            .dispatch('callList/Register', this.Call)
            .then((response) => {
              if (response.data == true) {
                this.$message.success('登记成功！')
                // 刷新数据
                this.GetQueryCallList()
                this.$Bus.$emit('RefreshAutoNum')
                this.dialogRegsiterVisible = false
                this.Call.PatientCode = ''
                this.Call.PatientName = ''
                this.Call.CallTime = ''
                this.Call.xhlb = ''
                this.Call.DeptID = ''
                this.Call.Doctors = ''
              } else {
                this.$message.error('登记失败！')
              }
            })
            .catch((error) => {
              this.$message.error('服务器错误，无法登记。')
            })
        } else {
          this.$message.error('请填入完整的信息！')
          return false
        }
      })
    },
    // 删除队列信息
    RemoveCall(row) {
      this.$confirm('确定删除这条队列信息？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store
            .dispatch('callList/removeCall', row.id)
            .then((response) => {
              if (response.data == true) {
                this.GetQueryCallList()
                this.$message({
                  type: 'success',
                  message: '删除成功!'
                })
              } else {
                this.$message.error('删除失败！')
              }
            })
            .catch((error) => {
              this.$message.error('删除失败！')
            })
        })
        .catch(() => {
          this.$message({
            type: 'info',
            message: '已取消删除'
          })
        })
    },
    // 上一页
    PagePrev(index) {
      this.PageIndex = index
      this.QueryInput.PageIndex = index
      this.GetQueryCallList()
    },
    // 点击下面的页数
    PageChange(index) {
      this.PageIndex = index
      this.QueryInput.PageIndex = index
      this.GetQueryCallList()
    },
    // 下一页
    PageNext(index) {
      this.PageIndex = index
      this.QueryInput.PageIndex = index
      this.GetQueryCallList()
    },
    // 每页显示的条数改变
    SizeChange(size) {
      this.PageSize = size
      this.QueryInput.PageSize = size
      this.GetQueryCallList()
    },

    // 选择诊室的更改事件 选出最后一次选择的诊室id
    EditorDeptChange(value) {
      if (value.length != 0) {
        this.QueryInput.DeptID = value[1]

        this.GetDoctorByDeptID(this.QueryInput.DeptID)
      }
      this.DoctorArry = []
      this.QueryInput.Doctors = ''
    },
    EditorDeptChange2(value) {
      if (value && value.length && value.length != 0) {
        this.Call.DeptID = value[1]

        this.GetDoctorByDeptID2(this.Call.DeptID)
      }
      this.DoctorArry2 = []
      this.Call.Doctors = ''
    },
    EditorDeptChange3(value) {
      if (value && value.length && value.length != 0) {
        this.EditorCall.DeptID = value[1]

        this.GetDoctorByDeptID3(this.EditorCall.DeptID)
      }
      this.DoctorArry3 = []
      this.EditorCall.Doctors = ''
    },
    EditorDeptChange4(value) {
      if (value && value.length && value.length != 0) {
        this.BatchRemove.DeptID = value[1]
        this.GetDoctorByDeptID4(this.BatchRemove.DeptID)
      }
      this.DoctorArry4 = []
      this.BatchRemove.Doctors = ''
    },

    // 初始数据
    fetchRules() {
      getAllConfigs().then(res => {
        if (res.success || res.code === 200) {
          const configs = res.data || []
          const ruleConfig = configs.find(c => c.configKey === this.CONFIG_KEY)
          if (ruleConfig && ruleConfig.configValue) {
            try {
              this.ruleForm = JSON.parse(ruleConfig.configValue)
            } catch (e) {
              console.error('OUTPATIENT规则解析失败', e)
            }
          }
        }
      })
    },
    saveRules() {
      const payload = {
        configKey: this.CONFIG_KEY,
        configValue: JSON.stringify(this.ruleForm),
        configType: 2,
        description: '门诊流水线呼叫与穿插核心控制规则'
      }
      saveConfig(payload).then(res => {
        if (res.success || res.code === 200) {
          this.$message.success('规则已下发至门诊各物理诊室生效！')
        } else {
          this.$message.error('规则分发失败！')
        }
      })
    },
    GetQueryCallList() {
      if (this.QueryInput.DeptID.length == 0) this.QueryInput.DeptID = ''

      this.loadingBool = true
      this.$store
        .dispatch('callList/getQueryCallList', this.QueryInput)
        .then((response) => {
          if (response.data.length == 0) {
            this.$message({
              message: '暂无数据',
              type: 'warning'
            })
            this.CallList = []
          } else {
            response.data.filter((item) => {
              if (item.CallTime) {
                item.CallTime = item.CallTime.replace('T', ' ')
              }
              if (item.CallDate) {
                item.CallDate = item.CallDate.replace('T', ' ')
              }
              if (item.EndDate) {
                item.EndDate = item.EndDate.replace('T', ' ')
              }
            })
            this.CallList = response.data
            this.TotalCount2 = response.pageInfo.totalCount
            this.TotalCount = response.pageInfo.totalCount
          }
          this.loadingBool = false
        })
        .catch((e) => {
          Message.error('服务器错误，无法获取患者队列信息！')
          this.loadingBool = false
        })
    },
    // 获取所有的诊室信息并强行筛除其他管线
    GetAllDepartment() {
      this.$store
        .dispatch('department/getAllDepartment')
        .then((response) => {
          this.DepartmentArry = response.filter(d => d.businessType === this.QueryInput.businessType)
        })
        .catch((error) => {
          Message.error('服务器错误！')
        })
    },
    // 根据选择的诊室 去查询相对应诊室下的所有医生
    GetDoctorByDeptID(DeptID) {
      this.$store
        .dispatch('callList/getDoctorByDeptID', DeptID)
        .then((response) => {
          this.DoctorArry = response
        })
        .catch((error) => {})
    },
    GetDoctorByDeptID2(DeptID) {
      this.$store
        .dispatch('callList/getDoctorByDeptID', DeptID)
        .then((response) => {
          this.DoctorArry2 = response
        })
        .catch((error) => {})
    },
    GetDoctorByDeptID3(DeptID) {
      this.$store
        .dispatch('callList/getDoctorByDeptID', DeptID)
        .then((response) => {
          this.DoctorArry3 = response
        })
        .catch((error) => {})
    },
    GetDoctorByDeptID4(DeptID) {
      this.$store
        .dispatch('callList/getDoctorByDeptID', DeptID)
        .then((response) => {
          this.DoctorArry4 = response
        })
        .catch((error) => {})
    },
    // 初始化诊室信息
    InitDept() {
      if (
        this.$store.state.department.DepartmentArry &&
        this.$store.state.department.DepartmentArry.length > 0
      ) {
        const allDepts = this.$store.state.department.DepartmentArry
        this.DepartmentArry = allDepts.filter(d => d.businessType === this.QueryInput.businessType)
      } else {
        this.GetAllDepartment()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .minimal-dialog .el-dialog__header {
  background-color: #f8f9fa;
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
}
::v-deep .minimal-dialog .el-form-item__label {
  font-weight: 500;
  color: #5f6368;
}
</style>
