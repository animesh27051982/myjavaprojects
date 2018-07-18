/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Currency;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import org.apache.commons.lang.StringUtils;

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
            adminService.initBilings();
            metricService.initMetricTypes();
            adminService.initCountries();    // We don't need this as an Entity.  Convert to standard Java object with converters.
            adminService.initBusinessUnit();
            adminService.initReportingUnits();
            adminService.initBUinRU();
            adminService.initCoEtoParentRU();
            adminService.initCompaniesInRUs();
            adminService.initPreparersReviewerForRU();
//            contractService.initContracts();
//            pobService.initPOBs();

            calculationService.initBusinessRules();
            //calculationService.initBusinessRulesEngine();

            // businessRuleService.executePOBCalculations(pob);// TODO - Remove
            //DroolsTest.execute();
        } catch (Exception ex) {
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.SEVERE, null, ex);
        }

        logger.info("Initializing App Objects Done");
    }

    public void initContract(String msAccDB) throws Exception {
        Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Processing initContract: " + msAccDB);

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        // Step 1: Loading or registering Oracle JDBC driver class
        try {

            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        } catch (ClassNotFoundException cnfex) {

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

            initContracts(connection, statement);
            initPOBs(connection, statement);
   
        }
        catch(SQLException sqlex){
            sqlex.printStackTrace();
        } finally {

            // Step 3: Closing database connection
            try {
                if (null != connection) {

                    // cleanup resources, once after processing
                    if(resultSet != null)
                        resultSet.close();
                    statement.close();

                    // and then finally close connection
                    connection.close();
                }
            } catch (SQLException sqlex) {
                sqlex.printStackTrace();
            }
        }
    }
    
    void initPOBs(Connection connection, Statement statement) throws SQLException, Exception {
        ResultSet resultSet = null;

//        try {    
            String platform = null;
            String ru = null;
            long contractId = -1;
            String customerName = null;
            String salesOrderNumber = null;
            String pobName = null;
            long pobId = -1;
            String revRecMethod = null;

            int count = 0;
            String line = null;
            
            resultSet = statement.executeQuery("SELECT ID, Name, Stage, Folders, `Name of POb`, `If OT, identify the revenue recognition method`, `C-Page ID` FROM tbl_POb");
 
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "ID\tCustomer Name\t\tStage\t\tFolders-RU\tName of POb\tRevenue recognition method\tContract ID");
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "==\t================\t===\t=======t=======");
 
            // processing returned data and printing into console
            while(resultSet.next()) {
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, resultSet.getInt(1) + "\t" + 
                        resultSet.getString(2) + "\t" + 
                        resultSet.getString(3) + "\t" +
                        resultSet.getString(4) + "\t" +
                        resultSet.getString(5) + "\t" +
                        resultSet.getString(6) + "\t" +
                        resultSet.getString(7));


                PerformanceObligation pob = new PerformanceObligation();
                pobId = resultSet.getInt(1);
                customerName = resultSet.getString(2);
                pobName = resultSet.getString(5);
                revRecMethod = resultSet.getString(6);

                contractId = resultSet.getInt(7);


                Contract contract = contractService.findContractById(contractId);
//
//                if (contract == null) {
//                    throw new IllegalStateException("POB refers to non-existent contract.  Invalid.  POB ID: " + pobId);
//                }

                if (contract == null) {
                    contract = new Contract();
                    contract.setId(contractId);
                    contract.setName(customerName + '-' + contractId);
                    contract.setSalesOrderNumber(salesOrderNumber);
                }

                pob.setContract(contract);
                pob.setName(pobName);
                pob.setId(pobId);
                pob.setRevRecMethod(revRecMethod);

//                //KJG
                pob = pobService.update(pob);
//                contract.getPerformanceObligations().add(pob);
//                contractService.update(contract);

                count++;
                if(count > 100) //only do 100 for now
                    break;
            }   
            resultSet.close();
            

//        }            
    }
    
    
    void initContracts(Connection connection, Statement statement) throws SQLException, Exception {
        ResultSet resultSet = null;

//        try {    
            String ru = null;
            long contractId = -1;
            String contractName = null;
            String salesOrderNumber = null;
            String contractCurrencyCode = null;

            int count = 0;
            String line = null;
            
            resultSet = statement.executeQuery("SELECT ID, Name, `BPC Reporting Unit`, `Sales Order #`, `Contract Currency` FROM tbl_Contracts");
 
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "ID\t\t\tName\tRU\tSales Order #\tCC");
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "==\t================\t===\t=======\t=====");
 
            // processing returned data and printing into console
            while(resultSet.next()) {
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, resultSet.getInt(1) + "\t" + 
                        resultSet.getString(2) + "\t" + 
                        resultSet.getString(3) + "\t" +
                        resultSet.getString(4) + "\t" +
                        resultSet.getString(5));
            

                
                contractId = resultSet.getInt(1);
                if (contractService.findContractById(contractId) != null) {
                    continue;  // we've already processed this contract.  dont' process the repeated lines.
                }
                contractName = resultSet.getString(2);
                String ruStr = StringUtils.substringBefore(resultSet.getString(3).trim(), "-");
                ru = ruStr.replace("RU", "").trim();
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "RU:\t" + ru);
                salesOrderNumber = resultSet.getString(4);
                contractCurrencyCode = resultSet.getString(5);

                //same code logic when intialContractUpload
                Contract contract = new Contract();
                contract.setId(contractId);
                contract.setName(contractName);
                contract.setSalesOrderNumber(salesOrderNumber);
                contract.setContractCurrency(Currency.getInstance(contractCurrencyCode));

                ReportingUnit reportingUnit = adminService.findReportingUnitByCode(ru);

                if (contract == null) {
                    throw new IllegalStateException("Countract refers to a non-existent RU.  Invalid.");
                }
                // KJG removing this code, we should never have a reporting unit that we are not prepared for.
                // We don't want to create here.  We'll have users create via front end and then reload.

//                if (reportingUnit == null) {
//                    reportingUnit = new ReportingUnit();
//                    reportingUnit.setCode(ru);
//                    reportingUnit.setId( (long)1100 ); //TODO: need remove after test
//                }
                if (reportingUnit != null)
                    contract.setReportingUnit(reportingUnit);
                //persist(contract);

                //update(contract);
                // KJG Adding code
                contract = contractService.update(contract);   // this gives us the JPA managed object.
                if (reportingUnit != null) {
                    reportingUnit.getContracts().add(contract);
                    adminService.update(reportingUnit);
                }

                count++;
                if(count > 100) //only do 100 for now
                    break;
            }   
            resultSet.close();
            
//        }            
    }    

}
