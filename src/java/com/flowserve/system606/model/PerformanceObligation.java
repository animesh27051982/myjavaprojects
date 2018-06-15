/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "PERFORMANCE_OBLIGATIONS")
public class PerformanceObligation implements Comparable<PerformanceObligation>, Serializable {

    private static final long serialVersionUID = 4995349370717535419L;
    private static final Logger LOG = Logger.getLogger(PerformanceObligation.class.getName());

    @Id
    @Column(name = "POB_ID")
    private Long id;

    @Column(name = "NAME")
    private String name;
    @Column(name = "IS_ACTIVE")
    private boolean active;

    //private String deactivationReason;  // create type class
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "POB_INPUTS", joinColumns = @JoinColumn(name = "POB_ID"), inverseJoinColumns = @JoinColumn(name = "INPUT_ID"))
    @MapKey(name = "inputType.id")
    private Map<Long, Input> inputs = new HashMap<Long, Input>();

//    @Column(name = "STANDALONE_SELLING_PRICE")
//    private BigDecimal standaloneSellingPrice;
//
//    @Column(name = "SSP_DETERMINATION_METHOD_ID")
//    private PriceDeterminationMethod sspDeterminationMethod;
//
//    @Column(name = "REVENUE_RECOGNITION_AMOUNT")
//    private BigDecimal revenueRecognitionAmount;
//
//    @JoinColumn(name = "REVENUE_RECOGNITION_METHOD_ID")
//    private RevenueRecognitionMethod revenueRecognitionMethod;
//
//    @Column(name = "EST_COST_AT_COMPLETION")
//    private BigDecimal estimatedCostAtCompletion;
//
//    @Column(name = "REVENUE_START_DATE")
//    private LocalDate revenueStartDate;
//
//    @Column(name = "REVENUE_END_DATE")
//    private LocalDate revenueEndDate;
    public PerformanceObligation() {
    }

    @Override
    public int compareTo(PerformanceObligation obj) {
        return this.id.compareTo(obj.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PerformanceObligation) {
            return this.id.equals(((PerformanceObligation) obj).getId());
        }
        return false;
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

    public Map<Long, Input> getInputs() {
        return inputs;
    }
}
