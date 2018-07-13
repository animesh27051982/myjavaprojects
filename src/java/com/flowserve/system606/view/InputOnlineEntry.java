/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.BillingEvent;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.CalculationService;
import com.flowserve.system606.service.ContractService;
import com.flowserve.system606.service.PerformanceObligationService;
import com.flowserve.system606.service.ReportingUnitService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
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

    private TreeNode rootTreeNode;
    private TreeNode billingTreeNode;
    @Inject
    private AdminService adminService;
    @Inject
    private PerformanceObligationService pobService;
    @Inject
    private CalculationService calculationService;
    private BigDecimal eacValue;
    @Inject
    private ViewSupport viewSupport;
    @Inject
    private ReportingUnitService reportingUnitService;
    @Inject
    private ContractService contractService;
    private String contractFilterText;

    private List<ReportingUnit> reportingUnits;

    @PostConstruct
    public void init() {
        reportingUnits = adminService.getPreparableReportingUnits();
        rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnits);
    }

    public void filterByContractText() {
        if (isEmpty(contractFilterText)) {
            rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
        } else {
            viewSupport.filterNodeTree(rootTreeNode, contractFilterText);
        }
    }

    public void addChildNodeAction(ActionEvent event) throws Exception {
        //Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.INFO, "message" + id);
        if (billingTreeNode == null) {
            // TODO: añadir excepcion no seleccionado
        }
        //Contract contract = contractService.findContractById(id);
        TreeNode pepe = new DefaultTreeNode(new BillingEvent(null, null, null, null, null, null), billingTreeNode);
        return;
    }

    public void clearFilterByContractText() {
        contractFilterText = null;
        rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
    }

    private boolean isEmpty(String text) {
        if (text == null || "".equals(contractFilterText.trim())) {
            return true;
        }

        return false;
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

    public void calculateOutputs(PerformanceObligation pob) throws Exception {
        calculationService.executeBusinessRules(pob);
    }

    public List<ReportingUnit> getReportingUnits() {
        return reportingUnits;
    }

    public TreeNode getRootTreeNode() {
        return rootTreeNode;
    }

    public String saveInputs() throws Exception {
        Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINE, "Saving inputs.");
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Inputs saved.", ""));

        reportingUnitService.calculateAndSave(reportingUnits);

        return "inputOnlineEntry";
    }

    public String getContractFilterText() {
        return contractFilterText;
    }

    public void setContractFilterText(String contractFilterText) {
        this.contractFilterText = contractFilterText;
    }

    public TreeNode getBillingTreeNode() {
        return billingTreeNode;
    }

}
