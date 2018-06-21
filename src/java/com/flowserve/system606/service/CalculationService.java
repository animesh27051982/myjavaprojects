/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;
import com.flowserve.system606.model.DateInput;
import com.flowserve.system606.model.DecimalInput;
import com.flowserve.system606.model.Input;
import com.flowserve.system606.model.InputTypeId;
import com.flowserve.system606.model.PerformanceObligation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.ejb.Stateless;

/*

@author Steve
*/
@Stateless
public class CalculationService {

    final int SCALE = 14;
    final int ROUNDING_METHOD = BigDecimal.ROUND_HALF_UP;

    public void calculatePercentComplete() {

    }

    public BigDecimal calculateRevenueEarnedToDateC2C(PerformanceObligation pob) {
        // Revenue Earned to Date C2C-Percent Complete x Updated Total Transaction Price
        // Column J
        // Total Trans Price Contract Curr
        // $252,444
        // $437,213
        
        //Column O
        // EOM Inputs 1
        // Allocated Trans Price (net LD) in 
        // Contract Curr
        // 252,444
        // 437,213


        // during your input processing, do an input.setCurrency(Currency.getInstance("USD"))

        // we may want to just pass the entire set of POB inputs.
        // you can then pass the same set to the nested calc and it can pull out what it needs.
        //actually, just pass in the POB itself
        // and it's a simple multiplication of two inputs....
        // percent complete .. float is prob good.
        // Float object type
        BigDecimal transactionPrice = pob.getDecimalValue(InputTypeId.TRANSACTION_PRICE);
                // ((DecimalInput)pob.getInput(InputTypeId.TRANSACTION_PRICE)).getValue();
        BigDecimal cumulativeCostsIncurred = pob.getDecimalValue(InputTypeId.CUMULATIVE_COST_INCURRED);
                // ((DecimalInput)pob.getInput(InputTypeId.CUMULATIVE_COST_INCURRED)).getValue();
        BigDecimal updatedEstimateAtCompletionTotalCosts = pob.getDecimalValue(InputTypeId.ESTIMATED_COST_AT_COMPLETION);
                // ((DecimalInput)pob.getInput(InputTypeId.ESTIMATED_COST_AT_COMPLETION)).getValue();
        
        BigDecimal percentComplete = calculatePercentComplete(cumulativeCostsIncurred, updatedEstimateAtCompletionTotalCosts);        

        return percentComplete.multiply(transactionPrice);
    }

    public BigDecimal calculatePercentComplete(BigDecimal cumulativeCostsIncurred, BigDecimal updatedEstimateAtCompletionTotalCosts) {

        return cumulativeCostsIncurred.divide(updatedEstimateAtCompletionTotalCosts, SCALE, ROUNDING_METHOD);
    }
    
    public BigDecimal revenueEarnedtoDateSL(PerformanceObligation pob) {
        LocalDate revenueStartDate = pob.getDateValue(InputTypeId.REVENUE_START_DATE);
                // ((DateInput)pob.getInput(InputTypeId.REVENUE_START_DATE)).getValue();
        BigDecimal transactionPrice = pob.getDecimalValue(InputTypeId.TRANSACTION_PRICE);
                // ((DecimalInput)pob.getInput(InputTypeId.TRANSACTION_PRICE)).getValue();
        LocalDate currentMonth = LocalDate.now();       
        LocalDate revenueEndDate = pob.getDateValue(InputTypeId.REVENUE_END_DATE);
                // ((DateInput)pob.getInput(InputTypeId.REVENUE_END_DATE)).getValue();

        long elapsedDays = ChronoUnit.DAYS.between(revenueStartDate, currentMonth);
        long revenueDays = ChronoUnit.DAYS.between(revenueStartDate, revenueEndDate);
        
        return transactionPrice.multiply(new BigDecimal(elapsedDays)).divide(new BigDecimal(revenueDays));
    }
    
    public BigDecimal monthlyRevenueRecognized(PerformanceObligation pob) {
        BigDecimal revenueEarnedToDateC2C = calculateRevenueEarnedToDateC2C(pob);
        BigDecimal cumulativeRevenuePriorPeriod = new BigDecimal(0);
        // Cumulative Revenue Prior Period: is that from last month?
        // yes.. we are going to have to look that up ourselves
        return revenueEarnedToDateC2C.subtract(cumulativeRevenuePriorPeriod);
    }
        
    
}


//		Revenue Earned to Date SL-(Current Month - Revenue Start Date)*(Updated Total Transaction Price/(Revenue End Date - Revenue Start Date))

// column AH - Straight Line POb Revenue Start Date
// column AI - Straight Line POb Revenue End Date

//		Monthly Revenue Recognized-Revenue Earned to Date-Cumulative Revenue Prior Period
// 
//		Monthly Liquidated Damages Recognized-Cumulative Liquidated Damages-Cumulative Liquidated Damages Prior Period
//		Monthly Net Revenue Recognized-Monthly Revenue Recognized-Monthly Liquidated Damages Recognized

//REVENUE_RECOGNITION_METHOD|POB|com.flowserve.system606.model.StringInput|Revenue Recognition Method|Revenue Recognition Method|POC POB Inputs|H|Group1|1
//TRANSACTION_PRICE|POB|com.flowserve.system606.model.DecimalInput|Transaction Price|Transaction Price|POC POB Inputs|O|Group1|2
//ESTIMATED_COST_AT_COMPLETION|POB|com.flowserve.system606.model.DecimalInput|Estimated Cost at Completion|Estimated Cost at Completion|POC POB Inputs|Q|Group1|3
//CUMULATIVE_COST_INCURRED|POB|com.flowserve.system606.model.DecimalInput|Cumulative Cost Incurred / Received Local Curr|Cumulative Cost Incurred / Received Local Curr|POC POB Inputs|S|Group1|4
//INTERCOMPANY_PROGRESS|POB|com.flowserve.system606.model.DecimalInput|Interco Progress (Not Received) Local Curr|Interco Progress (Not Received) Local Curr|POC POB Inputs|T|Group1|5
//THIRD_PARTY_PROGRESS|POB|com.flowserve.system606.model.DecimalInput|3rd Party Progress (Not Received) Local Curr|3rd Party Progress (Not Received) Local Curr|POC POB Inputs|U|Group1|6
//DELIVERY_DATE|POB|com.flowserve.system606.model.DateInput|Actual Shipment / Delivery Date (if complete)|Actual Shipment / Delivery Date (if complete)|POC POB Inputs|W|Group1|7
//CUMULATIVE_COST_OF_GOODS_SOLD|POB|com.flowserve.system606.model.DecimalInput|If partial POB shipped: Enter Cumulative COGS|If partial POB shipped: Enter Cumulative COGS|POC POB Inputs|X|Group1|8
//BILLING_AMOUNT_CONTRACT_CURRENCY|POB|com.flowserve.system606.model.DecimalInput|Billing Amount Contract Currency|Billing Amount Contract Currency|POC POB Inputs|Z|Group1|9
//BILLING_AMOUNT_LOCAL_CURRENCY|POB|com.flowserve.system606.model.DecimalInput|Billing Amount Local Curr|Billing Amount Local Curr|POC POB Inputs|AA|Group1|10
//BILLING_DATE|POB|com.flowserve.system606.model.DateInput|Billing Date|Billing Date|POC POB Inputs|AB|Group1|11
//BILLING_INVOICE_NUMBER|POB|com.flowserve.system606.model.StringInput|Billing Invoice Number|Billing Invoice Number|POC POB Inputs|AC|Group1|12