/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
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

@Entity
@Table(name = "EXCHANGE_RATES")
public class ExchangeRate implements Serializable {

    private static final long serialVersionUID = -383220321690601009L;
    private static final Logger LOG = Logger.getLogger(ExchangeRate.class.getName());
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FLS_SEQ")
    @SequenceGenerator(name = "FLS_SEQ", sequenceName = "FLS_SEQ", allocationSize = 1)
    @Column(name = "EXCHANGE_RATE_ID")
    private Long id;
    @Column(name = "TYPE")
    private String type;
    @Column(name = "FROM_CURRENCY")
    private Currency fromCurrency;
    @Column(name = "TO_CURRENCY")
    private Currency toCurrency;
    @OneToOne
    @JoinColumn(name = "FINANCIAL_PERIOD_ID")
    private FinancialPeriod financialPeriod;
    @Column(name = "CONVERSION_RATE", precision = 38, scale = 14)
    private BigDecimal conversionRate;

    public ExchangeRate() {
    }

    public ExchangeRate(String type, Currency fromCurrency, Currency toCurrency, FinancialPeriod financialPeriod, BigDecimal rate) {
        this.type = type;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.financialPeriod = financialPeriod;
        this.conversionRate = rate;
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

    public BigDecimal getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
    }

    public FinancialPeriod getFinancialPeriod() {
        return financialPeriod;
    }

    public void setFinancialPeriod(FinancialPeriod financialPeriod) {
        this.financialPeriod = financialPeriod;
    }

}
