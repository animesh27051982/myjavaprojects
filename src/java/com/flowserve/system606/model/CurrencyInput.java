package com.flowserve.system606.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.logging.Logger;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DECIMAL")
@AttributeOverride(name = "value", column = @Column(name = "DECIMAL_VALUE", precision = 38, scale = 14))
public class CurrencyInput extends Input<BigDecimal> {

    private static final Logger logger = Logger.getLogger(CurrencyInput.class.getName());

    private BigDecimal value;
    @Column(name = "INPUT_CURRENCY_CODE")
    private Currency inputCurrency;
    @Column(name = "LOCAL_CURRENCY_CODE")
    private Currency localCurrency;
    @Column(name = "LOCAL_CURRENCY_VALUE", precision = 38, scale = 14)
    private BigDecimal localCurrencyValue;
    @Column(name = "CONTRACT_CURRENCY_CODE")
    private Currency contractCurrency;
    @Column(name = "CONTRACT_CURRENCY_VALUE", precision = 38, scale = 14)
    private BigDecimal contractCurrencyValue;
    @Column(name = "REPORTING_CURRENCY_CODE")
    private Currency reportingCurrencyCode;
    @Column(name = "REPORTING_CURRENCY_VALUE", precision = 38, scale = 14)
    private BigDecimal reportingCurrencyValue;

    public CurrencyInput() {
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        if (value != null) {
            logger.info("setting value: " + value.toString());
        }
        this.value = value;
    }

    public Currency getInputCurrency() {
        return inputCurrency;
    }

    public void setInputCurrency(Currency inputCurrency) {
        this.inputCurrency = inputCurrency;
    }

    public Currency getLocalCurrency() {
        return localCurrency;
    }

    public void setLocalCurrency(Currency localCurrency) {
        this.localCurrency = localCurrency;
    }

    public BigDecimal getLocalCurrencyValue() {
        return localCurrencyValue;
    }

    public void setLocalCurrencyValue(BigDecimal localCurrencyValue) {
        this.localCurrencyValue = localCurrencyValue;
    }

    public Currency getContractCurrency() {
        return contractCurrency;
    }

    public void setContractCurrency(Currency contractCurrency) {
        this.contractCurrency = contractCurrency;
    }

    public BigDecimal getContractCurrencyValue() {
        return contractCurrencyValue;
    }

    public void setContractCurrencyValue(BigDecimal contractCurrencyValue) {
        this.contractCurrencyValue = contractCurrencyValue;
    }

    public Currency getReportingCurrencyCode() {
        return reportingCurrencyCode;
    }

    public void setReportingCurrencyCode(Currency reportingCurrencyCode) {
        this.reportingCurrencyCode = reportingCurrencyCode;
    }

    public BigDecimal getReportingCurrencyValue() {
        return reportingCurrencyValue;
    }

    public void setReportingCurrencyValue(BigDecimal reportingCurrencyValue) {
        this.reportingCurrencyValue = reportingCurrencyValue;
    }

}
