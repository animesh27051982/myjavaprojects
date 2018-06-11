/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.util.Currency;
import java.util.logging.Logger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "REPORTING_UNITS")
public class ReportingUnit implements Comparable<ReportingUnit>, Serializable {

    private static final long serialVersionUID = 8757812203684986897L;
    private static final Logger LOG = Logger.getLogger(ReportingUnit.class.getName());
    @Id
    @Column(name = "REPORTING_UNIT_ID")
    private Long id;

    @Column(name = "REPORTING_UNIT_NAME")
    private String name;
    @Column(name = "REPORTING_UNIT_NUMBER")
    private String number;
    @OneToOne
    @JoinColumn(name = "BUSINESS_PLATFORM_ID")
    private BusinessPlatform businessPlatform;

    @Column(name = "FUNCTIONAL_CURRENCY")
    private Currency functionalCurrency;

    public ReportingUnit() {
    }

    @Override
    public int compareTo(ReportingUnit obj) {
        return this.name.compareTo(obj.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReportingUnit) {
            return this.id.equals(((ReportingUnit) obj).getId());
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public BusinessPlatform getBusinessPlatform() {
        return businessPlatform;
    }

    public void setBusinessPlatform(BusinessPlatform businessPlatform) {
        this.businessPlatform = businessPlatform;
    }

    public Currency getFunctionalCurrency() {
        return functionalCurrency;
    }

    public void setFunctionalCurrency(Currency functionalCurrency) {
        this.functionalCurrency = functionalCurrency;
    }

}
