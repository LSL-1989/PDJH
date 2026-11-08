package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.CallAutoNumSyncResult;
import com.gocent.pdjh.entity.CallAutoNum;

import java.util.List;
import java.util.Map;

/** 自动排号序号（call_auto_num）维护：编号规则管理页 CRUD 与按权限同步 */
public interface CallAutoNumService {
    List<CallAutoNum> listAll();

    boolean create(Map<String, Object> body);

    boolean update(Map<String, Object> body);

    boolean remove(String gid);

    /**
     * 按当前用户角色已授权的主任工作台菜单，汇总当日 call_list 发号序号并 upsert 到 call_auto_num。
     *
     * @param token    登录 Token，用于解析角色与菜单权限
     * @param autoDate 同步日期 yyyy-MM-dd，空则取当天
     */
    CallAutoNumSyncResult syncFromCallList(String token, String autoDate);
}
