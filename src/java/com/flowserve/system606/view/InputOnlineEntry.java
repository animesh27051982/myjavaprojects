/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.BillingEvent;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.CalculationService;
import com.flowserve.system606.service.ContractService;
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
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
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
    private CalculationService calculationService;
    private BigDecimal eacValue;
    @Inject
    private ViewSupport viewSupport;
    @Inject
    private ContractService contractService;
    @Inject
    private WebSession webSession;

    private TreeNode selectedNode;

    private List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();
    private List<Contract> contracts;

    private String activeTabIndex;

    @PostConstruct
    public void init() {
        //reportingUnits = adminService.getPreparableReportingUnits();
        //contractFilterText = webSession.getFilterText();
        reportingUnits.clear();
        reportingUnits.add(webSession.getCurrentReportingUnit());
        rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnits);
        initContracts(reportingUnits);

        if (webSession.getFilterText() != null) {
            filterByContractText();
        }
        if (webSession.getSelectedContracts() != null) {
            filterByContracts();
        }
    }

    public void switchPeriod(FinancialPeriod period) {
        webSession.switchPeriod(period);
        init();
    }

    public void onTabChange(TabChangeEvent event) {
        activeTabIndex = event.getTab().getId();

    }

    public void onReportingUnitSelect(SelectEvent event) {
        webSession.setFilterText(null);
        webSession.setCurrentReportingUnit((ReportingUnit) event.getObject());
        init();
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

    public void filterByContracts() {
        rootTreeNode = viewSupport.generateNodeTree(reportingUnits);

        if (webSession.getSelectedContracts().length == 0) {
            rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
        } else {
            viewSupport.filterNodeTreeContracts(rootTreeNode, Arrays.asList(webSession.getSelectedContracts()));
        }
    }

    public void addBillingEvent(Contract contract) throws Exception {
        BillingEvent billingEvent = new BillingEvent();
        billingEvent.setContract(contract);
        billingEvent = adminService.update(billingEvent);
        contract.getBillingEvents().add(billingEvent);
        contractService.update(contract);

        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnits);
    }

    public void removeBillingEvent(BillingEvent bEvent) {
        Contract contract = bEvent.getContract();
        contract.getBillingEvents().remove(bEvent);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnits);
    }

    public void updateCumulativeCurrencies() {
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnits);
    }

    public void clearFilterByContractText() {
        webSession.setFilterText(null);
        webSession.setSelectedContracts(null);
        rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
    }

    private boolean isEmpty(String text) {
        if (text == null || "".equals(webSession.getFilterText().trim())) {
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
        try {
            calculationService.executeBusinessRules(pob, webSession.getCurrentPeriod());
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getCause().getCause().getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error calculateOutputs.", e);
        }
    }

    public List<ReportingUnit> getReportingUnits() {
        return reportingUnits;
    }

    public TreeNode getRootTreeNode() {
        return rootTreeNode;
    }

    public void saveInputs() throws Exception {
        adminService.update(reportingUnits);
        Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINE, "Saving inputs.");
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Inputs saved.", ""));
    }

    public void cancelEdits() throws Exception {
        Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINE, "Current edits canceled.");
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Current edits canceled.", ""));
        init();
    }

    public TreeNode getBillingTreeNode() {
        return billingTreeNode;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public int getProgress() {
        return 50;
    }

    public String getActiveTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(String activeTabIndex) {
        this.activeTabIndex = activeTabIndex;
    }

    public List<Contract> getContracts() {
        return contracts;
    }

    public void setContracts(List<Contract> contracts) {
        this.contracts = contracts;
    }
}
