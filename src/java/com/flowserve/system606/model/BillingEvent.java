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
import java.util.List;
import java.util.logging.Logger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "BILLING_EVENTS")
public class BillingEvent implements Accumulable, Comparable<BillingEvent>, Serializable {

    private static final long serialVersionUID = -1990764230607265489L;
    private static final Logger LOG = Logger.getLogger(Contract.class.getName());
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FLS_SEQ")
    @SequenceGenerator(name = "FLS_SEQ", sequenceName = "FLS_SEQ", allocationSize = 1)
    @Column(name = "BILL_ID")
    private Long id;

    @Column(name = "BILLING_DATE")
    private LocalDate billingDate;

    @Column(name = "DELIVERY_DATE")
    private LocalDate deliveryDate;

    @Column(name = "INVOICE_NUMBER")
    private String invoiceNumber;

    @Column(name = "AMOUNT_LOCAL_CURRENCY")
    private BigDecimal amountLocalCurrency;

    @Column(name = "AMOUNT_CONTRACT_CURRENCY")
    private BigDecimal amountContractCurrency;

    @ManyToOne
    @JoinColumn(name = "CONTRACT_ID")
    private Contract contract;

    public BillingEvent() {
    }

    public BillingEvent(LocalDate billingDate, LocalDate deliveryDate, String invoiceNumber, BigDecimal amountLocalCurrency, BigDecimal amountContractCurrency, Contract contract) {
        this.billingDate = billingDate;
        this.deliveryDate = deliveryDate;
        this.invoiceNumber = invoiceNumber;
        this.amountLocalCurrency = amountLocalCurrency;
        this.amountContractCurrency = amountContractCurrency;
        this.contract = contract;
    }

    @Override
    public int compareTo(BillingEvent obj) {
        return this.id.compareTo(obj.getId());
    }

    public String getName() {
        return "Billing Event " + getId();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(LocalDate billingDate) {
        this.billingDate = billingDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public BigDecimal getAmountLocalCurrency() {
        return amountLocalCurrency;
    }

    public void setAmountLocalCurrency(BigDecimal amountLocalCurrency) {
        this.amountLocalCurrency = amountLocalCurrency;
    }

    public BigDecimal getAmountContractCurrency() {
        return amountContractCurrency;
    }

    public void setAmountContractCurrency(BigDecimal amountContractCurrency) {
        this.amountContractCurrency = amountContractCurrency;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    @Override
    public List<Accumulable> getChildAccumulables() {
        return new ArrayList<Accumulable>();
    }

    public BigDecimal getPobCountRejected() {
        return new BigDecimal("10.0");
    }

}
