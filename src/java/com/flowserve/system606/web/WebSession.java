package com.flowserve.system606.web;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.AppInitializeService;
import com.flowserve.system606.service.FinancialPeriodService;
import com.flowserve.system606.service.ReportingUnitService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author kgraves
 */
@Named
@SessionScoped
public class WebSession implements Serializable {

    @Inject
    private FinancialPeriodService financialPeriodService;
    @Inject
    private ReportingUnitService reportingUnitService;
    @Inject
    private AdminService adminService;
    @Inject
    private AppInitializeService appInitializeService;
    private BusinessUnit editBusinessUnit;
    //private TreeNode reportingUnitTreeNode = new DefaultTreeNode();  // this is temp for calculatin testing.
    private ReportingUnit editReportingUnit;
    private User editUser;
    private Country country;
    private Holiday editHolidays;
    private Company editCompany;
    private Contract editContract;
    private FinancialPeriod editFinancialPeriod;
    private FinancialPeriod currentPeriod;
    private FinancialPeriod priorPeriod;
    //private Long currentReportingUnitId;
    private DataImportFile dataImportFile;
    private String filterText;
    private Contract[] selectedContracts;
    //private ReportingUnit adminReportingUnit;
    private ReportingUnit currentReportingUnit;

    // The currently logged in user.
    private User user;

    @PostConstruct
    public void init() {
        currentPeriod = financialPeriodService.getCurrentFinancialPeriod();
        priorPeriod = financialPeriodService.getPriorFinancialPeriod();
        Logger.getLogger(WebSession.class.getName()).log(Level.INFO, "WebSession current: " + currentPeriod.getId());
        Logger.getLogger(WebSession.class.getName()).log(Level.INFO, "WebSession prior: " + priorPeriod.getId());

        user = appInitializeService.getAdminUser();  // For now, may be overridden later by login.  Remove this call.
    }

    public boolean isAdmin() {
        return user.isAdmin();
    }

    public void switchPeriod(FinancialPeriod newCurrentPeriod) {
        currentPeriod = newCurrentPeriod;
        priorPeriod = currentPeriod.getPriorPeriod();
        //priorPeriod = financialPeriodService.calculatePriorPeriod(currentPeriod);
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

//    public TreeNode getReportingUnitTreeNode() {
//        return reportingUnitTreeNode;
//    }
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

//    public void setCurrentReportingUnitId(Long reportingUnitId) {
//        this.currentReportingUnitId = reportingUnitId;
//    }
//
//    public Long getCurrentReportingUnitId() {
//        return currentReportingUnitId;
//    }
    public DataImportFile getDataImportFile() {
        return dataImportFile;
    }

    public void setDataImportFile(DataImportFile dataImportFile) {
        this.dataImportFile = dataImportFile;
    }

    public String getFilterText() {
        return filterText;
    }

    public void setFilterText(String filterText) {
        this.filterText = filterText;
    }

    public Contract[] getSelectedContracts() {
        return selectedContracts;
    }

    public void setSelectedContracts(Contract[] selectedContracts) {
        this.selectedContracts = selectedContracts;
    }

    public User getUser() {
        return user;
    }

    public String getUsername() {
        if (user == null) {
            return "Default Admin";
        }

        return user.getName();
    }

    public String getUserTitle() {
        if (user == null || user.getTitle() == null) {
            return "";
        }

        return user.getTitle();
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<ReportingUnit> getPreparableReportingUnits() {
        List<ReportingUnit> rus = new ArrayList<ReportingUnit>();

        if (user.isAdmin()) {
            if (currentReportingUnit != null) {
                rus.add(currentReportingUnit);
            }
            return rus;
        }

        for (ReportingUnit ru : reportingUnitService.getPreparableReportingUnits(user)) {
            if (ru.isParent()) {
                rus.addAll(ru.getChildReportingUnits());
            } else {
                rus.add(ru);
            }
        }

        return rus;
    }

//    public void setAdminReportingUnit(ReportingUnit adminReportingUnit) {
//        if (adminReportingUnit != null) {
//            Logger.getLogger(WebSession.class.getName()).log(Level.INFO, "Setting Admin RU: " + adminReportingUnit.getCode());
//        }
//        this.adminReportingUnit = adminReportingUnit;
//    }
    public ReportingUnit getCurrentReportingUnit() {
        return currentReportingUnit;
    }

    public void setCurrentReportingUnit(ReportingUnit currentReportingUnit) {
        this.currentReportingUnit = currentReportingUnit;
    }

}
