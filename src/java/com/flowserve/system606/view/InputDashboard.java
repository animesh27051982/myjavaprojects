/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.ReportingUnitService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.TreeNode;

/**
 *
 * @author kgraves
 */
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

    private List<ReportingUnit> reportingUnits;

    @PostConstruct
    public void init() {
        reportingUnits = adminService.getPreparableReportingUnits();
        //rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
    }

    public List<ReportingUnit> getReportingUnits() {
        return reportingUnits;
    }

    public int getReportingUnitCount() {
        return reportingUnits.size();
    }

    public int getContractCount() {
        List<Contract> contracts = new ArrayList<Contract>();
        reportingUnits.forEach(reportingUnit -> contracts.addAll(reportingUnit.getContracts()));
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
