/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
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
@Table(name = "SL_LINES")
public class SubledgerLine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SL_LINE_SEQ")
    @SequenceGenerator(name = "SL_LINE_SEQ", sequenceName = "SL_LINE_SEQ", allocationSize = 1)
    @Column(name = "SL_HEADER_ID")
    private Long id;
    @Column(name = "LINE_NUMBER")
    private Long lineNumber;
    @Column(name = "ACCT_DESC")
    private String description;
    @OneToOne
    @JoinColumn(name = "SL_ACCOUNT_ID")
    private SubledgerAccount subledgerAccount;
    @OneToOne
    @JoinColumn(name = "COMPANY_ID")
    private Company company;
    @Column(name = "ACCOUNTED_DR")
    private BigDecimal accountedDr;
    @Column(name = "ACCOUNTED_CR")
    private BigDecimal accountedCr;
    @Column(name = "CURRENCY")
    private Currency currency;
    @OneToOne
    @JoinColumn(name = "FINANCIAL_PERIOD_ID")
    private FinancialPeriod financialPeriod;
    @Column(name = "ACCOUNTING_DATE")
    private LocalDate accountingDate;
    @OneToOne
    @JoinColumn(name="SL_BATCH_ID")
    private SubledgerBatch subledgerBatch;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SubledgerAccount getSubledgerAccount() {
        return subledgerAccount;
    }

    public void setSubledgerAccount(SubledgerAccount subledgerAccount) {
        this.subledgerAccount = subledgerAccount;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public BigDecimal getAccountedDr() {
        return accountedDr;
    }

    public void setAccountedDr(BigDecimal accountedDr) {
        this.accountedDr = accountedDr;
    }

    public BigDecimal getAccountedCr() {
        return accountedCr;
    }

    public void setAccountedCr(BigDecimal accountedCr) {
        this.accountedCr = accountedCr;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public FinancialPeriod getFinancialPeriod() {
        return financialPeriod;
    }

    public void setFinancialPeriod(FinancialPeriod financialPeriod) {
        this.financialPeriod = financialPeriod;
    }

    public LocalDate getAccountingDate() {
        return accountingDate;
    }

    public void setAccountingDate(LocalDate accountingDate) {
        this.accountingDate = accountingDate;
    }

    public SubledgerBatch getSubledgerBatch() {
        return subledgerBatch;
    }

    public void setSubledgerBatch(SubledgerBatch subledgerBatch) {
        this.subledgerBatch = subledgerBatch;
    }

    
    
}
