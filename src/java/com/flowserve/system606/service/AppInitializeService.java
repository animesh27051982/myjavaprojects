/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.User;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 *
 * @author shubhamv
 */
@Singleton
@Startup
public class AppInitializeService {

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    User adminUser;

    @Inject
    private AdminService adminService;
    @Inject
    private CurrencyService currencyService;
    @Inject
    private FinancialPeriodService financialPeriodService;
    @Inject
    private MetricService metricService;
    @Inject
    private CalculationService calculationService;

    @PostConstruct
    public void init() {
        logger.info("Initializing App Objects");

        try {
            adminUser = adminService.initUsers();
            adminService.initCompanies();
            financialPeriodService.initFinancialPeriods();
            // TODO - KJG - The legacy file does not contain Oct 17 data but prior period lookups try to find it, so just use dummy rates for now.
            currencyService.initCurrencyConverter(financialPeriodService.findById("SEP-17"));
            currencyService.initCurrencyConverter(financialPeriodService.findById("OCT-17"));
            //currencyService.initCurrencyConverter(financialPeriodService.findById("MAY-18"));
            adminService.initSubledgerAccount();
            adminService.initBilings();
            metricService.initMetricTypes();
            adminService.initCountries();
            adminService.initBusinessUnit();
            adminService.initReportingUnits();
            adminService.initBUinRU();
            adminService.initCoEtoParentRU();
            adminService.initCompaniesInRUs();
            adminService.initPreparersReviewerForRU();
            adminService.initPreparersReviewerForCOE();
            adminService.initReportingUnitWorkflowStatus();
            calculationService.initBusinessRules();

            financialPeriodService.openPeriod(financialPeriodService.getCurrentFinancialPeriod());

            //calculationService.initBusinessRulesEngine();
            // Uncomment for local file based POB loading current month only.
            //contractService.initContracts();
            //pobService.initPOBs();
            //currencyService.initCurrencyConverter(financialPeriodService.findById("APR-18"));
            // end
        } catch (Exception ex) {
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.SEVERE, null, ex);
        }

        logger.info("Initializing App Objects Done");
    }

    public User getAdminUser() {
        return adminUser;
    }

}
