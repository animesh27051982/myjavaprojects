/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.logging.Logger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "POB_MEASUREMENTS")
public class PerformanceMeasurement implements Comparable<PerformanceMeasurement>, Serializable {

    private static final long serialVersionUID = 2190805698054131338L;
    private static final Logger LOG = Logger.getLogger(PerformanceMeasurement.class.getName());

    @Id
    @Column(name = "POB_MEASUREMENT_ID")
    private Long id;

    @OneToOne
    @JoinColumn(name = "FINANCIAL_PERIOD_ID")
    private FinancialPeriod financialPeriod;

    @Column(name = "ALLOCATED_TRANSCACTION_PRICE")
    private BigDecimal allocatedTransactionPrice;

    @Column(name = "DISCOUNT_AMOUNT")
    private BigDecimal discountAmount;

    @Column(name = "ALLOCATED_DISCOUNTED_TRANSCACTION_PRICE")
    private BigDecimal allocatedDiscountedTransactionPrice;

    public PerformanceMeasurement() {
    }

    @Override
    public int compareTo(PerformanceMeasurement obj) {
        return this.id.compareTo(obj.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PerformanceMeasurement) {
            return this.id.equals(((PerformanceMeasurement) obj).getId());
        }
        return false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FinancialPeriod getFinancialPeriod() {
        return financialPeriod;
    }

    public void setFinancialPeriod(FinancialPeriod financialPeriod) {
        this.financialPeriod = financialPeriod;
    }

}
