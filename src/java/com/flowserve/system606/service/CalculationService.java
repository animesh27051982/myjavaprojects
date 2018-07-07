/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.CurrencyInput;
import com.flowserve.system606.model.DateInput;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Input;
import com.flowserve.system606.model.InputSet;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.Output;
import com.flowserve.system606.model.OutputSet;
import com.flowserve.system606.model.OutputType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.StringInput;
import com.flowserve.system606.model.ValueStore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author kgraves
 */
@Stateless
public class CalculationService {

    @Inject
    private FinancialPeriodService financialPeriodService;
    @Inject
    private InputService inputService;
    @Inject
    private OutputService outputService;
    @Inject
    private BusinessRuleService businessRulesService;

    private Input getInput(String inputTypeId, ValueStore valueStore) {  // TODO - Exception type to be thrown?
        FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();
        InputType inputType = inputService.findInputTypeById(inputTypeId);

        if (!inputSetExistsForPeriod(period, valueStore)) {
            initializeInputSetForPeriod(period, valueStore);
        }
        if (!inputExistsForPeriod(period, inputType, valueStore)) {
            initializeInputForPeriod(period, inputType, valueStore);
        }

        return valueStore.getPeriodInputSetMap().get(period).getTypeInputMap().get(inputType);
    }

    private boolean inputSetExistsForPeriod(FinancialPeriod period, ValueStore valueStore) {
        return valueStore.getPeriodInputSetMap().get(period) != null;
    }

    private void initializeInputSetForPeriod(FinancialPeriod period, ValueStore valueStore) {
        InputSet inputSet = new InputSet();
        if (valueStore instanceof PerformanceObligation) {
            inputSet.setPerformanceObligation((PerformanceObligation) valueStore);
        }
        valueStore.getPeriodInputSetMap().put(period, inputSet);
    }

    private boolean inputExistsForPeriod(FinancialPeriod period, InputType inputType, ValueStore valueStore) {
        return valueStore.getPeriodInputSetMap().get(period).getTypeInputMap().get(inputType) != null;
    }

    private void initializeInputForPeriod(FinancialPeriod period, InputType inputType, ValueStore valueStore) {
        try {
            Class<?> clazz = Class.forName(inputType.getInputClass());
            Input input = (Input) clazz.newInstance();
            input.setInputType(inputType);
            input.setInputSet(valueStore.getPeriodInputSetMap().get(period));
            valueStore.getPeriodInputSetMap().get(period).getTypeInputMap().put(inputType, input);
        } catch (Exception e) {
            Logger.getLogger(PerformanceObligationService.class.getName()).log(Level.SEVERE, "Severe exception initializing inputTypeId: " + inputType.getId(), e);
            throw new IllegalStateException("Severe exception initializing inputTypeId: " + inputType.getId(), e);
        }
    }

    public String getStringInputValue(String inputTypeId, ValueStore valueStore) {
        return (String) getInput(inputTypeId, valueStore).getValue();
    }

    public BigDecimal getDecimalInputValue(String inputTypeId, PerformanceObligation pob) {
        return (BigDecimal) getInput(inputTypeId, pob).getValue();
    }

    public LocalDate getDateInputValue(String inputTypeId, PerformanceObligation pob) {
        return (LocalDate) getInput(inputTypeId, pob).getValue();
    }

    public BigDecimal getCurrencyInputValue(String inputTypeId, PerformanceObligation pob) {
        return (BigDecimal) getInput(inputTypeId, pob).getValue();
    }

    public BigDecimal getCurrencyInputValuePriorPeriod(String inputTypeId, PerformanceObligation pob) {
        return getCurrencyInputValue(inputTypeId, pob);   // KJG TODO - Hack for now to just return this period's value for testing calcs.
    }

    public StringInput getStringInput(String inputTypeId, PerformanceObligation pob) {
        return (StringInput) getInput(inputTypeId, pob);
    }

    public DateInput getDateInput(String inputTypeId, PerformanceObligation pob) {
        return (DateInput) getInput(inputTypeId, pob);
    }

    public CurrencyInput getCurrencyInput(String inputTypeId, PerformanceObligation pob) {  // TODO KJG - This method should be getCurrencyInput() and the method above should be getCurrencyInputValue() waiting on this due to impact.
        return (CurrencyInput) getInput(inputTypeId, pob);
    }

    // Init missing output if needed.
    private Output getOutput(String outputTypeId, PerformanceObligation pob) {  // TODO - Exception type to be thrown?

        FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();
        OutputType outputType = outputService.findOutputTypeById(outputTypeId);

        try {
            if (pob.getPeriodOutputSetMap().get(period) == null) {
                OutputSet outputSet = new OutputSet();
                outputSet.setPerformanceObligation(pob);
                pob.getPeriodOutputSetMap().put(period, outputSet);

            }
            if (pob.getPeriodOutputSetMap().get(period).getIdOutputMap().get(outputType) == null) {
                Class<?> clazz = Class.forName(outputType.getOutputClass());
                Output output = (Output) clazz.newInstance();
                output.setOutputType(outputType);
                output.setOutputSet(pob.getPeriodOutputSetMap().get(period));
                pob.getPeriodOutputSetMap().get(period).getIdOutputMap().put(outputType, output);
            }
        } catch (Exception e) {
            Logger.getLogger(PerformanceObligationService.class.getName()).log(Level.SEVERE, "Severe exception initializing outputTypeId: " + outputTypeId, e);
            throw new IllegalStateException("Severe exception initializing outputTypeId: " + outputTypeId, e);
        }

        return pob.getPeriodOutputSetMap().get(period).getIdOutputMap().get(outputType);
    }

//    public void putOutputMessage(String outputTypeId, String message) {
//        getOutput(outputTypeId).setMessage(message);
//    }
    public void putCurrencyOutputValue(String outputTypeId, PerformanceObligation pob, BigDecimal value) {
        getOutput(outputTypeId, pob).setValue(value);
    }

    public BigDecimal getCurrencyOutputValuePriorPeriod(String outputTypeId, PerformanceObligation pob) {
        return new BigDecimal("10.0");
        //return getCurrencyOutputValue(outputTypeId);  // KJG TODO - Hack for now to just return this period's value for testing calcs.
    }

    public BigDecimal getCurrencyOutputValue(String outputTypeId, PerformanceObligation pob) {
        return (BigDecimal) getOutput(outputTypeId, pob).getValue();
    }

    public Output getCurrencyOutput(String outputTypeId, PerformanceObligation pob) {
        return getOutput(outputTypeId, pob);
    }

//    public boolean isInputRequired() {
//        for (Input input : periodInputSets.values()) {
//            if (input.getInputType().isRequired() && input.getValue() == null) {
//                return true;
//            }
//        }
//
//        return false;
//    }
    public void executeBusinessRules(ValueStore valueStore) throws Exception {
        Logger.getLogger(BusinessRuleService.class.getName()).log(Level.FINER, "Firing all business rules for: " + valueStore.getId());
        businessRulesService.executeBusinessRules(valueStore);
        Logger.getLogger(BusinessRuleService.class.getName()).log(Level.FINER, "Firing all business rules complete.");
    }

    public void executeBusinessRules(List<PerformanceObligation> pobs) throws Exception {
        for (PerformanceObligation pob : pobs) {
            executeBusinessRules(pob);
        }
    }
}
