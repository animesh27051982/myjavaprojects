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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
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
    private String id;   // MAY-18
    @Column(name = "NAME")
    private String name;
    @Column(name = "START_DATE")
    private LocalDate startDate;
    @Column(name = "END_DATE")
    private LocalDate endDate;
    @Column(name = "PERIOD_YEAR")
    private int periodYear;
    @Column(name = "PERIOD_NUMBER")  // TODO - Change to comparable int
    private Integer number;
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

    public FinancialPeriod(String id, String name, LocalDate startDate, LocalDate endDate, int periodYear, int compare, PeriodStatus status) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.periodYear = periodYear;
        this.number = number;
        this.status = status;
    }

    @Override
    public int compareTo(FinancialPeriod obj) {
        return this.endDate.compareTo(obj.getEndDate());
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

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

}
