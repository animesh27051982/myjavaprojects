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
import java.util.List;
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

    // Inputs will be pre-initialized.  should never need to put a new Input
//    public void putInput(Input input) {
//        inputs.put(input.getInputType().getId(), input);
//    }
    public Input getInput(InputTypeName inputName) {
        return inputs.get(inputName);
    }

    public String getStringInputValue(InputTypeName inputName) {
        return ((StringInput) inputs.get(inputName)).getValue();
    }

    public StringInput getStringInput(InputTypeName inputTypeName) {
        return ((StringInput) inputs.get(inputTypeName));
    }

    public DateInput getDateInput(InputTypeName inputName) {
        return ((DateInput) inputs.get(inputName));
    }

    public LocalDate getDateInputValue(InputTypeName inputName) {
        return ((DateInput) inputs.get(inputName)).getValue();
    }

    public BigDecimal getCurrencyInput(InputTypeName inputName) {
        Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "getCurrencyInput: " + inputName + " POB ID: " + this.getId());
        Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "Input Object: " + inputs.get(inputName));
        return ((CurrencyInput) inputs.get(inputName)).getValue();
    }

    public BigDecimal getCurrencyInputPriorPeriod(InputTypeName inputName) {
        return getCurrencyInput(inputName);   // KJG TODO - Hack for now to just return this period's value for testing calcs.
    }

    public CurrencyInput getCurrencyInp(InputTypeName inputName) {  // TODO KJG - This method should be getCurrencyInput() and the method above should be getCurrencyInputValue() waiting on this due to impact.
        return (CurrencyInput) inputs.get(inputName);
    }

    public void initializeOutputs(List<OutputType> outputTypes) throws Exception {
        for (OutputType outputType : outputTypes) {
            initializeOutput(outputType);
        }
    }

    public void initializeOutput(OutputType outputType) throws Exception {
        if (outputs.get(outputType.getName()) == null) {
            Class<?> clazz = Class.forName(outputType.getOutputClass());
            Output output = (Output) clazz.newInstance();
            output.setOutputType(outputType);
            output.setPerformanceObligation(this);
            outputs.put(outputType.getName(), output);
        }
    }

    public void initializeInputs(List<InputType> inputTypes) throws Exception {
        for (InputType inputType : inputTypes) {
            initializeInput(inputType);
        }
    }

    public void initializeInput(InputType inputType) throws Exception {
        if (inputs.get(inputType.getName()) == null) {
            Class<?> clazz = Class.forName(inputType.getInputClass());
            Input input = (Input) clazz.newInstance();
            input.setInputType(inputType);
            input.setPerformanceObligation(this);
            inputs.put(inputType.getName(), input);
        }
    }

//    public void putOutput(Output output) {
//        outputs.put(output.getOutputType().getId(), output);
//    }
    public void putOutputMessage(String outputTypeId, String message) {
        outputs.get(outputTypeId).setMessage(message);
    }

    public void putCurrencyOutput(OutputTypeName outputTypeName, BigDecimal value) {
        Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "putCurrencyOutput(): " + outputTypeName + ": " + value.toPlainString());
        outputs.get(outputTypeName).setValue(value);
    }

    public Output getOutput(OutputTypeName outputTypeName) {
        return outputs.get(outputTypeName);
    }

    public Output getOutputPriorPeriod(OutputTypeName outputTypeName) {
        return getOutput(outputTypeName);  // KJG TODO - Hack for now to just return this period's value for testing calcs.
    }

    public BigDecimal getCurrencyOutput(OutputTypeName outputTypeName) {
        if (((CurrencyOutput) outputs.get(outputTypeName)).getValue() == null) {
            Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "getCurrencyOutput(): " + outputTypeName + " is null");
        } else {
            Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "getCurrencyOutput(): " + outputTypeName + ": " + ((CurrencyOutput) outputs.get(outputTypeName)).getValue().toPlainString());
        }

        return ((CurrencyOutput) outputs.get(outputTypeName)).getValue();
    }

    public BigDecimal getCurrencyOutputPriorPeriod(String outputTypeId) {
        return new BigDecimal("100.0");
    }

//    public void putDecimalOutput(String outputTypeId, BigDecimal value) {
//        outputs.put(output.getOutputType().getId(), output);
//    }
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

    public BigDecimal getDecimalValue(InputTypeName inputName) {
        return ((DecimalInput) inputs.get(inputName)).getValue();
    }

    public LocalDate getDateValue(InputTypeName inputName) {
        return ((DateInput) inputs.get(inputName)).getValue();
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

    public void printInputs() {
        for (InputTypeName inputTypeName : inputs.keySet()) {
            if (inputs.get(inputTypeName) != null && inputs.get(inputTypeName).getValue() != null) {
                Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "InputTypeId: " + inputTypeName + "\tvalue: " + inputs.get(inputTypeName).getValue());
            }
        }
    }

    public void printOutputs() {
        for (Object obj : outputs.keySet()) {
            Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "Key: " + obj);
        }
        for (OutputTypeName outputTypeName : outputs.keySet()) {
            if (outputs.get(outputTypeName) != null && outputs.get(outputTypeName).getValue() != null) {
                Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "OutputTypeId: " + outputTypeName + "\tvalue: " + outputs.get(outputTypeName).getValue());
            }
        }
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
