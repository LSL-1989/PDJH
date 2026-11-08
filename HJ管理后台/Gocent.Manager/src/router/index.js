import { createRouter as createVueRouter, createWebHashHistory } from 'vue-router'

/* Layout */
import Layout from '@/layout'

// asyncRoutes: 保留导出以兼容 store/modules/user.js 中的引用（当前系统使用 constantRoutes 统一管理）
export const asyncRoutes = []

// constantRoutes：不需要动态判断权限的路由，如登录页、404、等通用页面
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/logins/index'),
    hidden: true
  },
  {
    path: '/h5',
    component: () => import('@/views/h5/index'),
    hidden: true
  },
  {
    path: '/Singin0/:code',
    component: () => import('@/views/SingIn/index'),
    hidden: true
  },
  {
    path: '/Singin2/:code',
    component: () => import('@/views/SingIn/index'),
    hidden: true
  },
  {
    path: '/Singin/:code',
    component: () => import('@/views/SingIn/index'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/404'),
    hidden: true
  },
  {
    path: '/redirect/:path(.*)',
    component: () => import('@/views/redirect/index'),
    hidden: true
  },
  // 1. 首页
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index'),
        meta: { title: '首页', icon: 'Home', affix: true, onCache: true }
      }
    ]
  },
  // 2. 主任工作台
  {
    path: '/directorDesk',
    component: Layout,
    name: 'directorDesk',
    redirect: '/directorDesk/outpatientDesk',
    meta: { title: '主任工作台', icon: 'el-icon-s-custom' },
    children: [
      {
        path: 'outpatientDesk',
        name: 'outpatientDesk',
        component: () => import('@/views/directorDesk/outpatientDesk/index'),
        meta: { title: '门诊主任工作台' }
      },
      {
        path: 'pharmacyDesk',
        name: 'pharmacyDesk',
        component: () => import('@/views/directorDesk/pharmacyDesk/index'),
        meta: { title: '药房主任工作台' }
      },
      {
        path: 'examDesk',
        name: 'examDesk',
        component: () => import('@/views/directorDesk/examDesk/index'),
        meta: { title: '体检中心主任工作台' }
      },
      {
        path: 'labDesk',
        name: 'labDesk',
        component: () => import('@/views/directorDesk/labDesk/index'),
        meta: { title: '检验主任工作台' }
      },
      {
        path: 'medtechDesk',
        name: 'medtechDesk',
        component: () => import('@/views/directorDesk/medtechDesk/index'),
        meta: { title: '医技主任工作台' }
      },
      {
        path: 'statistics',
        name: 'statisticsDesk',
        component: () => import('@/views/statistics/index'),
        meta: { title: '全院排队统计分析', icon: 'el-icon-s-data' }
      }
    ]
  },
  // 3. 设备与屏幕
  {
    path: '/deviceManage',
    component: Layout,
    name: 'deviceManage',
    redirect: '/deviceManage/ScreenManages',
    meta: { title: '设备与屏幕', icon: 'el-icon-monitor' },
    children: [
      {
        path: 'ScreenManages',
        name: 'ScreenManages',
        component: () => import('@/views/ScreenManages/index'),
        meta: { title: '屏幕管理' }
      },
      {
        path: 'SignDeviceManages',
        name: 'SignDeviceManages',
        component: () => import('@/views/SignDeviceManages/index'),
        meta: { title: '签到终端管理' }
      },
      {
        path: 'VoiceDeviceManages',
        name: 'VoiceDeviceManages',
        component: () => import('@/views/VoiceDeviceManages/index'),
        meta: { title: '独立音响管理' }
      },
      {
        path: 'AutoNumManages',
        name: 'AutoNumManages',
        component: () => import('@/views/AutoNumManages/index'),
        meta: { title: '编号规则管理' }
      }
    ]
  },
  // 4. 大屏显示内容配置
  {
    path: '/screenConfig',
    component: Layout,
    name: 'screenConfig',
    redirect: '/screenConfig/index',
    meta: { title: '大屏显示内容配置', icon: 'el-icon-video-camera' },
    children: [
      {
        path: 'index',
        name: 'screenConfigIndex',
        component: () => import('@/views/screenConfig/index'),
        meta: { title: '候诊区大屏配置' }
      }
    ]
  },
  // 4b. 门边屏显示配置
  {
    path: '/clinicConfig',
    component: Layout,
    name: 'clinicConfig',
    redirect: '/clinicConfig/index',
    meta: { title: '门边屏显示配置', icon: 'el-icon-mobile-phone' },
    children: [
      {
        path: 'index',
        name: 'clinicConfigIndex',
        component: () => import('@/views/clinicConfig/index'),
        meta: { title: '门边屏显示配置' }
      }
    ]
  },
  // 5. 院内业务树
  {
    path: '/hospitalInfo',
    component: Layout,
    name: 'hospitalInfo',
    redirect: '/hospitalInfo/DeptManages',
    meta: { title: '院内业务树', icon: 'el-icon-office-building' },
    children: [
      {
        path: 'DeptManages',
        name: 'DeptManages',
        component: () => import('@/views/DeptManages/index'),
        meta: { title: '科室管理' }
      },
      {
        path: 'OfficeRoomManages',
        name: 'OfficeRoomManages',
        component: () => import('@/views/OfficeRoomManages/index'),
        meta: { title: '物理诊室与终端资源' }
      },
      {
        path: 'DoctorProfile',
        name: 'DoctorProfile',
        component: () => import('@/views/DoctorProfile/index'),
        meta: { title: '医生管理' }
      }
    ]
  },
  // 6. 业务配置中心
  {
    path: '/businessConfig',
    component: Layout,
    name: 'businessConfig',
    redirect: '/businessConfig/ruleCenter',
    meta: { title: '业务配置中心', icon: 'el-icon-s-tools' },
    children: [
      {
        path: 'ruleCenter',
        name: 'ruleCenterIndex',
        component: () => import('@/views/ruleCenter/index'),
        meta: { title: '叫号规则配置中心' }
      },
      {
        path: 'doctorSchedule',
        name: 'doctorScheduleIndex',
        component: () => import('@/views/doctorSchedule/index'),
        meta: { title: '出诊排班管理' }
      },
      {
        path: 'nurseSchedule',
        name: 'nurseScheduleIndex',
        component: () => import('@/views/nurseSchedule/index'),
        meta: { title: '护士当班排班' }
      },
      {
        path: 'examPackage',
        name: 'examPackageIndex',
        component: () => import('@/views/examPackage/index'),
        meta: { title: '体检套餐管理' }
      },
      {
        path: 'medtechAppointment',
        name: 'medtechAppointmentIndex',
        component: () => import('@/views/medtechAppointment/index'),
        meta: { title: '医技预约时段配置' }
      }
    ]
  },
  // 7. 集成网关管理
  {
    path: '/systemIntegration',
    component: Layout,
    name: 'systemIntegration',
    redirect: '/systemIntegration/integrationConfig',
    meta: { title: '集成网关管理', icon: 'el-icon-connection' },
    children: [
      {
        path: 'integrationConfig',
        name: 'integrationConfig',
        component: () => import('@/views/integration/index'),
        meta: { title: '数据源接口配置' }
      },
      {
        path: 'integrationDashboard',
        name: 'integrationDashboard',
        component: () => import('@/views/integration/dashboard'),
        meta: { title: '运维护航沙盘', icon: 'el-icon-odometer' }
      },
      {
        path: 'auditLog',
        name: 'auditLog',
        component: () => import('@/views/auditLog/index'),
        meta: { title: '外部网关审计查询' }
      },
      {
        path: 'systemLog',
        name: 'systemLog',
        component: () => import('@/views/systemLog/index'),
        meta: { title: '业务事件日志轨迹' }
      },
      {
        path: 'patientFlowLog',
        name: 'patientFlowLog',
        component: () => import('@/views/patientFlowLog/index'),
        meta: { title: '患者流程日志' }
      }
    ]
  },
  // 8. 业务实施中心
  {
    path: '/implementation',
    component: Layout,
    name: 'implementation',
    redirect: '/implementation/wizard',
    meta: { title: '业务实施中心', icon: 'el-icon-guide' },
    children: [
      {
        path: 'wizard',
        name: 'wizard',
        component: () => import('@/views/implementation/index'),
        meta: { title: '业务实施中心' }
      }
    ]
  },
  // 9. 系统与权限管理
  {
    path: '/userManage',
    component: Layout,
    name: 'userManage',
    redirect: '/userManage/systemConfig',
    meta: { title: '系统与权限管理', icon: 'el-icon-s-operation' },
    children: [
      {
        path: 'systemConfig',
        name: 'SystemConfig',
        component: () => import('@/views/systemConfig/index'),
        meta: { title: '全局字典参数' }
      },
      {
        path: 'shiftType',
        name: 'shiftType',
        component: () => import('@/views/shiftType/index'),
        meta: { title: '排班类型' }
      },
      {
        path: 'DoctorTitle',
        name: 'DoctorTitle',
        component: () => import('@/views/DoctorTitle/index'),
        meta: { title: '医生职称' }
      },
      {
        path: 'maintenanceUser',
        name: 'maintenanceUser',
        component: () => import('@/views/maintenanceUser/index'),
        meta: { title: '用户管理' }
      },
      {
        path: 'maintenanceRole',
        name: 'maintenanceRole',
        component: () => import('@/views/maintenanceRole/index'),
        meta: { title: '角色管理' }
      },
      {
        path: 'maintenanceMenu',
        name: 'maintenanceMenu',
        component: () => import('@/views/maintenanceMenu/index'),
        meta: { title: '菜单管理' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/404', hidden: true }
]

const createRouter = () => createVueRouter({
  history: createWebHashHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: constantRoutes
})

const router = createRouter()

/** 清空动态路由并恢复 constantRoutes；供登出重置（当前 logout 未启用） */
export function resetRouter() {
  router.getRoutes().forEach(route => {
    if (route.name) {
      router.removeRoute(route.name)
    }
  })
  constantRoutes.forEach(route => {
    router.addRoute(route)
  })
}

export default router
