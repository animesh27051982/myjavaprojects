package com.flowserve.system606.web;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.PerformanceObligationService;
import java.io.Serializable;
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

    private BusinessUnit editBusinessUnit;
    //private List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();  // this is temp for calculatin testing.
    private TreeNode reportingUnitTreeNode = new DefaultTreeNode();  // this is temp for calculatin testing.
    private ReportingUnit editReportingUnit;
    private User editUser;
    private Country country;
    private Holiday editHolidays;
    private Company editCompany;
    private Contract editContract;
    @Inject
    private PerformanceObligationService performanceObligationService;
    @Inject
    private AdminService adminService;
    private FinancialPeriod editFinancialPeriod;

    @PostConstruct
    public void init() {   // this is temp for calculatin testing.
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

}
