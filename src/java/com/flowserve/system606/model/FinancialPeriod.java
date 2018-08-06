/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import javax.persistence.Temporal;
import static javax.persistence.TemporalType.TIMESTAMP;

@Entity
@Table(name = "FINANCIAL_PERIODS")
public class FinancialPeriod extends BaseEntity<String> implements Comparable<FinancialPeriod>, Serializable {

    private static final long serialVersionUID = 8354236373683763082L;
    private static final Logger LOG = Logger.getLogger(FinancialPeriod.class.getName());
    @Id
    @Column(name = "FINANCIAL_PERIOD_ID")
    private String id;
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FINANCIAL_PERIOD_SEQ")
    @SequenceGenerator(name = "FINANCIAL_PERIOD_SEQ", sequenceName = "FINANCIAL_PERIOD_SEQ", allocationSize = 50)
    @Column(name = "PERIOD_SEQ")
    private Long sequence;
    @Column(name = "NAME")
    private String name;
    @Column(name = "START_DATE")
    private LocalDate startDate;
    @Column(name = "END_DATE")
    private LocalDate endDate;
    @Column(name = "PERIOD_YEAR")
    private int periodYear;
    @Column(name = "PERIOD_MONTH")  // TODO - Change to comparable int
    private Integer periodMonth;
    @Column(name = "STATUS")
    private PeriodStatus status;
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

    public FinancialPeriod() {
    }

    public FinancialPeriod(String id, String name, LocalDate startDate, LocalDate endDate, int periodYear, int periodMonth, PeriodStatus status) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.periodYear = periodYear;
        this.periodMonth = periodMonth;
        this.status = status;
    }

    @Override
    public int compareTo(FinancialPeriod obj) {
        return this.endDate.compareTo(obj.getEndDate());
    }

    public boolean isAfter(FinancialPeriod period) {
        return this.endDate.compareTo(period.getEndDate()) > 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(int periodYear) {
        this.periodYear = periodYear;
    }

    public PeriodStatus getStatus() {
        return status;
    }

    public void setStatus(PeriodStatus status) {
        this.status = status;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(Integer periodMonth) {
        this.periodMonth = periodMonth;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public boolean isOpen() {
        if (this.status == null) {
            return false;
        }

        return this.status.equals(status.OPENED);
    }

    public boolean isClosed() {
        if (this.status == null) {
            return false;
        }

        return this.status.equals(status.CLOSED);
    }

    public boolean isNeverOpened() {
        if (this.status == null) {
            return false;
        }

        return this.status.equals(status.NEVER_OPENED);
    }

}
