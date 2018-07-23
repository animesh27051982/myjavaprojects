/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.view.ViewSupport;
import com.flowserve.system606.web.WebSession;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
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
    @Inject
    private WebSession webSession;

    private static final int HEADER_ROW_COUNT = 10;

    public void generateContractEsimatesReport(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
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

        BigDecimal trancationPrice = viewSupport.getCurrencyMetric("TRANSACTION_PRICE_CC", contract).getValue();
        BigDecimal loquidatedDamage = viewSupport.getCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", contract).getValue();
        BigDecimal EAC = viewSupport.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", contract).getValue();
        BigDecimal estimatedGrossProfit = viewSupport.getCurrencyMetric("CONTRACT_ESTIMATED_GROSS_PROFIT_LC", contract).getValue();
        BigDecimal estimatedGrossMargin = viewSupport.getDecimalMetric("CONTRACT_ESTIMATED_GROSS_MARGIN", contract).getValue();;

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

        int pobInsertionRow = 18;

        for (PerformanceObligation pob : contract.getPerformanceObligations()) {
            trancationPrice = viewSupport.getCurrencyMetric("TRANSACTION_PRICE_CC", pob).getValue();
            loquidatedDamage = viewSupport.getCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", pob).getValue();
            EAC = viewSupport.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", pob).getValue();
            estimatedGrossProfit = viewSupport.getCurrencyMetric("ESTIMATED_GROSS_PROFIT_LC", pob).getValue();
            estimatedGrossMargin = viewSupport.getDecimalMetric("ESTIMATED_GROSS_MARGIN", pob).getValue();;
            row = worksheet.getRow(pobInsertionRow);
            worksheet.shiftRows(pobInsertionRow, pobInsertionRow + 1, 1, true, false);
            cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(pob.getName());
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

            pobInsertionRow++;
        }

        workbook.write(outputStream);
        workbook.close();
        inputStream.close();
        outputStream.close();

    }

    public void generateReportfromInceptiontoDate(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-2");
            XSSFRow row;
            Cell cell = null;
            int rowid = HEADER_ROW_COUNT;
            XSSFRow contract_name = worksheet.getRow(1);
            cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contract.getName());

            BigDecimal loquidatedDamage = viewSupport.getCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", contract).getValue();
            BigDecimal localCostITDLC = viewSupport.getCurrencyMetric("COST_OF_GOODS_SOLD_ITD_LC", contract).getValue();
            BigDecimal contractPercentComplete = viewSupport.getDecimalMetric("CONTRACT_PERCENT_COMPLETE", contract).getValue();
            BigDecimal contractRevenueITD = viewSupport.getCurrencyMetric("CONTRACT_REVENUE_TO_RECOGNIZE_ITD_LC", contract).getValue();
            BigDecimal lossReserveITD = viewSupport.getCurrencyMetric("LOSS_RESERVE_ITD_LC", contract).getValue();
            BigDecimal grossProfitITD = viewSupport.getCurrencyMetric("CONTRACT_GROSS_PROFIT_LC", contract).getValue();
            BigDecimal grossMarginITD = viewSupport.getDecimalMetric("CONTRACT_GROSS_MARGIN", contract).getValue();
            BigDecimal costToComplete = viewSupport.getCurrencyMetric("CONTRACT_COST_TO_COMPLETE_LC", contract).getValue();
            BigDecimal billingsInExcess = viewSupport.getCurrencyMetric("CONTRACT_BILLINGS_IN_EXCESS_LC", contract).getValue();
            BigDecimal revenueInExcess = viewSupport.getCurrencyMetric("CONTRACT_REVENUE_IN_EXCESS_LC", contract).getValue();

            BigDecimal billToDate = contract.getTotalBillingsLocalCurrency();

            row = worksheet.getRow(rowid++);
            cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contractPercentComplete.doubleValue());
            cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contractRevenueITD.doubleValue());
            cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(loquidatedDamage.doubleValue());
            cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(localCostITDLC.doubleValue());
            cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(lossReserveITD.doubleValue());
            cell = row.getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(grossProfitITD.doubleValue());
            cell = row.getCell(7, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(grossMarginITD.doubleValue());
            cell = row.getCell(9, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(billToDate.doubleValue());
            cell = row.getCell(10, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(costToComplete.doubleValue());
            cell = row.getCell(11, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(billingsInExcess.doubleValue());
            cell = row.getCell(12, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(revenueInExcess.doubleValue());

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateReportMonthlyIncomeImpact(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-3");
            XSSFRow row;
            Cell cell = null;
            int rowid = HEADER_ROW_COUNT;
            XSSFRow contract_name = worksheet.getRow(1);
            cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contract.getName());

            BigDecimal revenueToRecognize = viewSupport.getCurrencyMetric("REVENUE_TO_RECOGNIZE_ITD_LC", contract).getValue();
            BigDecimal loquidatedDamage = viewSupport.getCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", contract).getValue();
            BigDecimal cumulativeCostGoodsSoldLC = viewSupport.getCurrencyMetric("CUMULATIVE_COST_OF_GOODS_SOLD_LC", contract).getValue();
            BigDecimal EstimatedGrossProfitLC = viewSupport.getCurrencyMetric("ESTIMATED_GROSS_PROFIT_LC", contract).getValue();

            row = worksheet.getRow(rowid++);
            cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(revenueToRecognize.doubleValue());
            cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(loquidatedDamage.doubleValue());
            cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(cumulativeCostGoodsSoldLC.doubleValue());
//          cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//          cell.setCellValue(EstimatedGrossProfitLC.doubleValue());
            cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(EstimatedGrossProfitLC.doubleValue());

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateReportQuarterlyIncomeImpact(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-4");
            XSSFRow row;
            Cell cell = null;
            int rowid = HEADER_ROW_COUNT;
            XSSFRow contract_name = worksheet.getRow(1);
            cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contract.getName());

            BigDecimal revenueToRecognize = viewSupport.getCurrencyMetric("REVENUE_TO_RECOGNIZE_ITD_LC", contract).getValue();
            BigDecimal loquidatedDamage = viewSupport.getCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", contract).getValue();
            BigDecimal cumulativeCostGoodsSoldLC = viewSupport.getCurrencyMetric("CUMULATIVE_COST_OF_GOODS_SOLD_LC", contract).getValue();
            BigDecimal EstimatedGrossProfitLC = viewSupport.getCurrencyMetric("ESTIMATED_GROSS_PROFIT_LC", contract).getValue();

            row = worksheet.getRow(rowid++);
            cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(revenueToRecognize.doubleValue());
            cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(loquidatedDamage.doubleValue());
            cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(cumulativeCostGoodsSoldLC.doubleValue());
//          cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//          cell.setCellValue(EstimatedGrossProfitLC.doubleValue());
            cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(EstimatedGrossProfitLC.doubleValue());

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateReportAnnualIncomeImpact(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-5");

            XSSFRow row;
            Cell cell = null;
            int rowid = HEADER_ROW_COUNT;
            XSSFRow contract_name = worksheet.getRow(1);
            cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contract.getName());

            BigDecimal revenueToRecognize = viewSupport.getCurrencyMetric("REVENUE_TO_RECOGNIZE_ITD_LC", contract).getValue();
            BigDecimal loquidatedDamage = viewSupport.getCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", contract).getValue();
            BigDecimal cumulativeCostGoodsSoldLC = viewSupport.getCurrencyMetric("CUMULATIVE_COST_OF_GOODS_SOLD_LC", contract).getValue();
            BigDecimal EstimatedGrossProfitLC = viewSupport.getCurrencyMetric("ESTIMATED_GROSS_PROFIT_LC", contract).getValue();

            row = worksheet.getRow(rowid++);
            cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(revenueToRecognize.doubleValue());
            cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(loquidatedDamage.doubleValue());
            cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(cumulativeCostGoodsSoldLC.doubleValue());
//          cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//          cell.setCellValue(EstimatedGrossProfitLC.doubleValue());
            cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(EstimatedGrossProfitLC.doubleValue());

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

}
