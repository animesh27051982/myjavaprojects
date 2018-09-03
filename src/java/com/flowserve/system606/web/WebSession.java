package com.flowserve.system606.web;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AppInitializeService;
import com.flowserve.system606.service.FinancialPeriodService;
import com.flowserve.system606.service.ReportingUnitService;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
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
    private AppInitializeService appInitializeService;
    private BusinessUnit editBusinessUnit;
    private ReportingUnit editReportingUnit;
    private User editUser;
    private Country country;
    private Holiday editHolidays;
    private Company editCompany;
    private Contract editContract;
    private String editFinancialPeriodId;
    private FinancialPeriod currentPeriod;
    private FinancialPeriod priorPeriod;
    private DataImportFile dataImportFile;
    private String filterText;
    private Contract[] selectedContracts;
    private ReportingUnit currentReportingUnit;
    private MetricType metricType;
    private Set<ReportingUnit> preparableReportingUnits = new TreeSet<ReportingUnit>();
    private Set<Long> expandedContractIds = new HashSet<Long>();

    // The currently logged in user.
    private User user;

    @PostConstruct
    public void init() {
        currentPeriod = financialPeriodService.getCurrentFinancialPeriod();
        priorPeriod = financialPeriodService.getPriorFinancialPeriod();

        if (user == null) {
            user = appInitializeService.getAdminUser();
        }

        preparableReportingUnits = new TreeSet<ReportingUnit>();

        if (user.isAdmin()) {
            if (currentReportingUnit != null) {
                preparableReportingUnits.add(currentReportingUnit);
            }
        } else {
            for (ReportingUnit ru : reportingUnitService.getPreparableReportingUnits(user)) {
                if (ru.isParent()) {
                    preparableReportingUnits.add(ru);
                    preparableReportingUnits.addAll(ru.getChildReportingUnits());
                } else {
                    preparableReportingUnits.add(ru);
                }
            }
        }
    }

    public boolean isAdmin() {
        return user.isAdmin();
    }

    public void switchPeriod(FinancialPeriod newCurrentPeriod) {
        currentPeriod = newCurrentPeriod;
        priorPeriod = currentPeriod.getPriorPeriod();
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

    public Collection<ReportingUnit> getPreparableReportingUnits() {
        return preparableReportingUnits;
    }

    public ReportingUnit getCurrentReportingUnit() {
        return currentReportingUnit;
    }

    public void setCurrentReportingUnit(ReportingUnit currentReportingUnit) {
        this.currentReportingUnit = currentReportingUnit;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public String getEditFinancialPeriodId() {
        return editFinancialPeriodId;
    }

    public void setEditFinancialPeriodId(String editFinancialPeriodId) {
        this.editFinancialPeriodId = editFinancialPeriodId;
    }

    public Set<Long> getExpandedContractIds() {
        return expandedContractIds;
    }

}
