/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.ExchangeRate;
import com.flowserve.system606.model.FinancialPeriod;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

    public void copyFile(String destination, String fileName, InputStream in) {
        try {
            // write the inputStream to a FileOutputStream
            OutputStream out = new FileOutputStream(new File(destination + fileName));

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            in.close();
            out.flush();
            out.close();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void ReadFile(String folder, String fileName) {
        try {
            //Create the input stream from the xlsx/xls file
            FileInputStream fis = new FileInputStream(folder + fileName);

            File fout = new File(folder + "currency.txt");
            //Create the file
            if (fout.exists()) {
                fout.delete();
                fout.createNewFile();
            } else {
                fout.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(fout);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            //Create Workbook instance for xlsx/xls file input stream
            Workbook workbook = null;
            if (fileName.toLowerCase().endsWith("xlsx")) {
                workbook = new XSSFWorkbook(fis);
            }
            //Get the Specifc sheet in the xlsx file
            Sheet sheet = workbook.getSheet("Oracle Load Rates");
            //this.ReadEachRecord(sheet, bw);
            File file = new File(folder + fileName);

            if (file.delete()) {
                //System.out.println("File deleted successfully");
            } else {
                System.out.println("Failed to delete the file");
            }
            bw.close();
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    public void ReadEachRecord(Sheet sheet, BufferedWriter bw) throws IOException {
//        //every sheet has rows, iterate over them
//        Iterator<Row> rowIterator = sheet.iterator();
//        while (rowIterator.hasNext()) {
//            String name = "";
//            String cnt_name = "";
//            String type = "";
//            String code = "";
//            Double usd = null;
//            //Get the row object
//            Row row = rowIterator.next();
//            if (row.getRowNum() == 0) {
//                continue;
//            }
//            //Every row has columns, get the column iterator and iterate over them
//            Iterator<Cell> cellIterator = row.cellIterator();
//
//            while (cellIterator.hasNext()) {
//                //Get the Cell object
//                Cell cell = cellIterator.next();
//
//                //check the cell type and process accordingly
//                switch (cell.getCellType()) {
//                    case Cell.CELL_TYPE_STRING:
//                        if (cnt_name.equalsIgnoreCase("")) {
//                            cnt_name = cell.getStringCellValue().trim();
//                        } else if (name.equalsIgnoreCase("")) {
//                            //2nd column
//                            name = cell.getStringCellValue().trim();
//                        } else if (type.equalsIgnoreCase("")) {
//                            //3rd column
//                            type = cell.getStringCellValue().trim();
//                        } else if (code.equalsIgnoreCase("")) {
//                            //4th column
//                            code = cell.getStringCellValue().trim();
//                        } else {
//
//                        }
//                        break;
//                    case Cell.CELL_TYPE_NUMERIC:
//                        //5th column
//                        usd = cell.getNumericCellValue();
//                }
//            } //end of cell iterator
//
//            bw.newLine();
//            bw.write(name + "\t" + cnt_name + "\t" + type + "\t" + code + "\t" + usd);
//        } //end of rows iterator
//
//    }

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

}
