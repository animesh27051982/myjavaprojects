package com.flowserve.system606.web;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.FinancialPeriodService;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 * @author kgraves
 */
@Named
@SessionScoped
public class WebSession implements Serializable {

    @Inject
    private FinancialPeriodService financialPeriodService;

    private BusinessUnit editBusinessUnit;
    private TreeNode reportingUnitTreeNode = new DefaultTreeNode();  // this is temp for calculatin testing.
    private ReportingUnit editReportingUnit;
    private User editUser;
    private Country country;
    private Holiday editHolidays;
    private Company editCompany;
    private Contract editContract;
    private FinancialPeriod editFinancialPeriod;
    private FinancialPeriod currentPeriod;
    private FinancialPeriod priorPeriod;

    @PostConstruct
    public void init() {
        currentPeriod = financialPeriodService.getCurrentFinancialPeriod();
        priorPeriod = financialPeriodService.getPriorFinancialPeriod();
        Logger.getLogger(WebSession.class.getName()).log(Level.INFO, "WebSession current: " + currentPeriod.getId());
        Logger.getLogger(WebSession.class.getName()).log(Level.INFO, "WebSession prior: " + priorPeriod.getId());
    }

    public void switchPeriod(FinancialPeriod newCurrentPeriod) {
        currentPeriod = newCurrentPeriod;
        priorPeriod = financialPeriodService.calculatePriorPeriod(currentPeriod);
    }

    public BusinessUnit getEditBusinessUnit() {
        return editBusinessUnit;
    }

    public void setEditBusinessUnit(BusinessUnit editBusinessUnit) {
        this.editBusinessUnit = editBusinessUnit;
    }

    public ReportingUnit getEditReportingUnit() {
        return editReportingUnit;
    }

    public void setEditReportingUnit(ReportingUnit editReportingUnit) {
        this.editReportingUnit = editReportingUnit;
    }

    public User getEditUser() {
        return editUser;
    }

    public void setEditUser(User editUser) {
        this.editUser = editUser;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public TreeNode getReportingUnitTreeNode() {
        return reportingUnitTreeNode;
    }

    public void setEditFinancialPeriod(FinancialPeriod editFinancialPeriod) {
        this.editFinancialPeriod = editFinancialPeriod;
    }

    public FinancialPeriod getEditFinancialPeriod() {
        return editFinancialPeriod;
    }

    public Holiday getEditHolidays() {
        return editHolidays;
    }

    public void setEditHolidays(Holiday editHolidays) {
        this.editHolidays = editHolidays;
    }

    public Company getEditCompany() {
        return editCompany;
    }

    public void setEditCompany(Company editCompany) {
        this.editCompany = editCompany;
    }

    public Contract getEditContract() {
        return editContract;
    }

    public void setEditContract(Contract editContract) {
        this.editContract = editContract;
    }

    public FinancialPeriod getCurrentPeriod() {
        return currentPeriod;
    }

    public void setCurrentPeriod(FinancialPeriod currentPeriod) {
        this.currentPeriod = currentPeriod;
    }

    public FinancialPeriod getPriorPeriod() {
        return priorPeriod;
    }

    public void setPriorPeriod(FinancialPeriod priorPeriod) {
        this.priorPeriod = priorPeriod;
    }

}
