/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.BillingEvent;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.RevenueMethod;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
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
    private Map<String, RevenueMethod> methodMap = new HashMap<String, RevenueMethod>();
    private static Logger logger = Logger.getLogger(BatchProcessingService.class.getName());

    //@Asynchronous
    public void calculateAllFinancials(List<ReportingUnit> reportingUnits, FinancialPeriod period) throws Exception {

        for (ReportingUnit reportingUnit : reportingUnits) {

            if (reportingUnit.isParent()) {
                continue;
            }
            try {
                ut.begin();
                logger.log(Level.INFO, "Calculating RU: " + reportingUnit.getCode() + " POB Count: " + reportingUnit.getPerformanceObligations().size());

                FinancialPeriod calculationPeriod = period;
                do {
                    calculationService.executeBusinessRulesAndSave((new ArrayList<Measurable>(reportingUnit.getPerformanceObligations())), calculationPeriod);
                } while ((calculationPeriod = financialPeriodService.calculateNextPeriodUntilCurrent(calculationPeriod)) != null);

                calculationPeriod = period;
                do {
                    calculationService.executeBusinessRulesAndSave((new ArrayList<Measurable>(reportingUnit.getContracts())), calculationPeriod);
                } while ((calculationPeriod = financialPeriodService.calculateNextPeriodUntilCurrent(calculationPeriod)) != null);

                logger.log(Level.INFO, "Flushing and clearing EntityManager");
                em.flush();
                em.clear();
                logger.log(Level.INFO, "Completed RU: " + reportingUnit.getCode());
                ut.commit();
            } catch (Exception e) {
                ut.rollback();
            }
        }
        logger.log(Level.INFO, "calculateAllFinancials() completed.");
    }

    //@Asynchronous
    public void processUploadedCalculationData(String msAccDB, String fileName) {
        logger.log(Level.INFO, "Processing POCI/O Data: " + msAccDB);

        Connection connection = null;
        Statement statement = null;

        try {
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
            String dbURL = "jdbc:ucanaccess://" + msAccDB;
            connection = DriverManager.getConnection(dbURL);
            statement = connection.createStatement();
            logger.info("Processing POCI Core finance data");
            processPOCIData(connection, statement, fileName);
            logger.info("Processing POCI billing data");
            processBillingInfoFromPOCI(connection, statement, fileName);
            logger.info("Processing POCI Third Party Commission data.");
            processPOCIThirdPartyCommissions(connection, statement, fileName);
        } catch (Exception e) {
            logger.log(Level.INFO, "Error processing POCI/O data: ", e);
        } finally {
            try {
                if (null != connection) {
                    statement.close();
                    connection.close();
                }
            } catch (SQLException sqlex) {
                logger.log(Level.INFO, "Error closing Access DB connection.", sqlex);
            }
        }

        logger.log(Level.INFO, "POCI/O import completed.");
    }

    void processPOCIData(Connection connection, Statement statement, String fileName) throws SQLException, Exception {

        ResultSet resultSet = null;
        String exPeriod = null;
        DataImportFile dataImport = null;
        List<String> importMessages = new ArrayList<String>();

        Set<ReportingUnit> rusToSave = new HashSet<ReportingUnit>();

        resultSet = statement.executeQuery(
                "SELECT Period,`POb NAME (TAB NAME)`,`Transaction Price/Changes to Trans Price (excl LDs)`,`Estimated at Completion (EAC)/ Changes to EAC (excl TPCs)`,"
                + "`Cumulative Costs Incurred`,`Liquidated Damages (LDs)/Changes to LDs` FROM `tbl_POCI_POb Changes` ORDER BY Period");

        logger.log(Level.INFO, "Period\tPOCC File Name\tC Page Number\tReporting Unit Number");
        logger.log(Level.INFO, "==\t================\t===\t=======");

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
                logger.log(Level.INFO, "Error " + e);
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
                                logger.log(Level.INFO, "RU is null ");
                                continue;
                            }
                            if (ru.getLocalCurrency() == null) {
                                String msg = "No local currency found for RU: " + ru.getCode();
                                importMessages.add(msg);
                                logger.log(Level.INFO, "RU local currency is null for RU: " + ru.getCode());
                                continue;
                            }
                            if (true) {
                                logger.log(Level.FINER, "Populating POB metrics: " + pob.getId());
                                calculationService.getCurrencyMetric("TRANSACTION_PRICE_CC", pob, period).setValue(tp);
                                calculationService.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", pob, period).setValue(eac);
                                calculationService.getCurrencyMetric("LOCAL_COSTS_CTD_LC", pob, period).setValue(costs);
                                if (isGreaterThanZero(costs)) {
                                    calculationService.getCurrencyMetric("THIRD_PARTY_COSTS_CTD_LC", pob, period).setValue(BigDecimal.ZERO);
                                    calculationService.getCurrencyMetric("INTERCOMPANY_COSTS_CTD_LC", pob, period).setValue(BigDecimal.ZERO);
                                }
                                if (isGreaterThanZero(tp) && ld == null) {
                                    calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_CTD_CC", pob, period).setValue(BigDecimal.ZERO);
                                } else {
                                    calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_CTD_CC", pob, period).setValue(ld);
                                }
                                //calculationService.executeBusinessRules(pob, period);
                                //calculationService.executeBusinessRules(pob.getContract(), period);
                                rusToSave.add(pob.getContract().getReportingUnit());
                            }
                        } else {
                            importMessages.add("These POBs not found : " + id);
                            //logger.log(Level.FINER, "These POBs not found : " + id);
                        }
                    } catch (NumberFormatException e) {
                        importMessages.add("This is not a Valid POB ID : " + lastId);
                        //logger.log(Level.INFO, "Error " + e);
                    }
                }

            } else {
                ut.rollback();
                importMessages.add("Financial Period not available : " + exPeriod);
                throw new IllegalStateException("Financial Period not available :" + exPeriod);

            }

            if ((count % 100) == 0) {
                logger.log(Level.INFO, "Processed " + count + " POCI import records");
                timeInterval = System.currentTimeMillis();
            }
            if ((count % 1000) == 0) {
                logger.log(Level.INFO, "Flushing data to database...");
                em.flush();
                em.clear();
            }

            count++;

        }

        dataImport.setFilename(fileName + " - tbl_POCI_1_POb Changes");
        dataImport.setUploadDate(LocalDateTime.now());
        dataImport.setCompany(adminService.findCompanyById("FLS"));
        dataImport.setDataImportMessages(importMessages);
        dataImport.setType("POCI DATA");
        adminService.persist(dataImport);
        logger.log(Level.INFO, "Flushing data to database...");
        em.flush();
        em.clear();
        ut.commit();

    }

    void processPOCIThirdPartyCommissions(Connection connection, Statement statement, String fileName) throws SQLException, Exception {

        ResultSet resultSet = null;
        String exPeriod = null;
        DataImportFile dataImport = null;
        List<String> importMessages = new ArrayList<String>();

        Set<ReportingUnit> rusToSave = new HashSet<ReportingUnit>();

        resultSet = statement.executeQuery(
                "SELECT Period,`POb NAME (TAB NAME)`,`Third Party Commissions (\"TPC\")` FROM `tbl_POCI` ORDER BY Period");

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
                logger.log(Level.INFO, "Error " + e);
            }

            if (period != null) {
                String id = resultSet.getString(2);
                if (id != null) {
                    String[] sp = id.split("-");
                    String lastId = sp[sp.length - 1].trim();
                    try {
                        BigDecimal thirdParyComm = resultSet.getBigDecimal(3);
                        // checking valid integer using parseInt() method
                        Integer.parseInt(lastId);
                        PerformanceObligation pob = performanceObligationService.findPerformanceObligationById(new Long(lastId));

                        if (pob != null) {
                            ReportingUnit ru = pob.getContract().getReportingUnit();
                            if (ru == null) {
                                //String msg = "No local currency found for RU: " + ru.getCode();
                                //importMSG.add(msg);
                                logger.log(Level.INFO, "RU is null ");
                                continue;
                            }
                            if (ru.getLocalCurrency() == null) {
                                String msg = "No local currency found for RU: " + ru.getCode();
                                importMessages.add(msg);
                                logger.log(Level.INFO, "RU local currency is null for RU: " + ru.getCode());
                                continue;
                            }
                            if (true) {
                                logger.log(Level.FINER, "Populating Contract TPC: " + pob.getId());
                                calculationService.getCurrencyMetric("THIRD_PARTY_COMMISSION_CTD_LC", pob.getContract(), period).setValue(thirdParyComm);
                                rusToSave.add(pob.getContract().getReportingUnit());
                            }
                        } else {
                            importMessages.add("These POBs not found : " + id);
                        }
                    } catch (NumberFormatException e) {
                        importMessages.add("This is not a Valid POB ID : " + lastId);
                    }
                }

            } else {
                ut.rollback();
                importMessages.add("Financial Period not available : " + exPeriod);
                throw new IllegalStateException("Financial Period not available :" + exPeriod);

            }

            if ((count % 1000) == 0) {
                logger.log(Level.INFO, "Processed " + count + " POCI TPC records");
                timeInterval = System.currentTimeMillis();

            }
            count++;

        }

        dataImport.setFilename(fileName + " - tbl_POCI (TPC Data run)");
        dataImport.setUploadDate(LocalDateTime.now());
        dataImport.setCompany(adminService.findCompanyById("FLS"));
        dataImport.setDataImportMessages(importMessages);
        dataImport.setType("POCI TPC DATA");
        adminService.persist(dataImport);
        logger.log(Level.INFO, "Flushing data to database...");
        em.flush();
        em.clear();
        ut.commit();
    }

    void processBillingInfoFromPOCI(Connection connection, Statement statement, String fileName) throws SQLException, Exception {

        ResultSet resultSet = null;
        String exPeriod = null;
        DataImportFile dataImport = null;
        List<String> importMessages = new ArrayList<String>();

        resultSet = statement.executeQuery("SELECT Period,`C Page Number`,`Contract Billings`,`Local Billings`  FROM `tbl_POCI-Billings` ORDER BY Period");
        String[] monthName = {"JAN", "FEB",
            "MAR", "APR", "MAY", "JUN", "JUL",
            "AUG", "SEP", "OCT", "NOV",
            "DEC"};

        dataImport = new DataImportFile();

        ut.begin();

        while (resultSet.next()) {

            FinancialPeriod period = null;//financialPeriodService.getCurrentFinancialPeriod();
            int year = 0;
            int month = 0;
            String periodDate = resultSet.getString(1);
            String[] pd = periodDate.split("-");
            try {
                // checking valid integer using parseInt() method
                year = Integer.parseInt(pd[0]);
                month = Integer.parseInt(pd[1]);
                String yrStr = Integer.toString(year);
                String finalYear = yrStr.substring(yrStr.length() - 2);
                exPeriod = monthName[month - 1] + "-" + finalYear;
                period = financialPeriodService.findById(exPeriod);
            } catch (NumberFormatException e) {
                logger.log(Level.INFO, "Error " + e);
            }

            if (period != null) {
                int id = resultSet.getInt(2);
                if (id != 0) {
                    BigDecimal cc = resultSet.getBigDecimal(3);
                    BigDecimal lc = resultSet.getBigDecimal(4);
                    Contract contract = contractService.findContractById(new Long(id));
                    if (contract != null) {
                        BillingEvent bEvent = new BillingEvent();
                        bEvent.setAmountContractCurrency(cc);
                        bEvent.setAmountLocalCurrency(lc);
                        bEvent.setInvoiceNumber("Migrated");
                        bEvent.setContract(contract);
                        bEvent.setBillingDate(LocalDate.of(year, month, 1));
                        contract.getBillingEvents().add(bEvent);
                        contractService.update(contract);
                    } else {
                        importMessages.add("Contract not found when processing billing: " + id);
                        logger.log(Level.FINER, "These POBs not found : " + id);
                    }

                }

            } else {
                ut.rollback();
                importMessages.add("Financial Period not available : " + exPeriod);
                throw new IllegalStateException("Financial Period not available :" + exPeriod);

            }

        }

        dataImport.setFilename(fileName + " - tbl_POCI-Billings");
        dataImport.setUploadDate(LocalDateTime.now());
        dataImport.setCompany(adminService.findCompanyById("FLS"));
        dataImport.setDataImportMessages(importMessages);
        dataImport.setType("POCI DATA");
        adminService.persist(dataImport);
        logger.log(Level.INFO, "Flushing POCI data to database...");
        em.flush();
        em.clear();
        ut.commit();
        logger.log(Level.INFO, "POCI data Import completed.");
    }

    private boolean isGreaterThanZero(BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) == 1) {
            return true;
        }

        return false;
    }

    public void processUploadedContractPobData(String msAccDB, String fileName) throws Exception {
        Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Processing initContract: " + msAccDB);

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        try {
            String dbURL = "jdbc:ucanaccess://" + msAccDB;
            connection = DriverManager.getConnection(dbURL);
            statement = connection.createStatement();
            processContracts(connection, statement, fileName);
            processPobs(connection, statement, fileName);
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

        logger.log(Level.INFO, "Contract and POB import completed.");
    }

    void processPobs(Connection connection, Statement statement, String fileName) throws SQLException, Exception {
        ResultSet resultSet = null;

        methodMap.put("POC/Cost-to-Cost", RevenueMethod.PERC_OF_COMP);
        methodMap.put("Not Over-Time", RevenueMethod.POINT_IN_TIME);
        methodMap.put("Straight-line", RevenueMethod.STRAIGHT_LINE);
        methodMap.put("Right to Invoice", RevenueMethod.RIGHT_TO_INVOICE);

        String platform = null;
        String ru = null;
        long contractId = -1;
        String customerName = null;
        String salesOrderNumber = null;
        String pobName = null;
        long pobId = -1;
        String revRecMethod = null;

        DataImportFile dataImport = null;
        List<String> importMessages = new ArrayList<String>();
        dataImport = new DataImportFile();
        int count = 0;

        resultSet = statement.executeQuery("SELECT ID, Name, Stage, Folders, `Name of POb`, `If OT, identify the revenue recognition method`, `C-Page ID` FROM tbl_POb");
        ut.begin();
        while (resultSet.next()) {

            PerformanceObligation pob = new PerformanceObligation();
            pobId = resultSet.getInt(1);

            if (performanceObligationService.findById(pobId) != null) {
                continue;
            }
            customerName = resultSet.getString(2);
            pobName = pobId + "-" + resultSet.getString(2);
            revRecMethod = resultSet.getString(6);

            contractId = resultSet.getInt(7);

            Contract contract = contractService.findContractById(contractId);

            if (contract == null) {
                importMessages.add("POB refers to non-existent contract.  Invalid.  POB ID: " + pobId + " contract Id: " + contractId);
                //Logger.getLogger(AppInitializeService.class.getName()).log(Level.WARNING, "POB refers to non-existent contract.  Invalid.  POB ID: " + pobId + " contract Id: " + contractId);
                continue;
            } else {
                pob.setContract(contract);
            }
            pob.setName(pobName);
            pob.setId(pobId);
            if (methodMap.get(revRecMethod) == null) {
                importMessages.add("POB revrec method not in our list: " + revRecMethod);
                //logger.log(Level.SEVERE, "POB revrec method not in our list: " + revRecMethod);
            }
            pob.setRevenueMethod(methodMap.get(revRecMethod));

            //pobService.update(pob);  // update or persist?
            performanceObligationService.persist(pob);
            contract.getPerformanceObligations().add(pob);
            contractService.update(contract);

            count++;
            if ((count % 1000) == 0) {
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Contract import count: " + count);
            }
        }

        dataImport.setFilename(fileName + " - tbl_POb");
        dataImport.setUploadDate(LocalDateTime.now());
        dataImport.setCompany(adminService.findCompanyById("FLS"));
        dataImport.setDataImportMessages(importMessages);
        dataImport.setType("Contract and Pobs");
        adminService.persist(dataImport);
        logger.log(Level.INFO, "Flushing and clearing EntityManager");
        em.flush();
        em.clear();
        ut.commit();
        resultSet.close();
        logger.log(Level.INFO, "POB imoprt complete.");
    }

    void processContracts(Connection connection, Statement statement, String fileName) throws SQLException, Exception {
        ResultSet resultSet = null;

        String ru = null;
        long contractId = 0;
        String contractName = null;
        String salesOrderNumber = null;
        String contractCurrencyCode = null;
        DataImportFile dataImport = null;
        List<String> importMessages = new ArrayList<String>();
        dataImport = new DataImportFile();
        int count = 0;
        String line = null;

        resultSet = statement.executeQuery("SELECT ID, Name, `BPC Reporting Unit`, `Sales Order #`, `Contract Currency` FROM tbl_Contracts");
        ut.begin();
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
            contract.setActive(true);
            contract.setId(contractId);
            contract.setName(contractName);
            contract.setSalesOrderNumber(salesOrderNumber);
            if (contractCurrencyCode != null && !contractCurrencyCode.isEmpty()) {
                contract.setContractCurrency(Currency.getInstance(contractCurrencyCode));
            } else {
                importMessages.add("Contract currency not found for contract: " + contractId + " code: " + contractCurrencyCode);
                logger.log(Level.SEVERE, "Contract currency not found for contract: " + contractId + " code: " + contractCurrencyCode);
                continue;
            }

            ReportingUnit reportingUnit = adminService.findReportingUnitByCode(ru);

            if (reportingUnit == null) {
                importMessages.add("Countract refers to a non-existent RU : " + resultSet.getString(3));
                throw new IllegalStateException("Countract refers to a non-existent RU.  Invalid.");
            }

            if (reportingUnit != null) {
                contract.setReportingUnit(reportingUnit);
            }
            //contract = contractService.update(contract);   // this gives us the JPA managed object.

            contractService.persist(contract);   // persist or update?
            if (reportingUnit != null) {
                reportingUnit.getContracts().add(contract);
                adminService.update(reportingUnit);
            }

            count++;

            if ((count % 1000) == 0) {
                Logger.getLogger(AppInitializeService.class.getName()).log(Level.INFO, "Contract import count: " + count);
            }
        }
        dataImport.setFilename(fileName + " - tbl_Contracts");
        dataImport.setUploadDate(LocalDateTime.now());
        dataImport.setCompany(adminService.findCompanyById("FLS"));
        dataImport.setDataImportMessages(importMessages);
        dataImport.setType("Contract and Pobs");
        adminService.persist(dataImport);
        ut.commit();

        resultSet.close();
        logger.log(Level.INFO, "Contract import complete.");
    }

}
