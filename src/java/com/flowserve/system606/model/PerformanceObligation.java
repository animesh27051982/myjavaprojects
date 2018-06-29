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
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
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
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "POB_INPUTS", joinColumns = @JoinColumn(name = "POB_ID"), inverseJoinColumns = @JoinColumn(name = "INPUT_ID"))
    @MapKey(name = "inputType.id")
    private Map<String, Input> inputs = new HashMap<String, Input>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "POB_OUTPUTS", joinColumns = @JoinColumn(name = "POB_ID"), inverseJoinColumns = @JoinColumn(name = "OUTPUT_ID"))
    @MapKey(name = "outputType.id")
    private Map<String, Output> outputs = new HashMap<String, Output>();

    public PerformanceObligation() {
    }

    public void putInput(Input input) {
        inputs.put(input.getInputType().getId(), input);
    }

    public Input getInput(String inputTypeId) {
        return inputs.get(inputTypeId);
    }

    public String getStringInput(String inputTypeId) {
        return ((StringInput) inputs.get(inputTypeId)).getValue();
    }

    public LocalDate getDateInput(String inputTypeId) {
        return ((DateInput) inputs.get(inputTypeId)).getValue();
    }

    public BigDecimal getCurrencyInput(String inputTypeId) {
        if (((CurrencyInput) inputs.get(inputTypeId)).getValue() == null) {
            Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "getCurrencyIntput(): " + inputTypeId + " is null");
        } else {
            Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "getCurrencyIntput(): " + inputTypeId + ": " + ((CurrencyInput) inputs.get(inputTypeId)).getValue().toPlainString());
        }

        return ((CurrencyInput) inputs.get(inputTypeId)).getValue();
    }

    public BigDecimal getCurrencyInputPriorPeriod(String inputTypeId) {
        return getCurrencyInput(inputTypeId);   // KJG TODO - Hack for now to just return this period's value for testing calcs.
    }

    public CurrencyInput getCurrencyInp(String inputTypeId) {  // TODO KJG - This method should be getCurrencyInput() and the method above should be getCurrencyInputValue() waiting on this due to impact.
        return (CurrencyInput) inputs.get(inputTypeId);
    }

    public void initializeOutputs(List<OutputType> outputTypes) throws Exception {
        for (OutputType outputType : outputTypes) {
            initializeOutput(outputType);
        }
    }

    public void initializeOutput(OutputType outputType) throws Exception {
        if (outputs.get(outputType) == null) {
            Class<?> clazz = Class.forName(outputType.getOutputClass());
            Output output = (Output) clazz.newInstance();
            output.setOutputType(outputType);
            outputs.put(outputType.getId(), output);
        }
    }

    public void initializeInputs(List<InputType> inputTypes) throws Exception {
        for (InputType inputType : inputTypes) {
            initializeInput(inputType);
        }
    }

    public void initializeInput(InputType inputType) throws Exception {
        if (inputs.get(inputType) == null) {
            Class<?> clazz = Class.forName(inputType.getInputClass());
            Input input = (Input) clazz.newInstance();
            input.setInputType(inputType);
            inputs.put(inputType.getId(), input);
        }
    }

//    public void putOutput(Output output) {
//        outputs.put(output.getOutputType().getId(), output);
//    }
    public void putOutputMessage(String outputTypeId, String message) {
        outputs.get(outputTypeId).setMessage(message);
    }

    public void putCurrencyOutput(String outputTypeId, BigDecimal value) {
        Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "putCurrencyOutput(): " + outputTypeId + ": " + value.toPlainString());
        outputs.get(outputTypeId).setValue(value);
    }

    public Output getOutput(String outputTypeId) {
        return outputs.get(outputTypeId);
    }

    public Output getOutputPriorPeriod(String outputTypeId) {
        return getOutput(outputTypeId);  // KJG TODO - Hack for now to just return this period's value for testing calcs.
    }

    public BigDecimal getCurrencyOutput(String outputTypeId) {
        if (((CurrencyOutput) outputs.get(outputTypeId)).getValue() == null) {
            Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "getCurrencyOutput(): " + outputTypeId + " is null");
        } else {
            Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "getCurrencyOutput(): " + outputTypeId + ": " + ((CurrencyOutput) outputs.get(outputTypeId)).getValue().toPlainString());
        }

        return ((CurrencyOutput) outputs.get(outputTypeId)).getValue();
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

    public BigDecimal getDecimalValue(String inputTypeId) {
        return ((DecimalInput) inputs.get(inputTypeId)).getValue();
    }

    public LocalDate getDateValue(String inputTypeId) {
        return ((DateInput) inputs.get(inputTypeId)).getValue();
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
        for (String inputTypeId : inputs.keySet()) {
            if (inputs.get(inputTypeId) != null && inputs.get(inputTypeId).getValue() != null) {
                Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "InputTypeId: " + inputTypeId + "\tvalue: " + inputs.get(inputTypeId).getValue());
            }
        }
    }

    public void printOutputs() {
        Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "message");
        for (String outputTypeId : outputs.keySet()) {
            if (outputs.get(outputTypeId) != null && outputs.get(outputTypeId).getValue() != null) {
                Logger.getLogger(PerformanceObligation.class.getName()).log(Level.FINER, "OutputTypeId: " + outputTypeId + "\tvalue: " + outputs.get(outputTypeId).getValue());
            }

        }
    }

    public BigDecimal getContractToLocalFxRate() {
        return new BigDecimal("1.0");
    }

    public BigDecimal getCurrencyValuePriorPeriod() {
        return new BigDecimal("500.0");
    }

    public BigDecimal getLiquidatedDamagesPriorPeriod() {
        return new BigDecimal("50.0");
    }
}
