/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

/**
 *
 * @author shubhamv
 */
public class StringMetricPriorPeriod {

    private StringMetric metric;
    private MetricType metricType;

    public StringMetricPriorPeriod(StringMetric priorPeriodMetric) {
        this.metric = priorPeriodMetric;
        this.metricType = priorPeriodMetric.getMetricType();
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public String getValue() {
        return metric.getValue();
    }

}
