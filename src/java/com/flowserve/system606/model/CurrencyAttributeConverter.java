package com.flowserve.system606.model;

import java.util.Currency;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class CurrencyAttributeConverter implements AttributeConverter<Currency, String> {

    @Override
    public String convertToDatabaseColumn(Currency currency) {
        System.out.println("currency()" + currency);
        if (currency == null) {
            return null;
        }
        return currency.getCurrencyCode();
    }

    @Override
    public Currency convertToEntityAttribute(String currencyCode) {
        System.out.println("Attribute()" + currencyCode);
        if (currencyCode == null) {
            return null;
        }
        return Currency.getInstance(currencyCode);
    }
}
