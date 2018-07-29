/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.Transient;

/**
 *
 * @author kgraves
 */
public abstract class TransientMeasurable<T extends Serializable> extends BaseEntity<T> {

    @Transient
    private Map<FinancialPeriod, MetricSet> periodMetricSetMap = new HashMap<FinancialPeriod, MetricSet>();

    public Metric getPeriodMetric(FinancialPeriod period, MetricType metricType) {
        return periodMetricSetMap.get(period).getTypeMetricMap().get(metricType);
    }

    public MetricSet initializeMetricSetForPeriod(FinancialPeriod period) {
        MetricSet metricSet = new MetricSet();
        periodMetricSetMap.put(period, metricSet);
        return metricSet;
    }

    public Metric initializeMetricForPeriod(FinancialPeriod period, MetricType metricType) {
        Metric metric = null;
        try {
            Class<?> clazz = Class.forName(MetricType.PACKAGE_PREFIX + metricType.getMetricClass());
            metric = (Metric) clazz.newInstance();
            metric.setMetricType(metricType);
            metric.setMetricSet(periodMetricSetMap.get(period));
            periodMetricSetMap.get(period).getTypeMetricMap().put(metricType, metric);
        } catch (Exception e) {
            Logger.getLogger(TransientMeasurable.class.getName()).log(Level.SEVERE, "Severe exception initializing metricTypeId: " + metricType.getId(), e);
            throw new IllegalStateException("Severe exception initializing metricTypeId: " + metricType.getId(), e);
        }

        return metric;
    }

    public boolean metricSetExistsForPeriod(FinancialPeriod period) {
        return periodMetricSetMap.get(period) != null;
    }

    public boolean metricExistsForPeriod(FinancialPeriod period, MetricType metricType) {
        return periodMetricSetMap.get(period).getTypeMetricMap().get(metricType) != null;
    }

}
