<template>
  <div v-loading="BodayLoading" class="body">
    <!-- 第一行 -->
    <el-row class="row1">
      <el-col :span="12">
        <div class="row1_tittle">
          <span><img
            src="@/assets/Sign_images/yyicon.png"
            alt=""
            style="
                width: 10%;
                vertical-align: middle;
                margin-right: 10px;
              "
          ></span>{{ Title }}
        </div>
      </el-col>
      <el-col :span="12">
        <div class="row1_time">{{ nowDate }}</div>
        <div class="row1_time">{{ nowTime }}</div>
      </el-col>
    </el-row>
    <!-- 第二行 -->
    <el-row class="row1daoji" />
    <!-- 第三行 -->
    <el-row class="row2">
      <div style="position: absolute; bottom: 0px; left: 0px">
        <img src="@/assets/Sign_images/huanjiang.png" alt="">
      </div>
      <el-col :span="14" :xs="24" :sm="8" class="row2_col1">
        <div class="row2_input">
          <el-input
            id="address"
            ref="address"
            v-model="SerialNumber"
            placeholder="请输入流水号！"
            @keyup.enter="KeyUp"
            @focus.prevent="showcountF"
          />
        </div>
        <div class="row2_tip">{{ ShowTip }}</div>
      </el-col>
      <el-col :span="10" :xs="24" :sm="16" class="row2_col2">
        <div class="row2_keyboard">
          <div class="row2_con">
            <el-row v-for="i in List.length / 3" :key="i">
              <el-col
                v-for="j in List.slice((i - 1) * 3, 3 * i)"
                :key="j"
                class="row2_col"
                :span="3"
              >
                <el-button @click="ClickInput(j)">{{ j }}</el-button>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {
  GetSignInfoByCode,
  GetCreditCardData,
  VerifyPatientSign,
  ConfirmPatientSign,
  GetDataTime
} from '@/api/singin'
export default {
  data() {
    return {
      nowDate: '',
      nowTime: '',
      SerialNumber: '', // 卡号
      ProtCode: '02',
      BodayLoading: false,
      ShowTip: '请输入流水序号或刷就诊卡签到！',
      Title: '排队叫号签到端', // 签到端标题
      Gid: '', // 签到端id
      GetCreditCardDataInput: {
        Gid: '',
        CardNo: ''
      },
      CardDataOutput: {}, // 患者信息
      List: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '确认', '退格']
    }
  },
  created() {
    this.ProtCode = this.$route.params.code
    // console.log('this.ProtCode', this.ProtCode, this.$route.params.code)
  },
  mounted() {
    this.GetDataTime()
    this.nowtimer = setInterval(() => {
      this.GetDataTime()
    }, 60000)
    setInterval(() => {
      this.$refs.address.focus()
    }, 1000)
    this.GetSignInfoByCode()
  },
  methods: {
    // 获取时间
    gettime() {
      // this.nowDate = new Date().toLocaleString();
      var date = new Date() // 时间对象
      var year = date.getFullYear() // 返回年
      var month = date.getMonth() + 1 // 返回0~11 月份
      var day = date.getDate() // 返回天
      var hour = date.getHours() // 返回小时
      var minutes = date.getMinutes() // 返回分
      var second = date.getSeconds() // 返回秒
      var time =
        year +
        '年' +
        month +
        '月' +
        day +
        '日' +
        hour +
        ':' +
        minutes +
        ':' +
        second
      this.nowDate = time
    },
    // 每个按钮点击事件
    ClickInput(value) {
      if (value == '确认') {
        if (this.SerialNumber.trim().length < 20) {
          if (this.SerialNumber && this.SerialNumber.trim().length > 0) {
            this.GetCreditCardData()
          } else this.$message.error('流水号为空！')
        } else {
          this.ShowTip = '流水号格式错误！'
          this.TimingTip()
        }
      } else if (value == '退格') {
        this.SerialNumber = this.SerialNumber.substr(
          0,
          this.SerialNumber.length - 1
        )
      } else this.SerialNumber += value
    },
    // 回车事件
    KeyUp() {
      // console.log("回车事件", this.SerialNumber);
      if (this.SerialNumber && this.SerialNumber.trim().length > 0) {
        this.GetCreditCardData()
      } else this.$message.error('流水号为空！')
    },
    // 获取签到端信息
    GetSignInfoByCode() {
      this.BodayLoading = true
      GetSignInfoByCode(this.ProtCode)
        .then((res) => {
          if (res && (res.success || res.code === 200)) {
            const { data } = res
            // console.log("获取签到端信息", res, data, this.ProtCode);
            this.Title = data.Title
            this.Gid = data.Gid
            this.GetCreditCardDataInput.Gid = data.Gid
          } else {
            this.ShowTip = '无法查询到签到端信息。'
            this.TimingTip()
            // this.$message.error("无法查询到签到端信息。");
          }
          this.BodayLoading = false
        })
        .catch((error) => {
          console.log(error)
          this.BodayLoading = false
          this.ShowTip = error.msg
          this.TimingTip()
          //   this.$message.error("服务器错误，无法查询到签到端信息！");
        })
    },
    // 获取刷卡数据
    GetCreditCardData() {
      this.BodayLoading = true
      this.GetCreditCardDataInput.CardNo = this.SerialNumber
      // console.log('this.SerialNumber', this.SerialNumber, this.GetCreditCardDataInput)
      GetCreditCardData(this.GetCreditCardDataInput)
        .then((res) => {
          // console.log("GetCreditCardData", res);
          if (res && (res.success || res.code === 200)) {
            const { data } = res
            // console.log("获取刷卡数据", res, data);
            this.CardDataOutput = data
            this.VerifyPatientSign()
          } else {
            this.SerialNumber = ''
            this.ShowTip = res.msg
            this.TimingTip()
            // this.$message.error("卡号错误，无法查询到患者信息！");
            this.BodayLoading = false
          }
        })
        .catch((error) => {
          this.SerialNumber = ''
          console.log(error)
          // this.ShowTip = "服务器错误，无法查询到患者信息！";
          this.TimingTip()
          //   this.$message.error("服务器错误，无法查询到患者信息！");
          this.BodayLoading = false
        })
    },
    // 判断是否签到
    VerifyPatientSign() {
      this.BodayLoading = true
      var data = {
        PatientID: this.CardDataOutput.PatientID,
        CheckType: this.CardDataOutput.CheckType,
        RecordDate: this.CardDataOutput.RecordDate
      }
      VerifyPatientSign(data)
        .then((res) => {
          if (res && (res.success || res.code === 200)) {
            // console.log("判断是否签到", res);
            if (res.data.IsSign == true) {
              this.$message.warning(
                `${this.CardDataOutput.PatientName}已签到，无需再签到！`
              )
              this.SerialNumber = ''
              this.BodayLoading = false
            } else {
              this.ConfirmPatientSign()
            }
          } else {
            this.SerialNumber = ''
            this.ShowTip = res.msg
            this.TimingTip()
            // this.$message.error("服务器错误，无法判断是否签到！");
            this.BodayLoading = false
          }
        })
        .catch((error) => {
          this.SerialNumber = ''
          console.log(error)
          this.BodayLoading = true
          // this.ShowTip = "服务器错误，无法判断是否签到！";
          this.TimingTip()
          //   this.$message.error("服务器错误，无法判断是否签到！");
        })
    },
    // 确认签到
    ConfirmPatientSign() {
      this.BodayLoading = true
      var data = {
        Gid: this.Gid,
        PatientInfo: this.CardDataOutput
      }
      // console.log("确认签到", data);
      ConfirmPatientSign(data)
        .then((res) => {
          if (res && (res.success || res.code === 200)) {
            // console.log("确认签到123", res);
            // this.$message.success(
            //   `签到成功，您排在第${res.data.xh}位，前面还有${res.data.TopOrder}个人！`
            // );

            this.ShowTip = `签到成功，您的序号是${res.data.xhlb}${res.data.xh}，前面还有${res.data.TopOrder}个人！`
            this.TimingTip()
          } else {
            this.ShowTip = res.msg
            // this.$message.error("签到失败！");
          }
          this.SerialNumber = ''
          this.BodayLoading = false
        })
        .catch((error) => {
          this.SerialNumber = ''
          console.log(error)
          // this.ShowTip = "服务器错误，无法签到！";
          //   this.$message.error("服务器错误，无法签到！");
          this.BodayLoading = false
        })
    },
    TimingTip() {
      setTimeout(() => {
        this.ShowTip = '请输入流水序号或刷就诊卡签到！'
      }, 5000)
    },
    showcountF() {
      // 文本框获取焦点但是不弹出键盘
      // console.log(this.$refs.address);
      // document.getElementById("address").setAttribute("readonly", "readonly");
      // this.$refs.address.setAttribute("readonly", "readonly");
    },
    GetDataTime() {
      GetDataTime()
        .then((res) => {
          // console.log("GetDataTime", res);
          const resDate = JSON.parse(res.data)
          this.nowDate =
            resDate.Year + '年' + resDate.Month + '月' + resDate.Day + '日'
          // this.nowTime = resDate.Time;
          this.nowTime = resDate.Hour24 + ':' + resDate.Minute
        })
        .catch((err) => {
          console.error(err)
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.body {
  width: 100%;
  height: 100%;
  padding: 15px 0px;
  background-color: #e1f3ff;
}
.row1daoji {
  padding-bottom: 20px;
  background: url("../../assets/Sign_images/banner1.png");
  background-size: cover;
  height: 300px;
}
.row1 {
  margin: 15px 0px 50px 0px;
  padding-bottom: 20px;
  border-bottom: 4px solid #7d80ee;

  .row1_tittle {
    font-size: 24px;
    margin-left: 35px;
  }

  .row1_time {
    font-size: 24px;
    text-align: right;
    margin-right: 25px;
  }
}

.row2 {
  padding: 0px 20%;

  .row2_col1 {
    .row2_input {
      // width: 68%;
      // margin-left: 50px;
      margin-bottom: 20px;

      ::v-deep .el-input__inner {
        height: 50px;
      }
    }

    .row2_tip {
      font-size: 22px;
      // margin-left: 50px;
      margin-top: 10%;
      color: red;
    }
  }

  .row2_col2 {
    margin-top: 90%;
    .row2_keyboard {
      text-align: center;

      //border: 1px solid gray;
      .row2_col {
        margin: 5px 1px 5px 1px;
        text-align: center;
        width: 110px;
      }

      .row2_con {
        display: inline-block;
        // border: 2px solid gray;
        // padding: 10px;
      }

      .el-button {
        width: 100px;
        height: 100px;
        font-size: 20px;
        background: url("../../assets/Sign_images/button3.png") no-repeat;
        background-size: cover;
        color: white;
      }
    }
  }
}
</style>
