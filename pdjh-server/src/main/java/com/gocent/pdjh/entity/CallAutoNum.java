package com.gocent.pdjh.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "call_auto_num")
public class CallAutoNum {

    @Id
    @Column(length = 36)
    @JsonProperty("GID")
    private String gid;

    @JsonProperty("Code")
    private String code;

    @Column(name = "did", length = 36)
    @JsonProperty("DID")
    private String did;

    @JsonProperty("Name")
    private String name;

    @Column(name = "auto_date")
    @JsonProperty("AutoDate")
    private LocalDate autoDate;

    @Column(name = "type_a")
    @JsonProperty("TypeA")
    private Integer typeA;

    @Column(name = "type_b")
    @JsonProperty("TypeB")
    private Integer typeB;

    @Column(name = "type_c")
    @JsonProperty("TypeC")
    private Integer typeC;
}
