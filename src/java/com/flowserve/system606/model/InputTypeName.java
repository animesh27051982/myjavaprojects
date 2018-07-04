package com.flowserve.system606.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kgraves
 */
public enum InputTypeName {

    REVENUE_RECOGNITION_METHOD,
    ESTIMATED_COST_AT_COMPLETION,
    TRANSACTION_PRICE,
    LIQUIDATED_DAMAGES,
    CUMULATIVE_COST_INCURRED,
    CUMULATIVE_INTERCOMPANY_PROGRESS,
    CUMULATIVE_THIRD_PARTY_PROGRESS,
    DELIVERY_DATE,
    CUMULATIVE_COST_OF_GOODS_SOLD,
    BILLING_AMOUNT_CC,
    BILLING_AMOUNT_LC,
    BILLING_DATE,
    BILLING_INVOICE_NUMBER,
    REVENUE_START_DATE,
    REVENUE_END_DATE,
    TOTAL_TRANS_PRICE_CC,
    BOOKING_DATE,
    ESTIMATED_COMPLETION_DATE;

    static Map<InputTypeName, InputType> inputTypeMap = new HashMap<InputTypeName, InputType>();

    public static InputType getInputType(InputTypeName inputTypeName) {
        return inputTypeMap.get(inputTypeName);
    }

    public static void putInputType(InputTypeName inputTypeName, InputType inputType) {
        if (inputTypeMap.get(inputTypeName) == null) {
            inputTypeMap.put(inputTypeName, inputType);
        }
    }
}
