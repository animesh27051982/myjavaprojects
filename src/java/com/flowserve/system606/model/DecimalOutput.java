package com.flowserve.system606.model;

import java.math.BigDecimal;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DECIMAL")
@AttributeOverride(name = "value", column = @Column(name = "DECIMAL_VALUE", precision = 38, scale = 14))
public class DecimalOutput extends Input<BigDecimal> {

    private BigDecimal value;
//    private Currency localCurrency;
//    private BigDecimal localCurrencyValue;
//
//    private Currency contractCurrency;
//    private Currency reportingCurrency;

    public DecimalOutput() {
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

}
