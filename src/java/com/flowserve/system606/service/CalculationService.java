/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.PerformanceObligation;
import javax.ejb.Stateless;

/**
 *
 * @author Steve
 */
@Stateless
public class CalculationService {
    
    public void calculatePercentComplete() {
        
    }
    
    public void calculateRevenueEarnedToDateC2C(PerformanceObligation pob) {
        // during your input processing, do an input.setCurrency(Currency.getInstance("USD"))
        // we may want to just pass the entire set of POB inputs.  
        // you can then pass the same set to the nested calc and it can pull out what it needs.
        //actually, just pass in the POB itself
        // and it's a simple multiplication of two inputs....

        // Updated Total Transaction Price  input will be of type Money.  output will be of type money.
        // percent complete .. float is prob good.
        // Float object type
    }
}
