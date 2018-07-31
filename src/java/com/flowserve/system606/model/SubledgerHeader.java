/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

//@Entity
@Table(name = "SL_HEADERS")
public class SubledgerHeader implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SL_HEADER_SEQ")
    @SequenceGenerator(name = "SL_HEADER_SEQ", sequenceName = "SL_HEADER_SEQ", allocationSize = 1)
    @Column(name = "SL_HEADER_ID")
    private Long id;
    @Column(name = "ACCOUNTING_DATE")
    private LocalDate accountingDate;
    @Column(name = "GL_TRANSFER_STATUS")
    private String glTransferStatus;
    @Column(name = "GL_TRANSFER_DATE")
    private LocalDate glTransferDate;
    @Column(name = "CATEGORY")
    private String category;
    @Column(name = "SL_HEADER_DESC")
    private String description;
    @OneToOne
    @JoinColumn(name = "SL_BATCH_ID")
    private SubledgerBatch subledgerBatch;
    @OneToOne
    @JoinColumn(name = "FINANCIAL_PERIOD_ID")
    private FinancialPeriod financialPeriod;
    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;
    @OneToOne
    @JoinColumn(name = "CURRENCY_METRIC_ID")
    private CurrencyMetric currencyMetric;
    @OneToOne
    @JoinColumn(name = "COMPANY_ID")
    private Company company;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "subledgerHeader")
    private List<SubledgerLine> lines = new ArrayList<SubledgerLine>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getAccountingDate() {
        return accountingDate;
    }

    public void setAccountingDate(LocalDate accountingDate) {
        this.accountingDate = accountingDate;
    }

    public String getGlTransferStatus() {
        return glTransferStatus;
    }

    public void setGlTransferStatus(String glTransferStatus) {
        this.glTransferStatus = glTransferStatus;
    }

    public LocalDate getGlTransferDate() {
        return glTransferDate;
    }

    public void setGlTransferDate(LocalDate glTransferDate) {
        this.glTransferDate = glTransferDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SubledgerBatch getSubledgerBatch() {
        return subledgerBatch;
    }

    public void setSubledgerBatch(SubledgerBatch subledgerBatch) {
        this.subledgerBatch = subledgerBatch;
    }

    public FinancialPeriod getFinancialPeriod() {
        return financialPeriod;
    }

    public void setFinancialPeriod(FinancialPeriod financialPeriod) {
        this.financialPeriod = financialPeriod;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public CurrencyMetric getCurrencyMetric() {
        return currencyMetric;
    }

    public void setCurrencyMetric(CurrencyMetric currencyMetric) {
        this.currencyMetric = currencyMetric;
    }

    public List<SubledgerLine> getLines() {
        return lines;
    }

    public void setLines(List<SubledgerLine> lines) {
        this.lines = lines;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

}
