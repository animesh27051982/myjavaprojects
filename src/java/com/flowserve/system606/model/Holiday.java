/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.time.LocalDate;
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
@Table(name = "HOLIDAYS")
public class Holiday implements Serializable, Comparable<Company> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "HOLIDAY_SEQ")
    @SequenceGenerator(name = "HOLIDAY_SEQ", sequenceName = "HOLIDAY_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;
    @Column(name = "NAME")
    private String name;

    @Column(name = "HOLIDAY_DATE")
    private LocalDate holidayDate;
    @OneToOne
    @JoinColumn(name = "COMPANY_ID")
    private Company company;

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

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    @Override
    public int compareTo(Company o) {
        return this.name.compareTo(o.getName());
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

}
