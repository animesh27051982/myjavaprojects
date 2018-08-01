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
import java.math.BigInteger;
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
        setCellValue(row, 2, loquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setCellValue(row, 5, estimatedGrossMargin);

        // Split the contract into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        printContractEsimatesPobsGroups(10, 18, worksheet, pocPobs, period);
        printContractEsimatesPobsGroups(11, 21, worksheet, pitPobs, period);
        printContractEsimatesPobsGroups(12, 24, worksheet, slPobs, period);

        printContractEsimatesPobsDetailLines(18, 24, worksheet, pocPobs, period);
        printContractEsimatesPobsDetailLines(21, 24, worksheet, pitPobs, period);
        printContractEsimatesPobsDetailLines(24, 24, worksheet, slPobs, period);

        return worksheet;
    }

    public void printContractEsimatesPobsGroups(int single, int total, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {

        XSSFRow row;
        BigDecimal transactionPrice = getTransactionPrice(pGroup, period);
        BigDecimal loquidatedDamage = getLiquidatedDamages(pGroup, period);
        BigDecimal EAC = getEAC(pGroup, period);
        BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(pGroup, period);
        BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(pGroup, period);;

        // Percentage of completion Pobs.  Set total row for POC POBs
        row = worksheet.getRow(single);
        setCellValue(row, 1, transactionPrice);
        setCellValue(row, 2, loquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setCellValue(row, 5, estimatedGrossMargin);

        // We have the same total 8 rows down
        row = worksheet.getRow(total);
        setCellValue(row, 1, transactionPrice);
        setCellValue(row, 2, loquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setCellValue(row, 5, estimatedGrossMargin);
    }

    public void printContractEsimatesPobsDetailLines(int insertRow, int shiftRow, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {
        XSSFRow row;
        Cell cell = null;
        if (pGroup.getPerformanceObligations().size() > 0) {
            worksheet.shiftRows(insertRow, shiftRow, pGroup.getPerformanceObligations().size(), true, false);

            for (PerformanceObligation pob : pGroup.getPerformanceObligations()) {
                BigDecimal transactionPrice = getTransactionPrice(pob, period);
                BigDecimal loquidatedDamage = getLiquidatedDamages(pob, period);
                BigDecimal EAC = getEAC(pob, period);
                BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(pob, period);
                BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(pob, period);;

                row = worksheet.createRow(insertRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getName());
                setCellValue(row, 1, transactionPrice);
                setCellValue(row, 2, loquidatedDamage);
                setCellValue(row, 3, EAC);
                setCellValue(row, 4, estimatedGrossProfit);
                setCellValue(row, 5, estimatedGrossMargin);

                insertRow++;
            }
        }
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

        BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
        BigDecimal localCostITDLC = getCostOfGoodsSold(contract, period);
        BigDecimal percentComplete = getPercentComplete(contract, period);
        BigDecimal revenueITD = getRevenueRecognizeITD(contract, period);
        BigDecimal lossReserveITD = getLossReserveITD(contract, period);
        BigDecimal grossProfitITD = getEstimatedGrossProfit(contract, period);
        BigDecimal grossMarginITD = getEstimatedGrossMargin(contract, period);
        BigDecimal costToComplete = getContractCostToCompleteLC(contract, period);
        BigDecimal billingsInExcess = getContractBillingsInExcess(contract, period);
        BigDecimal revenueInExcess = getContractRevenueInExcess(contract, period);
        BigDecimal billToDate = contract.getTotalBillingsLocalCurrency();

        row = worksheet.getRow(15);
        setCellValue(row, 1, percentComplete);
        setCellValue(row, 2, revenueITD);
        setCellValue(row, 3, liquidatedDamage);
        setCellValue(row, 4, localCostITDLC);
        setCellValue(row, 5, lossReserveITD);
        setCellValue(row, 6, grossProfitITD);
        setCellValue(row, 7, grossMarginITD);
        setCellValue(row, 9, billToDate);
        setCellValue(row, 10, costToComplete);
        setCellValue(row, 11, billingsInExcess);
        setCellValue(row, 12, revenueInExcess);

        // Split the contract into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        printInceptiontoDatePobsGroups(10, 18, worksheet, pocPobs, period, billToDate);
        printInceptiontoDatePobsGroups(11, 21, worksheet, pitPobs, period, billToDate);
        printInceptiontoDatePobsGroups(12, 24, worksheet, slPobs, period, billToDate);

        printInceptiontoDatePobsDetailLines(18, 24, worksheet, pocPobs, period);
        printInceptiontoDatePobsDetailLines(21, 24, worksheet, pitPobs, period);
        printInceptiontoDatePobsDetailLines(24, 24, worksheet, slPobs, period);

        return worksheet;
    }

    public void printInceptiontoDatePobsGroups(int single, int total, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period, BigDecimal billToDate) throws Exception {

        XSSFRow row;
        BigDecimal liquidatedDamage = getLiquidatedDamages(pGroup, period);
        BigDecimal localCostITDLC = getCostOfGoodsSold(pGroup, period);
        BigDecimal percentComplete = getPercentComplete(pGroup, period);
        BigDecimal revenueITD = getRevenueRecognizeITD(pGroup, period);
        BigDecimal lossReserveITD = getLossReserveITD(pGroup, period);
        BigDecimal grossProfitITD = getEstimatedGrossProfit(pGroup, period);
        BigDecimal grossMarginITD = getEstimatedGrossMargin(pGroup, period);
        BigDecimal costToComplete = getContractCostToCompleteLC(pGroup, period);
        BigDecimal billingsInExcess = getContractBillingsInExcess(pGroup, period);
        BigDecimal revenueInExcess = getContractRevenueInExcess(pGroup, period);

        // Percentage of completion Pobs.  Set total row for POC POBs
        row = worksheet.getRow(single);
        //setCellValue(row, 1, percentComplete);
        setCellValue(row, 2, revenueITD);
        setCellValue(row, 3, liquidatedDamage);
        setCellValue(row, 4, localCostITDLC);
        setCellValue(row, 5, lossReserveITD);
        setCellValue(row, 6, grossProfitITD);
        setCellValue(row, 7, grossMarginITD);
        setCellValue(row, 9, billToDate);
        setCellValue(row, 10, costToComplete);
        setCellValue(row, 11, billingsInExcess);
        setCellValue(row, 12, revenueInExcess);

        // We have the same total 8 rows down
        row = worksheet.getRow(total);
        //setCellValue(row, 1, percentComplete);
        setCellValue(row, 2, revenueITD);
        setCellValue(row, 3, liquidatedDamage);
        setCellValue(row, 4, localCostITDLC);
        setCellValue(row, 5, lossReserveITD);
        setCellValue(row, 6, grossProfitITD);
        setCellValue(row, 7, grossMarginITD);
        setCellValue(row, 9, billToDate);
        setCellValue(row, 10, costToComplete);
        setCellValue(row, 11, billingsInExcess);
        setCellValue(row, 12, revenueInExcess);
    }

    public void printInceptiontoDatePobsDetailLines(int insertRow, int shiftRow, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {
        XSSFRow row;
        Cell cell = null;
        if (pGroup.getPerformanceObligations().size() > 0) {
            worksheet.shiftRows(insertRow, shiftRow, pGroup.getPerformanceObligations().size(), true, false);
            for (PerformanceObligation pob : pGroup.getPerformanceObligations()) {
                BigDecimal liquidatedDamage = getLiquidatedDamages(pob, period);
                BigDecimal localCostITDLC = getCostOfGoodsSold(pob, period);
                BigDecimal percentComplete = getPercentComplete(pob, period);
                BigDecimal revenueITD = getRevenueRecognizeITD(pob, period);
                //BigDecimal lossReserveITD = getLossReserveITD(pob, period);
                BigDecimal grossProfitITD = getEstimatedGrossProfit(pob, period);
                BigDecimal grossMarginITD = getEstimatedGrossMargin(pob, period);
                //BigDecimal costToComplete = getContractCostToCompleteLC(pob, period);
//                BigDecimal billingsInExcess = getContractBillingsInExcess(pob, period);
//                BigDecimal revenueInExcess = getContractRevenueInExcess(pob, period);

                row = worksheet.createRow(insertRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getName());
                setCellValue(row, 1, percentComplete);
                setCellValue(row, 2, revenueITD);
                setCellValue(row, 3, liquidatedDamage);
                setCellValue(row, 4, localCostITDLC);
                //setCellValue(row, 5, lossReserveITD);
                setCellValue(row, 6, grossProfitITD);
                setCellValue(row, 7, grossMarginITD);
                //setCellValue(row, 10, costToComplete);
//                setCellValue(row, 11, billingsInExcess);
//                setCellValue(row, 12, revenueInExcess);

                insertRow++;
            }
        }
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

        BigDecimal revenueToRecognize = getRevenueRecognizeITD(contract, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
        BigDecimal cumulativeCostGoodsSoldLC = getCumulativeCostGoodsSoldLC(contract, period);
        BigDecimal EstimatedGrossProfitLC = getEstimatedGrossProfit(contract, period);

        row = worksheet.getRow(15);
        setCellValue(row, 1, revenueToRecognize);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, cumulativeCostGoodsSoldLC);
        setCellValue(row, 5, EstimatedGrossProfitLC);

        // Split the contract into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        printMonthlyPobsGroups(10, 18, worksheet, pocPobs, period);
        printMonthlyPobsGroups(11, 21, worksheet, pitPobs, period);
        printMonthlyPobsGroups(12, 24, worksheet, slPobs, period);

        printMonthlyPobsDetailLines(18, 24, worksheet, pocPobs, period);
        printMonthlyPobsDetailLines(21, 24, worksheet, pitPobs, period);
        printMonthlyPobsDetailLines(24, 24, worksheet, slPobs, period);

        return worksheet;
    }

    public void printMonthlyPobsGroups(int single, int total, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {

        XSSFRow row;
        BigDecimal revenueToRecognize = getRevenueRecognizeITD(pGroup, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(pGroup, period);
        BigDecimal cumulativeCostGoodsSoldLC = getCumulativeCostGoodsSoldLC(pGroup, period);
        BigDecimal EstimatedGrossProfitLC = getEstimatedGrossProfit(pGroup, period);

        row = worksheet.getRow(single);
        setCellValue(row, 1, revenueToRecognize);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, cumulativeCostGoodsSoldLC);
        setCellValue(row, 5, EstimatedGrossProfitLC);

        row = worksheet.getRow(total);
        setCellValue(row, 1, revenueToRecognize);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, cumulativeCostGoodsSoldLC);
        setCellValue(row, 5, EstimatedGrossProfitLC);
    }

    public void printMonthlyPobsDetailLines(int insertRow, int shiftRow, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {
        XSSFRow row;
        Cell cell = null;
        if (pGroup.getPerformanceObligations().size() > 0) {
            worksheet.shiftRows(insertRow, shiftRow, pGroup.getPerformanceObligations().size(), true, false);

            for (PerformanceObligation pob : pGroup.getPerformanceObligations()) {
                BigDecimal revenueToRecognize = getRevenueRecognizeITD(pob, period);
                BigDecimal liquidatedDamage = getLiquidatedDamages(pob, period);
                BigDecimal cumulativeCostGoodsSoldLC = getCumulativeCostGoodsSoldLC(pob, period);
                BigDecimal EstimatedGrossProfitLC = getEstimatedGrossProfit(pob, period);

                row = worksheet.createRow(insertRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getName());
                setCellValue(row, 1, revenueToRecognize);
                setCellValue(row, 2, liquidatedDamage);
                //setCellValue(row, 3, cumulativeCostGoodsSoldLC);
                setCellValue(row, 5, EstimatedGrossProfitLC);
                insertRow++;
            }
        }
    }

    public void generateReportQuarterlyIncomeImpact(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-3"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-5"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-4");

            List<FinancialPeriod> qtdPeriods = financialPeriodService.getQTDFinancialPeriods(viewSupport.getCurrentPeriod());
            worksheet = writeQuarterlyOrAnnualIncomeImpact(worksheet, contract, qtdPeriods);

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
            List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());
            worksheet = writeQuarterlyOrAnnualIncomeImpact(worksheet, contract, ytdPeriods);
            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writeQuarterlyOrAnnualIncomeImpact(XSSFSheet worksheet, Contract contract, List<FinancialPeriod> listPeriods) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        XSSFRow contract_name = worksheet.getRow(1);
        cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        BigDecimal revenueToRecognize = getAccuRevenueToRecognizeLC(contract, listPeriods);
        BigDecimal liquidatedDamage = getAccuLiquidatedDamageCC(contract, listPeriods);
        BigDecimal cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(contract, listPeriods);
        BigDecimal EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(contract, listPeriods);

        row = worksheet.getRow(15);
        setCellValue(row, 1, revenueToRecognize);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, cumulativeCostGoodsSoldLC);
        setCellValue(row, 5, EstimatedGrossProfitLC);

        // Split the contract into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        //calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        //calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        //calculationService.executeBusinessRules(slPobs, period);

        printQAByPobsGroups(10, 18, worksheet, pocPobs, listPeriods);
        printQAByPobsGroups(11, 21, worksheet, pitPobs, listPeriods);
        printQAByPobsGroups(12, 24, worksheet, slPobs, listPeriods);

        printQAByPobsDetailLines(18, 24, worksheet, pocPobs, listPeriods);
        printQAByPobsDetailLines(21, 24, worksheet, pitPobs, listPeriods);
        printQAByPobsDetailLines(24, 24, worksheet, slPobs, listPeriods);

        return worksheet;
    }

    public void printQAByPobsGroups(int single, int total, XSSFSheet worksheet, PerformanceObligationGroup pGroup, List<FinancialPeriod> listPeriods) throws Exception {

        BigDecimal revenueToRecognize = getAccuRevenueToRecognizeLC(pGroup, listPeriods);
        BigDecimal liquidatedDamage = getAccuLiquidatedDamageCC(pGroup, listPeriods);
        BigDecimal cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(pGroup, listPeriods);
        BigDecimal EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(pGroup, listPeriods);

        XSSFRow row;
        row = worksheet.getRow(single);
        setCellValue(row, 1, revenueToRecognize);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, cumulativeCostGoodsSoldLC);
        setCellValue(row, 5, EstimatedGrossProfitLC);

        row = worksheet.getRow(total);
        setCellValue(row, 1, revenueToRecognize);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, cumulativeCostGoodsSoldLC);
        setCellValue(row, 5, EstimatedGrossProfitLC);
    }

    public void printQAByPobsDetailLines(int insertRow, int shiftRow, XSSFSheet worksheet, PerformanceObligationGroup pGroup, List<FinancialPeriod> listPeriods) throws Exception {
        XSSFRow row;
        Cell cell = null;
        // Fill in the POB detail lines.  We'll shift the surrounding rows down as we go to make room for the detail lines.
        if (pGroup.getPerformanceObligations().size() > 0) {
            worksheet.shiftRows(insertRow, shiftRow, pGroup.getPerformanceObligations().size(), true, false);

            for (PerformanceObligation pob : pGroup.getPerformanceObligations()) {
                BigDecimal revenueToRecognize = getAccuRevenueToRecognizeLC(pob, listPeriods);
                BigDecimal liquidatedDamage = getAccuLiquidatedDamageCC(pob, listPeriods);
                BigDecimal cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(pob, listPeriods);
                BigDecimal EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(pob, listPeriods);

                row = worksheet.createRow(insertRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getName());
                setCellValue(row, 1, revenueToRecognize);
                setCellValue(row, 2, liquidatedDamage);
                setCellValue(row, 3, cumulativeCostGoodsSoldLC);
                setCellValue(row, 5, EstimatedGrossProfitLC);

                insertRow++;
            }
        }
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
            List<FinancialPeriod> qtdPeriods = financialPeriodService.getQTDFinancialPeriods(viewSupport.getCurrentPeriod());
            worksheet = writeQuarterlyOrAnnualIncomeImpact(worksheet, contract, qtdPeriods);

            worksheet = workbook.getSheet("Contract Summary-5");
            List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());
            worksheet = writeQuarterlyOrAnnualIncomeImpact(worksheet, contract, ytdPeriods);

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public void generateReportFinancialSummary(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
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
            XSSFSheet worksheet = workbook.getSheet("Financial Summary-1");

            worksheet = writeFinancialSummary1(worksheet, contract);
            worksheet = workbook.getSheet("Financial Summary-3");
            worksheet = writeFinancialSummary2(worksheet, contract);

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writeFinancialSummary1(XSSFSheet worksheet, Contract contract) throws Exception {
        Cell cell = null;
        XSSFRow rowTitle = worksheet.getRow(1);
        cell = rowTitle.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());
        for (int i = 0; i < ytdPeriods.size(); i++) {
            int colNum = i + 1;
            printSummaryByPobs(colNum, worksheet, contract, ytdPeriods.get(i));
        }
        return worksheet;
    }

    public void printSummaryByPobs(int colNum, XSSFSheet worksheet, Contract contract, FinancialPeriod period) throws Exception {

        XSSFRow row;
        int startRow = 7;
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        BigDecimal revenueToRecognize = getRevenueRecognizeITD(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);
        revenueToRecognize = getRevenueRecognizeITD(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);
        revenueToRecognize = getRevenueRecognizeITD(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);

        startRow = startRow + 2;
        BigDecimal liquidatedDamage = getLiquidatedDamages(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, liquidatedDamage);
        liquidatedDamage = getLiquidatedDamages(pitPobs, period);
        row = worksheet.getRow(startRow++);
        //getting NPE
        //setCellValue(row, colNum, liquidatedDamage);
        liquidatedDamage = getLiquidatedDamages(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, liquidatedDamage);

        startRow = startRow + 2;
        BigDecimal cumulativeCostGoodsSoldLC = getCumulativeCostGoodsSoldLC(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, cumulativeCostGoodsSoldLC);
        cumulativeCostGoodsSoldLC = getCumulativeCostGoodsSoldLC(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, cumulativeCostGoodsSoldLC);
        cumulativeCostGoodsSoldLC = getCumulativeCostGoodsSoldLC(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, cumulativeCostGoodsSoldLC);

        startRow = startRow + 2;
        BigDecimal costsIncurredCOGS = new BigDecimal(BigInteger.ZERO);//dummay data for now TODO Need to get values for all 3 pob group with real
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, costsIncurredCOGS);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, costsIncurredCOGS);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, costsIncurredCOGS);

        startRow = startRow + 2;
        BigDecimal reserveLCM_COGS = new BigDecimal(BigInteger.ZERO); //dummay data for now TODO Need to get values for all 3 pob group with real
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, reserveLCM_COGS);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, reserveLCM_COGS);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, reserveLCM_COGS);

        startRow = startRow + 2;
        BigDecimal totalCOGS = new BigDecimal(BigInteger.ZERO); //dummay data for now TODO Need to get values for all 3 pob group with real
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, totalCOGS);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, totalCOGS);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, totalCOGS);

        startRow = startRow + 3;
        BigDecimal grossProfitITD = getEstimatedGrossProfit(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossProfitITD);
        grossProfitITD = getEstimatedGrossProfit(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossProfitITD);
        grossProfitITD = getEstimatedGrossProfit(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossProfitITD);

        startRow = startRow + 2;
        BigDecimal grossMarginITD = getEstimatedGrossMargin(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossMarginITD);
        grossMarginITD = getEstimatedGrossMargin(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossMarginITD);
        grossMarginITD = getEstimatedGrossMargin(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossMarginITD);

    }

    public XSSFSheet writeFinancialSummary2(XSSFSheet worksheet, Contract contract) throws Exception {

        Cell cell = null;
        XSSFRow rowTitle = worksheet.getRow(1);
        cell = rowTitle.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());
        for (int i = 0; i < ytdPeriods.size(); i++) {
            int colNum = i + 1;
            printSummaryByContract(colNum, worksheet, contract, ytdPeriods.get(i));
        }
        return worksheet;
    }

    public void printSummaryByContract(int colNum, XSSFSheet worksheet, Contract contract, FinancialPeriod period) throws Exception {

        XSSFRow row;

        BigDecimal transactionPrice = getTransactionPrice(contract, period);
        BigDecimal revenueToRecognize = getRevenueRecognizeITD(contract, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
        BigDecimal EAC = getEAC(contract, period);
        BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(contract, period);
        BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(contract, period);
        BigDecimal localCostITDLC = getCostOfGoodsSold(contract, period);
        BigDecimal billToDate = contract.getTotalBillingsLocalCurrency();

        row = worksheet.getRow(6);
        setCellValue(row, colNum, transactionPrice);
        row = worksheet.getRow(7);
        setCellValue(row, colNum, revenueToRecognize);
        row = worksheet.getRow(11);
        setCellValue(row, colNum, liquidatedDamage);
        row = worksheet.getRow(16);
        setCellValue(row, colNum, EAC);
        row = worksheet.getRow(17);
        setCellValue(row, colNum, localCostITDLC);
        row = worksheet.getRow(20);
        setCellValue(row, colNum, estimatedGrossProfit);
        row = worksheet.getRow(21);
        //Getting NPE
        //setCellValue(row, colNum, estimatedGrossMargin);

        row = worksheet.getRow(25);
        setCellValue(row, colNum, transactionPrice);
        row = worksheet.getRow(26);
        setCellValue(row, colNum, billToDate);
        row = worksheet.getRow(29);
        setCellValue(row, colNum, liquidatedDamage);
        row = worksheet.getRow(32);
        setCellValue(row, colNum, EAC);
        row = worksheet.getRow(33);
        setCellValue(row, colNum, localCostITDLC);

    }

    public void generateCompanyReportFinancialSummary(InputStream inputStream, FileOutputStream outputStream) throws Exception {
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

    private BigDecimal getCostOfGoodsSold(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("COST_OF_GOODS_SOLD_ITD_LC", measureable, period).getValue();
    }

    private BigDecimal getPercentComplete(Measurable measureable, FinancialPeriod period) {
        return calculationService.getDecimalMetric("PERCENT_COMPLETE", measureable, period).getValue();
    }

    private BigDecimal getRevenueRecognizeITD(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("REVENUE_TO_RECOGNIZE_ITD_LC", measureable, period).getValue();
    }

    private BigDecimal getLossReserveITD(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("LOSS_RESERVE_ITD_LC", measureable, period).getValue();
    }

    private BigDecimal getContractCostToCompleteLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_COST_TO_COMPLETE_LC", measureable, period).getValue();
    }

    private BigDecimal getContractBillingsInExcess(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_BILLINGS_IN_EXCESS_LC", measureable, period).getValue();
    }

    private BigDecimal getContractRevenueInExcess(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_REVENUE_IN_EXCESS_LC", measureable, period).getValue();
    }

    private BigDecimal getCumulativeCostGoodsSoldLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CUMULATIVE_COST_OF_GOODS_SOLD_LC", measureable, period).getValue();
    }

    private BigDecimal getAccuRevenueToRecognizeLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("REVENUE_TO_RECOGNIZE_ITD_LC", measureable, qtdPeriods).getValue();
    }

    private BigDecimal getAccuLiquidatedDamageCC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("LIQUIDATED_DAMAGES_ITD_CC", measureable, qtdPeriods).getValue();
    }

    private BigDecimal getAccuCumulativeCostGoodsSoldLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("CUMULATIVE_COST_OF_GOODS_SOLD_LC", measureable, qtdPeriods).getValue();
    }

    private BigDecimal getAccuEstimatedGrossProfitLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("ESTIMATED_GROSS_PROFIT_LC", measureable, qtdPeriods).getValue();
    }

}
