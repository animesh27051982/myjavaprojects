/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "COMPANIES")
public class Company extends BaseEntity<String> implements Comparable<Company>, Accumulable, Serializable {

    private static final long serialVersionUID = -5428359272400395184L;
    private static final Logger LOG = Logger.getLogger(Company.class.getName());

    @Id
    @Column(name = "COMPANY_ID")
    private String id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "TYPE")
    private String type;
    @Column(name = "DESCRIPTION")
    private String description;
    @OneToOne
    @JoinColumn(name = "PERIOD_ID")
    private FinancialPeriod currentPeriod;
    @Column(name = "INPUT_FREEZE_WORKDAY")
    private Integer inputFreezeWorkday;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "company", cascade = CascadeType.MERGE)
    private List<ReportingUnit> reportingUnit = new ArrayList<ReportingUnit>();

    public Company() {
    }

    @Override
    public int compareTo(Company obj) {
        return this.name.compareTo(obj.getName());
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FinancialPeriod getCurrentPeriod() {
        return currentPeriod;
    }

    public void setCurrentPeriod(FinancialPeriod currentPeriod) {
        this.currentPeriod = currentPeriod;
    }

    public Integer getInputFreezeWorkday() {
        return inputFreezeWorkday;
    }

    public void setInputFreezeWorkday(Integer inputFreezeWorkday) {
        this.inputFreezeWorkday = inputFreezeWorkday;
    }

    public List<ReportingUnit> getReportingUnit() {
        return reportingUnit;
    }

    public void setReportingUnit(List<ReportingUnit> reportingUnit) {
        this.reportingUnit = reportingUnit;
    }

    @Override
    public List<Accumulable> getChildAccumulables() {
        return new ArrayList<Accumulable>(reportingUnit);
    }
}
