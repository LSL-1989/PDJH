package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "gw_mapping_rule")
public class GwMappingRule {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "endpoint_id", length = 36)
    private String endpointId;

    @Column(name = "source_field", length = 200)
    private String sourceField;

    @Column(name = "target_field", length = 100)
    private String targetField;

    @Column(name = "rule_expression", length = 500)
    private String ruleExpression;

    @Column(length = 255)
    private String description;
    
    @Column(name = "create_time")
    private Date createTime;
}
