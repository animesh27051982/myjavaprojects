/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.FinancialPeriodService;
import com.flowserve.system606.service.ReportingUnitService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import org.glassfish.soteria.identitystores.annotation.Credentials;
import org.glassfish.soteria.identitystores.annotation.EmbeddedIdentityStoreDefinition;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;
import org.primefaces.model.TreeNode;

/**
 *
 * @author kgraves
 */
@CustomFormAuthenticationMechanismDefinition(
        loginToContinue = @LoginToContinue(
                loginPage = "/login.xhtml",
                errorPage = ""
        )
)
@EmbeddedIdentityStoreDefinition({
    @Credentials(callerName = "jp", password = "jp", groups = {"users"})
    ,
        @Credentials(callerName = "arjan", password = "secret3", groups = {"foo"})}
)
@Named
@ViewScoped
public class InputDashboard implements Serializable {

    private static final Logger logger = Logger.getLogger(InputDashboard.class.getName());

    private TreeNode rootTreeNode;
    @Inject
    private AdminService adminService;
    @Inject
    private WebSession webSession;
    @Inject
    private ReportingUnitService reportingUnitService;
    @Inject
    private ViewSupport viewSupport;
     @Inject
    private FinancialPeriodService financialPeriodService;

    private List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();
    private ScheduleModel eventModel;
    
    @PostConstruct
    public void init() {
        if (webSession.getCurrentReportingUnit() != null) {
            reportingUnits.clear();
            reportingUnits.add(webSession.getCurrentReportingUnit());
        }
        
        try {
            List<Holiday> holidays = adminService.findHolidayList();
            Company company = adminService.findCompanyById("FLS");
            LocalDate freeze=financialPeriodService.CalcInputFreezeWorkday(LocalDate.now(), holidays,company.getInputFreezeWorkday());
            LocalDate poci=financialPeriodService.CalcInputFreezeWorkday(LocalDate.now(), holidays,company.getPociDueWorkday());
            eventModel = new DefaultScheduleModel();
            for (Holiday holiday : holidays) {
                Date date = Date.from(holiday.getHolidayDate().atStartOfDay(ZoneOffset.UTC).toInstant());
                 eventModel.addEvent(new DefaultScheduleEvent(holiday.getName(), date, date, true)); 
            }
            Date Freezeday = Date.from(freeze.atStartOfDay(ZoneOffset.UTC).toInstant());
            eventModel.addEvent(new DefaultScheduleEvent("Input Freeze Day", Freezeday, Freezeday, true));
            
            Date Pociworkday = Date.from(poci.atStartOfDay(ZoneOffset.UTC).toInstant());
            eventModel.addEvent(new DefaultScheduleEvent("POCI Due Workday", Pociworkday, Pociworkday, true)); 
            
        } catch (Exception ex) {
            Logger.getLogger(Calendar.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

      public ScheduleModel getEventModel() {
        return eventModel;
    }
    public List<ReportingUnit> getReportingUnits() {
        return reportingUnits;
    }

    public void onReportingUnitSelect(SelectEvent event) {
        webSession.setCurrentReportingUnit((ReportingUnit) event.getObject());
        init();
    }

    public int getReportingUnitCount() {
        return reportingUnits.size();
    }

    public int getContractCount() {
        List<Contract> contracts = new ArrayList<Contract>();
        if (reportingUnits.size() > 0) {
            for (ReportingUnit reportingUnit : reportingUnits) {
                contracts.addAll(reportingUnit.getContracts());
            }
        }
        return contracts.size();
    }

    public long getPobCount() {
        long pobCount = 0;
        for (ReportingUnit reportingUnit : reportingUnits) {
            pobCount += reportingUnit.getPobCount();
        }

        return pobCount;
    }

    public long getPobInputRequiredCount() {
        long pobCount = 0;
        for (ReportingUnit reportingUnit : reportingUnits) {
            pobCount += reportingUnit.getPobInputRequiredCount();
        }

        return pobCount;

    }
}
