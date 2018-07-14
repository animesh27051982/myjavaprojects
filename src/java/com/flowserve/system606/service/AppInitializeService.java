/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.User;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    private CurrencyService currencyService;
    @Inject
    private FinancialPeriodService financialPeriodService;
    @Inject
    private MetricService metricService;
    @Inject
    private CalculationService calculationService;
    @Inject
    private ContractService contractService;

    @PostConstruct
    public void init() {
        logger.info("Initializing App Objects");

        try {
            adminService.initUsers();
            financialPeriodService.initFinancialPeriods();
            adminService.initCompanies();
            currencyService.initCurrencyConverter();
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
            calculationService.initBusinessRulesEngine();

            // businessRuleService.executePOBCalculations(pob);// TODO - Remove
            //DroolsTest.execute();
        } catch (Exception ex) {
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.SEVERE, null, ex);
        }

        logger.info("Initializing App Objects Done");
    }

    public void initContract(String msAccDB) {
        Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Processing initContract: " + msAccDB);
 
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
 
        // Step 1: Loading or registering Oracle JDBC driver class
        try {
 
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        }
        catch(ClassNotFoundException cnfex) {
 
            System.out.println("Problem in loading or "
                    + "registering MS Access JDBC driver");
            cnfex.printStackTrace();
        }
 
        // Step 2: Opening database connection
        try {
 
            String dbURL = "jdbc:ucanaccess://" + msAccDB; 
 
            // Step 2.A: Create and get connection using DriverManager class
            connection = DriverManager.getConnection(dbURL); 
 
            // Step 2.B: Creating JDBC Statement 
            statement = connection.createStatement();
 
            // Step 2.C: Executing SQL &amp; retrieve data into ResultSet
            resultSet = statement.executeQuery("SELECT * FROM Contract");
 
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "ID\tTemplate\t\t\tName\tStage");
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "==\t================\t===\t=======");
 
            // processing returned data and printing into console
            while(resultSet.next()) {
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, resultSet.getInt(1) + "\t" + 
                        resultSet.getString(2) + "\t" + 
                        resultSet.getString(3) + "\t" +
                        resultSet.getString(4));
            }
        }
        catch(SQLException sqlex){
            sqlex.printStackTrace();
        }
        finally {
 
            // Step 3: Closing database connection
            try {
                if(null != connection) {
 
                    // cleanup resources, once after processing
                    resultSet.close();
                    statement.close();
 
                    // and then finally close connection
                    connection.close();
                }
            }
            catch (SQLException sqlex) {
                sqlex.printStackTrace();
            }
        }
    }

}
