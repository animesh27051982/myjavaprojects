/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.BusinessRuleService;
import com.flowserve.system606.service.PerformanceObligationService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.CellEditEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author kgraves
 */
@Named
@ViewScoped
public class InputOnlineEntry implements Serializable {

    private static final Logger logger = Logger.getLogger(InputOnlineEntry.class.getName());

    private TreeNode pobs;
    @Inject
    AdminService adminService;
    @Inject
    PerformanceObligationService performanceObligationService;
    @Inject
    BusinessRuleService businessRuleService;
    private BigDecimal eacValue;

    @PostConstruct
    public void init() {
        try {
            pobs = createPobs();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error init pobs", e);
        }
    }

    public TreeNode getPobs() {
        return pobs;
    }

    public TreeNode createPobs() throws Exception {
        TreeNode root = new DefaultTreeNode(new BusinessUnit(), null);

        // find reporting units assigned to user.  for now set two.
        List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();

        ReportingUnit ru1015 = adminService.findReportingUnitByCode("1015");
        Contract ru1015Contract = new Contract();
        ru1015Contract.setId(1234L);
        ru1015Contract.setName("Chevron Proucts - " + ru1015Contract.getId());
        ru1015Contract.setReportingUnit(ru1015);
        ru1015.getContract().add(ru1015Contract);

        PerformanceObligation po1015 = new PerformanceObligation();
        po1015.setContract(ru1015Contract);
        po1015.setId(11261L);
        po1015.setName("POC-11261");
        po1015.setRevRecMethod("POC");
        performanceObligationService.initializeInputs(po1015);
        performanceObligationService.initializeOutputs(po1015);
        ru1015Contract.getPerformanceObligations().add(po1015);
        logger.info("pob obj from view clas init 1015: " + po1015.toString());

        reportingUnits.add(ru1015);

        //reportingUnits.add(adminService.findReportingUnitByCode("1100"));
        ReportingUnit ru1100 = adminService.findReportingUnitByCode("1100");
        Contract ru1100Contract = new Contract();
        ru1100Contract.setId(1235L);
        ru1100Contract.setName("Phillips 66 - " + ru1100Contract.getId());
        ru1100Contract.setReportingUnit(ru1100);
        ru1100.getContract().add(ru1100Contract);

        PerformanceObligation po1100 = new PerformanceObligation();
        po1100.setContract(ru1100Contract);
        po1100.setId(88776L);
        po1100.setName("POC-88776");
        po1100.setRevRecMethod("POC");
        performanceObligationService.initializeInputs(po1100);
        performanceObligationService.initializeOutputs(po1100);
        ru1100Contract.getPerformanceObligations().add(po1100);
        logger.info("pob obj from view clas init 1100: " + po1100.toString());

        reportingUnits.add(ru1100);
        for (ReportingUnit reportingUnit : reportingUnits) {
            TreeNode reportingUnitNode = new DefaultTreeNode(reportingUnit, root);
            reportingUnitNode.setExpanded(true);
            for (Contract contract : reportingUnit.getContract()) {
                TreeNode contractNode = new DefaultTreeNode(contract, reportingUnitNode);
                contractNode.setExpanded(true);
                for (PerformanceObligation pob : contract.getPerformanceObligations()) {
                    TreeNode pobNode = new DefaultTreeNode(pob, contractNode);
                }
            }
        }

        return root;
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
        logger.info("calculateoutputs pob obj from view clas: " + pob.toString());
        printInputs(pob);
        businessRuleService.executeBusinessRules(pob);
    }
}
