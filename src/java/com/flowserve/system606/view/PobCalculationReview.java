/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.CalculationService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.primefaces.model.TreeNode;

/**
 * @author kgraves
 */
@Named
@ViewScoped
public class PobCalculationReview implements Serializable {

    private static final Logger logger = Logger.getLogger(PobCalculationReview.class.getName());

    private TreeNode rootTreeNode;
    @Inject
    private CalculationService calculationService;
    private BigDecimal eacValue;
    @Inject
    private ViewSupport viewSupport;
    @Inject
    private AdminService adminService;
    @Inject
    private WebSession webSession;
    //private String contractFilterText;
    private List<Contract> contracts;
    List<ReportingUnit> reportingUnits = null;

    @PostConstruct
    public void init() {
        try {
            //contractFilterText = ;
            reportingUnits = new ArrayList<ReportingUnit>();
            reportingUnits.add(webSession.getCurrentReportingUnit());
            Logger.getLogger(PobCalculationReview.class.getName()).log(Level.INFO, "POB Calc review.  Current RU: " + webSession.getCurrentReportingUnit().getCode());
            rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
            initContracts(reportingUnits);

            if (webSession.getFilterText() != null) {
                filterByContractText();
            }
            if (webSession.getSelectedContracts() != null) {
                filterByContracts();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error init pobs", e);
        }
    }

    public void initContracts(List<ReportingUnit> reportingUnits) {
        contracts = new ArrayList<Contract>();
        reportingUnits.forEach(reportingUnit -> contracts.addAll(reportingUnit.getContracts()));
    }

    public void filterByContractText() {
        if (isEmpty(webSession.getFilterText())) {
            rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
        } else {
            viewSupport.filterNodeTree(rootTreeNode, webSession.getFilterText());
        }
    }

    private boolean isEmpty(String text) {
        if (text == null || "".equals(webSession.getFilterText().trim())) {
            return true;
        }

        return false;
    }

    public void clearFilterByContractText() {
        webSession.setFilterText(null);
        webSession.setSelectedContracts(null);
        rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
    }

    public void filterByContracts() {
        rootTreeNode = viewSupport.generateNodeTree(reportingUnits);

        if (webSession.getSelectedContracts().length == 0) {
            rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
        } else {
            viewSupport.filterNodeTreeContracts(rootTreeNode, Arrays.asList(webSession.getSelectedContracts()));
        }
    }

    public void switchPeriod(FinancialPeriod period) {
        webSession.switchPeriod(period);
        init();
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

    public void calculateAndSave() {
        try {
            calculationService.calculateAndSave(reportingUnits, webSession.getCurrentPeriod());
        } catch (Exception e) {
            Logger.getLogger(PobCalculationReview.class.getName()).log(Level.INFO, "Error recalculating: ", e);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error recalculating: ", e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public void printInputs(PerformanceObligation pob) {
//        Map<String, Input> inputs = pob.getInputs();
//        for (String inputTypeId : inputs.keySet()) {
//            logger.info("execute BR.  inputTypeId: " + inputTypeId + "\tvalue: " + inputs.get(inputTypeId).getValue());
//        }
    }

    public void calculateOutputs(PerformanceObligation pob) throws Exception {
        printInputs(pob);
        calculationService.executeBusinessRules(pob, webSession.getCurrentPeriod());
    }

    public TreeNode getRootTreeNode() {
        return rootTreeNode;
    }

    public List<Contract> getContracts() {
        return contracts;
    }

    public void setContracts(List<Contract> contracts) {
        this.contracts = contracts;
    }

}
