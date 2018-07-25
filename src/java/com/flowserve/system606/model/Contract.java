/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import javax.persistence.Table;

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

    @OneToOne
    @JoinColumn(name = "FINANCIAL_SOURCE_SYSTEM_ID")
    private FinancialSystem financialSourceSystem;

    @Column(name = "STATED_CONTRACT_PRICE_USD")
    private BigDecimal statedContractPriceUSD;

    @Column(name = "FUNCTIONAL_CURRENCY_TO_USD")
    private BigDecimal functionalCurrencyToUSD;

    @Column(name = "CONTRACT_CURRENCY_TO_USD")
    private BigDecimal contractCurrencyToUSD;

    @Column(name = "CONTRACT_TO_FUNC_CURRENCY")
    private BigDecimal ocntractToFunctionalCurrency;

    @Column(name = "TOTAL_TRANSACTION_PRICE")
    private BigDecimal totalTransactionPrice;

    @Column(name = "EST_CONTRACT_LEAD_TIME_MONTHS")
    private int estimatedContractLeadTimeMonths;

    @Column(name = "FORMAL_ACCEPTANCE_DATE")
    private LocalDate formalAcceptanceDate;

    @Column(name = "CONTRACT_CURRENCY")
    private Currency contractCurrency;

    @Column(name = "IS_CHANGE_ORDER")
    private boolean isChangeOrder;

    @Column(name = "STANDALONE_SELLING_PRICE")
    private BigDecimal standaloneSellingPrice;

    @Column(name = "TOTAL_EST_COSTS_COMPLETION")
    private BigDecimal estimatedCostsAtCompletion;

    @Column(name = "TOTAL_LIQUIDATED_DMG_AMOUNT")
    private BigDecimal totalLiquidatedDamagesAmount;

    @Column(name = "TOTAL_THIRD_PARTY_COMISSION")
    private BigDecimal totalThirdPartyComission;

    @OneToOne
    @JoinColumn(name = "SALES_DESTINATION_COUNTRY_ID")
    //private Country salesDestinationCountry;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "contract", cascade = CascadeType.ALL)
    private List<PerformanceObligation> performanceObligations = new ArrayList<PerformanceObligation>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "CONTRACT_METRIC_SET", joinColumns = @JoinColumn(name = "CONTRACT_ID"), inverseJoinColumns = @JoinColumn(name = "METRIC_SET_ID"))
    private Map<FinancialPeriod, MetricSet> periodMetricSetMap = new HashMap<FinancialPeriod, MetricSet>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillingEvent> billingEvents = new ArrayList<BillingEvent>();

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

    public boolean metricSetExistsForPeriod(FinancialPeriod period) {
        return periodMetricSetMap.get(period) != null;
    }

    public boolean metricExistsForPeriod(FinancialPeriod period, MetricType metricType) {
        return periodMetricSetMap.get(period).getTypeMetricMap().get(metricType) != null;
    }

    public void initializeMetricSetForPeriod(FinancialPeriod period) {
        periodMetricSetMap.put(period, new MetricSet());
    }

    public Metric getPeriodMetric(FinancialPeriod period, MetricType metricType) {
        return periodMetricSetMap.get(period).getTypeMetricMap().get(metricType);
    }

    public void initializeMetricForPeriod(FinancialPeriod period, MetricType metricType) {
        try {
            if (metricType.isContractLevel()) {
                Class<?> clazz = Class.forName(MetricType.PACKAGE_PREFIX + metricType.getMetricClass());
                Metric metric = (Metric) clazz.newInstance();
                metric.setMetricType(metricType);
                metric.setMetricSet(periodMetricSetMap.get(period));
                periodMetricSetMap.get(period).getTypeMetricMap().put(metricType, metric);
            }
        } catch (Exception e) {
            Logger.getLogger(Contract.class.getName()).log(Level.SEVERE, "Severe exception initializing metricTypeId: " + metricType.getId(), e);
            throw new IllegalStateException("Severe exception initializing metricTypeId: " + metricType.getId(), e);
        }
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
}
