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
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
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

    public BigDecimal getCurrencyInput(String inputTypeId) {
        return ((CurrencyInput) inputs.get(inputTypeId)).getValue();
    }

    public void initializeOutputs(List<OutputType> outputTypes) throws Exception {
        for (OutputType outputType : outputTypes) {
            initializeOutput(outputType);
        }
    }

    public void initializeOutput(OutputType outputType) throws Exception {
        Class<?> clazz = Class.forName(outputType.getOutputClass());
        Output output = (Output) clazz.newInstance();
        outputs.put(outputType.getId(), output);
    }

    public void putOutput(Output output) {
        outputs.put(output.getOutputType().getId(), output);
    }

    public void putCurrencyOutput(String outputTypeId, BigDecimal value) {
        LOG.info("class: " + value.getClass());
        outputs.get(outputTypeId).setValue(value);
    }

    public Output getOutput(String outputTypeId) {
        return outputs.get(outputTypeId);
    }

    public BigDecimal getCurrencyOutput(String outputTypeId) {
        return ((CurrencyOutput) outputs.get(outputTypeId)).getValue();
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

}
