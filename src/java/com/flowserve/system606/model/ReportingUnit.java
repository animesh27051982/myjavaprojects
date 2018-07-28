/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "REPORTING_UNITS")
public class ReportingUnit extends TransientMeasurable<Long> implements Measurable, Comparable<ReportingUnit>, Serializable {

    private static final long serialVersionUID = 8757812203684986897L;
    private static final Logger LOG = Logger.getLogger(ReportingUnit.class.getName());
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REPORTING_UNIT_SEQ")
    @SequenceGenerator(name = "REPORTING_UNIT_SEQ", sequenceName = "REPORTING_UNIT_SEQ", allocationSize = 50)
    @Column(name = "REPORTING_UNIT_ID")
    private Long id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "CODE")
    private String code;
    @ManyToOne
    @JoinColumn(name = "BUSINESS_UNIT_ID")
    private BusinessUnit businessUnit;
    @ManyToOne
    @JoinColumn(name = "COUNTRY_ID")
    private Country country;
    @Column(name = "LOCAL_CURRENCY")
    private Currency localCurrency;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "REPORTING_UNIT_PREPARERS", joinColumns = @JoinColumn(name = "REPORTING_UNIT_ID"), inverseJoinColumns = @JoinColumn(name = "USER_ID"))
    private List<User> preparers = new ArrayList<User>();
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "REPORTING_UNIT_APPROVERS", joinColumns = @JoinColumn(name = "REPORTING_UNIT_ID"), inverseJoinColumns = @JoinColumn(name = "USER_ID"))
    private List<User> approvers = new ArrayList<User>();
    @Column(name = "IS_ACTIVE")
    private boolean active;
    @ManyToOne
    @JoinColumn(name = "PARENT_ID")
    private ReportingUnit parent;

    @ManyToOne
    @JoinColumn(name = "COMPANY_ID")
    private Company company;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "reportingUnit", cascade = CascadeType.ALL)
    private List<Contract> contracts = new ArrayList<Contract>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
    private List<ReportingUnit> childReportingUnits = new ArrayList<ReportingUnit>();

    public ReportingUnit() {
    }

    @Override
    public int compareTo(ReportingUnit obj) {
        return this.code.compareTo(obj.getCode());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Currency getLocalCurrency() {
        return localCurrency;
    }

    public Currency getContractCurrency() {
        return localCurrency;
    }

    public Currency getReportingCurrency() {
        return company.getReportingCurrency();
    }

    public void setLocalCurrency(Currency localCurrency) {
        this.localCurrency = localCurrency;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public List<User> getApprovers() {
        return approvers;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<User> getPreparers() {
        return preparers;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(BusinessUnit businessUnit) {
        this.businessUnit = businessUnit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Contract> getContracts() {
        return contracts;
    }

    public List<Measurable> getChildMeasurables() {
        return new ArrayList<Measurable>(contracts);
    }

    public BigDecimal getPobCountRejected() {  // TODO - Remove. Temp code for UI.
        return new BigDecimal("10.0");
    }

    public List<PerformanceObligation> getPerformanceObligations() {
        List<PerformanceObligation> pobs = new ArrayList<PerformanceObligation>();
        contracts.forEach(contract -> pobs.addAll(contract.getPerformanceObligations()));

        return pobs;
    }

    public int getPobCount() {
        return getPerformanceObligations().size();
    }

    public long getPobInputRequiredCount() {
        return 0l;
        //return getPerformanceObligations().stream().filter(PerformanceObligation::isInputRequired).count();
    }

    public long getPobInvalidCount() {
        return 0L;
    }

    public ReportingUnit getParent() {
        return parent;
    }

    public void setParent(ReportingUnit parent) {
        this.parent = parent;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public List<ReportingUnit> getChildReportingUnits() {
        return childReportingUnits;
    }

    public void setChildReportingUnits(List<ReportingUnit> childReportingUnits) {
        this.childReportingUnits = childReportingUnits;
    }

    public boolean checkIsParent() {
        return childReportingUnits.size() > 0;
    }
}
