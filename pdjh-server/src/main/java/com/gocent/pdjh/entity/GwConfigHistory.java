package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "gw_config_history")
public class GwConfigHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "ENDPOINT", "MAPPING_RULES"
    @Column(name = "entity_type", length = 50)
    private String entityType;

    // The ID of the GwEndpoint or Connection
    @Column(name = "entity_id", length = 36)
    private String entityId;

    // JSON snapshot of the configuration at this point in time
    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "operation_type", length = 20)
    private String operationType; // CREATE, UPDATE, DELETE

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
}
