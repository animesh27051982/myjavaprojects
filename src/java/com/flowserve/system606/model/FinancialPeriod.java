/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.logging.Logger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "FINANCIAL_PERIODS")
public class FinancialPeriod implements Comparable<FinancialPeriod>, Serializable {

    private static final long serialVersionUID = 8354236373683763082L;
    private static final Logger LOG = Logger.getLogger(FinancialPeriod.class.getName());
    @Id
    @Column(name = "FINANCIAL_PERIOD_ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "OPEN_DATE")
    private LocalDate openDate;

    @Column(name = "CLOSE_DATE")
    private LocalDate closeDate;

    @Column(name = "IS_OPEN")
    private boolean open;

    @Column(name = "YEAR")
    private int year;

    @Column(name = "MONTH")
    private int month;

    public FinancialPeriod() {
    }

    @Override
    public int compareTo(FinancialPeriod obj) {
        return this.id.compareTo(obj.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FinancialPeriod) {
            return this.id.equals(((FinancialPeriod) obj).getId());
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

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(LocalDate closeDate) {
        this.closeDate = closeDate;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

}
