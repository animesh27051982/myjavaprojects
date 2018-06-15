package com.flowserve.system606.model;

import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "INPUT_TYPES")
public class InputType extends BaseEntity<Long> implements Serializable {

    private static final long serialVersionUID = -8382719960002472187L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FLS_SEQ")
    @SequenceGenerator(name = "FLS_SEQ", sequenceName = "FLS_SEQ", allocationSize = 1)
    @Column(name = "INPUT_TYPE_ID")
    private Long id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "EXCEL_SHEET")
    private String excelSheet;  // for reading from xlsx
    @Column(name = "EXCEL_COL")
    private String excelCol;
    @Column(name = "GROUP_NAME")
    private String groupName;  // for grouping tabs in calc review screens
    @Column(name = "GROUP_POSITION")
    private int groupPosition;
    @Column(name = "EFFECTIVE_FROM")
    private LocalDate effectiveFrom;
    @Column(name = "EFFECTIVE_TO")
    private LocalDate effectiveTo;
    @Column(name = "IS_ACTIVE")
    private boolean active;   // maybe redundant

    public InputType() {
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExcelSheet() {
        return excelSheet;
    }

    public void setExcelSheet(String excelSheet) {
        this.excelSheet = excelSheet;
    }

    public String getExcelCol() {
        return excelCol;
    }

    public void setExcelCol(String excelCol) {
        this.excelCol = excelCol;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getGroupPosition() {
        return groupPosition;
    }

    public void setGroupPosition(int groupPosition) {
        this.groupPosition = groupPosition;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
