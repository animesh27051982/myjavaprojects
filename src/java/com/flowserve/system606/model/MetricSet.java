package com.flowserve.system606.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import static javax.persistence.TemporalType.TIMESTAMP;

@Entity
@Table(name = "METRIC_SETS")
public class MetricSet extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "METRIC_SET_SEQ")
    @SequenceGenerator(name = "METRIC_SET_SEQ", sequenceName = "METRIC_SET_SEQ", allocationSize = 50)
    @Column(name = "METRIC_SET_ID")
    private Long id;
    @Column(name = "VERSION")
    private int version;
    @Column(name = "FILENAME")
    private String filename;
    @OneToOne
    @JoinColumn(name = "CREATED_BY_ID")
    private User createdBy;
    @Temporal(TIMESTAMP)
    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;
    @Column(name = "MOST_RECENT")
    private boolean mostRecent;
//    @OneToOne
//    @JoinColumn(name = "POB_ID")
//    private PerformanceObligation performanceObligation;
    //@OneToOne
    //@JoinColumn(name = "CONTRACT_ID")
    //private Contract contract;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, mappedBy = "metricSet")
    private Map<MetricType, Metric> typeMetricMap = new HashMap<MetricType, Metric>();

    public MetricSet() {
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isMostRecent() {
        return mostRecent;
    }

    public void setMostRecent(boolean mostRecent) {
        this.mostRecent = mostRecent;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Map<MetricType, Metric> getTypeMetricMap() {
        return typeMetricMap;
    }

//    public PerformanceObligation getPerformanceObligation() {
//        return performanceObligation;
//    }
//
//    public void setPerformanceObligation(PerformanceObligation performanceObligation) {
//        this.performanceObligation = performanceObligation;
//    }
//
//
//    public Contract getContract() {
//        return contract;
//    }
//
//    public void setContract(Contract contract) {
//        this.contract = contract;
//    }
}
