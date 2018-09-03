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
@Table(name = "REPORTING_UNIT_JOURNALS")
public class ReportingUnitJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RU_JOURNAL_SEQ")
    @SequenceGenerator(name = "RU_JOURNAL_SEQ", sequenceName = "RU_JOURNAL_SEQ", allocationSize = 1)
    @Column(name = "RU_JOURNAL_ID")
    private Long id;
    @Column(name = "JOURNAL_SATUS")
    private JournalStatus journalStatus;
    @Column(name = "IS_BALANCED")
    private boolean balanced;
    @OneToOne
    @JoinColumn(name = "FINANCIAL_PERIOD_ID")
    private FinancialPeriod period;
    @OneToOne
    @JoinColumn(name = "REPORTING_UNIT_ID")
    private ReportingUnit reportingUnit;

    List<ContractJournal> contractJournals = new ArrayList<ContractJournal>();

    public void addContractJournal(ContractJournal contractJournal) {
        contractJournals.add(contractJournal);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public ReportingUnit getReportingUnit() {
        return reportingUnit;
    }

    public void setReportingUnit(ReportingUnit reportingUnit) {
        this.reportingUnit = reportingUnit;
    }

}
