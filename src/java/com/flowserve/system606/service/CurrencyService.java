/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.CurrencyMetric;
import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.model.ExchangeRate;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.Metric;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author shubhamv
 */
@Stateless

public class CurrencyService {

    private static final Logger logger = Logger.getLogger(CurrencyService.class.getName());
    private static final int SCALE = 14;

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    @Inject
    FinancialPeriodService financialPeriodService;
    @Inject
    AdminService adminService;
    private static Map<String, ExchangeRate> exchangeRateCache = new HashMap<String, ExchangeRate>();

    public void convertCurrency(Metric metric, Measurable measurable, FinancialPeriod period) throws Exception {
        if (!(metric instanceof CurrencyMetric)) {
            return;
        }
        CurrencyMetric currencyMetric = (CurrencyMetric) metric;

        if (currencyMetric.getLcValue() == null && currencyMetric.getCcValue() == null) {
            return;
        }

        if (metric.getMetricType().getMetricCurrencyType() == null) {
            throw new IllegalStateException("There is no currency type defined for the metric type " + metric.getMetricType().getId() + ".  Please contact a system administrator.");
        }
        if (measurable.getLocalCurrency() == null) {
            throw new IllegalStateException("There is no local currency defined.  Please contact a system administrator.");
        }
        if (measurable.getContractCurrency() == null) {
            throw new IllegalStateException("There is no contract currency defined.  Please contact a system administrator.");
        }
        if (measurable.getReportingCurrency() == null) {
            throw new IllegalStateException("There is no reporting currency defined.  Please contact a system administrator.");
        }

        if (currencyMetric.isLocalCurrencyMetric()) {

            // if lcVaue is null then return;
            if (currencyMetric.getLcValue() == null) {
                return;
            }

            if (BigDecimal.ZERO.equals(currencyMetric.getLcValue())) {
                currencyMetric.setCcValue(BigDecimal.ZERO);
                currencyMetric.setRcValue(BigDecimal.ZERO);
            } else {
                currencyMetric.setCcValue(convert(currencyMetric.getLcValue(), measurable.getLocalCurrency(), measurable.getContractCurrency(), period));
                currencyMetric.setRcValue(convert(currencyMetric.getLcValue(), measurable.getLocalCurrency(), measurable.getReportingCurrency(), period));
            }
        } else if (currencyMetric.isContractCurrencyMetric()) {

            // if ccValue is null then return
            if (currencyMetric.getCcValue() == null) {
                return;
            }

            if (BigDecimal.ZERO.equals(currencyMetric.getCcValue())) {
                currencyMetric.setLcValue(BigDecimal.ZERO);
                currencyMetric.setRcValue(BigDecimal.ZERO);
            } else {
                currencyMetric.setLcValue(convert(currencyMetric.getCcValue(), measurable.getContractCurrency(), measurable.getLocalCurrency(), period));
                currencyMetric.setRcValue(convert(currencyMetric.getCcValue(), measurable.getContractCurrency(), measurable.getReportingCurrency(), period));
            }
        }
    }

    public void persist1(Object object) {
        em.persist(object);
    }

    public List<ExchangeRate> findRatesByPeriod(FinancialPeriod period) throws Exception {
        Query query = em.createQuery("SELECT er FROM ExchangeRate er WHERE er.financialPeriod = :PERIOD");
        query.setParameter("PERIOD", period);
        return (List<ExchangeRate>) query.getResultList();
    }

    public ExchangeRate findRateByFromToPeriod(Currency fromCurrency, Currency toCurrency, FinancialPeriod period) throws Exception {
        // Using NamedQuery for performance.
        Query query = em.createNamedQuery("Currency.findRateByFromToPeriod");
        query.setHint("eclipselink.QUERY_RESULTS_CACHE", "TRUE");
        query.setParameter("PERIOD", period);
        query.setParameter("FROM", fromCurrency);
        query.setParameter("TO", toCurrency);

        return (ExchangeRate) query.getSingleResult();  // use singleresult here since we always expect to find one and only one value.  anything otherwise is an exception.
    }

    public void deleteExchangeRate() throws Exception {
        em.createQuery("DELETE FROM ExchangeRate e").executeUpdate();
    }

    public void persist(ExchangeRate eRate) throws Exception {
        em.persist(eRate);
    }

    public BigDecimal convert(BigDecimal amount, Currency fromCurrency, Currency toCurrency, FinancialPeriod period) throws Exception {  // throw an application defined exception here instead of Exception

        ExchangeRate exchangeRate = null;
        try {
            String cacheKey = period.getName() + fromCurrency.getCurrencyCode() + toCurrency.getCurrencyCode();
            exchangeRate = exchangeRateCache.get(cacheKey);
            if (exchangeRate == null) {
                exchangeRate = findRateByFromToPeriod(fromCurrency, toCurrency, period);
                exchangeRateCache.put(cacheKey, exchangeRate);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to find an exchange rate from " + fromCurrency.getCurrencyCode() + " to " + toCurrency.getCurrencyCode() + " in period " + period.getId());
        }
        return amount.multiply(exchangeRate.getConversionRate()).setScale(SCALE, BigDecimal.ROUND_HALF_UP);
    }

    public void initCurrencyConverter(FinancialPeriod period) throws Exception {

        exchangeRateCache.clear();
        List<ExchangeRate> er = findRatesByPeriod(period);
        //Logger.getLogger(CurrencyService.class.getName()).log(Level.INFO, "rate count: " + er.size());
        //Logger.getLogger(CurrencyService.class.getName()).log(Level.INFO, "period: " + period.getId());

        final int SCALE = 14;
        final int ROUNDING_METHOD = BigDecimal.ROUND_HALF_UP;

        if (er.isEmpty()) {
            logger.info("Initializing exchange rates for: " + period.getId());

            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/currency_rate_file/currency.txt"), "UTF-8"));
            //deleteExchangeRate();
            String line = null;

            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                String[] from = line.split("\\t");
                if (!from[2].equalsIgnoreCase("")) {
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/currency_rate_file/currency.txt"), "UTF-8"));

                    String innerLine = null;
                    while ((innerLine = reader2.readLine()) != null) {
                        if (innerLine.trim().length() == 0) {
                            continue;
                        }

                        String[] to = innerLine.split("\\t");
                        if (!to[2].equalsIgnoreCase("")) {
                            BigDecimal usdRate = new BigDecimal("1.0");
                            BigDecimal sourceRate = new BigDecimal(from[4]);
                            BigDecimal targetRate = new BigDecimal(to[4]);

                            String type = from[2];
                            Currency fromCurrency = Currency.getInstance(from[3]);
                            Currency toCurrency = Currency.getInstance(to[3]);
                            LocalDate effectiveDate = LocalDate.now().plusDays(30);
                            //Currency Conversion Formula
                            BigDecimal rate = usdRate.divide(sourceRate, SCALE, ROUNDING_METHOD).multiply(targetRate);

                            ExchangeRate exchangeRate = new ExchangeRate(type, fromCurrency, toCurrency, period, rate);
                            persist(exchangeRate);
                            //logger.info("From Country: " + effectiveDate + "  To Country: " + toCurrency + "   Rate" + rate);
                        }
                    }
                    reader2.close();
                }
            }
            reader.close();
            logger.info("Finished initializing exchange rates.");
        }

        logger.info("Testing conversion of 500 INR to EUR.  Should be 6.3440521593  result: " + convert(new BigDecimal(500), Currency.getInstance("INR"), Currency.getInstance("EUR"), period));
    }

    public void processExchangeRates(InputStream fis, String filename) throws Exception {  // Need an application exception type defined.
        final int SCALE = 14;
        final int ROUNDING_METHOD = BigDecimal.ROUND_HALF_UP;
        List<ExchangeRate> exchangeRate = new ArrayList<ExchangeRate>();
        DataImportFile dataImport = new DataImportFile();
        List<String> importMSG = new ArrayList<String>();
        exchangeRateCache.clear();
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet worksheetforPeriod = workbook.getSheet("Currency Rates");//workbook.getSheetAt(0);

            if (worksheetforPeriod == null) {
                importMSG.add("Invalid xlsx file. Currency Rates Sheet can not be found");
                throw new IllegalStateException("Invalid xlsx file. Currency Rates Sheet can not be found");
            }
            XSSFRow rowPeriod;
            Cell cellPeriod = null;

            rowPeriod = worksheetforPeriod.getRow(0);
            cellPeriod = rowPeriod.getCell(CellReference.convertColStringToIndex("B"));
            int month = (int) cellPeriod.getNumericCellValue();
            rowPeriod = worksheetforPeriod.getRow(1);
            cellPeriod = rowPeriod.getCell(CellReference.convertColStringToIndex("B"));
            int year = (int) cellPeriod.getNumericCellValue();
            if (month == 0 || year == 0) {
                importMSG.add("Can't read financial period from Currency Rates Sheet");
                throw new IllegalStateException("Can't read financial period from Currency Rates Sheet");
            }
            String[] shortMonth = {"JAN", "FEB",
                "MAR", "APR", "MAY", "JUN", "JUL",
                "AUG", "SEP", "OCT", "NOV",
                "DEC"};
            String yrStr = Integer.toString(year);
            String finalYear = yrStr.substring(yrStr.length() - 2);
            String exPeriod = shortMonth[month - 1] + "-" + finalYear;

            FinancialPeriod period = financialPeriodService.findById(exPeriod);
            if (period == null) {
                importMSG.add("Can not find Financial Period for  : " + exPeriod);
                throw new IllegalStateException("Can not find Financial Period for  : " + exPeriod);
            }
            List<ExchangeRate> er = findRatesByPeriod(period);
            if (er.isEmpty()) {
                Cell cellFrom = null;
                int rowidFrom = 10;
                XSSFSheet worksheetFrom = workbook.getSheet("Summary");
                if (worksheetFrom == null) {
                    importMSG.add("Invalid xlsx file. Summary Sheet can not be found");
                    throw new IllegalStateException("Invalid xlsx file. Summary Sheet can not be found");
                }
                for (Row rowFrom : worksheetFrom) {
                    if (rowFrom.getRowNum() < rowidFrom) {
                        continue;
                    }
                    //rowFrom = worksheetFrom.getRow(rowidFrom++);
                    cellFrom = rowFrom.getCell(CellReference.convertColStringToIndex("A"));
                    if (cellFrom == null || ((XSSFCell) cellFrom).getRawValue() == null) {
                        continue;
                    }
                    cellFrom = rowFrom.getCell(CellReference.convertColStringToIndex("B"));
                    if (cellFrom == null || ((XSSFCell) cellFrom).getRawValue() == null) {
                        continue;
                    }
                    Cell cellTo = null;
                    int rowidTo = 10;
                    XSSFSheet worksheetTo = workbook.getSheet("Summary");
                    for (Row rowTo : worksheetTo) {
                        if (rowTo.getRowNum() < rowidTo) {
                            continue;
                        }
                        //rowTo = worksheetTo.getRow(rowidTo++);
                        cellTo = rowTo.getCell(CellReference.convertColStringToIndex("A"));
                        if (cellTo == null || ((XSSFCell) cellTo).getRawValue() == null) {
                            continue;
                        }
                        cellTo = rowTo.getCell(CellReference.convertColStringToIndex("B"));
                        if (cellTo == null || ((XSSFCell) cellTo).getRawValue() == null) {
                            continue;
                        }
                        BigDecimal sourceRate;
                        String type;
                        Currency fromCurrency;
                        BigDecimal targetRate;
                        Currency toCurrency;
                        BigDecimal usdRate = new BigDecimal("1.0");
                        try {
                            cellFrom = rowFrom.getCell(CellReference.convertColStringToIndex("H"));
                            sourceRate = new BigDecimal(cellFrom.getNumericCellValue());
                            cellFrom = rowFrom.getCell(CellReference.convertColStringToIndex("B"));
                            type = cellFrom.getStringCellValue();
                            cellFrom = rowFrom.getCell(CellReference.convertColStringToIndex("C"));
                            fromCurrency = Currency.getInstance(cellFrom.getStringCellValue());
                        } catch (Exception rce) {
                            importMSG.add("Summary Sheet Row: " + (rowFrom.getRowNum() + 1) + " Cell:" + (cellFrom.getColumnIndex() + 1) + " Massage: " + rce.getMessage());
                            throw new Exception("Summary Sheet Row: " + (rowFrom.getRowNum() + 1) + " Cell:" + (cellFrom.getColumnIndex() + 1) + " Massage: " + rce.getMessage());
                        }

                        try {
                            cellTo = rowTo.getCell(CellReference.convertColStringToIndex("H"));
                            targetRate = new BigDecimal(cellTo.getNumericCellValue());

                            cellTo = rowTo.getCell(CellReference.convertColStringToIndex("C"));
                            toCurrency = Currency.getInstance(cellTo.getStringCellValue());
                        } catch (Exception rce) {
                            importMSG.add("Summary Sheet Row: " + (rowTo.getRowNum() + 1) + " Cell:" + (cellTo.getColumnIndex() + 1) + " Massage: " + rce.getMessage());
                            throw new Exception("Summary Sheet Row: " + (rowTo.getRowNum() + 1) + " Cell:" + (cellTo.getColumnIndex() + 1) + " Massage: " + rce.getMessage());
                        }
                        //Currency Conversion Formula
                        BigDecimal rate = usdRate.divide(sourceRate, SCALE, ROUNDING_METHOD).multiply(targetRate);
                        //logger.info("type: " + type + "  fromCurrency: " + fromCurrency + "   toCurrency" + toCurrency + "   period" + period + "   rate" + rate);
                        ExchangeRate exRate = new ExchangeRate();
                        exRate.setType(type);
                        exRate.setFromCurrency(fromCurrency);
                        exRate.setToCurrency(toCurrency);
                        exRate.setFinancialPeriod(period);
                        exRate.setConversionRate(rate);
                        exchangeRate.add(exRate);

                    }

                }
                for (ExchangeRate ex : exchangeRate) {
                    persist(ex);
                }

            } else {
                importMSG.add("Exchange rate data already exists for this period");
                throw new IllegalStateException("Exchange rate data already exists for this period");
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        } finally {
            dataImport.setFilename(filename);
            dataImport.setUploadDate(LocalDateTime.now());
            dataImport.setCompany(adminService.findCompanyById("FLS"));
            dataImport.setDataImportMessages(importMSG);
            dataImport.setType("Exchange Rate");
            adminService.persist(dataImport);
            fis.close();
        }
    }

    public void processLegacyExchangeRates(String msAccDB) throws Exception {  // Need an application exception type defined.
        final int SCALE = 14;
        final int ROUNDING_METHOD = BigDecimal.ROUND_HALF_UP;
        Logger.getLogger(CurrencyService.class.getName()).log(Level.INFO, "Processing POCI Data: " + msAccDB);

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;
        FinancialPeriod period = null;
        List<ExchangeRate> exchangeRate = new ArrayList<ExchangeRate>();
        exchangeRateCache.clear();
        DataImportFile dataImport = new DataImportFile();
        List<String> importMessages = new ArrayList<String>();
        // Step 1: Loading or registering Oracle JDBC driver class
        try {

            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        } catch (ClassNotFoundException cnfex) {
            Logger.getLogger(CurrencyService.class.getName()).log(Level.INFO, "Problem in loading or registering MS Access JDBC driver");
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
            resultSet = statement.executeQuery("SELECT Period FROM `tbl_ExchangeRates` GROUP BY Period");

            int count = 0;
            // processing returned data and printing into console
            while (resultSet.next()) {

                if (!resultSet.getString(1).equalsIgnoreCase("2018-6")) {
                    String periodDate = resultSet.getString(1);
                    String[] sp = periodDate.split("-");
                    try {
                        String[] monthName = {"JAN", "FEB",
                            "MAR", "APR", "MAY", "JUN", "JUL",
                            "AUG", "SEP", "OCT", "NOV",
                            "DEC"};
                        // checking valid integer using parseInt() method
                        int year = Integer.parseInt(sp[0]);
                        int month = Integer.parseInt(sp[1]);
                        String yrStr = Integer.toString(year);
                        String finalYear = yrStr.substring(yrStr.length() - 2);
                        String exPeriod = monthName[month - 1] + "-" + finalYear;

                        period = financialPeriodService.findById(exPeriod);
                    } catch (NumberFormatException e) {
                        Logger.getLogger(CurrencyService.class.getName()).log(Level.INFO, "Error " + e);
                    } catch (Exception e) {
                        importMessages.add("Invalid Financial Period : " + periodDate);
                        throw new Exception("Invalid Financial Period : " + periodDate);
                    }
                    if (adminService.findExchangeRatesByFinancialPeriod(period) == null) {
                        count++;
                        PreparedStatement statement1 = connection.prepareStatement("SELECT Currency,ISOCodeAlpha,CUSDPeriodEndRate FROM `tbl_ExchangeRates` WHERE Period = ?");
                        statement1.setString(1, resultSet.getString(1));
                        resultSet1 = statement1.executeQuery();
                        while (resultSet1.next()) {
                            BigDecimal sourceRate;
                            String type;
                            Currency fromCurrency;
                            BigDecimal targetRate;
                            Currency toCurrency;
                            BigDecimal usdRate = new BigDecimal("1.0");

                            PreparedStatement statement2 = connection.prepareStatement("SELECT ISOCodeAlpha,CUSDPeriodEndRate FROM `tbl_ExchangeRates` WHERE Period = ?");
                            statement2.setString(1, resultSet.getString(1));
                            resultSet2 = statement2.executeQuery();
                            while (resultSet2.next()) {

                                sourceRate = resultSet1.getBigDecimal(3);
                                type = resultSet1.getString(1);
                                fromCurrency = Currency.getInstance(resultSet1.getString(2));

                                targetRate = resultSet2.getBigDecimal(2);
                                if (targetRate == null && resultSet2.getString(1).equalsIgnoreCase("USD")) {
                                    targetRate = usdRate;
                                }
                                if (sourceRate == null && resultSet1.getString(2).equalsIgnoreCase("USD")) {
                                    sourceRate = usdRate;
                                    type = "(Dollar)";
                                }
                                toCurrency = Currency.getInstance(resultSet2.getString(1));

                                //Currency Conversion Formula
                                BigDecimal rate = usdRate.divide(sourceRate, SCALE, ROUNDING_METHOD).multiply(targetRate);
                                ExchangeRate exRate = new ExchangeRate();
                                exRate.setType(type);
                                exRate.setFromCurrency(fromCurrency);
                                exRate.setToCurrency(toCurrency);
                                exRate.setFinancialPeriod(period);
                                exRate.setConversionRate(rate);
                                exchangeRate.add(exRate);
                            }
                        }
                        for (ExchangeRate ex : exchangeRate) {
                            persist(ex);
                        }
                    }
                }

            }
            if (count == 0) {
                importMessages.add("All legacy financial years exchange rates already available in DB");
                throw new Exception("All legacy financial years exchange rates already available in DB");
            }
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            importMessages.add("Table can not found : TBL_EXCHANGERATES");
            throw new Exception("Table can not found : TBL_EXCHANGERATES");
        } finally {
            dataImport.setFilename("TBL_EXCHANGERATES");
            dataImport.setUploadDate(LocalDateTime.now());
            dataImport.setCompany(adminService.findCompanyById("FLS"));
            dataImport.setDataImportMessages(importMessages);
            dataImport.setType("Legacy Exchange Rate");
            adminService.persist(dataImport);
            // Step 3: Closing database connection
            try {
                if (null != connection) {
                    if (null != resultSet) {
                        // cleanup resources, once after processing
                        resultSet.close();
                        statement.close();
                    }

                    // and then finally close connection
                    connection.close();
                }
            } catch (SQLException sqlex) {
                sqlex.printStackTrace();
            }
        }

        Logger.getLogger(CurrencyService.class.getName()).log(Level.INFO, "Exchange rate import complete.");
    }
}
