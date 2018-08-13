/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import static javax.persistence.TemporalType.TIMESTAMP;

@Entity
@Table(name = "CONTRACTS")
public class Contract extends BaseEntity<Long> implements MetricStore, Measurable, Comparable<Contract>, Serializable {

    private static final long serialVersionUID = -1990764230607265489L;
    private static final Logger LOG = Logger.getLogger(Contract.class.getName());

    @Id
    @Column(name = "CONTRACT_ID")
    private Long id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "CONTRACT_TYPE_ID")
    private ContractType contractType;
    @ManyToOne
    @JoinColumn(name = "REPORTING_UNIT_ID")
    private ReportingUnit reportingUnit;
    @OneToOne
    @JoinColumn(name = "CUSTOMER_ID")
    private Customer customer;
    @OneToOne
    @JoinColumn(name = "BUSINESS_PLATFORM_ID")
    private BusinessPlatform businessPlatform;
    @Column(name = "CUSOTMER_PURCHASE_ORDER_NUM")
    private String customerPurchaseOrderNumber;
    @Column(name = "SALES_ORDER_NUM")
    private String salesOrderNumber;
    @Column(name = "TOTAL_TRANSACTION_PRICE")
    private BigDecimal totalTransactionPrice;
    @Column(name = "CONTRACT_CURRENCY")
    private Currency contractCurrency;
    @OneToOne
    @JoinColumn(name = "CREATED_BY_ID")
    private User createdBy;
    @Temporal(TIMESTAMP)
    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;
    @OneToOne
    @JoinColumn(name = "LAST_UPDATED_BY_ID")
    private User lastUpdatedBy;
    @Temporal(TIMESTAMP)
    @Column(name = "LAST_UPDATE_DATE")
    private LocalDateTime lastUpdateDate;
    @Column(name = "IS_ACTIVE")
    private boolean active;
    @OneToOne
    @JoinColumn(name = "SALES_DESTINATION_COUNTRY_ID")
    private Country salesDestinationCountry;
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, mappedBy = "contract")
    @OrderBy("id ASC")
    private List<PerformanceObligation> performanceObligations = new ArrayList<PerformanceObligation>();
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinTable(name = "CONTRACT_METRIC_SET", joinColumns = @JoinColumn(name = "CONTRACT_ID"), inverseJoinColumns = @JoinColumn(name = "METRIC_SET_ID"))
    private Map<FinancialPeriod, MetricSet> periodMetricSetMap = new HashMap<FinancialPeriod, MetricSet>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "contract", orphanRemoval = true)
    private List<BillingEvent> billingEvents = new ArrayList<BillingEvent>();
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinTable(name = "CONTRACT_APPROVAL_REQUEST", joinColumns = @JoinColumn(name = "CONTRACT_ID"), inverseJoinColumns = @JoinColumn(name = "APPROVAL_REQUEST_ID"))
    private Map<FinancialPeriod, ApprovalRequest> periodApprovalRequestMap = new HashMap<FinancialPeriod, ApprovalRequest>();

    public Contract() {
    }

    @Override
    public int compareTo(Contract obj) {
        return this.id.compareTo(obj.getId());
    }

    public BigDecimal getTotalBillingsLocalCurrency() {
        BigDecimal localCur = BigDecimal.ZERO;
        for (BillingEvent be : billingEvents) {
            if (be.getAmountLocalCurrency() != null && be.getAmountLocalCurrency().compareTo(BigDecimal.ZERO) != 0) {
                localCur = localCur.add(be.getAmountLocalCurrency());
            }
        }
        return localCur;
    }

    public BigDecimal getTotalBillingsContractCurrency() {
        BigDecimal contractCur = BigDecimal.ZERO;
        for (BillingEvent be : billingEvents) {
            if (be.getAmountContractCurrency() != null && be.getAmountContractCurrency().compareTo(BigDecimal.ZERO) != 0) {
                contractCur = contractCur.add(be.getAmountContractCurrency());
            }
        }
        return contractCur;
    }

    public List<PerformanceObligation> getPobsByRevenueMethod(RevenueMethod revenueMethod) {
        List<PerformanceObligation> pobs = new ArrayList<PerformanceObligation>();

        for (PerformanceObligation pob : performanceObligations) {
            if (revenueMethod.equals(pob.getRevenueMethod())) {
                pobs.add(pob);
            }
        }

        return pobs;
    }

    public ApprovalRequest getPeriodApprovalRequest(FinancialPeriod period) {
        return periodApprovalRequestMap.get(period);
    }

    public void putPeriodApprovalRequest(FinancialPeriod period, ApprovalRequest approvalRequest) {
        periodApprovalRequestMap.put(period, approvalRequest);
    }

    public boolean metricSetExistsForPeriod(FinancialPeriod period) {
        return periodMetricSetMap.get(period) != null;
    }

    public boolean metricExistsForPeriod(FinancialPeriod period, MetricType metricType) {
        return periodMetricSetMap.get(period).getTypeMetricMap().get(metricType) != null;
    }

    public MetricSet initializeMetricSetForPeriod(FinancialPeriod period) {
        MetricSet metricSet = new MetricSet();
        periodMetricSetMap.put(period, metricSet);
        return metricSet;
    }

    public Metric getPeriodMetric(FinancialPeriod period, MetricType metricType) {
        return periodMetricSetMap.get(period).getTypeMetricMap().get(metricType);
    }

    public Metric initializeMetricForPeriod(FinancialPeriod period, MetricType metricType) {
        Metric metric = null;
        try {
            if (metricType.isContractLevel()) {
                Class<?> clazz = Class.forName(MetricType.PACKAGE_PREFIX + metricType.getMetricClass());
                metric = (Metric) clazz.newInstance();
                metric.setMetricType(metricType);
                metric.setMetricSet(periodMetricSetMap.get(period));
                periodMetricSetMap.get(period).getTypeMetricMap().put(metricType, metric);
            }
        } catch (Exception e) {
            Logger.getLogger(Contract.class.getName()).log(Level.SEVERE, "Severe exception initializing metricTypeId: " + metricType.getId(), e);
            throw new IllegalStateException("Severe exception initializing metricTypeId: " + metricType.getId(), e);
        }

        if (metric == null) {
            Logger.getLogger(Contract.class.getName()).log(Level.FINER, "Null metric at contract level!:  " + metricType.getCode());
        }
        return metric;
    }

    public Currency getLocalCurrency() {
        return this.getReportingUnit().getLocalCurrency();
    }

    public Currency getReportingCurrency() {
        return this.getReportingUnit().getCompany().getReportingCurrency();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReportingUnit getReportingUnit() {
        return reportingUnit;
    }

    public void setReportingUnit(ReportingUnit reportingUnit) {
        this.reportingUnit = reportingUnit;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BusinessPlatform getBusinessPlatform() {
        return businessPlatform;
    }

    public void setBusinessPlatform(BusinessPlatform businessPlatform) {
        this.businessPlatform = businessPlatform;
    }

    public String getCustomerPurchaseOrderNumber() {
        return customerPurchaseOrderNumber;
    }

    public void setCustomerPurchaseOrderNumber(String customerPurchaseOrderNumber) {
        this.customerPurchaseOrderNumber = customerPurchaseOrderNumber;
    }

    public String getSalesOrderNumber() {
        return salesOrderNumber;
    }

    public void setSalesOrderNumber(String salesOrderNumber) {
        this.salesOrderNumber = salesOrderNumber;
    }

    public BigDecimal getTotalTransactionPrice() {
        return totalTransactionPrice;
    }

    public void setTotalTransactionPrice(BigDecimal totalTransactionPrice) {
        this.totalTransactionPrice = totalTransactionPrice;
    }

    public List<PerformanceObligation> getPerformanceObligations() {
        return performanceObligations;
    }

    public List<Measurable> getChildMeasurables() {
        return new ArrayList<Measurable>(performanceObligations);
    }

    public Currency getContractCurrency() {
        return contractCurrency;
    }

    public void setContractCurrency(Currency contractCurrency) {
        this.contractCurrency = contractCurrency;
    }

    public List<BillingEvent> getBillingEvents() {
        return billingEvents;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public void setContractType(ContractType contractType) {
        this.contractType = contractType;
    }

    public Country getSalesDestinationCountry() {
        return salesDestinationCountry;
    }

    public void setSalesDestinationCountry(Country salesDestinationCountry) {
        this.salesDestinationCountry = salesDestinationCountry;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public User getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(User lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public LocalDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(LocalDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
