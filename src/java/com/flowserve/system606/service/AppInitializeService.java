/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.User;
import java.util.List;
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

    List<User> admin = null;
    User ad;

    @Inject
    private AdminService adminService;
    @Inject
    private PerformanceObligationService pobService;
    @Inject
    private ContractService contractService;
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
            adminService.initUsers();
            financialPeriodService.initFinancialPeriods();
            adminService.initCompanies();
            currencyService.initCurrencyConverter();
            adminService.initBilings();
            metricService.initMetricTypes();
            adminService.initCountries();    // We don't need this as an Entity.  Convert to standard Java object with converters.
            adminService.initBusinessUnit();
            adminService.initReportingUnits();
            adminService.initBUinRU();
            adminService.initCoEtoParentRU();
            adminService.initCompaniesInRUs();
            adminService.initPreparersReviewerForRU();
            contractService.initContracts();
            pobService.initPOBs();

            calculationService.initBusinessRules();
            //calculationService.initBusinessRulesEngine();

            // businessRuleService.executePOBCalculations(pob);// TODO - Remove
            //DroolsTest.execute();
        } catch (Exception ex) {
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.SEVERE, null, ex);
        }

        logger.info("Initializing App Objects Done");
    }

}
