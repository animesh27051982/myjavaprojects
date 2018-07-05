/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.util.logging.Logger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "COMPANIES")
public class Company extends BaseEntity<Long> implements Comparable<Company>, Serializable {

    private static final long serialVersionUID = -5428359272400395184L;
    private static final Logger LOG = Logger.getLogger(Company.class.getName());

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FLS_SEQ")
    @SequenceGenerator(name = "FLS_SEQ", sequenceName = "FLS_SEQ", allocationSize = 1)
    @Column(name = "COMPANY_ID")
    private Long id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "TYPE")
    private String type;
    @Column(name = "CODE")
    private String code;
    @Column(name = "DESCRIPTION")
    private String description;
    @OneToOne
    @JoinColumn(name = "PERIOD_ID")
    private FinancialPeriod currentPeriod;

    public Company() {
    }

    @Override
    public int compareTo(Company obj) {
        return this.name.compareTo(obj.getName());
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
}
