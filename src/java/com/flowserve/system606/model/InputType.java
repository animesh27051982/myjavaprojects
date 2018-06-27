package com.flowserve.system606.model;

import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import static javax.persistence.TemporalType.DATE;

@Entity
@Table(name = "INPUT_TYPES")
public class InputType extends BaseEntity<String> implements Comparable<InputType>, Serializable {

    private static final long serialVersionUID = -8382719960002472187L;

    @Id
    @Column(name = "INPUT_TYPE_ID")
    private String id;
    @Column(name = "OWNER_ENTITY_TYPE")
    private String ownerEntityType;
    @Column(name = "INPUT_CLASS")
    private String inputClass;
    @Column(name = "NAME")
    private String name;
    @Column(name = "DESCRIPTION")
    private String description;
    @Column(name = "INPUT_CURRENCY_TYPE")
    private CurrencyType inputCurrencyType;
    @Column(name = "EXCEL_SHEET")
    private String excelSheet;  // for reading from xlsx
    @Column(name = "EXCEL_COL")
    private String excelCol;
    @Column(name = "GROUP_NAME")
    private String groupName;  // for grouping tabs in calc review screens
    @Column(name = "GROUP_POSITION")
    private int groupPosition;
    @Temporal(DATE)
    @Column(name = "EFFECTIVE_FROM")
    private LocalDate effectiveFrom;
    @Temporal(DATE)
    @Column(name = "EFFECTIVE_TO")
    private LocalDate effectiveTo;
    @Column(name = "IS_ACTIVE")
    private boolean active;   // maybe redundant

    public InputType() {
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getInputClass() {
        return inputClass;
    }

    public void setInputClass(String inputClass) {
        this.inputClass = inputClass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerEntityType() {
        return ownerEntityType;
    }

    public void setOwnerEntityType(String ownerEntityType) {
        this.ownerEntityType = ownerEntityType;
    }

    public CurrencyType getInputCurrencyType() {
        return inputCurrencyType;
    }

    public void setInputCurrencyType(CurrencyType inputCurrencyType) {
        this.inputCurrencyType = inputCurrencyType;
    }

    @Override
    public int compareTo(InputType o) {
        return this.name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InputType) {
            return this.name.equals(((InputType) obj).getName());
        }
        return false;
    }
}
