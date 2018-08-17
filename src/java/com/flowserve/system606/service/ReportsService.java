/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.PerformanceObligationGroup;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.RevenueMethod;
import com.flowserve.system606.view.PobOutput;
import com.flowserve.system606.view.ViewSupport;
import com.flowserve.system606.web.WebSession;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
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
    private MetricService metricService;
    @Inject
    private AdminService adminService;
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
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-1");

            worksheet = writeContractEsimatesReport(worksheet, contract);
            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));
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

        BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
        BigDecimal localCostCTDLC = getCostOfGoodsSold(contract, period);
        BigDecimal percentComplete = getPercentComplete(contract, period);
        BigDecimal revenueCTD = getRevenueRecognizeCTD(contract, period);
        BigDecimal lossReserveCTD = getLossReserveCTD(contract, period);
        BigDecimal grossProfitCTD = getEstimatedGrossProfit(contract, period);
        BigDecimal grossMarginCTD = getEstimatedGrossMargin(contract, period);
        BigDecimal costToComplete = getContractCostToCompleteLC(contract, period);
        BigDecimal billingsInExcess = getContractBillingsInExcess(contract, period);
        BigDecimal revenueInExcess = getContractRevenueInExcess(contract, period);
        BigDecimal billToDate = contract.getTotalBillingsLocalCurrency();

        row = worksheet.getRow(15);

        // TODO - Please change all setCell lines to the same approach below.
        setCellValue(row, 1, transactionPrice);
        setCellValue(row, 2, loquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setCellValue(row, 5, estimatedGrossMargin);

        setCellValue(row, 7, percentComplete);
        setCellValue(row, 8, revenueCTD);
        setCellValue(row, 9, liquidatedDamage);
        setCellValue(row, 10, localCostCTDLC);
        setCellValue(row, 11, lossReserveCTD);
        setCellValue(row, 12, grossProfitCTD);
        setCellValue(row, 13, grossMarginCTD);
        setCellValue(row, 15, billToDate);
        setCellValue(row, 16, costToComplete);
        setCellValue(row, 17, billingsInExcess);
        setCellValue(row, 18, revenueInExcess);

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
        BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(pGroup, period);

        BigDecimal liquidatedDamage = getLiquidatedDamages(pGroup, period);
        BigDecimal localCostCTDLC = getCostOfGoodsSold(pGroup, period);
        BigDecimal percentComplete = getPercentComplete(pGroup, period);
        BigDecimal revenueCTD = getRevenueRecognizeCTD(pGroup, period);
        BigDecimal lossReserveCTD = getLossReserveCTD(pGroup, period);
        BigDecimal grossProfitCTD = getEstimatedGrossProfit(pGroup, period);
        BigDecimal grossMarginCTD = getEstimatedGrossMargin(pGroup, period);
        BigDecimal costToComplete = getContractCostToCompleteLC(pGroup, period);
        BigDecimal billingsInExcess = getContractBillingsInExcess(pGroup, period);
        BigDecimal revenueInExcess = getContractRevenueInExcess(pGroup, period);
        BigDecimal billToDate = new BigDecimal(BigInteger.ZERO);

        // Percentage of completion Pobs.  Set total row for POC POBs
        row = worksheet.getRow(single);
        setCellValue(row, 1, transactionPrice);
        setCellValue(row, 2, loquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setCellValue(row, 5, estimatedGrossMargin);

        //setCellValue(row, 7, percentComplete);
        setCellValue(row, 8, revenueCTD);
        setCellValue(row, 9, liquidatedDamage);
        setCellValue(row, 10, localCostCTDLC);
        setCellValue(row, 11, lossReserveCTD);
        setCellValue(row, 12, grossProfitCTD);
        setCellValue(row, 13, grossMarginCTD);
        setCellValue(row, 15, billToDate);
        setCellValue(row, 16, costToComplete);
        setCellValue(row, 17, billingsInExcess);
        setCellValue(row, 18, revenueInExcess);

        // We have the same total 8 rows down
        row = worksheet.getRow(total);
        setCellValue(row, 1, transactionPrice);
        setCellValue(row, 2, loquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setCellValue(row, 5, estimatedGrossMargin);

        //setCellValue(row, 7, percentComplete);
        setCellValue(row, 8, revenueCTD);
        setCellValue(row, 9, liquidatedDamage);
        setCellValue(row, 10, localCostCTDLC);
        setCellValue(row, 11, lossReserveCTD);
        setCellValue(row, 12, grossProfitCTD);
        setCellValue(row, 13, grossMarginCTD);
        setCellValue(row, 15, billToDate);
        setCellValue(row, 16, costToComplete);
        setCellValue(row, 17, billingsInExcess);
        setCellValue(row, 18, revenueInExcess);
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
                BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(pob, period);

                BigDecimal liquidatedDamage = getLiquidatedDamages(pob, period);
                BigDecimal localCostCTDLC = getCostOfGoodsSold(pob, period);
                BigDecimal percentComplete = getPercentComplete(pob, period);
                BigDecimal revenueCTD = getRevenueRecognizeCTD(pob, period);
                //BigDecimal lossReserveCTD = getLossReserveCTD(pob, period);
                BigDecimal grossProfitCTD = getEstimatedGrossProfit(pob, period);
                BigDecimal grossMarginCTD = getEstimatedGrossMargin(pob, period);
                //BigDecimal costToComplete = getContractCostToCompleteLC(pob, period);
//                BigDecimal billingsInExcess = getContractBillingsInExcess(pob, period);
//                BigDecimal revenueInExcess = getContractRevenueInExcess(pob, period);

                row = worksheet.createRow(insertRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getName());
                setCellValue(row, 1, transactionPrice);
                setCellValue(row, 2, loquidatedDamage);
                setCellValue(row, 3, EAC);
                setCellValue(row, 4, estimatedGrossProfit);
                setCellValue(row, 5, estimatedGrossMargin);

                //setCellValue(row, 7, percentComplete);
                setCellValue(row, 8, revenueCTD);
                setCellValue(row, 9, liquidatedDamage);
                setCellValue(row, 10, localCostCTDLC);
                //setCellValue(row, 11, lossReserveCTD);
                setCellValue(row, 12, grossProfitCTD);
                setCellValue(row, 13, grossMarginCTD);
//                setCellValue(row, 15, billToDate);
//                setCellValue(row, 16, costToComplete);
//                setCellValue(row, 17, billingsInExcess);
//                setCellValue(row, 18, revenueInExcess);

                insertRow++;
            }
        }
    }

    public void generateReportByFinancialPeriod(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-2");

            worksheet = writeReportByFinancialPeriod(worksheet, contract);
            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));
            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writeReportByFinancialPeriod(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        XSSFRow contract_name = worksheet.getRow(1);
        cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        FinancialPeriod period = webSession.getCurrentPeriod();

        BigDecimal revenueToRecognize = getRevenueRecognizeCTD(contract, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
        BigDecimal cumulativeCostGoodsSoldLC = getCumulativeCostGoodsSoldLC(contract, period);
        BigDecimal EstimatedGrossProfitLC = getEstimatedGrossProfit(contract, period);

        row = worksheet.getRow(15);
        setCellValue(row, 1, revenueToRecognize);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, cumulativeCostGoodsSoldLC);
        setCellValue(row, 5, EstimatedGrossProfitLC);

        List<FinancialPeriod> qtdPeriods = financialPeriodService.getQTDFinancialPeriods(viewSupport.getCurrentPeriod());
        revenueToRecognize = getAccuRevenueToRecognizeLC(contract, qtdPeriods);
        liquidatedDamage = getAccuLiquidatedDamageCC(contract, qtdPeriods);
        cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(contract, qtdPeriods);
        EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(contract, qtdPeriods);

        setCellValue(row, 9, revenueToRecognize);
        setCellValue(row, 10, liquidatedDamage);
        setCellValue(row, 11, cumulativeCostGoodsSoldLC);
        setCellValue(row, 13, EstimatedGrossProfitLC);

        List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());
        revenueToRecognize = getAccuRevenueToRecognizeLC(contract, ytdPeriods);
        liquidatedDamage = getAccuLiquidatedDamageCC(contract, ytdPeriods);
        cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(contract, ytdPeriods);
        EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(contract, ytdPeriods);

        setCellValue(row, 17, revenueToRecognize);
        setCellValue(row, 18, liquidatedDamage);
        setCellValue(row, 19, cumulativeCostGoodsSoldLC);
        setCellValue(row, 21, EstimatedGrossProfitLC);

        // Split the contract into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        printFinancialPobsGroups(10, 18, worksheet, pocPobs, period, qtdPeriods, ytdPeriods);
        printFinancialPobsGroups(11, 21, worksheet, pitPobs, period, qtdPeriods, ytdPeriods);
        printFinancialPobsGroups(12, 24, worksheet, slPobs, period, qtdPeriods, ytdPeriods);

        printFinancialPobsDetailLines(18, 24, worksheet, pocPobs, period, qtdPeriods, ytdPeriods);
        printFinancialPobsDetailLines(21, 24, worksheet, pitPobs, period, qtdPeriods, ytdPeriods);
        printFinancialPobsDetailLines(24, 24, worksheet, slPobs, period, qtdPeriods, ytdPeriods);

        return worksheet;
    }

    public void printFinancialPobsGroups(int single, int total, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period, List<FinancialPeriod> qtdPeriods, List<FinancialPeriod> ytdPeriods) throws Exception {

        XSSFRow row;
        //for monthly report
        BigDecimal revenueToRecognize = getRevenueRecognizeCTD(pGroup, period);
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

        //for quartly report
        revenueToRecognize = getAccuRevenueToRecognizeLC(pGroup, qtdPeriods);
        liquidatedDamage = getAccuLiquidatedDamageCC(pGroup, qtdPeriods);
        cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(pGroup, qtdPeriods);
        EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(pGroup, qtdPeriods);

        row = worksheet.getRow(single);
        setCellValue(row, 9, revenueToRecognize);
        setCellValue(row, 10, liquidatedDamage);
        setCellValue(row, 11, cumulativeCostGoodsSoldLC);
        setCellValue(row, 13, EstimatedGrossProfitLC);

        row = worksheet.getRow(total);
        setCellValue(row, 9, revenueToRecognize);
        setCellValue(row, 10, liquidatedDamage);
        setCellValue(row, 11, cumulativeCostGoodsSoldLC);
        setCellValue(row, 13, EstimatedGrossProfitLC);

        //for annually report
        revenueToRecognize = getAccuRevenueToRecognizeLC(pGroup, ytdPeriods);
        liquidatedDamage = getAccuLiquidatedDamageCC(pGroup, ytdPeriods);
        cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(pGroup, ytdPeriods);
        EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(pGroup, ytdPeriods);

        row = worksheet.getRow(single);
        setCellValue(row, 17, revenueToRecognize);
        setCellValue(row, 18, liquidatedDamage);
        setCellValue(row, 19, cumulativeCostGoodsSoldLC);
        setCellValue(row, 21, EstimatedGrossProfitLC);

        row = worksheet.getRow(total);
        setCellValue(row, 17, revenueToRecognize);
        setCellValue(row, 18, liquidatedDamage);
        setCellValue(row, 19, cumulativeCostGoodsSoldLC);
        setCellValue(row, 21, EstimatedGrossProfitLC);

    }

    public void printFinancialPobsDetailLines(int insertRow, int shiftRow, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period, List<FinancialPeriod> qtdPeriods, List<FinancialPeriod> ytdPeriods) throws Exception {
        XSSFRow row;
        Cell cell = null;
        if (pGroup.getPerformanceObligations().size() > 0) {
            worksheet.shiftRows(insertRow, shiftRow, pGroup.getPerformanceObligations().size(), true, false);

            for (PerformanceObligation pob : pGroup.getPerformanceObligations()) {
                //for monthly report
                BigDecimal revenueToRecognize = getRevenueRecognizeCTD(pob, period);
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

                //for quaterly report
                revenueToRecognize = getAccuRevenueToRecognizeLC(pob, qtdPeriods);
                liquidatedDamage = getAccuLiquidatedDamageCC(pob, qtdPeriods);
                cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(pob, qtdPeriods);
                EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(pob, qtdPeriods);

                setCellValue(row, 9, revenueToRecognize);
                setCellValue(row, 10, liquidatedDamage);
                setCellValue(row, 11, cumulativeCostGoodsSoldLC);
                setCellValue(row, 13, EstimatedGrossProfitLC);

                //for annually report
                revenueToRecognize = getAccuRevenueToRecognizeLC(pob, ytdPeriods);
                liquidatedDamage = getAccuLiquidatedDamageCC(pob, ytdPeriods);
                cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(pob, ytdPeriods);
                EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(pob, ytdPeriods);

                setCellValue(row, 17, revenueToRecognize);
                setCellValue(row, 18, liquidatedDamage);
                setCellValue(row, 19, cumulativeCostGoodsSoldLC);
                setCellValue(row, 21, EstimatedGrossProfitLC);
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

            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));
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
            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));

            worksheet = workbook.getSheet("Contract Summary-2");
            worksheet = writeReportByFinancialPeriod(worksheet, contract);
            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));
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
            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));
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

        BigDecimal revenueToRecognize = getRevenueRecognizeCTD(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);
        revenueToRecognize = getRevenueRecognizeCTD(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);
        revenueToRecognize = getRevenueRecognizeCTD(slPobs, period);
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
        BigDecimal grossProfitCTD = getEstimatedGrossProfit(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossProfitCTD);
        grossProfitCTD = getEstimatedGrossProfit(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossProfitCTD);
        grossProfitCTD = getEstimatedGrossProfit(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossProfitCTD);

        startRow = startRow + 2;
        BigDecimal grossMarginCTD = getEstimatedGrossMargin(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossMarginCTD);
        grossMarginCTD = getEstimatedGrossMargin(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossMarginCTD);
        grossMarginCTD = getEstimatedGrossMargin(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, grossMarginCTD);

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
        BigDecimal revenueToRecognize = getRevenueRecognizeCTD(contract, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
        BigDecimal EAC = getEAC(contract, period);
        BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(contract, period);
        BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(contract, period);
        BigDecimal localCostCTDLC = getCostOfGoodsSold(contract, period);
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
        setCellValue(row, colNum, localCostCTDLC);
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
        setCellValue(row, colNum, localCostCTDLC);

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
        return calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_CTD_CC", measureable, period).getValue();
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
        if (value == null) {
            return;
        }
        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        CellStyle currentStyle = cell.getCellStyle();
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(currentStyle);

    }

    private BigDecimal getCostOfGoodsSold(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("COST_OF_GOODS_SOLD_CTD_LC", measureable, period).getValue();
    }

    private BigDecimal getPercentComplete(Measurable measureable, FinancialPeriod period) {
        return calculationService.getDecimalMetric("PERCENT_COMPLETE", measureable, period).getValue();
    }

    private BigDecimal getRevenueRecognizeCTD(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("REVENUE_TO_RECOGNIZE_CTD_CC", measureable, period).getValue();
    }

    private BigDecimal getLossReserveCTD(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("LOSS_RESERVE_CTD_LC", measureable, period).getValue();
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
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("REVENUE_TO_RECOGNIZE_CTD_CC", measureable, qtdPeriods).getValue();
    }

    private BigDecimal getAccuLiquidatedDamageCC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("LIQUIDATED_DAMAGES_CTD_CC", measureable, qtdPeriods).getValue();
    }

    private BigDecimal getAccuCumulativeCostGoodsSoldLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("CUMULATIVE_COST_OF_GOODS_SOLD_LC", measureable, qtdPeriods).getValue();
    }

    private BigDecimal getAccuEstimatedGrossProfitLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("ESTIMATED_GROSS_PROFIT_LC", measureable, qtdPeriods).getValue();
    }

    public void generatePobOutput() throws Exception {

        // Move to C:\tmp on local machine.  This way it also works on Unix servers.
        String inputFile = "/tmp/POB_Output.accdb";

        // Let's change to
        // Add access DB back to project.
        // Change front end button to p:fileDownload
        // When user clicks button, copy the DB to a new temp copy  in the /tmp dir
        // Populate new file.
        // Download the new file.
        // I've highlighted the contract level metrics below.   can you create a new table for contracts?
        // You'll need to change the structure of the Access DB to remove the contract fields and move to new table.
        // Create another table for billings.  Same format just output all billing data.  Billing data is contract level, so no POB ID there.
        // Below only outputs for current period.  We'll need another outer loop for looping through all periods all the way to current period, starting at NOV-17.
        // I have added convenience method for financlaiPeriod.getNextPeriod() for other reasons, you can use it here.  Or you can start at current period and loop backward
        // through NOV-17, but we do need NOV-17.
        // Please use calculationService.getXXMetric instead of viewSupport below.  ViewSupport is designed to support the classes in the view package.  Web tier.
        // calc service method accepts period param.
        // General Note: NOV-17 is our start point for historical data.
        Logger.getLogger(PobOutput.class.getName()).log(Level.INFO, "dbPath " + inputFile);
        Logger.getLogger(CurrencyService.class.getName()).log(Level.INFO, "Processing POCI Data: " + inputFile);
        List<MetricType> metricTypes = metricService.getAllPobExcelInputMetricTypes();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        // Step 1: Loading or registering Oracle JDBC driver class
        try {
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        } catch (ClassNotFoundException cnfex) {
            Logger.getLogger(CurrencyService.class.getName()).log(Level.INFO, "Problem in loading or registering MS Access JDBC driver");
        }

        try {
            String dbURL = "jdbc:ucanaccess://" + inputFile;
            Logger.getLogger(ReportsService.class.getName()).log(Level.INFO, "dbURL : " + dbURL);
            connection = DriverManager.getConnection(dbURL);
            PreparedStatement pst = connection.prepareStatement("INSERT INTO POB_Output "
                    + "(RU_CODE, CONTRACT_ID, POB_NAME, REVENUE_RECOGNITION_METHOD, TRANSACTION_PRICE_CC,LIQUIDATED_DAMAGES_CTD_CC,"
                    + "ESTIMATED_COST_AT_COMPLETION_LC,LOCAL_COSTS_CTD_LC,THIRD_PARTY_COSTS_CTD_LC,INTERCOMPANY_COSTS_CTD_LC,DELIVERY_DATE,"
                    + "CUMULATIVE_COST_OF_GOODS_SOLD_LC,PARTIAL_SHIPMENT_COSTS_LC,TOTAL_TRANS_PRICE_CC,BOOKING_DATE,THIRD_PARTY_COMMISSION_CTD_LC,"
                    + "THIRD_PARTY_COMMISSION_TO_RECOGNIZE_LC,ESTIMATED_GROSS_PROFIT_LC,CONTRACT_GROSS_PROFIT_LC,ESTIMATED_GROSS_MARGIN,PERCENT_COMPLETE,"
                    + "CONTRACT_COST_TO_COMPLETE_LC,CONTRACT_BILLINGS_IN_EXCESS_LC,CONTRACT_REVENUE_IN_EXCESS_LC,CONTRACT_PERCENT_COMPLETE,COST_OF_GOODS_SOLD_CTD_LC,"
                    + "COST_OF_GOODS_SOLD_PERIOD_LC,COST_GOODS_SOLD_BACKLOG_LC,CHANGE_IN_EAC_LC,REVENUE_TO_RECOGNIZE_CTD_CC,CONTRACT_REVENUE_TO_RECOGNIZE_CTD_CC,"
                    + "REVENUE_TO_RECOGNIZE_PERIOD_CC,LIQUIDATED_DAMAGES_TO_RECOGNIZE_CTD_CC,LIQUIDATED_DAMAGES_TO_RECOGNIZE_PERIOD_CC,LIQUIDATED_DAMAGES_BACKLOG_CC,"
                    + "NET_PERIOD_SALES_CC,TRANSACTION_PRICE_BACKLOG_CC,TRANSACTION_PRICE_NET_LD_LC,REMAINING_ESTIMATE_COMPLETE_LC,PROJECT_GAIN_LOSS_LC,"
                    + "PROJECT_GAIN_LOSS_BACKLOG_LC,CTD_STANDARD_COSTS_COGS_LC,LOSS_CONTRACT_ADJUSTED_LC,TOTAL_COST_GOODS_SOLD_LC,LOSS_RESERVE_CTD_LC,"
                    + "LOSS_RESERVE_PERIOD_ADJ_LC,LOSS_RESERVE_ADJ_CUMULATIVE_LC,GROSS_PROFIT_LOSS_PERIOD_LC) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                    + ", ?, ?, ?, ?, ?)");

            String[] reportingUnits = {"1100", "5050",
                "7866", "8405", "1205", "8225"};

            for (String code : reportingUnits) {
                ReportingUnit reportingUnit = adminService.findReportingUnitByCode(code);
                for (Contract contract : reportingUnit.getContracts()) {
                    for (PerformanceObligation pob : contract.getPerformanceObligations()) {
                        Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree POB ID: " + pob.getId());
                        //pst.setString(1,name);
                        pst.setString(1, reportingUnit.getCode());
                        pst.setString(2, String.valueOf(contract.getId()));
                        pst.setString(3, pob.getName());

                        // Use RevenueMethod .shortName here.
                        pst.setBigDecimal(4, BigDecimal.ZERO);//TODO REVENUE_RECOGNITION_METHOD

                        pst.setBigDecimal(5, zeroIfNull(viewSupport.getCurrencyMetric("TRANSACTION_PRICE_CC", pob).getCcValue()));
                        pst.setBigDecimal(6, zeroIfNull(viewSupport.getCurrencyMetric("LIQUIDATED_DAMAGES_CTD_CC", pob).getCcValue()));
                        pst.setBigDecimal(7, zeroIfNull(viewSupport.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", pob).getLcValue()));
                        pst.setBigDecimal(8, zeroIfNull(viewSupport.getCurrencyMetric("LOCAL_COSTS_CTD_LC", pob).getLcValue()));
                        pst.setBigDecimal(9, zeroIfNull(viewSupport.getCurrencyMetric("THIRD_PARTY_COSTS_CTD_LC", pob).getLcValue()));
                        pst.setBigDecimal(10, zeroIfNull(viewSupport.getCurrencyMetric("INTERCOMPANY_COSTS_CTD_LC", pob).getLcValue()));
                        pst.setDate(11, null);//TODO DELIVERY_DATE
                        pst.setBigDecimal(12, zeroIfNull(viewSupport.getCurrencyMetric("CUMULATIVE_COST_OF_GOODS_SOLD_LC", pob).getLcValue()));
                        pst.setBigDecimal(13, zeroIfNull(viewSupport.getCurrencyMetric("PARTIAL_SHIPMENT_COSTS_LC", pob).getLcValue()));
                        pst.setBigDecimal(14, zeroIfNull(BigDecimal.ZERO));//TODO NPE viewSupport.getCurrencyMetric("TOTAL_TRANS_PRICE_CC", pob).getCcValue());
                        pst.setDate(15, null);//TODO BOOKING_DATE
                        pst.setBigDecimal(16, zeroIfNull(BigDecimal.ZERO));//TODO NEP viewSupport.getCurrencyMetric("THIRD_PARTY_COMMISSION_CTD_LC", pob).getLcValue());
                        // Contract level.
                        pst.setBigDecimal(17, BigDecimal.ZERO); //zeroIfNull(viewSupport.getCurrencyMetric("THIRD_PARTY_COMMISSION_TO_RECOGNIZE_LC", pob).getLcValue()));
                        // END
                        pst.setBigDecimal(18, zeroIfNull(viewSupport.getCurrencyMetric("ESTIMATED_GROSS_PROFIT_LC", pob).getLcValue()));
                        // Contract
                        pst.setBigDecimal(19, BigDecimal.ZERO); //zeroIfNull(viewSupport.getCurrencyMetric("CONTRACT_GROSS_PROFIT_LC", pob).getLcValue()));
                        //
                        pst.setBigDecimal(20, zeroIfNull(viewSupport.getDecimalMetric("ESTIMATED_GROSS_MARGIN", pob).getValue()));
                        pst.setBigDecimal(21, zeroIfNull(viewSupport.getDecimalMetric("PERCENT_COMPLETE", pob).getValue()));

                        // Remove these fo POB , they are contract level.  Move to another method for the contract table.
                        pst.setBigDecimal(22, BigDecimal.ZERO); // viewSupport.getCurrencyMetric("CONTRACT_COST_TO_COMPLETE_LC", pob).getLcValue());
                        pst.setBigDecimal(23, BigDecimal.ZERO); //viewSupport.getCurrencyMetric("CONTRACT_BILLINGS_IN_EXCESS_LC", pob).getLcValue());
                        pst.setBigDecimal(24, BigDecimal.ZERO); //viewSupport.getCurrencyMetric("CONTRACT_REVENUE_IN_EXCESS_LC", pob).getLcValue());
                        pst.setBigDecimal(25, BigDecimal.ZERO); //viewSupport.getDecimalMetric("CONTRACT_PERCENT_COMPLETE", pob).getValue());
                        // END

                        pst.setBigDecimal(26, zeroIfNull(viewSupport.getCurrencyMetric("COST_OF_GOODS_SOLD_CTD_LC", pob).getLcValue()));
                        pst.setBigDecimal(27, zeroIfNull(viewSupport.getCurrencyMetric("COST_OF_GOODS_SOLD_PERIOD_LC", pob).getLcValue()));
                        pst.setBigDecimal(28, zeroIfNull(viewSupport.getCurrencyMetric("COST_GOODS_SOLD_BACKLOG_LC", pob).getLcValue()));
                        pst.setBigDecimal(29, zeroIfNull(viewSupport.getCurrencyMetric("CHANGE_IN_EAC_LC", pob).getLcValue()));
                        pst.setBigDecimal(30, zeroIfNull(viewSupport.getCurrencyMetric("REVENUE_TO_RECOGNIZE_CTD_CC", pob).getCcValue()));

                        // Cocntract level.
                        pst.setBigDecimal(31, BigDecimal.ZERO); //viewSupport.getCurrencyMetric("CONTRACT_REVENUE_TO_RECOGNIZE_CTD_CC", pob).getCcValue());
                        //

                        pst.setBigDecimal(32, zeroIfNull(viewSupport.getCurrencyMetric("REVENUE_TO_RECOGNIZE_PERIOD_CC", pob).getCcValue()));
                        pst.setBigDecimal(33, zeroIfNull(viewSupport.getCurrencyMetric("LIQUIDATED_DAMAGES_TO_RECOGNIZE_CTD_CC", pob).getCcValue()));
                        pst.setBigDecimal(34, zeroIfNull(viewSupport.getCurrencyMetric("LIQUIDATED_DAMAGES_TO_RECOGNIZE_PERIOD_CC", pob).getCcValue()));
                        pst.setBigDecimal(35, zeroIfNull(viewSupport.getCurrencyMetric("LIQUIDATED_DAMAGES_BACKLOG_CC", pob).getCcValue()));
                        pst.setBigDecimal(36, zeroIfNull(viewSupport.getCurrencyMetric("NET_PERIOD_SALES_CC", pob).getCcValue()));
                        pst.setBigDecimal(37, zeroIfNull(viewSupport.getCurrencyMetric("TRANSACTION_PRICE_BACKLOG_CC", pob).getCcValue()));
                        pst.setBigDecimal(38, zeroIfNull(viewSupport.getCurrencyMetric("TRANSACTION_PRICE_NET_LD_LC", pob).getLcValue()));
                        pst.setBigDecimal(39, zeroIfNull(viewSupport.getCurrencyMetric("REMAINING_ESTIMATE_COMPLETE_LC", pob).getLcValue()));
                        pst.setBigDecimal(40, zeroIfNull(viewSupport.getCurrencyMetric("PROJECT_GAIN_LOSS_LC", pob).getLcValue()));
                        pst.setBigDecimal(41, zeroIfNull(viewSupport.getCurrencyMetric("PROJECT_GAIN_LOSS_BACKLOG_LC", pob).getLcValue()));
                        pst.setBigDecimal(42, zeroIfNull(viewSupport.getCurrencyMetric("CTD_STANDARD_COSTS_COGS_LC", pob).getLcValue()));
                        pst.setBigDecimal(43, zeroIfNull(viewSupport.getCurrencyMetric("LOSS_CONTRACT_ADJUSTED_LC", pob).getLcValue()));

                        // KJG Contract level
                        pst.setBigDecimal(44, BigDecimal.ZERO); //zeroIfNull(viewSupport.getCurrencyMetric("TOTAL_COST_GOODS_SOLD_LC", pob).getLcValue()));
                        pst.setBigDecimal(45, BigDecimal.ZERO); //viewSupport.getCurrencyMetric("LOSS_RESERVE_CTD_LC", pob).getLcValue());
                        pst.setBigDecimal(46, BigDecimal.ZERO); //viewSupport.getCurrencyMetric("LOSS_RESERVE_PERIOD_ADJ_LC", pob).getLcValue());
                        pst.setBigDecimal(47, BigDecimal.ZERO); // viewSupport.getCurrencyMetric("LOSS_RESERVE_ADJ_CUMULATIVE_LC", pob).getLcValue());
                        pst.setBigDecimal(48, BigDecimal.ZERO); //zeroIfNull(viewSupport.getCurrencyMetric("GROSS_PROFIT_LOSS_PERIOD_LC", pob).getLcValue()));
                        // END

                        pst.executeUpdate();
                    }
                }
            }

            connection.close();

        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
        }

        Logger.getLogger(ReportsService.class.getName()).log(Level.INFO, "Complete.");
    }

    BigDecimal zeroIfNull(BigDecimal decimal) {
        if (decimal == null) {
            return BigDecimal.ZERO;
        }

        return decimal;
    }

}
