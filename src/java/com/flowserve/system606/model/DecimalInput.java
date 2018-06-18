package com.flowserve.system606.model;

import java.math.BigDecimal;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DECIMAL")
@AttributeOverride(name = "value", column = @Column(name = "DECIMAL_VALUE"))
public class DecimalInput extends Input<BigDecimal> {

    private BigDecimal value;

    public DecimalInput() {
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

}
