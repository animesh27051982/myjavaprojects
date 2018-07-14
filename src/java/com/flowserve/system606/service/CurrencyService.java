/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.ExchangeRate;
import com.flowserve.system606.model.FinancialPeriod;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
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

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    @Inject
    FinancialPeriodService financialPeriodService;

    public void persist1(Object object) {
        em.persist(object);
    }

    public List<ExchangeRate> findRatesByPeriod(FinancialPeriod period) throws Exception {
        Query query = em.createQuery("SELECT er FROM ExchangeRate er WHERE er.financialPeriod = :PERIOD");
        query.setParameter("PERIOD", period);
        return (List<ExchangeRate>) query.getResultList();
    }

    public ExchangeRate findRateByFromToPeriod(Currency fromCurrency, Currency toCurrency, FinancialPeriod period) throws Exception {
        Query query = em.createQuery("SELECT er FROM ExchangeRate er WHERE er.financialPeriod = :PERIOD and er.fromCurrency = :FROM and er.toCurrency = :TO");
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
        ExchangeRate er = findRateByFromToPeriod(fromCurrency, toCurrency, period);
        return amount.multiply(er.getConversionRate()).setScale(14, BigDecimal.ROUND_HALF_UP);
    }

    public void initCurrencyConverter() throws Exception {
        FinancialPeriod period = financialPeriodService.findById("MAY-18");
        List<ExchangeRate> er = findRatesByPeriod(period);

        final int SCALE = 14;
        final int ROUNDING_METHOD = BigDecimal.ROUND_HALF_UP;

        if (er.isEmpty()) {
            logger.info("Initializing exchange rates.");

            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/currency_rate_file/currency.txt"), "UTF-8"));
            deleteExchangeRate();
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
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet worksheetforPeriod = workbook.getSheet("Currency Rates");//workbook.getSheetAt(0);

            if (worksheetforPeriod == null) {
                throw new IllegalStateException("Invalid xlsx file.  Currency Rates Sheet can not be found");
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
                throw new IllegalStateException("Can't read financial period from Currency Rates Sheet");
            }
            String yrStr = Integer.toString(year);
            String finalYear = yrStr.substring(yrStr.length() - 2);
            String exPeriod = Month.of(month).name() + "-" + finalYear;

            FinancialPeriod period = financialPeriodService.findById(exPeriod);
            List<ExchangeRate> er = findRatesByPeriod(period);
            if (er.isEmpty()) {
                Cell cellFrom = null;
                int rowidFrom = 1;
                XSSFSheet worksheetFrom = workbook.getSheet("Oracle Load Rates");
                if (worksheetFrom == null) {
                    throw new IllegalStateException("Invalid xlsx file.  Oracle Load Rates Sheet can not be found");
                }
                for (Row rowFrom : worksheetFrom) {
                    if (rowFrom.getRowNum() < rowidFrom) {
                        continue;
                    }
                    rowFrom = worksheetFrom.getRow(rowidFrom++);
                    cellFrom = rowFrom.getCell(CellReference.convertColStringToIndex("B"));
                    if (cellFrom == null || ((XSSFCell) cellFrom).getRawValue() == null) {
                        continue;
                    }
                    cellFrom = rowFrom.getCell(CellReference.convertColStringToIndex("C"));
                    if (cellFrom == null || ((XSSFCell) cellFrom).getRawValue() == null) {
                        continue;
                    }
                    Cell cellTo = null;
                    int rowidTo = 1;
                    XSSFSheet worksheetTo = workbook.getSheet("Oracle Load Rates");
                    for (Row rowTo : worksheetTo) {
                        if (rowTo.getRowNum() < rowidTo) {
                            continue;
                        }
                        rowTo = worksheetTo.getRow(rowidTo++);
                        cellTo = rowTo.getCell(CellReference.convertColStringToIndex("B"));
                        if (cellTo == null || ((XSSFCell) cellTo).getRawValue() == null) {
                            continue;
                        }
                        cellTo = rowTo.getCell(CellReference.convertColStringToIndex("C"));
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
                            cellFrom = rowFrom.getCell(CellReference.convertColStringToIndex("E"));
                            sourceRate = new BigDecimal(cellFrom.getNumericCellValue());
                            cellFrom = rowFrom.getCell(CellReference.convertColStringToIndex("C"));
                            type = cellFrom.getStringCellValue();
                            cellFrom = rowFrom.getCell(CellReference.convertColStringToIndex("D"));
                            fromCurrency = Currency.getInstance(cellFrom.getStringCellValue());
                        } catch (Exception rce) {
                            throw new Exception("Oracle Load Rates Sheet Row: " + (rowFrom.getRowNum() + 1) + " Cell:" + (cellFrom.getColumnIndex() + 1) + " Massage: " + rce.getMessage());
                        }

                        try {
                            cellTo = rowTo.getCell(CellReference.convertColStringToIndex("E"));
                            targetRate = new BigDecimal(cellTo.getNumericCellValue());

                            cellTo = rowTo.getCell(CellReference.convertColStringToIndex("D"));
                            toCurrency = Currency.getInstance(cellTo.getStringCellValue());
                        } catch (Exception rce) {
                            throw new Exception("Oracle Load Rates Sheet Row: " + (rowTo.getRowNum() + 1) + " Cell:" + (cellTo.getColumnIndex() + 1) + " Massage: " + rce.getMessage());
                        }
                        //Currency Conversion Formula
                        BigDecimal rate = usdRate.divide(sourceRate, SCALE, ROUNDING_METHOD).multiply(targetRate);

                        ExchangeRate exRate = new ExchangeRate();
                        exRate.setType(type);
                        exRate.setFromCurrency(fromCurrency);
                        exRate.setToCurrency(toCurrency);
                        exRate.setFinancialPeriod(period);
                        exRate.setConversionRate(rate);
                        exchangeRate.add(exRate);

                        //logger.info("type: " + type + "  fromCurrency: " + fromCurrency + "   toCurrency" + toCurrency + "   period" + period + "   rate" + rate);
                    }

                }
                for (ExchangeRate ex : exchangeRate) {
                    persist(ex);
                }

            } else {
                throw new IllegalStateException("Exchange rate data already exists for this period");
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        } finally {
            fis.close();
        }
    }
}
