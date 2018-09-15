/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.FinancialPeriodService;
import com.flowserve.system606.service.LoadTestService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;


/**
 *
 * @author user
 */
@Named(value = "calculateParallelDataset")
@RequestScoped
public class CalculateParallelDataset {

    /**
     * Creates a new instance of CalculateParallelDataset
     */
    
    @Inject
    private AdminService adminService;

    @Inject
    private FinancialPeriodService financialPeriodService;
    @Inject
    private LoadTestService loadTestService;
    private List<ReportingUnit> reportingUnitsToProcess = new ArrayList<ReportingUnit>();

    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    
    public CalculateParallelDataset()
    {
    }
    
    @PostConstruct
    public void init()
    {
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("1100"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("1200"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("5050"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("7866"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("8405"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("1205"));
        reportingUnitsToProcess.add(adminService.findReportingUnitByCode("8225"));
    }
    
     public String calcAllNov17Parallel() throws Exception
     {
        Instant start = Instant.now();
        
        LocalDateTime currentDate = LocalDateTime.now();
        logger.log(Level.INFO,"Start at :" + currentDate);
        logger.log(Level.INFO,"Start at :" + currentDate.getHour());
        FinancialPeriod startPeriod = financialPeriodService.findById("NOV-17");
        for (ReportingUnit reportingUnit : reportingUnitsToProcess)
        { //adminService.findAllReportingUnits()

            try
            {
                logger.log(Level.INFO, "Calculating RU: " + reportingUnit.getCode() + " POB Count: " + reportingUnit.getPerformanceObligations().size());
                loadTestService.calculateParallel(reportingUnit, startPeriod);

                logger.log(Level.INFO, "Completed RU: " + reportingUnit.getCode());
            } 
            catch (Exception e)
            {
                Logger.getLogger(CalculateParallelDataset.class.getName()).log(Level.INFO, "Error calculating", e);
            }
        }
        
        Instant end = Instant.now();
        Duration interval = Duration.between(start, end);
        int min = (int) (interval.getSeconds() / 60);
        int sec = (int) (interval.getSeconds() - (min * 60));
        logger.log(Level.INFO, "calculateAllFinancials() completed in MIN : " + min + " SEC : " + sec);
        
        return "calculateParallelDataSet";

    }
    
}
