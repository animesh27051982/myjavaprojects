/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.BillingEvent;
import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.CurrencyMetric;
import com.flowserve.system606.model.DecimalMetric;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.CalculationService;
import com.flowserve.system606.service.CurrencyService;
import com.flowserve.system606.service.FinancialPeriodService;
import com.flowserve.system606.service.MetricService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author shubhamv
 */
@Named(value = "viewSupport")
@ViewScoped
public class ViewSupport implements Serializable {

    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    private BusinessUnit businessUnit = new BusinessUnit();
    private ReportingUnit reportingUnit = new ReportingUnit();
    List<User> users = new ArrayList<User>();
    @Inject
    private AdminService adminService;
    @Inject
    private WebSession webSession;
    @Inject
    private MetricService metricService;
    private String searchString = "";
    @Inject
    private FinancialPeriodService financialPeriodService;
    @Inject
    private CalculationService calculationService;
    @Inject
    private CurrencyService currencyService;

    private List<FinancialPeriod> allPeriods = new ArrayList<FinancialPeriod>();

    //FinancialPeriod period = null;
    //FinancialPeriod priorPeriod = null;
    public ViewSupport() {
    }

    @PostConstruct
    public void init() {
        // TODO - need to pull from elsewhre.
        //period = financialPeriodService.getCurrentFinancialPeriod();
        //priorPeriod = financialPeriodService.getPriorFinancialPeriod();
        allPeriods = financialPeriodService.findAllPeriods();
        businessUnit = webSession.getEditBusinessUnit();
        reportingUnit = webSession.getEditReportingUnit();
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

    public List<Company> completeCompany(String searchString) {
        List<Company> sites = null;

        try {
            sites = adminService.searchCompany(searchString);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", " company error  " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            Logger.getLogger(HolidaysEdit.class.getName()).log(Level.SEVERE, "Error Company", e);
        }
        return sites;
    }

    public List<ReportingUnit> completeReportingUnit(String searchString) {
        List<ReportingUnit> rUnit = null;

        try {
            rUnit = adminService.searchReportingUnits(searchString);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", " RU search error  " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error ru search.", e);
        }
        return rUnit;
    }

    public List<ReportingUnit> parentReportingUnit(String searchString) {
        List<ReportingUnit> rUnit = null;

        try {
            rUnit = adminService.parentReportingUnits(searchString, this.reportingUnit);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", " site location error  " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error siteLocations.", e);
        }
        return rUnit;
    }

    public List<BusinessUnit> CompleteParentBusinessUnit(String searchString) {
        List<BusinessUnit> sites = null;

        try {
            sites = adminService.searchParentBu(searchString, this.businessUnit);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", " site location error  " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error siteLocations.", e);
        }
        return sites;
    }

    public List<BusinessUnit> completeBusinessUnit(String searchString) {
        List<BusinessUnit> sites = null;

        try {
            sites = adminService.searchSites(searchString);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", " site location error  " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error siteLocations.", e);
        }
        return sites;
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
                for (BillingEvent billEvent : contract.getBillingEvents()) {
                    Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree POB ID: " + billEvent.getId());
                    new DefaultTreeNode(billEvent, contractNode);
                }
            }
        }

        return root;
    }

    public void filterNodeTree(TreeNode root, String contractFilterText) {
        //List<TreeNode> contractsToRemove = new ArrayList<TreeNode>();
        List<TreeNode> pobsToRemove = new ArrayList<TreeNode>();

        for (TreeNode reportingUnit : root.getChildren()) {
            String ruName = ((ReportingUnit) reportingUnit.getData()).getName();
            if (Pattern.compile(Pattern.quote(contractFilterText), Pattern.CASE_INSENSITIVE).matcher(ruName).find()
                    || Pattern.compile(Pattern.quote(contractFilterText), Pattern.CASE_INSENSITIVE).matcher(ruName).find()) {
                continue;
            }

            for (TreeNode contract : reportingUnit.getChildren()) {
                String contractName = ((Contract) contract.getData()).getName();
                String contractId = ((Contract) contract.getData()).getId().toString();

                if (Pattern.compile(Pattern.quote(contractFilterText), Pattern.CASE_INSENSITIVE).matcher(contractName).find()
                        || Pattern.compile(Pattern.quote(contractFilterText), Pattern.CASE_INSENSITIVE).matcher(contractId).find()) {
                    continue;
                }

                for (TreeNode pob : contract.getChildren()) {
                    String pobName = ((PerformanceObligation) pob.getData()).getName();
                    String pobId = ((PerformanceObligation) pob.getData()).getId().toString();

                    if (!Pattern.compile(Pattern.quote(contractFilterText), Pattern.CASE_INSENSITIVE).matcher(pobName).find()
                            && !Pattern.compile(Pattern.quote(contractFilterText), Pattern.CASE_INSENSITIVE).matcher(pobId).find()) {
                        pobsToRemove.add(pob);
                    }
                }
            }
        }

        // Ugly but necessary to prevent ConcurrentModificationException
        for (TreeNode pob : pobsToRemove) {
            pob.getParent().getChildren().remove(pob);
        }

//        for (TreeNode contract : contractsToRemove) {
//            contract.getParent().getChildren().remove(contract);
//        }
        List<TreeNode> reportingUnitsToRemove = new ArrayList<TreeNode>();
        List<TreeNode> contractsToRemove = new ArrayList<TreeNode>();

        for (TreeNode reportingUnit : root.getChildren()) {
            if (reportingUnit.getChildCount() == 0) {
                reportingUnitsToRemove.add(reportingUnit);
            }
            for (TreeNode contract : reportingUnit.getChildren()) {
                if (contract.getChildCount() == 0) {
                    contractsToRemove.add(contract);
                }
            }

        }

        for (TreeNode ru : reportingUnitsToRemove) {
            ru.getParent().getChildren().remove(ru);
        }
        for (TreeNode contract : contractsToRemove) {
            contract.getParent().getChildren().remove(contract);
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

    public CurrencyMetric getCurrencyMetric(String metricTypeId, Measurable measurable) throws Exception {

        return calculationService.getCurrencyMetric(metricTypeId, measurable, webSession.getCurrentPeriod());
    }

    public DecimalMetric getDecimalMetric(String metricTypeId, Measurable measurable) throws Exception {
        return calculationService.getDecimalMetric(metricTypeId, measurable, webSession.getCurrentPeriod());
    }

    public CurrencyMetric getCurrencyMetricPriorPeriod(String metricTypeId, Measurable measurable) throws Exception {
        return calculationService.getCurrencyMetric(metricTypeId, measurable, webSession.getPriorPeriod());
    }

    public List<FinancialPeriod> getAllPeriods() {
        return allPeriods;
    }

    public String getExchangeRate(Contract contract) throws Exception {
        return currencyService.findRateByFromToPeriod(contract.getContractCurrency(), contract.getLocalCurrency(), webSession.getCurrentPeriod()).getConversionRate().toPlainString();
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

    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(BusinessUnit businessUnit) {
        this.businessUnit = businessUnit;
    }

    public ReportingUnit getReportingUnit() {
        return reportingUnit;
    }

    public void setReportingUnit(ReportingUnit reportingUnit) {
        this.reportingUnit = reportingUnit;
    }
}
