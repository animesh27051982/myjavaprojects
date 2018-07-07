/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.util.Map;

/**
 *
 * @author kgraves
 */
public interface ValueStore {

    public Map<FinancialPeriod, InputSet> getPeriodInputSetMap();

    public Long getId();

}
