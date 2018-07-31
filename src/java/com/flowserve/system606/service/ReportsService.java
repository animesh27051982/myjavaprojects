/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.PerformanceObligationGroup;
import com.flowserve.system606.model.RevenueMethod;
import com.flowserve.system606.view.ViewSupport;
import com.flowserve.system606.web.WebSession;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
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
    private ViewSupport viewSupport;
    @Inject
    private WebSession webSession;
    @Inject
    private FinancialPeriodService financialPeriodService;

    private static final int HEADER_ROW_COUNT = 10;

    public void generateContractEsimatesReport(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-1");

            worksheet = writeContractEsimatesReport(worksheet, contract);
            workbook.write(outputStream);
            workbook.close();
        }
        inputStream.close();
        outputStream.close();
    }

    public XSSFSheet writeContractEsimatesReport(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = 0;
        XSSFRow rowTitle = worksheet.getRow(1);
        cell = rowTitle.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        // For now, get the period from the user's session.  Later we will be passing in the period from the reports generation page.
        FinancialPeriod period = webSession.getCurrentPeriod();

        // Execute buisness rules on the contract to fill in any values we might need at the contract level (e.g. gross margin)
        calculationService.executeBusinessRules(contract, period);

        // Get the contract level values for the contract total row on the "Contract Summary Totals" row.
        BigDecimal transactionPrice = getTransactionPrice(contract, period);
        BigDecimal loquidatedDamage = getLiquidatedDamages(contract, period);
        BigDecimal EAC = getEAC(contract, period);
        BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(contract, period);
        BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(contract, period);

        row = worksheet.getRow(15);

        // TODO - Please change all setCell lines to the same approach below.
        setCellValue(row, 1, transactionPrice);
//        cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//        cell.setCellValue(transactionPrice.doubleValue());
        cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(loquidatedDamage.doubleValue());
        cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(EAC.doubleValue());
        cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossProfit.doubleValue());
        cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossMargin.doubleValue());

        // Split the contract into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        transactionPrice = getTransactionPrice(pocPobs, period);
        loquidatedDamage = getLiquidatedDamages(pocPobs, period);
        EAC = getEAC(pocPobs, period);
        estimatedGrossProfit = getEstimatedGrossProfit(pocPobs, period);
        estimatedGrossMargin = getEstimatedGrossMargin(pocPobs, period);;

        // Percentage of completion Pobs.  Set total row for POC POBs
        row = worksheet.getRow(10);
        cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(transactionPrice.doubleValue());
        cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(loquidatedDamage.doubleValue());
        cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(EAC.doubleValue());
        cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossProfit.doubleValue());
        cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossMargin.doubleValue());

        // We have the same total 8 rows down
        row = worksheet.getRow(18);
        cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(transactionPrice.doubleValue());
        cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(loquidatedDamage.doubleValue());
        cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(EAC.doubleValue());
        cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossProfit.doubleValue());
        cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossMargin.doubleValue());

        // Point in time pobs.
        transactionPrice = getTransactionPrice(pitPobs, period);
        loquidatedDamage = getLiquidatedDamages(pitPobs, period);
        EAC = getEAC(pitPobs, period);
        estimatedGrossProfit = getEstimatedGrossProfit(pitPobs, period);
        estimatedGrossMargin = getEstimatedGrossMargin(pitPobs, period);;

        // Same thing, 2 rows of totals.
        row = worksheet.getRow(11);
        cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(transactionPrice.doubleValue());
        cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(loquidatedDamage.doubleValue());
        cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(EAC.doubleValue());
        cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossProfit.doubleValue());
        cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossMargin.doubleValue());
        row = worksheet.getRow(21);
        cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(transactionPrice.doubleValue());
        cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(loquidatedDamage.doubleValue());
        cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(EAC.doubleValue());
        cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossProfit.doubleValue());
        cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossMargin.doubleValue());

        // straight line pobs.
        transactionPrice = getTransactionPrice(slPobs, period);
        loquidatedDamage = getLiquidatedDamages(slPobs, period);
        EAC = getEAC(slPobs, period);
        estimatedGrossProfit = getEstimatedGrossProfit(slPobs, period);
        estimatedGrossMargin = getEstimatedGrossMargin(slPobs, period);;

        row = worksheet.getRow(12);
        cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(transactionPrice.doubleValue());
        cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(loquidatedDamage.doubleValue());
        cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(EAC.doubleValue());
        cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossProfit.doubleValue());
        cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossMargin.doubleValue());
        row = worksheet.getRow(24);
        cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(transactionPrice.doubleValue());
        cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(loquidatedDamage.doubleValue());
        cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(EAC.doubleValue());
        cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossProfit.doubleValue());
        cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossMargin.doubleValue());

        int pobInsertionRow = 18;

        // Fill in the POB detail lines.  We'll shift the surrounding rows down as we go to make room for the detail lines.
        if (pocPobs.getPerformanceObligations().size() > 0) {
            worksheet.shiftRows(pobInsertionRow, pobInsertionRow + 6, pocPobs.getPerformanceObligations().size(), true, false);

            for (PerformanceObligation pob : pocPobs.getPerformanceObligations()) {
                transactionPrice = getTransactionPrice(pob, period);
                loquidatedDamage = getLiquidatedDamages(pob, period);
                EAC = getEAC(pob, period);
                estimatedGrossProfit = getEstimatedGrossProfit(pob, period);
                estimatedGrossMargin = getEstimatedGrossMargin(pob, period);;

                row = worksheet.createRow(pobInsertionRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getName());
                cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(transactionPrice.doubleValue());
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
        }

        pobInsertionRow += 3;

        // Same thing for point in time POBs.  This time we need to shift less due to not as much data below this area.
        if (pitPobs.getPerformanceObligations().size() > 0) {
            worksheet.shiftRows(pobInsertionRow, pobInsertionRow + 3, pitPobs.getPerformanceObligations().size(), true, false);

            for (PerformanceObligation pob : pitPobs.getPerformanceObligations()) {
                transactionPrice = getTransactionPrice(pob, period);
                loquidatedDamage = getLiquidatedDamages(pob, period);
                EAC = getEAC(pob, period);
                estimatedGrossProfit = getEstimatedGrossProfit(pob, period);
                estimatedGrossMargin = getEstimatedGrossMargin(pob, period);;

                row = worksheet.createRow(pobInsertionRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getName());
                cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(transactionPrice.doubleValue());
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
        }

        pobInsertionRow += 3;

        // Only need to shift one row
        if (slPobs.getPerformanceObligations().size() > 0) {
            worksheet.shiftRows(pobInsertionRow, pobInsertionRow, slPobs.getPerformanceObligations().size(), true, false);

            for (PerformanceObligation pob : slPobs.getPerformanceObligations()) {
                transactionPrice = getTransactionPrice(pob, period);
                loquidatedDamage = getLiquidatedDamages(pob, period);
                EAC = getEAC(pob, period);
                estimatedGrossProfit = getEstimatedGrossProfit(pob, period);
                estimatedGrossMargin = getEstimatedGrossMargin(pob, period);;

                row = worksheet.createRow(pobInsertionRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getName());
                cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(transactionPrice.doubleValue());
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
        }

        return worksheet;
    }

    public void generateReportfromInceptiontoDate(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-2");
            worksheet = writefromInceptiontoDate(worksheet, contract);
            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writefromInceptiontoDate(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        XSSFRow contract_name = worksheet.getRow(1);
        cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        FinancialPeriod period = webSession.getCurrentPeriod();

        BigDecimal loquidatedDamage = getLiquidatedDamages(contract, period);
        //  Please create helper methods for these as well.  Like the line above.
        BigDecimal localCostITDLC = calculationService.getCurrencyMetric("COST_OF_GOODS_SOLD_ITD_LC", contract, period).getValue();
        BigDecimal percentComplete = calculationService.getDecimalMetric("PERCENT_COMPLETE", contract, period).getValue();
        BigDecimal revenueITD = calculationService.getCurrencyMetric("REVENUE_TO_RECOGNIZE_ITD_LC", contract, period).getValue();
        BigDecimal lossReserveITD = calculationService.getCurrencyMetric("LOSS_RESERVE_ITD_LC", contract, period).getValue();
        BigDecimal grossProfitITD = getEstimatedGrossProfit(contract, period);
        BigDecimal grossMarginITD = getEstimatedGrossMargin(contract, period);
        BigDecimal costToComplete = calculationService.getCurrencyMetric("CONTRACT_COST_TO_COMPLETE_LC", contract, period).getValue();
        BigDecimal billingsInExcess = calculationService.getCurrencyMetric("CONTRACT_BILLINGS_IN_EXCESS_LC", contract, period).getValue();
        BigDecimal revenueInExcess = calculationService.getCurrencyMetric("CONTRACT_REVENUE_IN_EXCESS_LC", contract, period).getValue();

        BigDecimal billToDate = contract.getTotalBillingsLocalCurrency();

        row = worksheet.getRow(rowid++);
        cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(percentComplete.doubleValue());
        cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(revenueITD.doubleValue());
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

        int pobInsertionRow = 18;

        List<PerformanceObligation> pobs = contract.getPerformanceObligations();
        worksheet.shiftRows(18, 24, pobs.size(), true, false);

        for (PerformanceObligation pob : contract.getPerformanceObligations()) {
            loquidatedDamage = getLiquidatedDamages(pob, period);
            localCostITDLC = calculationService.getCurrencyMetric("COST_OF_GOODS_SOLD_ITD_LC", pob, period).getValue();
            percentComplete = calculationService.getDecimalMetric("PERCENT_COMPLETE", pob, period).getValue();
            revenueITD = calculationService.getCurrencyMetric("REVENUE_TO_RECOGNIZE_ITD_LC", pob, period).getValue();
            //lossReserveITD = viewSupport.getCurrencyMetric("LOSS_RESERVE_ITD_LC", pob).getValue();
            grossProfitITD = getEstimatedGrossProfit(pob, period);
            grossMarginITD = getEstimatedGrossMargin(pob, period);
//            costToComplete = calculationService.getCurrencyMetric("COST_TO_COMPLETE_LC", pob).getValue();
//            billingsInExcess = calculationService.getCurrencyMetric("BILLINGS_IN_EXCESS_LC", pob).getValue();
//            revenueInExcess = calculationService.getCurrencyMetric("REVENUE_IN_EXCESS_LC", pob).getValue();
            row = worksheet.createRow(pobInsertionRow);
            cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(pob.getName());
            cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(percentComplete.doubleValue());
            cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(revenueITD.doubleValue());
            cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(loquidatedDamage.doubleValue());
            cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(localCostITDLC.doubleValue());
            cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            //cell.setCellValue(lossReserveITD.doubleValue());
            cell = row.getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(grossProfitITD.doubleValue());
            cell = row.getCell(7, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(grossMarginITD.doubleValue());
            cell = row.getCell(9, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            //cell.setCellValue(billToDate.doubleValue());
            cell = row.getCell(10, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            //cell.setCellValue(costToComplete.doubleValue());
            cell = row.getCell(11, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            //cell.setCellValue(billingsInExcess.doubleValue());
            cell = row.getCell(12, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            //cell.setCellValue(revenueInExcess.doubleValue());

            pobInsertionRow++;

        }
        return worksheet;
    }

    public void generateReportMonthlyIncomeImpact(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-3");

            worksheet = writeMonthlyIncomeImpact(worksheet, contract);
            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writeMonthlyIncomeImpact(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        XSSFRow contract_name = worksheet.getRow(1);
        cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        FinancialPeriod period = webSession.getCurrentPeriod();

        BigDecimal revenueToRecognize = calculationService.getCurrencyMetric("REVENUE_TO_RECOGNIZE_ITD_LC", contract, period).getValue();
        BigDecimal loquidatedDamage = getLiquidatedDamages(contract, period);
        BigDecimal cumulativeCostGoodsSoldLC = calculationService.getCurrencyMetric("CUMULATIVE_COST_OF_GOODS_SOLD_LC", contract, period).getValue();
        BigDecimal EstimatedGrossProfitLC = getEstimatedGrossProfit(contract, period);

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

        int pobInsertionRow = 18;

        List<PerformanceObligation> pobs = contract.getPerformanceObligations();
        worksheet.shiftRows(18, 24, pobs.size(), true, false);

        for (PerformanceObligation pob : contract.getPerformanceObligations()) {
            revenueToRecognize = calculationService.getCurrencyMetric("REVENUE_TO_RECOGNIZE_ITD_LC", pob, period).getValue();
            loquidatedDamage = getLiquidatedDamages(pob, period);
            cumulativeCostGoodsSoldLC = calculationService.getCurrencyMetric("CUMULATIVE_COST_OF_GOODS_SOLD_LC", pob, period).getValue();
            EstimatedGrossProfitLC = getEstimatedGrossProfit(pob, period);

            cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(pob.getName());
            row = worksheet.createRow(pobInsertionRow);
            cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(revenueToRecognize.doubleValue());
            cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(loquidatedDamage.doubleValue());
            cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            //below throwing NullPointerException
            //cell.setCellValue(cumulativeCostGoodsSoldLC.doubleValue());
            cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(EstimatedGrossProfitLC.doubleValue());

            pobInsertionRow++;

        }
        return worksheet;
    }

    public void generateReportQuarterlyIncomeImpact(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-4");

            worksheet = writeQuarterlyIncomeImpact(worksheet, contract);

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writeQuarterlyIncomeImpact(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        XSSFRow contract_name = worksheet.getRow(1);
        cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        List<FinancialPeriod> qtdPeriods = financialPeriodService.getQTDFinancialPeriods(viewSupport.getCurrentPeriod());

        BigDecimal revenueToRecognize = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("REVENUE_TO_RECOGNIZE_ITD_LC", contract, qtdPeriods).getValue();
        BigDecimal loquidatedDamage = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("LIQUIDATED_DAMAGES_ITD_CC", contract, qtdPeriods).getValue();
        BigDecimal cumulativeCostGoodsSoldLC = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("CUMULATIVE_COST_OF_GOODS_SOLD_LC", contract, qtdPeriods).getValue();
        BigDecimal EstimatedGrossProfitLC = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("ESTIMATED_GROSS_PROFIT_LC", contract, qtdPeriods).getValue();

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

        int pobInsertionRow = 18;

        List<PerformanceObligation> pobs = contract.getPerformanceObligations();
        worksheet.shiftRows(18, 24, pobs.size(), true, false);

        for (PerformanceObligation pob : contract.getPerformanceObligations()) {
            revenueToRecognize = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("REVENUE_TO_RECOGNIZE_ITD_LC", pob, qtdPeriods).getValue();
            loquidatedDamage = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("LIQUIDATED_DAMAGES_ITD_CC", pob, qtdPeriods).getValue();
            cumulativeCostGoodsSoldLC = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("CUMULATIVE_COST_OF_GOODS_SOLD_LC", pob, qtdPeriods).getValue();
            EstimatedGrossProfitLC = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("ESTIMATED_GROSS_PROFIT_LC", pob, qtdPeriods).getValue();

            cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(pob.getName());
            row = worksheet.createRow(pobInsertionRow);
            cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(revenueToRecognize.doubleValue());
            cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(loquidatedDamage.doubleValue());
            cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            //below throwing NullPointerException
            //cell.setCellValue(cumulativeCostGoodsSoldLC.doubleValue());
            cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(EstimatedGrossProfitLC.doubleValue());

            pobInsertionRow++;

        }
        return worksheet;
    }

    public void generateReportAnnualIncomeImpact(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-5");

            worksheet = writeAnnualIncomeImpact(worksheet, contract);

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writeAnnualIncomeImpact(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        XSSFRow contract_name = worksheet.getRow(1);
        cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());

        BigDecimal revenueToRecognize = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("REVENUE_TO_RECOGNIZE_ITD_LC", contract, ytdPeriods).getValue();
        BigDecimal loquidatedDamage = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("LIQUIDATED_DAMAGES_ITD_CC", contract, ytdPeriods).getValue();
        BigDecimal cumulativeCostGoodsSoldLC = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("CUMULATIVE_COST_OF_GOODS_SOLD_LC", contract, ytdPeriods).getValue();
        BigDecimal EstimatedGrossProfitLC = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("ESTIMATED_GROSS_PROFIT_LC", contract, ytdPeriods).getValue();

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

        int pobInsertionRow = 18;

        List<PerformanceObligation> pobs = contract.getPerformanceObligations();
        worksheet.shiftRows(18, 24, pobs.size(), true, false);

        for (PerformanceObligation pob : contract.getPerformanceObligations()) {
            revenueToRecognize = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("REVENUE_TO_RECOGNIZE_ITD_LC", pob, ytdPeriods).getValue();
            loquidatedDamage = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("LIQUIDATED_DAMAGES_ITD_CC", pob, ytdPeriods).getValue();
            cumulativeCostGoodsSoldLC = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("CUMULATIVE_COST_OF_GOODS_SOLD_LC", pob, ytdPeriods).getValue();
            EstimatedGrossProfitLC = calculationService.getAccumulatedCurrencyMetricAcrossPeriods("ESTIMATED_GROSS_PROFIT_LC", pob, ytdPeriods).getValue();

            cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(pob.getName());
            row = worksheet.createRow(pobInsertionRow);
            cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(revenueToRecognize.doubleValue());
            cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(loquidatedDamage.doubleValue());
            cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            //below throwing NullPointerException
            //cell.setCellValue(cumulativeCostGoodsSoldLC.doubleValue());
            cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(EstimatedGrossProfitLC.doubleValue());

            pobInsertionRow++;

        }
        return worksheet;
    }

    public void generateJournalEntryReport(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Journal Entry-1"));
            XSSFSheet worksheet = workbook.getSheet("Journal Entry-2");

            XSSFRow row;
            Cell cell = null;
            int rowid = HEADER_ROW_COUNT;
            XSSFRow contract_name = worksheet.getRow(2);
            cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(contract.getName());

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateCombineReport(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-1");
            calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
            worksheet = writeContractEsimatesReport(worksheet, contract);

            worksheet = workbook.getSheet("Contract Summary-2");
            worksheet = writefromInceptiontoDate(worksheet, contract);

            worksheet = workbook.getSheet("Contract Summary-3");
            worksheet = writeMonthlyIncomeImpact(worksheet, contract);

            worksheet = workbook.getSheet("Contract Summary-4");
            worksheet = writeQuarterlyIncomeImpact(worksheet, contract);

            worksheet = workbook.getSheet("Contract Summary-5");
            worksheet = writeAnnualIncomeImpact(worksheet, contract);

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateReportFinancialSummary(InputStream inputStream, FileOutputStream outputStream) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            workbook.removeSheetAt(workbook.getSheetIndex("Journal Entry"));
            workbook.removeSheetAt(workbook.getSheetIndex("Journal Entry-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Journal Entry-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Financial Summary"));
            workbook.removeSheetAt(workbook.getSheetIndex("Disclosures"));
            workbook.removeSheetAt(workbook.getSheetIndex("Disclosures-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Disclosures-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Disclosures-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Disclosures-4"));
            workbook.removeSheetAt(workbook.getSheetIndex("Disclosures-5"));
            workbook.removeSheetAt(workbook.getSheetIndex("Disclosures-6"));
            workbook.removeSheetAt(workbook.getSheetIndex("Disclosures-7"));

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateReportDisclosures(InputStream inputStream, FileOutputStream outputStream) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-4"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            workbook.removeSheetAt(workbook.getSheetIndex("Journal Entry"));
            workbook.removeSheetAt(workbook.getSheetIndex("Journal Entry-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Journal Entry-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Financial Summary"));
            workbook.removeSheetAt(workbook.getSheetIndex("Financial Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Financial Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Financial Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Disclosures"));

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateReportJournalEntry(InputStream inputStream, FileOutputStream outputStream) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {

            workbook.removeSheetAt(workbook.getSheetIndex("Journal Entry-2"));

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    private BigDecimal getTransactionPrice(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("TRANSACTION_PRICE_CC", measureable, period).getValue();
    }

    private BigDecimal getLiquidatedDamages(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", measureable, period).getValue();
    }

    private BigDecimal getEAC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", measureable, period).getValue();
    }

    private BigDecimal getEstimatedGrossProfit(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("ESTIMATED_GROSS_PROFIT_LC", measureable, period).getValue();
    }

    private BigDecimal getEstimatedGrossMargin(Measurable measureable, FinancialPeriod period) {
        return calculationService.getDecimalMetric("ESTIMATED_GROSS_MARGIN", measureable, period).getValue();
    }

    private void setCellValue(XSSFRow row, int cellNum, BigDecimal value) {
        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(value.doubleValue());
    }

}
