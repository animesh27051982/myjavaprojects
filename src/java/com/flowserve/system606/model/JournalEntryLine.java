/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.math.BigDecimal;
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

/**
 *
 * @author kgraves
 */
@Entity
@Table(name = "JOURNAL_ENTRY_LINES")
public class JournalEntryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "JE_LINE_SEQ")
    @SequenceGenerator(name = "JE_LINE_SEQ", sequenceName = "JE_LINE_SEQ", allocationSize = 30)
    @Column(name = "JE_LINE_ID")
    private Long id;

    @OneToOne
    @JoinColumn(name = "ACCOUNT_ID")
    private Account account;
    @Column(name = "DEBIT_AMOUNT")
    private BigDecimal debitAmount;
    @Column(name = "CREDIT_AMOUNT")
    private BigDecimal creditAmount;
    @Column(name = "CURRENCY")
    private Currency currency;
    @Column(name = "REVENUE_METHOD")
    private RevenueMethod revenueMethod;
    @OneToOne
    @JoinColumn(name = "JE_HEADER_ID")
    private JournalEntryHeader journalEntryHeader;

    public JournalEntryLine() {
    }

    public JournalEntryLine(JournalEntryHeader header, Account account, BigDecimal debitAmount, BigDecimal creditAmount, Currency currency) {
        this.journalEntryHeader = header;
        this.account = account;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.currency = currency;
    }

    public JournalEntryLine(JournalEntryHeader header, Account account, Currency currency, RevenueMethod revenueMethod) {
        this.journalEntryHeader = header;
        this.account = account;
        this.currency = currency;
        this.revenueMethod = revenueMethod;
    }

    public JournalEntryLine(JournalEntryHeader header, Account account, Currency currency) {
        this.journalEntryHeader = header;
        this.account = account;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public RevenueMethod getRevenueMethod() {
        return revenueMethod;
    }

    public void setRevenueMethod(RevenueMethod revenueMethod) {
        this.revenueMethod = revenueMethod;
    }

    public JournalEntryHeader getJournalEntryHeader() {
        return journalEntryHeader;
    }

    public void setJournalEntryHeader(JournalEntryHeader journalEntryHeader) {
        this.journalEntryHeader = journalEntryHeader;
    }

}
