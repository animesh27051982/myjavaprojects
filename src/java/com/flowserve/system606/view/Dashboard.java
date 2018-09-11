/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.WorkflowStatus;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.FinancialPeriodService;
import com.flowserve.system606.service.ReportingUnitService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections4.ListUtils;
import org.jboss.weld.util.collections.Sets;
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
    @Inject
    private WebSession webSession;
    @Inject
    private ReportingUnitService reportingUnitService;
    @Inject
    private ViewSupport viewSupport;
    private Set<ReportingUnit> relevantReportingUnits = new TreeSet<ReportingUnit>();

    @PostConstruct
    public void init() {
        relevantReportingUnits.clear();

        for (ReportingUnit ru : reportingUnitService.getViewableReportingUnits(webSession.getUser())) {
            relevantReportingUnits.add(ru);
        }
        for (ReportingUnit ru : reportingUnitService.getPreparableReportingUnits(webSession.getUser())) {
            relevantReportingUnits.add(ru);
        }
        for (ReportingUnit ru : reportingUnitService.getReviewableReportingUnits(webSession.getUser())) {
            relevantReportingUnits.add(ru);
        }
        for (ReportingUnit ru : reportingUnitService.getApprovableReportingUnits(webSession.getUser())) {
            relevantReportingUnits.add(ru);
        }

        if (webSession.getCurrentReportingUnit() != null) {
            relevantReportingUnits.add(webSession.getCurrentReportingUnit());
        }
    }

    public List<ReportingUnit> getRelevantReportingUnits() {
        List<ReportingUnit> ruc = new ArrayList<ReportingUnit>();
        List<ReportingUnit> rus = new ArrayList<ReportingUnit>();

        for (ReportingUnit ru : relevantReportingUnits) {
            if (ru.getContractCount() != 0) {

                ruc.add(ru);

            } else {
                rus.add(ru);
            }
        }
        // List<ReportingUnit> sortruc = ruc.stream().collect(Collectors.toList());
        Collections.sort(ruc, (ReportingUnit o1, ReportingUnit o2) -> o1.getCode().compareTo(o2.getCode()));

        // List<ReportingUnit> sortrus = rus.stream().collect(Collectors.toList());
        Collections.sort(rus, (ReportingUnit o1, ReportingUnit o2) -> o1.getCode().compareTo(o2.getCode()));

//        ruc = new TreeSet<ReportingUnit>(sortruc);
//        rus = new TreeSet<ReportingUnit>(sortrus);
        List<ReportingUnit> combinedSet = ListUtils.union(ruc, rus);
        //Set<ReportingUnit> combinedSet = Sets.union(ruc, rus);
        return combinedSet;
    }

    public void onReportingUnitSelect(SelectEvent event) {
        webSession.setFilterText(null);
        init();
    }

    public void clearReportingUnit(SelectEvent event) {
        webSession.setFilterText(null);
        webSession.setCurrentReportingUnit(null);
        init();
    }

    public WorkflowStatus getWorkflowStatus(ReportingUnit reportingUnit) {
        return reportingUnit.getWorkflowStatus(webSession.getCurrentPeriod());
    }

    public int getContractCount(ReportingUnit reportingUnit) {
        return reportingUnit.getContracts().size();
    }

    public long getPobCount(ReportingUnit reportingUnit) {
        return reportingUnit.getPobCount();
    }

    public long getPobInputRequiredCount(ReportingUnit reportingUnit) {
        return reportingUnit.getPobInputRequiredCount();

    }

}
