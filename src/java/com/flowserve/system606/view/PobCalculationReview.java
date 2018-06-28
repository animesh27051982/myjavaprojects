/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.service.BusinessRuleService;
import com.flowserve.system606.service.InputService;
import com.flowserve.system606.service.OutputService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.CellEditEvent;
import org.primefaces.model.TreeNode;

/**
 * @author kgraves
 */
@Named
@ViewScoped
public class PobCalculationReview implements Serializable {

    private static final Logger logger = Logger.getLogger(PobCalculationReview.class.getName());

    private TreeNode pobs;
    @Inject
    BusinessRuleService businessRuleService;
    private BigDecimal eacValue;
    @Inject
    private WebSession webSession;
    @Inject
    private InputService inputService;
    @Inject
    private OutputService outputService;

    @PostConstruct
    public void init() {
        try {
            pobs = webSession.getReportingUnitTreeNode();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error init pobs", e);
        }
    }

    public TreeNode getPobs() {
        return pobs;
    }

    public String getInputTypeName(String inputTypeId) {
        return inputService.findInputTypeById(inputTypeId).getName();
    }

    public String getOutputTypeName(String outputTypeId) {
        return outputService.findOutputTypeById(outputTypeId).getName();
    }

    public void onCellEdit(CellEditEvent event) {
        Object oldValue = event.getOldValue();
        Object newValue = event.getNewValue();

        if (newValue != null && !newValue.equals(oldValue)) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Cell Changed", "Old: " + oldValue + ", New:" + newValue);
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public BigDecimal getEacValue() {
        return eacValue;
    }

    public void setEacValue(BigDecimal eacValue) {
        this.eacValue = eacValue;
    }

    public void printInputs(PerformanceObligation pob) {
//        Map<String, Input> inputs = pob.getInputs();
//        for (String inputTypeId : inputs.keySet()) {
//            logger.info("execute BR.  inputTypeId: " + inputTypeId + "\tvalue: " + inputs.get(inputTypeId).getValue());
//        }
    }

    public void calculateOutputs(PerformanceObligation pob) throws Exception {
        printInputs(pob);
        businessRuleService.executeBusinessRules(pob);
    }
}
