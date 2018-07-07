/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import static javax.persistence.TemporalType.TIMESTAMP;

@Entity
@Table(name = "PERFORMANCE_OBLIGATIONS")
public class PerformanceObligation extends BaseEntity<Long> implements Calculable, Comparable<PerformanceObligation>, Serializable {

    private static final long serialVersionUID = 4995349370717535419L;
    private static final Logger LOG = Logger.getLogger(PerformanceObligation.class.getName());

    @Id
    @Column(name = "POB_ID")
    private Long id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "REV_REC_METHOD")
    private String revRecMethod;
    @ManyToOne
    @JoinColumn(name = "CONTRACT_ID")
    private Contract contract;
    @Column(name = "IS_ACTIVE")
    private boolean active;
    @OneToOne
    @JoinColumn(name = "CREATED_BY_ID")
    private User createdBy;
    @Temporal(TIMESTAMP)
    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;
    @OneToOne
    @JoinColumn(name = "LAST_UPDATED_BY_ID")
    private User lastUpdatedBy;
    @Temporal(TIMESTAMP)
    @Column(name = "LAST_UPDATE_DATE")
    private LocalDateTime lastUpdateDate;

    //private String deactivationReason;  // create type class
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "performanceObligation")
    private Map<FinancialPeriod, InputSet> periodInputSetMap = new HashMap<FinancialPeriod, InputSet>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "performanceObligation")
    private Map<FinancialPeriod, OutputSet> periodOutputSetMap = new HashMap<FinancialPeriod, OutputSet>();

    public PerformanceObligation() {
    }

    @Override
    public int compareTo(PerformanceObligation obj) {
        return this.id.compareTo(obj.getId());
    }

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public User getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(User lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public LocalDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(LocalDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public String getRevRecMethod() {
        return revRecMethod;
    }

    public void setRevRecMethod(String revRecMethod) {
        this.revRecMethod = revRecMethod;
    }

    // TODO - Temp code remove.  This is to support temp code from the JSF UI until we finish the calculations.
    public BigDecimal getPobCountRejected() {
        return new BigDecimal("10.0");
    }

    public BigDecimal getContractToLocalFxRate() {
        return new BigDecimal("1.0");
    }

    public BigDecimal getCurrencyValuePriorPeriod() {
        return new BigDecimal("10.0");
    }

    public BigDecimal getLiquidatedDamagesPriorPeriod() {
        return new BigDecimal("10.0");
    }

    public BigDecimal getTransactionPriceBacklogCC() {
        return new BigDecimal("50.0");
    }

    public Map<FinancialPeriod, InputSet> getPeriodInputSetMap() {
        return periodInputSetMap;
    }

    public Map<FinancialPeriod, OutputSet> getPeriodOutputSetMap() {
        return periodOutputSetMap;
    }
}
