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
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

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

    @EJB
    private AdminService adminService;
    @EJB
    private PerformanceObligationService pobService;
    @EJB
    private CurrencyService currencyService;
    @EJB
    private FinancialPeriodService financialPeriodService;
    @EJB
    private InputService inputService;
    @EJB
    private OutputService outputService;
    @EJB
    private BusinessRuleService businessRuleService;
    @EJB
    private ContractService contractService;
    @EJB
    private TemplateService templateService;

    @PostConstruct
    public void init() {
        logger.info("Initializing App Objects");

        try {
            adminService.initUsers();
            financialPeriodService.initFinancialPeriods();
            currencyService.initCurrencyConverter();
            inputService.initInputTypes();
            outputService.initOutputTypes();
            adminService.initCountries();    // We don't need this as an Entity.  Convert to standard Java object with converters.

            adminService.initReportingUnits();
            contractService.initContracts();
            pobService.initPOBs();

            businessRuleService.initBusinessRules();
            businessRuleService.initBusinessRulesEngine();
            adminService.initAssignPreparersForReportingUnit();
            // businessRuleService.executePOBCalculations(pob);// TODO - Remove
            //DroolsTest.execute();
        } catch (Exception ex) {
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.SEVERE, null, ex);
        }

        logger.info("Initializing App Objects Done");
    }

}
