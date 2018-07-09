/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

/**
 * Non-entity wrapper class for the rules engine. Provides an immutable interface for the underlying Metric for the purpose of accessing prior period values.
 * This class provides two benefits.
 *
 * 1) Provides an additional Drools fact type for easy rule authoring. 2) Prevents accidental modification to prior period metrics by the rule author.
 *
 * @author kgraves
 */
public class MetricPriorPeriod {

    private Metric metric;
    private MetricType metricType;

    public MetricPriorPeriod(Metric priorPeriodMetric) {
        this.metric = priorPeriodMetric;
        this.metricType = priorPeriodMetric.getMetricType();
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public Object getValue() {
        return metric.getValue();
    }
}
