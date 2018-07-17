/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.view.ViewSupport;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author shubhamc
 */
@Stateless
public class ReportsService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    @Inject
    private CalculationService calculationService;

    @Inject
    private MetricService metricService;
    @Inject
    private ContractService contractService;
    @Inject
    private ViewSupport viewSupport;
    private static final int HEADER_ROW_COUNT = 10;

    public void generateContractEsimatesReport(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        calculationService.executeBusinessRulesForContract(contract);
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
        workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
        workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
        workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
        XSSFSheet worksheet = workbook.getSheet("Contract Summary-1");
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        XSSFRow rowTitle = worksheet.getRow(1);
        cell = rowTitle.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        BigDecimal trancationPrice = viewSupport.getAccumulatedCurrencyMetricValue("TRANSACTION_PRICE_CC", contract);
        BigDecimal loquidatedDamage = viewSupport.getAccumulatedCurrencyMetricValue("LIQUIDATED_DAMAGES_ITD_CC", contract);
        BigDecimal EAC = viewSupport.getAccumulatedCurrencyMetricValue("ESTIMATED_COST_AT_COMPLETION_LC", contract);
        BigDecimal estimatedGrossProfit = viewSupport.getAccumulatedCurrencyMetricValue("ESTIMATED_GROSS_PROFIT_LC", contract);
        //BigDecimal estimatedGrossMargin = viewSupport.getAccumulatedCurrencyMetricValue("ESTIMATED_GROSS_MARGIN", contract);
        BigDecimal estimatedGrossMargin = new BigDecimal(0);
        if (estimatedGrossProfit.compareTo(BigDecimal.ZERO) > 0) {
            estimatedGrossMargin = estimatedGrossProfit.divide(trancationPrice, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
        }

        row = worksheet.getRow(rowid++);
        cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(trancationPrice.doubleValue());
        cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(loquidatedDamage.doubleValue());
        cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(EAC.doubleValue());
        cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossProfit.doubleValue());
        cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossMargin.doubleValue());

        workbook.write(outputStream);
        workbook.close();
        inputStream.close();
        outputStream.close();

    }

    public void generateReportfromInceptiontoDate(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-2");
            XSSFRow row;
            Cell cell = null;
            //int rowid = HEADER_ROW_COUNT;
            XSSFRow contract_name = worksheet.getRow(1);
            cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contract.getName());

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateReportMonthlyIncomeImpact(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-3");
            XSSFRow row;
            Cell cell = null;
            //int rowid = HEADER_ROW_COUNT;
            XSSFRow contract_name = worksheet.getRow(1);
            cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contract.getName());

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateReportQuarterlyIncomeImpact(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-4");
            XSSFRow row;
            Cell cell = null;
            //int rowid = HEADER_ROW_COUNT;
            XSSFRow contract_name = worksheet.getRow(1);
            cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contract.getName());

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateReportAnnualIncomeImpact(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-5");

            XSSFRow row;
            Cell cell = null;
            //int rowid = HEADER_ROW_COUNT;
            XSSFRow contract_name = worksheet.getRow(1);
            cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contract.getName());

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

}
