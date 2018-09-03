/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.CalculationService;
import com.flowserve.system606.service.FinancialPeriodService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@RequestScoped
public class CalculateInitialDataSet implements Serializable {

    @Inject
    private AdminService adminService;
    @Inject
    private CalculationService calculationService;
    @Inject
    private FinancialPeriodService financialPeriodService;
    List<DataImportFile> dataImportFile = new ArrayList<DataImportFile>();
    private List<ReportingUnit> reportingUnitsToProcess = new ArrayList<ReportingUnit>();

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    @PostConstruct
    public void init() {
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("1100"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("5050"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("7866"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("8405"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("1205"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("8225"));
    }

    public String calcAllNov17() throws Exception {
        FinancialPeriod startPeriod = financialPeriodService.findById("NOV-17");

        // Full load of all RUs.  Comment this out and use specific RUs for local testing.
        Logger.getLogger(CalculateInitialDataSet.class.getName()).log(Level.INFO, "Calculating RUs from initial data set...");

        for (ReportingUnit reportingUnit : reportingUnitsToProcess) {
//            if (reportingUnit.isParent()) {
//                continue;
//            }
            try {
                logger.log(Level.INFO, "Calculating RU: " + reportingUnit.getCode() + " POB Count: " + reportingUnit.getPerformanceObligations().size());
                calculationService.calculateAndSave(reportingUnit, startPeriod);
                logger.log(Level.INFO, "Completed RU: " + reportingUnit.getCode());
            } catch (Exception e) {
            }
        }
        logger.log(Level.INFO, "calculateAllFinancials() completed.");

        return "calculateInitialDataSet";
    }

}
