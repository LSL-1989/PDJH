/**
 * 护士工作站能力清单（登录展示 + 工作台功能导航）
 * 与 ActionPanel / AlertInbox 实际能力对齐，勿重复造业务接口
 */

export const CAPABILITY_GROUPS = [
  {
    id: 'monitor',
    name: '监测中心',
    color: '#1E88E5',
    bg: 'rgba(30, 136, 229, 0.12)',
    items: [
      { id: 'kpi', name: '诊区 KPI', desc: '候诊·未签到·过号·告警·诊室开放率', icon: 'DataAnalysis' },
      { id: 'rooms', name: '诊室矩阵', desc: '按科室分组展示开放/拥堵/停诊', icon: 'OfficeBuilding' },
      { id: 'alerts', name: '告警收件箱', desc: '超时·过号·未签到·缺诊室·停诊', icon: 'Bell', action: 'alerts' },
      { id: 'search', name: '患者检索', desc: '卡号/排队号/姓名一键定位', icon: 'Search', action: 'search' },
      { id: 'ws', name: '实时推送', desc: 'WebSocket 自动刷新队列状态', icon: 'Connection' }
    ]
  },
  {
    id: 'signin',
    name: '签到服务',
    color: '#43A047',
    bg: 'rgba(67, 160, 71, 0.12)',
    items: [
      { id: 'signin', name: '签到兜底', desc: '扫码失败时代签，需选原因', icon: 'CircleCheck', action: 'sign-in' },
      { id: 'override', name: 'Override 签到', desc: '跳过预约时段校验', icon: 'EditPen' },
      { id: 'bind', name: '补绑诊室', desc: '候诊未绑定诊室时修正', icon: 'Location' }
    ]
  },
  {
    id: 'queue',
    name: '队列干预',
    color: '#FB8C00',
    bg: 'rgba(251, 140, 0, 0.12)',
    items: [
      { id: 'revive', name: '过号复活', desc: '过号患者重新进入队列', icon: 'RefreshLeft' },
      { id: 'cutin', name: '手动插队', desc: '调整排队顺序（审计留痕）', icon: 'Top' },
      { id: 'swap', name: '转诊', desc: '跨科室调整候诊队列', icon: 'Switch' },
      { id: 'consult', name: '会诊转诊', desc: '会诊场景专用转诊类型', icon: 'Share' },
      { id: 'freeze', name: '冻结 / 解冻', desc: '暂停或恢复患者排队', icon: 'Lock' }
    ]
  },
  {
    id: 'dept',
    name: '科室与退号',
    color: '#E53935',
    bg: 'rgba(229, 57, 53, 0.1)',
    items: [
      { id: 'suspend', name: '科室停诊', desc: '确认停诊并处置候诊患者', icon: 'Warning' },
      { id: 'resume', name: '恢复出诊', desc: '解除科室停诊状态', icon: 'CircleCheck' },
      { id: 'refund', name: 'HIS 已退', desc: '收费退号后同步作废队列', icon: 'DocumentDelete' }
    ]
  }
]

/** 叶子能力总数（展示用） */
export const TOTAL_CAPABILITIES = CAPABILITY_GROUPS.reduce((n, g) => n + g.items.length, 0)

/** 需先选患者后触发的能力 id */
export const PATIENT_REQUIRED_IDS = new Set([
  'override', 'bind', 'revive', 'cutin', 'swap', 'consult', 'freeze', 'refund'
])
