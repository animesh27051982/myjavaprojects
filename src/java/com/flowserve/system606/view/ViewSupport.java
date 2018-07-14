/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Accumulable;
import com.flowserve.system606.model.BillingEvent;
import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.Metric;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.CalculationService;
import com.flowserve.system606.service.MetricService;
import com.flowserve.system606.service.PerformanceObligationService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author shubhamv
 */
@Named(value = "viewSupport")
@ApplicationScoped
public class ViewSupport implements Serializable {

    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    List<User> users = new ArrayList<User>();
    @Inject
    private AdminService adminService;
    @Inject
    private MetricService metricService;
    private String searchString = "";
    @Inject
    private PerformanceObligationService pobService;
    @Inject
    private CalculationService calculationService;

    /**
     * Creates a new instance of ViewSupport
     */
    public ViewSupport() {
    }

    public List<User> completeUser(String searchString) {
        List<User> users = null;
        try {
            users = adminService.findByStartsWithLastName(searchString);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", " user search error  " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error search users.", e);
        }

        return users;

    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public List<Currency> getAllCurrencies() {
        List<Currency> currencyList = new ArrayList<Currency>();
        currencyList.addAll(Currency.getAvailableCurrencies());
        Collections.sort(currencyList, (c1, c2) -> c1.getCurrencyCode().compareTo(c2.getCurrencyCode()));

        return currencyList;
    }

    public TreeNode generateNodeTree(List<ReportingUnit> reportingUnits) {
        TreeNode root = new DefaultTreeNode(new BusinessUnit(), null);

        for (ReportingUnit reportingUnit : reportingUnits) {
            Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree RU Name: " + reportingUnit.getName());
            TreeNode reportingUnitNode = new DefaultTreeNode(reportingUnit, root);
            reportingUnitNode.setExpanded(true);
            for (Contract contract : reportingUnit.getContracts()) {
                Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree Contract Name: " + contract.getName());
                TreeNode contractNode = new DefaultTreeNode(contract, reportingUnitNode);
                contractNode.setExpanded(true);
                for (PerformanceObligation pob : contract.getPerformanceObligations()) {
                    Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree POB ID: " + pob.getId());
                    new DefaultTreeNode(pob, contractNode);
                }
            }
        }

        return root;
    }

    public TreeNode generateNodeTreeForBilling(List<ReportingUnit> reportingUnits) {
        TreeNode root = new DefaultTreeNode(new BusinessUnit(), null);

        for (ReportingUnit reportingUnit : reportingUnits) {
            Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree RU Name: " + reportingUnit.getName());
            TreeNode reportingUnitNode = new DefaultTreeNode(reportingUnit, root);
            reportingUnitNode.setExpanded(true);
            for (Contract contract : reportingUnit.getContracts()) {
                Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree Contract Name: " + contract.getName());
                TreeNode contractNode = new DefaultTreeNode(contract, reportingUnitNode);
                contractNode.setExpanded(true);
                for (BillingEvent billEvent : contract.getBillingEvent()) {
                    Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree POB ID: " + billEvent.getId());
                    new DefaultTreeNode(billEvent, contractNode);
                }
            }
        }

        return root;
    }

    public void filterNodeTree(TreeNode root, String contractFilterText) {
        List<TreeNode> contractsToRemove = new ArrayList<TreeNode>();

        for (TreeNode reportingUnit : root.getChildren()) {
            for (TreeNode contract : reportingUnit.getChildren()) {
                String contractName = ((Contract) contract.getData()).getName();
                String contractId = ((Contract) contract.getData()).getId().toString();

                if (!Pattern.compile(Pattern.quote(contractFilterText), Pattern.CASE_INSENSITIVE).matcher(contractName).find()
                        && !Pattern.compile(Pattern.quote(contractFilterText), Pattern.CASE_INSENSITIVE).matcher(contractId).find()) {
                    contractsToRemove.add(contract);
                }
            }
        }

        // Ugly but necessary to prevent ConcurrentModificationException
        for (TreeNode contract : contractsToRemove) {
            contract.getParent().getChildren().remove(contract);
        }

        List<TreeNode> reportingUnitsToRemove = new ArrayList<TreeNode>();

        for (TreeNode reportingUnit : root.getChildren()) {
            if (reportingUnit.getChildCount() == 0) {
                reportingUnitsToRemove.add(reportingUnit);
            }
        }

        for (TreeNode ru : reportingUnitsToRemove) {
            ru.getParent().getChildren().remove(ru);
        }

    }

    public void filterNodeTreeContracts(TreeNode root, List<Contract> contracts) {
        List<TreeNode> contractsToRemove = new ArrayList<TreeNode>();

        for (TreeNode reportingUnit : root.getChildren()) {
            for (TreeNode contract : reportingUnit.getChildren()) {
                if (!contracts.contains(((Contract) contract.getData()))) {
                    contractsToRemove.add(contract);
                }
            }
        }

        // Ugly but necessary to prevent ConcurrentModificationException
        for (TreeNode contract : contractsToRemove) {
            contract.getParent().getChildren().remove(contract);
        }

        List<TreeNode> reportingUnitsToRemove = new ArrayList<TreeNode>();

        for (TreeNode reportingUnit : root.getChildren()) {
            if (reportingUnit.getChildCount() == 0) {
                reportingUnitsToRemove.add(reportingUnit);
            }
        }

        for (TreeNode ru : reportingUnitsToRemove) {
            ru.getParent().getChildren().remove(ru);
        }

    }

    public String getMetricTypeDescription(String metricTypeId) {
        return metricService.findMetricTypeById(metricTypeId).getDescription();
    }

    public BigDecimal getCurrencyMetricValue(String metricTypeId, PerformanceObligation pob) {
        Logger.getLogger(ViewSupport.class.getName()).log(Level.FINER, "metricTypeId: " + metricTypeId);
        return calculationService.getCurrencyMetricValue(metricTypeId, pob);
    }

    public BigDecimal getAccumulatedCurrencyMetricValue(String metricTypeId, Accumulable accumulable) {
        return calculationService.getAccumulatedCurrencyMetricValue(metricTypeId, accumulable);
    }

    public Metric getCurrencyMetric(String metricTypeId, PerformanceObligation pob) {
        return calculationService.getCurrencyMetric(metricTypeId, pob);
    }

    public BigDecimal getCurrencyMetricValuePriorPeriod(String outputTypeId, PerformanceObligation pob) {
        return calculationService.getCurrencyMetricValuePriorPeriod(outputTypeId, pob);
    }
}
