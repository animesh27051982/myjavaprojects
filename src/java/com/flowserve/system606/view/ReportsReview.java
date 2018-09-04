/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.TreeNode;

/**
 *
 * @author shubhamc
 */
@Named
@ViewScoped
public class ReportsReview implements Serializable {

    private static final Logger logger = Logger.getLogger(ReportsList.class.getName());

    private TreeNode rootTreeNode;
    ReportingUnit reportingUnit;
    private TreeNode selectedNode;
    private List<Contract> contracts;

    @Inject
    private ViewSupport viewSupport;
    @Inject
    private WebSession webSession;
    @Inject
    private AdminService adminService;

    @PostConstruct
    public void init() {
        reportingUnit = adminService.findReportingUnitById(webSession.getCurrentReportingUnit().getId());
        rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
        initContracts();
        if (webSession.getSelectedContracts() != null && webSession.getSelectedContracts().length > 0) {
            filterByContracts();
        }
        if (webSession.getFilterText() != null) {
            filterByContractText();
        }
    }

    public TreeNode getRootTreeNode() {
        return rootTreeNode;
    }

    public void setRootTreeNode(TreeNode rootTreeNode) {
        this.rootTreeNode = rootTreeNode;
    }

    public ReportingUnit getReportingUnit() {
        return reportingUnit;
    }

    public void setReportingUnit(ReportingUnit reportingUnit) {
        this.reportingUnit = reportingUnit;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public void onReportingUnitSelect(SelectEvent event) {
        webSession.setFilterText(null);
        webSession.setCurrentReportingUnit((ReportingUnit) event.getObject());
        init();
    }

    public void switchPeriod(FinancialPeriod period) {
        webSession.switchPeriod(period);
        init();
    }

    public void filterByContracts() {
        rootTreeNode = viewSupport.generateNodeTree(reportingUnit);

        if (webSession.getSelectedContracts().length == 0) {
            rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
        } else {
            viewSupport.filterNodeTreeContracts(rootTreeNode, Arrays.asList(webSession.getSelectedContracts()));
        }
    }

    private boolean isEmpty(String text) {
        if (text == null || "".equals(webSession.getFilterText().trim())) {
            return true;
        }

        return false;
    }

    public void initContracts() {
        contracts = reportingUnit.getContracts();
    }

    public List<Contract> getContracts() {
        return contracts;
    }

    public void setContracts(List<Contract> contracts) {
        this.contracts = contracts;
    }

    public void filterByContractText() {
        if (isEmpty(webSession.getFilterText())) {
            rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
        } else {
            viewSupport.filterNodeTree(rootTreeNode, webSession.getFilterText());
        }
    }

}
