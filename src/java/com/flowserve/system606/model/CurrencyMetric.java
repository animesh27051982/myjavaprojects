package com.flowserve.system606.model;

import java.math.BigDecimal;
import java.util.logging.Logger;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("CURRENCY")
@AttributeOverride(name = "value", column = @Column(name = "DECIMAL_VALUE", precision = 38, scale = 14))
public class CurrencyMetric extends Metric<BigDecimal> {

    private static final Logger logger = Logger.getLogger(CurrencyMetric.class.getName());

    private BigDecimal value;
    @Column(name = "LOCAL_CURRENCY_VALUE", precision = 38, scale = 14)
    private BigDecimal localCurrencyValue;
    @Column(name = "CONTRACT_CURRENCY_VALUE", precision = 38, scale = 14)
    private BigDecimal contractCurrencyValue;
    @Column(name = "REPORTING_CURRENCY_VALUE", precision = 38, scale = 14)
    private BigDecimal reportingCurrencyValue;

    public CurrencyMetric() {
    }

    public BigDecimal getValue() {
        return value;
    }

    public BigDecimal getlcValue() {
        return localCurrencyValue;
    }

    public BigDecimal getccValue() {
        return contractCurrencyValue;
    }

    public BigDecimal getrcValue() {
        return reportingCurrencyValue;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getLocalCurrencyValue() {
        return localCurrencyValue;
    }

    public void setLocalCurrencyValue(BigDecimal localCurrencyValue) {
        this.localCurrencyValue = localCurrencyValue;
    }

    public BigDecimal getContractCurrencyValue() {
        return contractCurrencyValue;
    }

    public void setContractCurrencyValue(BigDecimal contractCurrencyValue) {
        this.contractCurrencyValue = contractCurrencyValue;
    }

    public BigDecimal getReportingCurrencyValue() {
        return reportingCurrencyValue;
    }

    public void setReportingCurrencyValue(BigDecimal reportingCurrencyValue) {
        this.reportingCurrencyValue = reportingCurrencyValue;
    }

    public boolean isLocalCurrencyMetric() {
        return this.getMetricType().getMetricCurrencyType().equals(CurrencyType.LOCAL);
    }

    public boolean isContractCurrencyMetric() {
        return this.getMetricType().getMetricCurrencyType().equals(CurrencyType.CONTRACT);
    }

}
