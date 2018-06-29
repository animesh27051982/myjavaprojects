package com.flowserve.system606.model;

/**
 * @author kgraves
 */
public final class OutputTypeId {

    public static final String PERCENT_COMPLETE = "PERCENT_COMPLETE";
    public static final String REVENUE_EARNED_TO_DATE = "REVENUE_EARNED_TO_DATE";
    public static final String ESTIMATED_GROSS_PROFIT = "ESTIMATED_GROSS_PROFIT";
    public static final String ESTIMATED_GROSS_MARGIN = "ESTIMATED_GROSS_MARGIN";
    public static final String CUMULATIVE_TOTAL_ITD_COSTS = "CUMULATIVE_TOTAL_ITD_COSTS";

    public static final String CHANGE_IN_ESTIMATED_COST_AT_COMPLETION = "CHANGE_IN_ESTIMATED_COST_AT_COMPLETION";
    //public static final String TRANSACTION_PRICE1 = "TRANSACTION_PRICE1";  // this is an input
    public static final String CUMULATIVE_REVENUE_TO_RECOGNIZE = "CUMULATIVE_REVENUE_TO_RECOGNIZE";
    //public static final String PRIOR_PERIOD_CUMULATIVE_REVENUE_TO_RECOGNIZE = "PRIOR_PERIOD_CUMULATIVE_REVENUE_TO_RECOGNIZE";  // we'll get this using pob.getCurrencyValuePriorPeriod()
    public static final String PERIOD_REVENUE = "PERIOD_REVENUE";
    public static final String PERIOD_EX_RATE = "PERIOD_EX_RATE";
    public static final String PERIOD_REVENUE_LOCAL_CURRENCY = "PERIOD_REVENUE_LOCAL_CURRENCY";
    public static final String CUMULATIVE_REVENUE_LOCAL_CURRENCY = "CUMULATIVE_REVENUE_LOCAL_CURRENCY";

    public static final String CUMULATIVE_LIQUIDATED_DAMAGES_CC = "CUMULATIVE_LIQUIDATED_DAMAGES_CC";
    public static final String LIQUIDATED_DAMAGES_RECOGNIZE_CURRENT_PERIOD_CC = "LIQUIDATED_DAMAGES_RECOGNIZE_CURRENT_PERIOD_CC";
    public static final String LIQUIDATED_DAMAGES_RECOGNIZE_CURRENT_PERIOD_LC = "LIQUIDATED_DAMAGES_RECOGNIZE_CURRENT_PERIOD_LC";

//    public static final String ESTIMATED_COST_AT_COMPLETION = "ESTIMATED_COST_AT_COMPLETION";
//    public static final String CUMULATIVE_COST_INCURRED = "CUMULATIVE_COST_INCURRED";
//    public static final String INTERCOMPANY_PROGRESS = "INTERCOMPANY_PROGRESS";
//    public static final String THIRD_PARTY_PROGRESS = "THIRD_PARTY_PROGRESS";
//    public static final String DELIVERY_DATE = "DELIVERY_DATE";
//    public static final String CUMULATIVE_COST_OF_GOODS_SOLD = "CUMULATIVE_COST_OF_GOODS_SOLD";
//    public static final String BILLING_AMOUNT_CONTRACT_CURRENCY = "BILLING_AMOUNT_CONTRACT_CURRENCY";
//    public static final String BILLING_AMOUNT_LOCAL_CURRENCY = "BILLING_AMOUNT_LOCAL_CURRENCY";
//    public static final String BILLING_DATE = "BILLING_DATE";
//    public static final String BILLING_INVOICE_NUMBER = "BILLING_INVOICE_NUMBER";
}
