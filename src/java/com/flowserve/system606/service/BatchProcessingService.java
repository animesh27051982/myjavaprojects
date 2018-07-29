/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import org.apache.commons.lang.StringUtils;

/**
 * @author kgraves
 *
 * This EJB uses bean managed transactions due to high load batch processing
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BatchProcessingService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    @Inject
    private CalculationService calculationService;
    @Inject
    private FinancialPeriodService financialPeriodService;
    @Inject
    PerformanceObligationService performanceObligationService;
    @Inject
    private ContractService contractService;
    @Inject
    private AdminService adminService;
    @Resource
    private UserTransaction ut;

    private Map<String, String> methodMap = new HashMap<String, String>();

    @Asynchronous
    public void calculateAllFinancials(List<ReportingUnit> reportingUnits, FinancialPeriod period) throws Exception {

        for (ReportingUnit reportingUnit : reportingUnits) {

            if (reportingUnit.isParent()) {
                continue;
            }
            ut.begin();
            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Calculating RU: " + reportingUnit.getCode() + "...");

            FinancialPeriod calculationPeriod = period;
            do {
                calculationService.executeBusinessRulesAndSave((new ArrayList<Measurable>(reportingUnit.getPerformanceObligations())), calculationPeriod);
            } while ((calculationPeriod = financialPeriodService.calculateNextPeriodUntilCurrent(calculationPeriod)) != null);

            calculationPeriod = period;
            do {
                calculationService.executeBusinessRulesAndSave((new ArrayList<Measurable>(reportingUnit.getContracts())), calculationPeriod);
            } while ((calculationPeriod = financialPeriodService.calculateNextPeriodUntilCurrent(calculationPeriod)) != null);
            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Completed RU: " + reportingUnit.getCode());
            ut.commit();
            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Flushing EntityManager");
            em.flush();
            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Clearing EntityManager");
            em.clear();
        }
        Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "calculateAllFinancials() completed.");
    }

    @Asynchronous
    public void processUploadedCalculationData(String msAccDB, String fileName) throws Exception {

        // KJG - Remove this after UAT along with the deleteAllMetrics() method itself.
        Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Processing POCI Data: " + msAccDB);

        Connection connection = null;

        Statement statement = null;
        ResultSet resultSet = null;
        String exPeriod = null;
        DataImportFile dataImport = null;
        List<String> importMessages = new ArrayList<String>();

        Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");

        Set<ReportingUnit> rusToSave = new HashSet<ReportingUnit>();

        try {
            String dbURL = "jdbc:ucanaccess://" + msAccDB;
            connection = DriverManager.getConnection(dbURL);
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(
                    "SELECT Period,`POb NAME (TAB NAME)`,`Transaction Price/Changes to Trans Price (excl LDs)`,`Estimated at Completion (EAC)/ Changes to EAC (excl TPCs)`,"
                    + "`Cumulative Costs Incurred`,`Liquidated Damages (LDs)/Changes to LDs` FROM `tbl_POCI_1_POb Changes` ORDER BY Period");

            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Period\tPOCC File Name\tC Page Number\tReporting Unit Number");
            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "==\t================\t===\t=======");

            int count = 0;
            long timeInterval = System.currentTimeMillis();

            String[] monthName = {"JAN", "FEB",
                "MAR", "APR", "MAY", "JUN", "JUL",
                "AUG", "SEP", "OCT", "NOV",
                "DEC"};

            dataImport = new DataImportFile();

            ut.begin();

            while (resultSet.next()) {

                FinancialPeriod period = null;//financialPeriodService.getCurrentFinancialPeriod();
                String periodDate = resultSet.getString(1);
                String[] pd = periodDate.split("-");
                try {
                    // checking valid integer using parseInt() method
                    int year = Integer.parseInt(pd[0]);
                    int month = Integer.parseInt(pd[1]);
                    String yrStr = Integer.toString(year);
                    String finalYear = yrStr.substring(yrStr.length() - 2);
                    exPeriod = monthName[month - 1] + "-" + finalYear;
                    period = financialPeriodService.findById(exPeriod);
                } catch (NumberFormatException e) {
                    Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Error " + e);
                }

                if (period != null) {
                    String id = resultSet.getString(2);
                    if (id != null) {
                        String[] sp = id.split("-");
                        String lastId = sp[sp.length - 1].trim();
                        try {
                            BigDecimal tp = resultSet.getBigDecimal(3);
                            BigDecimal eac = resultSet.getBigDecimal(4);
                            BigDecimal costs = resultSet.getBigDecimal(5);
                            BigDecimal ld = resultSet.getBigDecimal(6);
                            // checking valid integer using parseInt() method
                            Integer.parseInt(lastId);
                            PerformanceObligation pob = performanceObligationService.findPerformanceObligationById(new Long(lastId));
                            if (pob != null) {
                                ReportingUnit ru = pob.getContract().getReportingUnit();
                                if (ru == null) {
                                    //String msg = "No local currency found for RU: " + ru.getCode();
                                    //importMSG.add(msg);
                                    Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "RU is null ");
                                    continue;
                                }
                                if (ru.getLocalCurrency() == null) {
                                    //String msg = "No local currency found for RU: " + ru.getCode();
                                    //importMSG.add(msg);
                                    Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "RU local currency is null ");
                                    continue;
                                }
                                //if (ru.getCode().equalsIgnoreCase("1015")) {
                                //if (ru.getCode().equalsIgnoreCase("1100") || ru.getCode().equalsIgnoreCase("1015") || ru.getCode().equalsIgnoreCase("8225") || ru.getCode().equalsIgnoreCase("5200")) {
                                if (true) {
                                    Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Populating POB metrics: " + pob.getId());
                                    calculationService.getCurrencyMetric("TRANSACTION_PRICE_CC", pob, period).setValue(tp);
                                    calculationService.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", pob, period).setValue(eac);
                                    calculationService.getCurrencyMetric("LOCAL_COSTS_ITD_LC", pob, period).setValue(costs);
                                    if (isGreaterThanZero(costs)) {
                                        calculationService.getCurrencyMetric("THIRD_PARTY_COSTS_ITD_LC", pob, period).setValue(BigDecimal.ZERO);
                                        calculationService.getCurrencyMetric("INTERCOMPANY_COSTS_ITD_LC", pob, period).setValue(BigDecimal.ZERO);
                                    }
                                    if (isGreaterThanZero(tp) && ld == null) {
                                        calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", pob, period).setValue(BigDecimal.ZERO);
                                    } else {
                                        calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", pob, period).setValue(ld);
                                    }
                                    //calculationService.executeBusinessRules(pob, period);
                                    //calculationService.executeBusinessRules(pob.getContract(), period);
                                    rusToSave.add(pob.getContract().getReportingUnit());
                                }
                            } else {
                                importMessages.add("These POBs not found : " + id);
                                //Logger.getLogger(BatchProcessingService.class.getName()).log(Level.FINER, "These POBs not found : " + id);
                            }
                        } catch (NumberFormatException e) {
                            importMessages.add("This is not a Valid POB ID : " + lastId);
                            //Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Error " + e);
                        }
                    }

                } else {
                    importMessages.add("Financial Period not available : " + exPeriod);
                    throw new IllegalStateException("Financial Period not available :" + exPeriod);

                }

                if ((count % 100) == 0) {
                    Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Processed " + count + " POCI import records");
                    Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Interval time: " + ((System.currentTimeMillis() - timeInterval)) / 1000);
                    timeInterval = System.currentTimeMillis();

                }
                count++;

            }

            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Saving RUs...");
            dataImport.setFilename(fileName + " - tbl_POCI_1_POb Changes");
            dataImport.setUploadDate(LocalDateTime.now());
            dataImport.setCompany(adminService.findCompanyById("FLS"));
            dataImport.setDataImportMessages(importMessages);
            adminService.persist(dataImport);
            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Import completed.");

            ut.commit();
            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Flushing EntityManager");
            em.flush();
            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Clearing EntityManager");
            em.clear();

        } catch (SQLException sqlex) {
            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Error processing input file: ", sqlex);
        } catch (Exception e) {
            Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Error processing input file: ", e);
            throw new Exception(e.getMessage());
        } finally {
            try {
                if (null != connection) {
                    resultSet.close();
                    statement.close();
                    connection.close();
                }
            } catch (SQLException sqlex) {
                sqlex.printStackTrace();
            }
        }

        Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "POCI import complete.");
    }

    private boolean isGreaterThanZero(BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) == 1) {
            return true;
        }

        return false;
    }

    public void processUploadedContractPobData(String msAccDB) throws Exception {
        Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Processing initContract: " + msAccDB);

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        try {
            String dbURL = "jdbc:ucanaccess://" + msAccDB;
            connection = DriverManager.getConnection(dbURL);
            statement = connection.createStatement();
            processContracts(connection, statement);
            processPobs(connection, statement);
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

        Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Flushing EntityManager");
        em.flush();
        Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Clearing EntityManager");
        em.clear();
        Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Contract and POB import completed.");
    }

    void processPobs(Connection connection, Statement statement) throws SQLException, Exception {
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

        resultSet = statement.executeQuery("SELECT ID, Name, Stage, Folders, `Name of POb`, `If OT, identify the revenue recognition method`, `C-Page ID` FROM tbl_POb");

        while (resultSet.next()) {

            PerformanceObligation pob = new PerformanceObligation();
            pobId = resultSet.getInt(1);

            if (performanceObligationService.findById(pobId) != null) {
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
                Logger.getLogger(BatchProcessingService.class.getName()).log(Level.SEVERE, "POB revrec method not in our list: " + revRecMethod);
            }
            pob.setRevRecMethod(methodMap.get(revRecMethod));

            ut.begin();
            //pobService.update(pob);  // update or persist?
            performanceObligationService.persist(pob);
            contract.getPerformanceObligations().add(pob);
            contractService.update(contract);
            ut.commit();

            count++;
            if ((count % 100) == 0) {
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Contract import count: " + count);
            }
        }
        resultSet.close();
        Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "POB imoprt complete.");
    }

    void processContracts(Connection connection, Statement statement) throws SQLException, Exception {
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

        while (resultSet.next()) {

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
                Logger.getLogger(BatchProcessingService.class.getName()).log(Level.SEVERE, "Contract currency not found for contract: " + contractId + " code: " + contractCurrencyCode);
                continue;
            }

            ReportingUnit reportingUnit = adminService.findReportingUnitByCode(ru);

            if (reportingUnit == null) {
                throw new IllegalStateException("Countract refers to a non-existent RU.  Invalid.");
            }

            if (reportingUnit != null) {
                contract.setReportingUnit(reportingUnit);
            }
            //contract = contractService.update(contract);   // this gives us the JPA managed object.
            ut.begin();
            contractService.persist(contract);   // persist or update?
            if (reportingUnit != null) {
                reportingUnit.getContracts().add(contract);
                adminService.update(reportingUnit);
            }
            ut.commit();

            count++;

            if ((count % 100) == 0) {
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Contract import count: " + count);
            }
        }

        resultSet.close();
        Logger.getLogger(BatchProcessingService.class.getName()).log(Level.INFO, "Contract import complete.");
    }

}