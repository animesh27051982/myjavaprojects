/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import static javax.persistence.TemporalType.TIMESTAMP;

@Entity
@Table(name = "PERFORMANCE_OBLIGATIONS")
public class PerformanceObligation extends BaseEntity<Long> implements Comparable<PerformanceObligation>, Serializable {

    private static final long serialVersionUID = 4995349370717535419L;
    private static final Logger LOG = Logger.getLogger(PerformanceObligation.class.getName());

    @Id
    @Column(name = "POB_ID")
    private Long id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "REV_REC_METHOD")
    private String revRecMethod;
    @ManyToOne
    @JoinColumn(name = "CONTRACT_ID")
    private Contract contract;
    @Column(name = "IS_ACTIVE")
    private boolean active;
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

    //private String deactivationReason;  // create type class
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "performanceObligation")
    @MapKeyEnumerated(EnumType.STRING)
    private Map<InputTypeName, Input> inputs = new HashMap<InputTypeName, Input>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "performanceObligation")
    @MapKeyEnumerated(EnumType.STRING)
    private Map<OutputTypeName, Output> outputs = new HashMap<OutputTypeName, Output>();

    public PerformanceObligation() {
    }

    public boolean isInitialized() {
        return inputs.keySet().size() != 0;
    }

    /**
     * This method will insert a blank Input if it does not exist. We need this behavior in order to support the creation of new Inputs at a later date. Without
     * this, callers would get NPE upon retrieving anything non-existent. This needs improvement however since we are dependent on a static enum which has to be
     * modified for each new input type. A later release can be taken further in this regard, but we don't have the luxury of time for phase 1.
     *
     * @param inputName
     * @return
     */
    private Input getInput(InputTypeName inputName) {
        if (inputs.get(inputName) == null) {
            initializeInput(InputTypeName.getInputType(inputName));
        }
        return inputs.get(inputName);
    }

    public String getStringInputValue(InputTypeName inputName) {
        return ((StringInput) getInput(inputName)).getValue();
    }

    public StringInput getStringInput(InputTypeName inputName) {
        return ((StringInput) getInput(inputName));
    }

    public DateInput getDateInput(InputTypeName inputName) {
        return ((DateInput) getInput(inputName));
    }

    public CurrencyInput getCurrencyInput(InputTypeName inputName) {  // TODO KJG - This method should be getCurrencyInput() and the method above should be getCurrencyInputValue() waiting on this due to impact.
        return (CurrencyInput) getInput(inputName);
    }

    public BigDecimal getDecimalInputValue(InputTypeName inputName) {
        return ((DecimalInput) getInput(inputName)).getValue();
    }

    public LocalDate getDateInputValue(InputTypeName inputName) {
        return ((DateInput) getInput(inputName)).getValue();
    }

    public BigDecimal getCurrencyInputValue(InputTypeName inputName) {
        return ((CurrencyInput) getInput(inputName)).getValue();
    }

    public BigDecimal getCurrencyInputValuePriorPeriod(InputTypeName inputName) {
        return getCurrencyInputValue(inputName);   // KJG TODO - Hack for now to just return this period's value for testing calcs.
    }

    // Init missing output if needed.
    private Output getOutput(OutputTypeName outputTypeName) {
        if (outputs.get(outputTypeName) == null) {
            initializeOutput(OutputTypeName.getOutputType(outputTypeName));
        }
        return outputs.get(outputTypeName);
    }

    public void putOutputMessage(OutputTypeName outputTypeName, String message) {
        getOutput(outputTypeName).setMessage(message);
    }

    public void putCurrencyOutputValue(OutputTypeName outputTypeName, BigDecimal value) {
        getOutput(outputTypeName).setValue(value);
    }

    public BigDecimal getCurrencyOutputValuePriorPeriod(OutputTypeName outputTypeName) {
        return new BigDecimal("10.0");
        //return getCurrencyOutputValue(outputTypeName);  // KJG TODO - Hack for now to just return this period's value for testing calcs.
    }

    public BigDecimal getCurrencyOutputValue(OutputTypeName outputTypeName) {
        return ((CurrencyOutput) getOutput(outputTypeName)).getValue();
    }

    private void initializeOutput(OutputType outputType) {
        try {

            if (outputs.get(outputType.getName()) == null) {
                Class<?> clazz = Class.forName(outputType.getOutputClass());
                Output output = (Output) clazz.newInstance();
                output.setOutputType(outputType);
                output.setPerformanceObligation(this);
                outputs.put(outputType.getName(), output);
            }
        } catch (Exception exception) {
            Logger.getLogger(PerformanceObligation.class.getName()).log(Level.SEVERE, "Error initializeOutputs", exception);
        }
    }

    private void initializeInput(InputType inputType) {
        try {
            if (inputs.get(inputType.getName()) == null) {
                Class<?> clazz = Class.forName(inputType.getInputClass());
                Input input = (Input) clazz.newInstance();
                input.setInputType(inputType);
                input.setPerformanceObligation(this);
                inputs.put(inputType.getName(), input);

                Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "Created Input: " + inputType.getName() + " on POD: " + this.getId());
            }
        } catch (Exception exception) {
            Logger.getLogger(PerformanceObligation.class.getName()).log(Level.SEVERE, "Error initializeIntput", exception);
        }
    }

    @Override
    public int compareTo(PerformanceObligation obj) {
        return this.id.compareTo(obj.getId());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public String getRevRecMethod() {
        return revRecMethod;
    }

    public void setRevRecMethod(String revRecMethod) {
        this.revRecMethod = revRecMethod;
    }

    // TODO - Temp code remove.  This is to support temp code from the JSF UI until we finish the calculations.
    public BigDecimal getPobCountRejected() {
        return new BigDecimal("10.0");
    }

    public BigDecimal getContractToLocalFxRate() {
        return new BigDecimal("1.0");
    }

    public BigDecimal getCurrencyValuePriorPeriod() {
        return new BigDecimal("10.0");
    }

    public BigDecimal getLiquidatedDamagesPriorPeriod() {
        return new BigDecimal("10.0");
    }

    public BigDecimal getTransactionPriceBacklogCC() {
        return new BigDecimal("50.0");
    }

    public boolean isInputRequired() {
        for (Input input : inputs.values()) {
            if (input.getInputType().isRequired() && input.getValue() == null) {
                return true;
            }
        }

        return false;
    }
}
