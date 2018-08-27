/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class BillingEventLegacy implements Comparable<CurrencyEvent>, Serializable {

    private static final long serialVersionUID = -1990764230607265489L;
    private static final Logger LOG = Logger.getLogger(Contract.class.getName());
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BILLING_EVENT_SEQ")
    @SequenceGenerator(name = "BILLING_EVENT_SEQ", sequenceName = "BILLING_EVENT_SEQ", allocationSize = 50)
    @Column(name = "BILL_ID")
    private Long id;
    @Column(name = "DESCRIPTION")
    private String description;

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

    public BillingEventLegacy() {
    }

    public BillingEventLegacy(LocalDate billingDate, LocalDate deliveryDate, String invoiceNumber, BigDecimal amountLocalCurrency, BigDecimal amountContractCurrency, Contract contract) {
        this.billingDate = billingDate;
        this.deliveryDate = deliveryDate;
        this.invoiceNumber = invoiceNumber;
        this.amountLocalCurrency = amountLocalCurrency;
        this.amountContractCurrency = amountContractCurrency;
        this.contract = contract;
    }

    @Override
    public int compareTo(CurrencyEvent obj) {
        return this.id.compareTo(obj.getId());
    }

    public String getName() {
        String name = "Billing Event ";
        if (getId() != null) {
            name = "Billing Event " + getId();
        }
        return name;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
