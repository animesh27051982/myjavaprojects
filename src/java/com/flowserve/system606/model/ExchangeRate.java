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
import java.util.logging.Logger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "EXCHANGE_RATES")
public class ExchangeRate implements Serializable {

    private static final long serialVersionUID = -383220321690601009L;
    private static final Logger LOG = Logger.getLogger(ExchangeRate.class.getName());
    @Id
    @Column(name = "EXCHANGE_RATE_ID")
    private Long id;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "FROM_CURRENCY")
    private Currency fromCurrency;

    @Column(name = "TO_CURRENCY")
    private Currency toCurrency;

    @Column(name = "EFFECTIVE_DATE")
    private LocalDate effectiveDate;

    @Column(name = "RATE")
    private BigDecimal rate;

    public ExchangeRate() {
    }

    public ExchangeRate(String type, Currency fromCurrency, Currency toCurrency, LocalDate effectiveDate, BigDecimal rate) {
        this.type = type;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.effectiveDate = effectiveDate;
        this.rate = rate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ExchangeRate) {
            return this.id.equals(((ExchangeRate) obj).getId());
        }
        return false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(Currency fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public Currency getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(Currency toCurrency) {
        this.toCurrency = toCurrency;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

}
