/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.util.ArrayList;
import java.util.List;
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
@Table(name = "CONTRACT_JOURNALS")
public class ContractJournal {

    private static final long serialVersionUID = -383211321690601009L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CONTRACT_JOURNAL_SEQ")
    @SequenceGenerator(name = "CONTRACT_JOURNAL_SEQ", sequenceName = "CONTRACT_JOURNAL_SEQ", allocationSize = 1)
    @Column(name = "CONTRACT_JOURNAL_ID")
    private Long id;
    @Column(name = "JOURNAL_STATUS")
    private JournalStatus journalStatus;
    @Column(name = "IS_BALANCED")
    private boolean balanced;
    @OneToOne
    @JoinColumn(name = "FINANCIAL_PERIOD_ID")
    private FinancialPeriod period;
    @OneToOne
    @JoinColumn(name = "CONTRACT_ID")
    private Contract contract;

    private List<JournalEntryHeader> journalEntryHeaders = new ArrayList<JournalEntryHeader>();

    public ContractJournal() {
    }

    public ContractJournal(Contract contract) {
        this.contract = contract;
    }

    public void addJournalEntryHeader(JournalEntryHeader jeHeader) {
        journalEntryHeaders.add(jeHeader);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public JournalStatus getJournalStatus() {
        return journalStatus;
    }

    public void setJournalStatus(JournalStatus journalStatus) {
        this.journalStatus = journalStatus;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public FinancialPeriod getPeriod() {
        return period;
    }

    public void setPeriod(FinancialPeriod period) {
        this.period = period;
    }
}
