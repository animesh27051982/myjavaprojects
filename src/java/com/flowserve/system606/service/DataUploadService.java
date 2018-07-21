/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author shubhamv
 */
@Stateless
public class DataUploadService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    @Inject
    private ContractService contractService;
    @Inject
    private PerformanceObligationService pobService;
    @Inject
    private AdminService adminService;
    @Inject
    private CalculationService calculationService;
    @Inject
    private FinancialPeriodService financialPeriodService;

    private Map<String, String> methodMap = new HashMap<String, String>();

    public void persist(Object object) {
        em.persist(object);
    }

    public void processUploadedCalculationData(String msAccDB) throws Exception {
        Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, "Processing POCI Data: " + msAccDB);

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
            resultSet = statement.executeQuery("SELECT Period,`POb NAME (TAB NAME)`,`Transaction Price/Changes to Trans Price (excl LDs)`,`Estimated at Completion (EAC)/ Changes to EAC (excl TPCs)`,`Cumulative Costs Incurred`,`Liquidated Damages (LDs)/Changes to LDs` FROM `tbl_POCI_1_POb Changes`");

            Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, "Period\tPOCC File Name\tC Page Number\tReporting Unit Number");
            Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, "==\t================\t===\t=======");

            // processing returned data and printing into console
            while (resultSet.next()) {
//                Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, resultSet.getString(1) + "\t"
//                        + resultSet.getString(2) + "\t"
//                        + resultSet.getBigDecimal(3)+ "\t"
//                        + resultSet.getBigDecimal(4)+ "\t"
//                        + resultSet.getBigDecimal(5)+ "\t"
//                        + resultSet.getBigDecimal(6)+ "\t"
//                );

                if (resultSet.getString(1).equalsIgnoreCase("2018-5")) {
                    FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();
                    String id = resultSet.getString(2);
                    if (id != null) {
                        String[] sp = id.split("-");
                        String lastId = sp[sp.length - 1].trim();
                        try {
                            // checking valid integer using parseInt() method
                            Integer.parseInt(lastId);
                            PerformanceObligation pob = pobService.findPerformanceObligationById(new Long(lastId));
                            if (pob != null) {
                                calculationService.getCurrencyMetric("TRANSACTION_PRICE_CC", pob, period).setValue(resultSet.getBigDecimal(3));
                                calculationService.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", pob, period).setValue(resultSet.getBigDecimal(4));
                                calculationService.getCurrencyMetric("LOCAL_COSTS_ITD_LC", pob, period).setValue(resultSet.getBigDecimal(5));
                                calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", pob, period).setValue(resultSet.getBigDecimal(6));
//                                if (pob.getContract().getReportingUnit().getCode().equalsIgnoreCase("8025")) {
//                                    calculationService.executeBusinessRules(pob);
//                                }

                            } else {
                                Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, "These POBs not found : " + id);
                            }
                        } catch (NumberFormatException e) {
                            Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, "Error " + e);
                        }
                    }
                } else {
                    Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, "Other Financial Period : " + resultSet.getString(1));
                }
            }
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
        } finally {

            // Step 3: Closing database connection
            try {
                if (null != connection) {

                    // cleanup resources, once after processing
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

    public void initContract(String msAccDB) throws Exception {
        Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Processing initContract: " + msAccDB);

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        try {
            String dbURL = "jdbc:ucanaccess://" + msAccDB;
            connection = DriverManager.getConnection(dbURL);
            statement = connection.createStatement();
            initContracts(connection, statement);
            initPOBs(connection, statement);
        } finally {
            try {
                if (null != connection) {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                    statement.close();
                    connection.close();
                }
            } catch (SQLException sqlex) {
                sqlex.printStackTrace();
            }
        }
    }

    void initPOBs(Connection connection, Statement statement) throws SQLException, Exception {
        ResultSet resultSet = null;

        methodMap.put("POC/Cost-to-Cost", "POC");
        methodMap.put("Not Over-Time", "NOT");
        methodMap.put("Straight-line", "SL");
        methodMap.put("Right to Invoice", "RTI");

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

        //Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "ID\tCustomer Name\t\tStage\t\tFolders-RU\tName of POb\tRevenue recognition method\tContract ID");
        //Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "==\t================\t===\t=======t=======");
        while (resultSet.next()) {
//            Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, resultSet.getInt(1) + "\t"
//                    + resultSet.getString(2) + "\t"
//                    + resultSet.getString(3) + "\t"
//                    + resultSet.getString(4) + "\t"
//                    + resultSet.getString(5) + "\t"
//                    + resultSet.getString(6) + "\t"
//                    + resultSet.getString(7));

            PerformanceObligation pob = new PerformanceObligation();
            pobId = resultSet.getInt(1);

            if (pobService.findById(pobId) != null) {
                continue;
            }
            customerName = resultSet.getString(2);
            pobName = resultSet.getString(5);
            revRecMethod = resultSet.getString(6);

            contractId = resultSet.getInt(7);

            Contract contract = contractService.findContractById(contractId);

            if (contract == null) {
                //throw new IllegalStateException("POB refers to non-existent contract.  Invalid.  POB ID: " + pobId);
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.WARNING, "POB refers to non-existent contract.  Invalid.  POB ID: " + pobId + " contract Id: " + contractId);
                continue;
            } else {
                pob.setContract(contract);
            }
            pob.setName(pobName);
            pob.setId(pobId);
            if (methodMap.get(revRecMethod) == null) {
                Logger.getLogger(DataUploadService.class.getName()).log(Level.SEVERE, "POB revrec method not in our list: " + revRecMethod);
            }
            pob.setRevRecMethod(methodMap.get(revRecMethod));

            pob = pobService.update(pob);
            contract.getPerformanceObligations().add(pob);
            contractService.update(contract);

            count++;
            if ((count % 100) == 0) {
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Contract import count: " + count);
            }
        }
        resultSet.close();
    }

    void initContracts(Connection connection, Statement statement) throws SQLException, Exception {
        ResultSet resultSet = null;

//        try {
        String ru = null;
        long contractId = 0;
        String contractName = null;
        String salesOrderNumber = null;
        String contractCurrencyCode = null;

        int count = 0;
        String line = null;

        resultSet = statement.executeQuery("SELECT ID, Name, `BPC Reporting Unit`, `Sales Order #`, `Contract Currency` FROM tbl_Contracts");

        //Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "ID\t\t\tName\tRU\tSales Order #\tCC");
        //Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "==\t================\t===\t=======\t=====");
        // processing returned data and printing into console
        while (resultSet.next()) {
//            Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, resultSet.getInt(1) + "\t"
//                    + resultSet.getString(2) + "\t"
//                    + resultSet.getString(3) + "\t"
//                    + resultSet.getString(4) + "\t"
//                    + resultSet.getString(5));

            contractId = resultSet.getInt(1);
            if (contractService.findContractById(contractId) != null) {
                continue;  // we've already processed this contract.  dont' process the repeated lines.
            }
            contractName = resultSet.getString(2);
            String ruStr = StringUtils.substringBefore(resultSet.getString(3).trim(), "-");
            ru = ruStr.replace("RU", "").trim();
            //Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "RU:\t" + ru);
            salesOrderNumber = resultSet.getString(4);
            contractCurrencyCode = resultSet.getString(5);

            //same code logic when intialContractUpload
            Contract contract = new Contract();
            contract.setId(contractId);
            contract.setName(contractName);
            contract.setSalesOrderNumber(salesOrderNumber);
            if (contractCurrencyCode != null && !contractCurrencyCode.isEmpty()) {
                contract.setContractCurrency(Currency.getInstance(contractCurrencyCode));
            } else {
                Logger.getLogger(DataUploadService.class.getName()).log(Level.SEVERE, "Contract currency not found for contract: " + contractId + " code: " + contractCurrencyCode);
                continue;
            }

            ReportingUnit reportingUnit = adminService.findReportingUnitByCode(ru);

            if (contract == null) {
                throw new IllegalStateException("Countract refers to a non-existent RU.  Invalid.");
            }

            if (reportingUnit != null) {
                contract.setReportingUnit(reportingUnit);
            }
            contract = contractService.update(contract);   // this gives us the JPA managed object.
            if (reportingUnit != null) {
                reportingUnit.getContracts().add(contract);
                adminService.update(reportingUnit);
            }

            count++;

            if ((count % 100) == 0) {
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Contract import count: " + count);
            }
        }
        resultSet.close();
    }

}
