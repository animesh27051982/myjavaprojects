/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

/**
 *
 * @author kgraves
 */
public interface Calculable {

    public Metric getPeriodMetric(FinancialPeriod period, MetricType metricType);

    public void initializeMetricSetForPeriod(FinancialPeriod period);

    public void initializeMetricForPeriod(FinancialPeriod period, MetricType metricType);

    public boolean metricSetExistsForPeriod(FinancialPeriod period);

    public boolean metricExistsForPeriod(FinancialPeriod period, MetricType metricType);

    public Long getId();

}
