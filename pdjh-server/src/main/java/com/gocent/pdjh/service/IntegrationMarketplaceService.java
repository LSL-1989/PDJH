package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.GwEndpoint;
import com.gocent.pdjh.entity.GwMappingRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class IntegrationMarketplaceService {

    @Autowired
    private IntegrationConfigService configService;

    // In-memory static templates for MVP
    private final List<Map<String, Object>> marketplaceTemplates = new ArrayList<>();

    @PostConstruct
    public void initTemplates() {
        addTemplate("tpl_winning_reg_01", "HTTP", "卫宁健康 (Winning)", "门诊挂号",
                "卫宁门诊挂号 JSON，目标字段与入队口径一致",
                endpoint("卫宁门诊挂号同步", "WINNING_REG_SYNC", "GET",
                        "/api/winning/outpatient/registrations?date={date}", null),
                List.of(
                        rule("$.data[*].PATIENT_ID", "patientCode", null, "患者标识"),
                        rule("$.data[*].PATIENT_NAME", "patientName", null, "患者姓名"),
                        rule("$.data[*].DEPT_CODE", "deptID", null, "科室"),
                        rule("$.data[*].ROOM_ID", "officeRoomId", null, "诊室"),
                        rule("$.data[*].DOCTOR_NO", "doctorID", null, "医生工号"),
                        rule("$.data[*].QUEUE_NO", "showNo", null, "显示号码"),
                        rule("-", "sourceType", "CONST:CLINIC", "来源固定为门诊")
                ));

        addTemplate("tpl_neusoft_queue_01", "JDBC", "东软 (Neusoft)", "门诊视图轮询",
                "东软只读视图。定时任务只拉取并记审计，不会自动入队",
                endpoint("东软门诊视图轮询", "NEUSOFT_VIEW_SYNC", "POLLING",
                        "SELECT pat_id, pat_name, dept_id, room_id, queue_no FROM V_OUTPATIENT_QUEUE WHERE reg_date = CURRENT_DATE",
                        "0/30 * * * * ?"),
                List.of(
                        rule("pat_id", "patientCode", null, "患者标识"),
                        rule("pat_name", "patientName", null, "患者姓名"),
                        rule("dept_id", "deptID", null, "科室"),
                        rule("room_id", "officeRoomId", null, "诊室"),
                        rule("queue_no", "showNo", null, "显示号码"),
                        rule("-", "sourceType", "CONST:CLINIC", "来源固定为门诊")
                ));

        addTemplate("tpl_pharmacy_rx_01", "HTTP", "药房", "处方与药品大类",
                "取药签到前同步药品大类，供中西药分窗。无大类时业务侧回退最短队列",
                endpoint("药房处方同步", "PHARMACY_RX_SYNC", "GET", "/api/his/pharmacy/prescriptions?code={patientCode}", null),
                List.of(
                        rule("$.data.patientCode", "patientCode", null, "患者标识"),
                        rule("$.data.patientName", "patientName", null, "患者姓名"),
                        rule("$.data.deptCode", "deptID", null, "药房"),
                        rule("$.data.windowId", "officeRoomId", null, "窗口"),
                        rule("$.data.drugCategory", "sflbbigtype", null, "药品大类"),
                        rule("-", "sourceType", "CONST:PHARMACY", "来源固定为药房")
                ));

        addTemplate("tpl_lab_order_01", "HTTP", "检验", "检验申请",
                "检验申请同步，带项目编码",
                endpoint("检验申请同步", "LAB_ORDER_SYNC", "GET", "/api/lis/orders?code={patientCode}", null),
                List.of(
                        rule("$.data.patientCode", "patientCode", null, "患者标识"),
                        rule("$.data.patientName", "patientName", null, "患者姓名"),
                        rule("$.data.deptCode", "deptID", null, "检验科室"),
                        rule("$.data.windowId", "officeRoomId", null, "窗口"),
                        rule("$.data.itemCode", "itemCode", null, "项目编码"),
                        rule("-", "sourceType", "CONST:OUTPATIENT_EXAM", "来源固定为医技")
                ));

        addTemplate("tpl_his_register_01", "WEBHOOK", "HIS", "挂号推送",
                "对应固定地址 /api/integration/webhook/his/register。本模板用于挂接 IP 白名单，报文仍按标准字段解析",
                endpoint("HIS挂号推送", "HIS_REGISTER", "WEBHOOK", "/integration/webhook/his/register", null),
                List.of(
                        rule("$.patientCode", "patientCode", null, "患者标识"),
                        rule("$.patientName", "patientName", null, "患者姓名"),
                        rule("$.deptId", "deptID", null, "科室"),
                        rule("$.officeRoomId", "officeRoomId", null, "诊室"),
                        rule("$.showNo", "showNo", null, "显示号码"),
                        rule("-", "sourceType", "CONST:CLINIC", "来源固定为门诊")
                ));

        addTemplate("tpl_exam_status_01", "WEBHOOK", "LIS/PACS", "检查完成回推",
                "对应固定地址 /api/integration/webhook/exam/status。对方 state=4 表示完成，由该接口转成本地完结并算下一站",
                endpoint("检查完成回推", "EXAM_STATUS", "WEBHOOK", "/integration/webhook/exam/status", null),
                List.of(
                        rule("$.patientCode", "patientCode", null, "患者标识"),
                        rule("$.itemCode", "itemCode", null, "项目编码"),
                        rule("$.state", "state", null, "对方完成标志，4 为完成")
                ));
    }

    private void addTemplate(String id, String connectionType, String vendor, String name, String description,
                             GwEndpoint endpoint, List<GwMappingRule> rules) {
        Map<String, Object> template = new HashMap<>();
        template.put("id", id);
        template.put("connectionType", connectionType);
        template.put("vendor", vendor);
        template.put("name", name);
        template.put("description", description);
        template.put("endpoint", endpoint);
        template.put("rules", rules);
        marketplaceTemplates.add(template);
    }

    private GwEndpoint endpoint(String name, String code, String method, String path, String cron) {
        GwEndpoint endpoint = new GwEndpoint();
        endpoint.setName(name);
        endpoint.setEndpointCode(code);
        endpoint.setMethod(method);
        endpoint.setPath(path);
        endpoint.setCronExpression(cron);
        return endpoint;
    }

    private GwMappingRule rule(String source, String target, String expression, String desc) {
        GwMappingRule rule = new GwMappingRule();
        rule.setSourceField(source);
        rule.setTargetField(target);
        rule.setRuleExpression(expression);
        rule.setDescription(desc);
        return rule;
    }

    public List<Map<String, Object>> getMarketplaceTemplates() {
        return marketplaceTemplates;
    }

    /**
     * 把模板写成端点和映射。connectionType 与当前连接不一致时拒绝。
     * operator 写入配置快照。
     */
    public void importTemplate(String templateId, String connectionId, String operator) throws Exception {
        Map<String, Object> template = marketplaceTemplates.stream()
                .filter(t -> t.get("id").equals(templateId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        com.gocent.pdjh.entity.GwConnection connection = configService.getConnection(connectionId);
        if (connection == null) {
            throw new IllegalArgumentException("连接不存在");
        }
        String expectType = String.valueOf(template.get("connectionType"));
        if (!expectType.equalsIgnoreCase(connection.getType())) {
            throw new IllegalArgumentException("该模板需要 " + expectType + " 连接，当前是 " + connection.getType());
        }

        GwEndpoint draftEndpoint = (GwEndpoint) template.get("endpoint");
        @SuppressWarnings("unchecked")
        List<GwMappingRule> draftRules = (List<GwMappingRule>) template.get("rules");

        // Clone and Save Endpoint
        GwEndpoint newEndpoint = new GwEndpoint();
        newEndpoint.setConnectionId(connectionId);
        newEndpoint.setName(draftEndpoint.getName() + " (Imported)");
        String code = draftEndpoint.getEndpointCode();
        if ("HIS_REGISTER".equals(code) || "EXAM_STATUS".equals(code)) {
            newEndpoint.setEndpointCode(code);
        } else {
            newEndpoint.setEndpointCode(code + "_" + (System.currentTimeMillis() % 10000));
        }
        newEndpoint.setMethod(draftEndpoint.getMethod());
        newEndpoint.setPath(draftEndpoint.getPath());
        newEndpoint.setCronExpression(draftEndpoint.getCronExpression());
        
        GwEndpoint savedEndpoint = configService.saveEndpoint(newEndpoint, operator);

        for (GwMappingRule draftRule : draftRules) {
            GwMappingRule newRule = new GwMappingRule();
            newRule.setEndpointId(savedEndpoint.getId());
            newRule.setSourceField(draftRule.getSourceField());
            newRule.setTargetField(draftRule.getTargetField());
            newRule.setRuleExpression(draftRule.getRuleExpression());
            newRule.setDescription(draftRule.getDescription());
            configService.saveMappingRule(newRule, operator);
        }
    }
}
