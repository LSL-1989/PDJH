<template>
  <div class="app-container">
    <el-card>
      <!-- 条件查询区 -->
      <div class="list-toolbar list-toolbar--flat">
        <el-form inline :model="QueryInput" class="list-toolbar__left">
          <el-form-item label="诊室">
            <el-select v-model="QueryInput.DeptID" @change="EditorDeptChange">
              <el-option
                v-for="item in DepartmentArry"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="排队时间">
            <el-date-picker
              v-model="QueryInput.CallTime"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              :shortcuts="pickerOptions.shortcuts"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              icon="el-icon-search"
              @click="GetDeptCallList"
            >查询</el-button>
          </el-form-item>
        </el-form>
        <div class="list-toolbar__right">
          <el-button
            type="primary"
            icon="el-icon-edit-outline"
            @click="dialogRegsiterVisible = true"
          >患者登记</el-button>
          <el-button
            type="danger"
            icon="el-icon-delete"
            @click="dialogDeleteVisible = true"
          >批量删除</el-button>
        </div>
      </div>

      <!-- 内容区 -->
      <el-row>
        <el-table
          v-loading="loadingBool"
          :data="CallList"
          style="width: 100%"
          border
          :stripe="true"
        >
          <el-table-column prop="PatientCode" label="患者编码" width="180" />
          <el-table-column prop="PatientName" label="患者姓名" width="180" />
          <el-table-column prop="sflbbigtype" label="队列类别" width="180" />
          <el-table-column prop="CallTime" label="排队时间" width="200" />
          <el-table-column prop="sflbmc" label="患者备注" width="180" />
          <el-table-column prop="xh" label="序号" width="120" />
          <el-table-column prop="xhlb" label="序号类别" width="120" />
          <el-table-column prop="DeptID" label="诊室ID" width="180" />
          <el-table-column prop="OutPatientOffice" label="诊室名称" width="180" />
          <el-table-column prop="Doctors" label="医生" width="180" />
          <el-table-column prop="remark" label="状态备注" width="180" />
          <el-table-column prop="state" label="状态" width="120" align="center">
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
          <el-table-column prop="pym" label="拼音码" width="180" />
          <el-table-column prop="CallDate" label="呼叫时间" width="180" />
          <el-table-column prop="EndDate" label="完成时间" width="180" />
          <table-action-column :count="3">
            <template #default="{ row }">
              <table-action-icon icon="el-icon-edit" tip="编辑" @click="EditorShow(row)" />
              <table-action-icon icon="el-icon-sort" tip="调整" type="success" @click="SortCall(row)" />
              <table-action-icon icon="el-icon-delete" tip="删除" type="danger" @click="RemoveCall(row)" />
            </template>
          </table-action-column>
        </el-table>
        <!-- 分页 -->
        <el-pagination
          background
          :current-page="1"
          :page-sizes="[20, 30, 50]"
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

    <!-- 患者登记弹窗 -->
    <el-dialog title="患者登记" v-model="dialogRegsiterVisible" class="minimal-dialog" width="60%">
      <el-row>
        <el-form
          ref="RegisterForm"
          :model="SignIn"
          label-position="right"
          label-width="80px"
          size="small"
          :rules="RegisterRules"
        >
          <el-row>
            <el-col :span="12">
              <el-form-item label="流水号" prop="PatientID">
                <el-input
                  v-model="SignIn.PatientID"
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
                  v-model="SignIn.PatientName"
                  v-loading="loading"
                  element-loading-text="拼命加载中"
                  element-loading-spinner="el-icon-loading"
                  element-loading-background="rgba(0, 0, 0, 0.8)"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row>
            <el-form-item label="诊 室" prop="DeptID">
              <el-select
                v-model="SignIn.DepartmentID"
                @change="EditorSignInDeptChange"
              >
                <el-option
                  v-for="item in DepartmentArry"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                /></el-select>
            </el-form-item>
          </el-row>
        </el-form>
      </el-row>
      <template #footer><div class="dialog-footer">
        <el-button @click="dialogRegsiterVisible = false">取 消</el-button>
        <el-button
          type="primary"
          :disabled="RegistersiSdisabled"
          @click="GetDataTime()"
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
import {
  GetSignInfoByCode,
  VerifyPatientSign,
  ConfirmPatientSign,
  GetDataTime
} from '@/api/singin'
export default {
  name: 'PatientCallList',
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
      loading: false,
      DeptId: '', // 用户所在科室ID
      DeptParentId: '', // 用户所在科室父ID
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
      ProtCode: '04', // 获取签到端数据ProtCode
      Gid: '', // 签到端id
      GetCreditCardDataInput: {
        Gid: '',
        CardNo: ''
      },
      SignIn: {
        PatientID: '', // 流水号
        PatientName: '', // 患者姓名
        Sex: 0, // 性别
        Age: 0, // 岁
        CheckType: '', // sflbmc
        RecordDate: '', // 时间
        Department: '', // 诊室名称
        DepartmentID: '', // 诊室ID，默认00000000-0000-0000-0000-000000000000
        Item: '', // 项目名称
        Status: 0 // 状态
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
        DeptID: '',
        // Doctors: "",
        CallTime: '',
        Type: 1,
        PageIndex: 1,
        PageSize: 20 // 初始数据条数
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
    this.GetUserClinic()
    this.ProtCode = localStorage.getItem('SignCode')
    this.GetSignInfoByCode()
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
                this.GetDeptCallList()
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
          this.GetDeptCallList()
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
            this.GetDeptCallList()
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
              this.GetDeptCallList()
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
    // RegisterCall(formName) {
    //   this.$refs[formName].validate((valid) => {
    //     if (valid) {
    //       this.$store
    //         .dispatch("callList/Register", this.Call)
    //         .then((response) => {
    //           if (response.data == true) {
    //             this.$message.success("登记成功！");
    //             //刷新数据
    //             this.GetDeptCallList();
    //             this.$Bus.$emit("RefreshAutoNum");
    //             this.dialogRegsiterVisible = false;
    //             this.Call.PatientCode = "";
    //             this.Call.PatientName = "";
    //             this.Call.CallTime = "";
    //             this.Call.xhlb = "";
    //             this.Call.DeptID = "";
    //             this.Call.Doctors = "";
    //           } else {
    //             this.$message.error("登记失败！");
    //           }
    //         })
    //         .catch((error) => {
    //           this.$message.error("服务器错误，无法登记。");
    //         });
    //     } else {
    //       this.$message.error("请填入完整的信息！");
    //       return false;
    //     }
    //   });
    // },
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
                this.GetDeptCallList()
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
      this.GetDeptCallList()
    },
    // 点击下面的页数
    PageChange(index) {
      this.PageIndex = index
      this.QueryInput.PageIndex = index
      this.GetDeptCallList()
    },
    // 下一页
    PageNext(index) {
      this.PageIndex = index
      this.QueryInput.PageIndex = index
      this.GetDeptCallList()
    },
    // 每页显示的条数改变
    SizeChange(size) {
      this.PageSize = size
      this.QueryInput.PageSize = size
      this.GetDeptCallList()
    },

    // 选择诊室的更改事件 选出最后一次选择的诊室id
    EditorDeptChange(value) {
      if (value.length != 0) {
        // this.QueryInput.DeptID = value[1];
        // alert(value);
        let obj = {}

        obj = this.DepartmentArry.filter((item) => {
          // 这里的Data就是上面遍历的数据源
          return item.value === value // 筛选出匹配数据
        })
        this.QueryInput.Type = obj[0].Type
        this.SignIn.CheckType = obj[0].Queue
        // this.GetDoctorByDeptID(this.QueryInput.DeptID);
      }
      // this.DoctorArry = [];
      // this.QueryInput.Doctors = "";
    },
    EditorSignInDeptChange(value) {
      if (value.length != 0) {
        let obj = {}

        obj = this.DepartmentArry.filter((item) => {
          // 这里的Data就是上面遍历的数据源
          return item.value === value // 筛选出匹配数据
        })
        this.SignIn.CheckType = obj[0].Queue
      }
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
    GetQueryCallList() {
      if (this.QueryInput.DeptID.length == 0) this.QueryInput.DeptID = ''
      // console.log(this.QueryInput);
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
    // 初始数据
    GetDeptCallList() {
      if (this.QueryInput.DeptID.length == 0) this.QueryInput.DeptID = ''
      // console.log(this.QueryInput);
      this.loadingBool = true
      this.$store
        .dispatch('callList/GetDeptCallList', this.QueryInput)
        .then((response) => {
          // console.log("GetDeptCallList", response);
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
    // 获取所有诊室
    GetAllDepartment() {
      this.$store
        .dispatch('department/getAllDepartment')
        .then((response) => {
          console.log('getAllDepartment', response)
          this.DepartmentArry = response
        })
        .catch((error) => {
          Message.error('服务器错误！')
        })
    },
    // 获取用户所在导诊诊室以及复诊室（彩超一，彩超二，彩超等）
    GetUserParentDeptID() {
      var DepartmentInput = {
        ID: this.DeptId
      }
      this.$store
        .dispatch('department/GetUserParentDeptID', DepartmentInput)
        .then((response) => {
          // console.log("GetUserParentDeptID", response);
          this.DepartmentArry = response
          this.QueryInput.DeptID = this.DepartmentArry[0].value
          this.GetDeptCallList()
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
        this.DepartmentArry = this.$store.state.department.DepartmentArry
      } else {
        // this.GetAllDepartment();//获取所有诊室
        this.GetUserParentDeptID() // 获取用户所在诊室及其父诊室
      }
    },
    // 根据用户信息获取所在诊室
    GetUserClinic() {
      var TokenInput = {
        UserNo: localStorage.getItem('UserNo')
      }
      this.$store
        .dispatch('user/GetUserClinic', TokenInput)
        .then((response) => {
          // console.log("GetUserClinic", response);
          this.DeptId = response.DeptId
          this.InitDept() // 初始话数据
        })
        .catch((error) => {
          console.log('GetUserClinicerror', error)
        })
    },
    // 获取签到端信息
    GetSignInfoByCode() {
      // this.BodayLoading = true;
      console.log()
      GetSignInfoByCode(this.ProtCode)
        .then((res) => {
          // console.log("获取签到端信息", res, this.ProtCode);
          if (res && res.result == true) {
            const { data } = res

            // this.Title = data.Title;
            this.Gid = data.Gid
            this.GetCreditCardDataInput.Gid = data.Gid
          } else {
            // this.ShowTip = "无法查询到签到端信息。";
            // this.TimingTip();
            this.$message.error('无法查询到签到端信息。')
          }
          this.BodayLoading = false
        })
        .catch((error) => {
          console.log(error)
          // this.BodayLoading = false;
          // this.ShowTip = error.msg;
          // this.TimingTip();
          this.$message.error('服务器错误，无法查询到签到端信息！')
        })
    },
    // 判断是否签到
    VerifyPatientSign() {
      var data = {
        PatientID: this.SignIn.PatientID, // 流水号
        CheckType: this.SignIn.CheckType, // 收费类别名称，选择诊室获取this.CardDataOutput.CheckType
        RecordDate: this.SignIn.RecordDate // 获取当前日期
      }
      VerifyPatientSign(data)
        .then((res) => {
          // console.log("判断是否签到", res);
          if (res && res.result == true) {
            if (res.data.IsSign == true) {
              this.$message.warning(
                `${this.SignIn.PatientName}已签到，无需再签到！`
              )
            } else {
              this.ConfirmPatientSign() // 确认签到
            }
          } else {
            this.$message.error(res.msg)
          }
        })
        .catch((error) => {
          // this.SerialNumber = "";
          console.log(error)
          // this.BodayLoading = true;
          // this.ShowTip = "服务器错误，无法判断是否签到！";
          // this.TimingTip();
          this.$message.error('服务器错误，无法判断是否签到！')
        })
    },
    // 确认签到
    ConfirmPatientSign() {
      var data = {
        Gid: this.Gid,
        PatientInfo: this.SignIn
      }
      // console.log("确认签到", data);
      ConfirmPatientSign(data)
        .then((res) => {
          // console.log("确认签到123", res);
          if (res && res.result == true) {
            this.$message.success(
              `签到成功，您的序号是${res.data.xhlb}${res.data.xh}，前面还有${res.data.TopOrder}个人！`
            )

            // this.ShowTip = `签到成功，您排在第${res.data.xh}位，前面还有${res.data.TopOrder}个人！`;
            // this.TimingTip();
          } else {
            // this.ShowTip = res.msg;
            this.$message.error('签到失败！')
          }
        })
        .catch((error) => {
          this.$message.error('服务器错误，无法签到！')
        })
    },
    GetDataTime() {
      GetDataTime()
        .then((res) => {
          // console.log("GetDataTime", res);
          const resDate = JSON.parse(res.data)
          this.SignIn.RecordDate = resDate.DateTime
          // 为了时间准确，先获取时间后在判断是否已签到
          this.VerifyPatientSign()
        })
        .catch((err) => {
          console.error(err)
        })
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
