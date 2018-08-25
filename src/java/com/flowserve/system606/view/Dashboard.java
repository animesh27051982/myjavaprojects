/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Company;
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
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;
import org.primefaces.model.TreeNode;

/**
 *
 * @author kgraves
 */
@Named
@ViewScoped
public class Dashboard implements Serializable {

    private static final Logger logger = Logger.getLogger(Dashboard.class.getName());

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
    private int contractCount = 0;
    private int pobRequiredCount = 0;
    private int pobCount = 0;

    @PostConstruct
    public void init() {
        reportingUnits.clear();
        // TODO - These could be stale if redeploy.  Reload every time instead of session.
        reportingUnits.addAll(webSession.getPreparableReportingUnits());

        try {
            List<Holiday> holidays = adminService.findHolidayList();
            Company company = adminService.findCompanyById("FLS");
            LocalDate freeze = financialPeriodService.calcInputFreezeWorkday(LocalDate.now(), holidays, company.getInputFreezeWorkday());
            LocalDate poci = financialPeriodService.calcInputFreezeWorkday(LocalDate.now(), holidays, company.getPociDueWorkday());
            eventModel = new DefaultScheduleModel();
            for (Holiday holiday : holidays) {
                Date date = Date.from(holiday.getHolidayDate().atStartOfDay(ZoneOffset.UTC).toInstant());
                eventModel.addEvent(new DefaultScheduleEvent(holiday.getName(), date, date, true));
            }
            Date Freezeday = Date.from(freeze.atStartOfDay(ZoneOffset.UTC).toInstant());
            eventModel.addEvent(new DefaultScheduleEvent("Input Freeze Day", Freezeday, Freezeday, true));

            Date Pociworkday = Date.from(poci.atStartOfDay(ZoneOffset.UTC).toInstant());
            eventModel.addEvent(new DefaultScheduleEvent("POCI Due Workday", Pociworkday, Pociworkday, true));

            if (reportingUnits.size() > 0) {
                for (ReportingUnit reportingUnit : reportingUnits) {
                    contractCount += reportingUnit.getContracts().size();
                }
            }

            for (ReportingUnit reportingUnit : reportingUnits) {
                pobRequiredCount += reportingUnit.getPobInputRequiredCount();
            }

            for (ReportingUnit reportingUnit : reportingUnits) {
                pobCount += reportingUnit.getPobCount();
            }

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
        Logger.getLogger(Dashboard.class.getName()).log(Level.INFO, "RU Select Dashboard");
        webSession.setFilterText(null);
        init();
    }

    public void clearReportingUnit(SelectEvent event) {
        webSession.setFilterText(null);
        webSession.setCurrentReportingUnit(null);
        init();
    }

    public int getReportingUnitCount() {
        return reportingUnits.size();
    }

    public int getContractCount() {
        return contractCount;
    }

    public long getPobCount() {
        return pobCount;
    }

    public long getPobInputRequiredCount() {
        return pobRequiredCount;

    }
}
