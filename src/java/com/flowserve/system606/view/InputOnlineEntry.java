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
import com.flowserve.system606.service.PerformanceObligationService;
import com.flowserve.system606.service.ReportingUnitService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @Inject
    private ReportingUnitService reportingUnitService;
    @Inject
    private PerformanceObligationService performanceObligationService;
    private BigDecimal eacValue;
    @Inject
    private ViewSupport viewSupport;
    @Inject
    private ContractService contractService;
    @Inject
    private WebSession webSession;
    private TreeNode selectedNode;
    private List<Contract> contracts;
    private String activeTabIndex;
    private ReportingUnit reportingUnit;

    @PostConstruct
    public void init() {
        reportingUnit = adminService.findReportingUnitById(webSession.getCurrentReportingUnit().getId());
        rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
        initContracts(reportingUnit);

        if (webSession.getFilterText() != null) {
            filterByContractText();
        }
        if (webSession.getSelectedContracts() != null && webSession.getSelectedContracts().length > 0) {
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
        //webSession.setAdminReportingUnit(((ReportingUnit) event.getObject()));
        init();
    }

//    public void initContracts(List<ReportingUnit> reportingUnits) {
//        contracts = new ArrayList<Contract>();
//        reportingUnits.forEach(reportingUnit -> contracts.addAll(reportingUnit.getContracts()));
//    }
    public void initContracts(ReportingUnit reportingUnit) {
        contracts = reportingUnit.getContracts();
    }

    public void filterByContractText() {
        if (isEmpty(webSession.getFilterText())) {
            //rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
            rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
            billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
        } else {
            viewSupport.filterNodeTree(rootTreeNode, webSession.getFilterText());
            viewSupport.filterBillingNodeTree(billingTreeNode, webSession.getFilterText());
        }
    }

    public void filterByContracts() {
        rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
        if (webSession.getSelectedContracts().length == 0) {
            rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
            billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
        } else {
            viewSupport.filterNodeTreeContracts(rootTreeNode, Arrays.asList(webSession.getSelectedContracts()));
            viewSupport.filterNodeTreeContracts(billingTreeNode, Arrays.asList(webSession.getSelectedContracts()));
        }
    }

    public void addBillingEvent(Contract contract) throws Exception {
        BillingEvent billingEvent = new BillingEvent();
        billingEvent.setContract(contract);
        billingEvent = adminService.update(billingEvent);
        contract.getBillingEvents().add(billingEvent);
        contractService.update(contract);

        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
    }

    public void removeBillingEvent(BillingEvent bEvent) {
        Contract contract = bEvent.getContract();
        contract.getBillingEvents().remove(bEvent);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
    }

    public void updateCumulativeCurrencies() {
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
    }

    public void clearFilterByContractText() {
        webSession.setFilterText(null);
        webSession.setSelectedContracts(null);
        rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
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
            Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.INFO, "Calcing outputs...");
            Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.INFO, "Setting POB last updated by: " + webSession.getUser().getName());
            updateAuditInfo(pob);

            calculationService.executeBusinessRules(pob, webSession.getCurrentPeriod());

        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getCause().getCause().getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error calculateOutputs.", e);
        }
    }

    public TreeNode getRootTreeNode() {
        return rootTreeNode;
    }

    public void saveInputs() throws Exception {
        if (viewSupport.isPeriodClosed()) {
            Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINE, "Saving inputs.");
            adminService.update(reportingUnit);
            for (Contract contract : reportingUnit.getContracts()) {
                Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINER, "Updating Contract: " + contract.getName());
                contractService.update(contract);
            }
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Inputs saved.", ""));
            Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINE, "Inputs saved.");
        }
    }

//    public boolean isUpdatable() {
//        return reportingUnitService.isUpdatable(reportingUnit, webSession.getCurrentPeriod(), webSession.getUser());
//    }
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

    public ReportingUnit getReportingUnit() {
        return reportingUnit;
    }

    private void updateAuditInfo(PerformanceObligation pob) {
        pob.setLastUpdatedBy(webSession.getUser());
        pob.setLastUpdateDate(LocalDateTime.now());
        pob.getContract().setLastUpdatedBy(webSession.getUser());
        pob.getContract().setLastUpdateDate(LocalDateTime.now());
    }
}
