<template>
  <div id="Prolife">
    <div class="HospitalType">
      <div class="ScreensDeptName">
        {{ ScreensInput.Name }}
      </div>
    </div>
    <div class="Doctorprolife">
      <div id="left">
        <div class="Doctor">
          <el-image
            style="width: 100%; height: 100%"
            :src="baseURLs + letof.Intro1"
            :fit="fit"
          ></el-image>
        </div>
      </div>
      <div id="right">
        <div class="Doctorintroduce">
          <div class="DoctorName">
            <b>{{ letof.Intro2 }}</b>
          </div>
          <div class="DoctorRank">{{ letof.Intro3 }}</div>
          <div class="introduce">
            <span class="introduceRemmarks">医生简介:{{ letof.Remarks }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<style lang="scss" scoped>
#Prolife {
  width: 100%;
  height: 100%;
  // border: 1px solid rgb(64, 27, 0);
  position: relative;
  display: flex;
  flex-direction: column;
}
.Doctorprolife {
  display: flex;
  margin-top: 10px;
  justify-content: space-around;
  align-items: center;
  height: 75%;
}
.Doctor {
  width: 100%;
  height: 90%;
  .DoctorName,
  .DoctorRank {
    text-align: center;
    // background-color: rgb(2,142,177);
    // padding: 4px;
    // box-shadow: 1px 1px 5px white inset;
    // margin-top:5px;
    color: #000000;
  }
}
.Doctorintroduce {
  width: 100%;
  height: 90%;
}
.DoctorName {
  font-size: 42px;
}
.DoctorRank {
  margin-top: 2px;
  font-size: 40px;
}
.HospitalType {
  // display: flex;
  //  justify-content: center;
  //  text-align: center;
  height: 25%;
  width: 98%;
  margin: 0 auto;
  color: white;
}
.ScreensDeptName {
  width: 100%;
  height: 100%;
  font-size: 50px;
  border-radius: 10px;
  background-color: #29afa8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}
#left {
  display: flex;
  align-items: center;
  justify-content: center;
  // flex-direction: column;
  width: 30%;
  height: 100%;
}
@keyframes rowup {
  0% {
    top: 0%;
  }
  100% {
    top: -100%;
  }
}
#right {
  width: 65%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  .introduce {
    color: #666666;
    width: 100%;
    word-break: break-word;
    height: 60%;
    overflow-y: auto;
    font-size: 24px;
    margin-top: 2px;
  }

  .introduce::-webkit-scrollbar {
    display: none;
  }
}
.introduceRemmarks {
  position: relative;
  animation: 10s rowup linear infinite normal;
}
</style>
<script>
import { baseURL } from "../../../../public/config";
export default {
  name: "Prolife",
  data() {
    return {
      url: "https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg",
      fit: "cover",
      name: "五育升是发",
      rank: "主治医生是",
      introduce:
        "asljdklasjdkasjdlkasjkdaslkdjaklsdjkasjdkaskjkldjkalskdjalkjdjlkaskljdlkjasdldajlasdjldasjladskjldasjladadladlakjdsjlaksdlkadsjlkadslkadladlkakaljdladalsd",
      letof: {
        Intro1: "",
        Intro2: "",
        Intro3: "",
        Remarks: "",
      },
      baseURLs: "",
    };
  },
  props: ["ScreensInput"],
  mounted() {
    // this.getDateTime();
    // this.nowTimes();
    this.baseURLs = baseURL;
  },
  watch: {
    ScreensInput() {
      this.GetScreenIntro();
      window.setInterval(() => {
        this.GetScreenIntro(); //定时刷新医生简介
      }, 10 * 1000 * 60);
    },
  },
  methods: {
    GetScreenIntro() {
      //根据屏幕id获取屏幕介绍
      this.$axios
        .post("/api/Screens/GetDocIntro", { ID: this.ScreensInput.ID })
        .then((res) => {
          console.log("GetDocIntro", res);
          this.letof = res.data.data;
        })
        .catch((err) => {
          console.error(err);
        });
    },
  },
};
</script>