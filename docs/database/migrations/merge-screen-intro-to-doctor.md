# 合并 screen_intro 至 doctor 迁移说明

## 背景

历史表 `screen_intro` 仅存 DoctorProfile 展示字段，与排班/HIS/诊室屏使用的 `doctor` 表重复。合并后 **doctor 为唯一主数据**。

## 字段对照

| screen_intro | doctor |
|--------------|--------|
| asid | work_no |
| intro2 | name |
| intro3 | title |
| intro1 | avatar |
| remarks | remarks |
| specialty | specialty |
| status (0/1) | display_status |
| create_date | create_date |

## 执行步骤

1. 部署含扩展字段的后端（Hibernate `ddl-auto=update` 或手工 ALTER）
2. 备份数据库
3. 执行脚本：

```bash
mysql -h主机 -u用户 -p 库名 < scripts/migrate_screen_intro_to_doctor.sql
```

4. 验证 DoctorProfile、doctorSchedule、HIS 同步
5. 确认无误后取消脚本末尾注释，执行 `DROP TABLE screen_intro`

## 回滚

- 合并前备份 `screen_intro` 与 `doctor` 全表
- 若仅误跑迁移 SQL，可从备份恢复；已 DROP 表则需全库回滚

## API 变更

| 废弃 | 替代 |
|------|------|
| POST `/api/Intros/GetIntrosList` | POST `/api/Doctor/GetPageList` |
| POST `/api/Intros/InsertIntros` | POST `/api/Doctor/Save` |
| POST `/api/Intros/ChangeIntros` | POST `/api/Doctor/Save` |
| POST `/api/Intros/DeleteIntros` | POST `/api/Doctor/Delete` |
| POST `/api/Intros/UploadIntrosAsync` | POST `/api/Doctor/UploadAvatar` |

## 遗留屏

`HJ候诊区大屏/prolife.vue`（**HJ候诊区大屏**）仍调用旧接口 `Screens/GetDocIntro`，新后端未实现；**HJ诊室门边屏终端**已读 `doctor` 表，不受影响。
