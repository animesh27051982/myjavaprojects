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
import java.util.List;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "CONTRACTS")
public class Contract extends BaseEntity<Long> implements Comparable<Contract>, Serializable {

    private static final long serialVersionUID = -1990764230607265489L;
    private static final Logger LOG = Logger.getLogger(Contract.class.getName());
    @Id
    @Column(name = "CONTRACT_ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "CONTRACT_TYPE_ID")
    private ContractType contractType;

    @OneToOne
    @JoinColumn(name = "PREPARER_USER_ID")
    private User preparer;

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

    @Column(name = "FUNCTIONAL_CURRENCY")
    private Currency functionalCurrency;

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

    @OneToMany(mappedBy = "contract", cascade = CascadeType.PERSIST)
    private List<PerformanceObligation> performanceObligations = new ArrayList<PerformanceObligation>();

    public Contract() {
    }

    @Override
    public int compareTo(Contract obj) {
        return this.id.compareTo(obj.getId());
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
        return performanceObligations != null ? performanceObligations : new ArrayList<>();
    }
   
    public void setPerformanceObligations(List<PerformanceObligation> pobs) {
        this.performanceObligations = new ArrayList<>(pobs);
    }
    
    // TODO - KJG - Remove.  Temp code for calc pages.
    public BigDecimal getPobCountRejected() {
        return new BigDecimal("10.0");
    }
}
