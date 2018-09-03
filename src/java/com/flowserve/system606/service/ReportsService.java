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
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
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

//    public void generateContractEsimatesReport(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
//        calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
//        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
//            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
//            XSSFSheet worksheet = workbook.getSheet("Contract Summary-1");
//
//            worksheet = writeContractEsimatesReport(worksheet, contract);
//            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
//            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));
//            workbook.write(outputStream);
//            workbook.close();
//        }
//        inputStream.close();
//        outputStream.close();
//    }
    public XSSFSheet writeContractEsimatesReport(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = 0;
        XSSFRow rowTitle = worksheet.getRow(1);
        cell = rowTitle.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        // For now, get the period from the user's session.  Later we will be passing in the period from the reports generation page.
        FinancialPeriod period = webSession.getCurrentPeriod();
        row = worksheet.getRow(3);
        row.getCell(0).setCellValue(Date.valueOf(period.getEndDate()));

        // Execute buisness rules on the contract to fill in any values we might need at the contract level (e.g. gross margin)
        //calculationService.executeBusinessRules(contract, period);
        // Get the contract level values for the contract total row on the "Contract Summary Totals" row.
        BigDecimal transactionPrice = getTransactionPrice(contract, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
        BigDecimal EAC = getEAC(contract, period);
        BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(contract, period);
        BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(contract, period);

        BigDecimal liquidatedDamagePeriod = getLiquidatedDamagesPeriod(contract, period);
        BigDecimal localCostCTDLC = getCostOfGoodsSold(contract, period);
        BigDecimal percentComplete = getPercentComplete(contract, period);
        BigDecimal revenueCTD = getRevenueRecognizeCTD(contract, period);
        BigDecimal lossReserveCTD = getLossReserveCTD(contract, period);
        BigDecimal grossProfitCTD = getEstimatedGrossProfit(contract, period);
        BigDecimal grossMarginCTD = getEstimatedGrossMargin(contract, period);
        BigDecimal costToComplete = getContractCostToCompleteLC(contract, period);
        BigDecimal billingsInExcess = getContractBillingsInExcess(contract, period);
        BigDecimal revenueInExcess = getContractRevenueInExcess(contract, period);
        BigDecimal billToDate = getContractBillingsCTDLC(contract, period);
        BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(contract, period);
        BigDecimal thirdPartyCommCTD = getCThirdPartyCommissionCTDLC(contract, period);
        BigDecimal thirdPartyRecogLC = getCThirdPartyCommRegLC(contract, period);

        row = worksheet.getRow(13);
        setCellValue(row, 11, lossReservePeriodADJLC);

        row = worksheet.getRow(14);
        setCellValue(row, 6, thirdPartyCommCTD);
        setCellValue(row, 14, thirdPartyRecogLC);

        row = worksheet.getRow(16);

        // TODO - Please change all setCell lines to the same approach below.
        setCellValue(row, 1, transactionPrice);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setPercentCellValue(row, 5, estimatedGrossMargin);

        setPercentCellValue(row, 7, percentComplete);
        setCellValue(row, 8, revenueCTD);
        setCellValue(row, 9, liquidatedDamagePeriod);
        setCellValue(row, 10, localCostCTDLC);
        setCellValue(row, 11, lossReserveCTD);
        setCellValue(row, 12, grossProfitCTD);
        setPercentCellValue(row, 13, grossMarginCTD);
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

        printContractEsimatesPobsGroups(10, 19, worksheet, pocPobs, period);
        printContractEsimatesPobsGroups(11, 22, worksheet, pitPobs, period);
        printContractEsimatesPobsGroups(12, 25, worksheet, slPobs, period);

        int shiftInRow = printContractEsimatesPobsDetailLines(19, 25, worksheet, pocPobs, period);
        shiftInRow = printContractEsimatesPobsDetailLines(shiftInRow + 3, shiftInRow + 6, worksheet, pitPobs, period);
        shiftInRow = printContractEsimatesPobsDetailLines(shiftInRow + 3, shiftInRow + 6, worksheet, slPobs, period);

        return worksheet;
    }

    public void printContractEsimatesPobsGroups(int single, int total, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {

        XSSFRow row;
        BigDecimal transactionPrice = getTransactionPrice(pGroup, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(pGroup, period);
        BigDecimal EAC = getEAC(pGroup, period);
        BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(pGroup, period);
        BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(pGroup, period);

        BigDecimal liquidatedDamagePeriod = getLiquidatedDamagesPeriod(pGroup, period);
        BigDecimal localCostCTDLC = getCostOfGoodsSold(pGroup, period);
        //BigDecimal percentComplete = getPercentComplete(pGroup, period);
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
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setPercentCellValue(row, 5, estimatedGrossMargin);

        setCellValue(row, 8, revenueCTD);
        setCellValue(row, 9, liquidatedDamagePeriod);
        setCellValue(row, 10, localCostCTDLC);
        setCellValue(row, 11, lossReserveCTD);
        setCellValue(row, 12, grossProfitCTD);
        setPercentCellValue(row, 13, grossMarginCTD);
        setCellValue(row, 15, billToDate);
        setCellValue(row, 16, costToComplete);
        setCellValue(row, 17, billingsInExcess);
        setCellValue(row, 18, revenueInExcess);

        // We have the same total 8 rows down
        row = worksheet.getRow(total);
        setCellValue(row, 1, transactionPrice);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setPercentCellValue(row, 5, estimatedGrossMargin);

        //setCellValue(row, 7, percentComplete);
        setCellValue(row, 8, revenueCTD);
        setCellValue(row, 9, liquidatedDamagePeriod);
        setCellValue(row, 10, localCostCTDLC);
        setCellValue(row, 11, lossReserveCTD);
        setCellValue(row, 12, grossProfitCTD);
        setPercentCellValue(row, 13, grossMarginCTD);
        setCellValue(row, 15, billToDate);
        setCellValue(row, 16, costToComplete);
        setCellValue(row, 17, billingsInExcess);
        setCellValue(row, 18, revenueInExcess);
    }

    public int printContractEsimatesPobsDetailLines(int insertRow, int shiftRow, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {
        XSSFRow row;
        Cell cell = null;
        if (pGroup.getPerformanceObligations().size() > 0) {
            worksheet.shiftRows(insertRow, shiftRow, pGroup.getPerformanceObligations().size(), true, false);

            for (PerformanceObligation pob : pGroup.getPerformanceObligations()) {
                BigDecimal transactionPrice = getTransactionPrice(pob, period);
                BigDecimal liquidatedDamage = getLiquidatedDamages(pob, period);
                BigDecimal EAC = getEAC(pob, period);
                BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(pob, period);
                BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(pob, period);

                BigDecimal liquidatedDamagePeriod = getLiquidatedDamagesPeriod(pob, period);
                BigDecimal localCostCTDLC = getCostOfGoodsSold(pob, period);
                BigDecimal revenueCTD = getRevenueRecognizeCTD(pob, period);
                BigDecimal grossProfitCTD = getEstimatedGrossProfit(pob, period);
                BigDecimal grossMarginCTD = getEstimatedGrossMargin(pob, period);

                row = worksheet.createRow(insertRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getName());
                setCellValue(row, 1, transactionPrice);
                setCellValue(row, 2, liquidatedDamage);
                setCellValue(row, 3, EAC);
                setCellValue(row, 4, estimatedGrossProfit);
                setPercentCellValue(row, 5, estimatedGrossMargin);

                setCellValue(row, 8, revenueCTD);
                setCellValue(row, 9, liquidatedDamagePeriod);
                setCellValue(row, 10, localCostCTDLC);
                setCellValue(row, 12, grossProfitCTD);
                setPercentCellValue(row, 13, grossMarginCTD);

                insertRow++;
            }
        }
        return insertRow;
    }

//    public void generateReportByFinancialPeriod(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
//        calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
//        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
//            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
//            XSSFSheet worksheet = workbook.getSheet("Contract Summary-2");
//
//            worksheet = writeReportByFinancialPeriod(worksheet, contract);
//            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
//            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));
//            workbook.write(outputStream);
//        }
//        inputStream.close();
//        outputStream.close();
//
//    }
    public XSSFSheet writeReportByFinancialPeriod(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        XSSFRow contract_name = worksheet.getRow(1);
        cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        FinancialPeriod period = webSession.getCurrentPeriod();
        row = worksheet.getRow(3);
        row.getCell(0).setCellValue(Date.valueOf(period.getEndDate()));

        BigDecimal revenueToRecognize = getRevenueRecognizeCTD(contract, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
        BigDecimal cumulativeCostGoodsSoldLC = getCumulativeCostGoodsSoldLC(contract, period);
        BigDecimal EstimatedGrossProfitLC = getEstimatedGrossProfit(contract, period);
        BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(contract, period);

        BigDecimal tcpIncured = getTPCIncured(contract, period);
        BigDecimal acceleratedTCP = getAcceleratedTPC(contract, period);

        row = worksheet.getRow(13);
        setCellValue(row, 4, lossReservePeriodADJLC);

        row = worksheet.getRow(14);
        setCellValue(row, 6, tcpIncured);
        setCellValue(row, 7, acceleratedTCP);

        row = worksheet.getRow(16);
        setCellValue(row, 1, revenueToRecognize);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, cumulativeCostGoodsSoldLC);
        setCellValue(row, 5, EstimatedGrossProfitLC);

        List<FinancialPeriod> qtdPeriods = financialPeriodService.getQTDFinancialPeriods(viewSupport.getCurrentPeriod());
        revenueToRecognize = getAccuRevenueToRecognizeLC(contract, qtdPeriods);
        liquidatedDamage = getAccuLiquidatedDamageCC(contract, qtdPeriods);
        cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(contract, qtdPeriods);
        EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(contract, qtdPeriods);
        lossReservePeriodADJLC = getAccuLossReservePeriodADJLC(contract, qtdPeriods);
        tcpIncured = getAccuTPCIncured(contract, qtdPeriods);
        acceleratedTCP = getAccuAcceleratedTPC(contract, qtdPeriods);

        row = worksheet.getRow(13);
        setCellValue(row, 12, lossReservePeriodADJLC);

        row = worksheet.getRow(14);
        setCellValue(row, 14, tcpIncured);
        setCellValue(row, 15, acceleratedTCP);

        row = worksheet.getRow(16);
        setCellValue(row, 9, revenueToRecognize);
        setCellValue(row, 10, liquidatedDamage);
        setCellValue(row, 11, cumulativeCostGoodsSoldLC);
        setCellValue(row, 13, EstimatedGrossProfitLC);

        List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());
        revenueToRecognize = getAccuRevenueToRecognizeLC(contract, ytdPeriods);
        liquidatedDamage = getAccuLiquidatedDamageCC(contract, ytdPeriods);
        cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(contract, ytdPeriods);
        EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(contract, ytdPeriods);
        lossReservePeriodADJLC = getAccuLossReservePeriodADJLC(contract, ytdPeriods);
        tcpIncured = getAccuTPCIncured(contract, ytdPeriods);
        acceleratedTCP = getAccuAcceleratedTPC(contract, ytdPeriods);

        row = worksheet.getRow(13);
        setCellValue(row, 20, lossReservePeriodADJLC);

        row = worksheet.getRow(14);
        setCellValue(row, 22, tcpIncured);
        setCellValue(row, 23, acceleratedTCP);

        row = worksheet.getRow(16);
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

        printFinancialPobsGroups(10, 19, worksheet, pocPobs, period, qtdPeriods, ytdPeriods);
        printFinancialPobsGroups(11, 22, worksheet, pitPobs, period, qtdPeriods, ytdPeriods);
        printFinancialPobsGroups(12, 25, worksheet, slPobs, period, qtdPeriods, ytdPeriods);

        printFinancialPobsDetailLines(19, 25, worksheet, pocPobs, period, qtdPeriods, ytdPeriods);
        printFinancialPobsDetailLines(22, 25, worksheet, pitPobs, period, qtdPeriods, ytdPeriods);
        printFinancialPobsDetailLines(25, 25, worksheet, slPobs, period, qtdPeriods, ytdPeriods);

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

            worksheet = writeJournalEntryReport(worksheet, contract);
            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));
            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writeJournalEntryReport(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        XSSFRow contract_name = worksheet.getRow(2);
        cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());
        FinancialPeriod period = webSession.getCurrentPeriod();
        row = worksheet.getRow(5);
        row.getCell(0).setCellValue(Date.valueOf(period.getEndDate()));

        BigDecimal revenueToRecognizePeriod = getRevenueRecognizePeriodLC(contract, period);
        BigDecimal liquidatedDamageRecognizePeriodLC = getLiquidatedDamagesRecognizePeriodLC(contract, period);
        BigDecimal costGoodsSoldPeriodLC = getCostGoodsSoldPeriodLC(contract, period);
        BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(contract, period);
        BigDecimal revenueInExcess = getContractRevenueInExcess(contract, period);
        BigDecimal billingsInExcess = getContractBillingsInExcess(contract, period);
        BigDecimal bilingsPeriodLC = getContractBillingsPeriodLC(contract, period);
        BigDecimal commExp = getTPCIncured(contract, period);
        row = worksheet.getRow(14);
        setCellValue(row, 2, revenueToRecognizePeriod);
        setCellValue(row, 3, liquidatedDamageRecognizePeriodLC);
        setCellValue(row, 4, costGoodsSoldPeriodLC);
        setCellValue(row, 5, lossReservePeriodADJLC);
        setCellValue(row, 6, commExp);
        setCellValue(row, 7, BigDecimal.ZERO);//TODO FX_GAIN_LOSS
        setCellValue(row, 8, bilingsPeriodLC);
        setCellValue(row, 9, costGoodsSoldPeriodLC);
        setCellValue(row, 10, lossReservePeriodADJLC);
        setCellValue(row, 11, revenueInExcess);
        setCellValue(row, 12, billingsInExcess);
        setCellValue(row, 13, lossReservePeriodADJLC);
        setCellValue(row, 14, commExp);

        //For Contract Billings row
        row = worksheet.getRow(13);
        setCellValue(row, 8, bilingsPeriodLC);

        // Split the contract into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        printJournalEntryPobsGroups(9, worksheet, pocPobs, period);
        printJournalEntryPobsGroups(10, worksheet, pitPobs, period);
        printJournalEntryPobsGroups(11, worksheet, slPobs, period);
        return worksheet;
    }

    public void printJournalEntryPobsGroups(int single, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {

        XSSFRow row;
        BigDecimal revenueToRecognizePeriod = getRevenueRecognizePeriodLC(pGroup, period);
        BigDecimal liquidatedDamageRecognizePeriodLC = getLiquidatedDamagesRecognizePeriodLC(pGroup, period);
        BigDecimal costGoodsSoldPeriodLC = getCostGoodsSoldPeriodLC(pGroup, period);
        BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(pGroup, period);
        BigDecimal revenueInExcess = getContractRevenueInExcess(pGroup, period);
        BigDecimal billingsInExcess = getContractBillingsInExcess(pGroup, period);
        BigDecimal commExp = getTPCIncured(pGroup, period);
        // Percentage of completion Pobs.  Set total row for POC POBs
        row = worksheet.getRow(single);
        setCellValue(row, 2, revenueToRecognizePeriod);
        setCellValue(row, 3, liquidatedDamageRecognizePeriodLC);
        setCellValue(row, 4, costGoodsSoldPeriodLC);
        setCellValue(row, 5, lossReservePeriodADJLC);
        setCellValue(row, 6, commExp);
        setCellValue(row, 7, BigDecimal.ZERO);//TODO FX_GAIN_LOSS
        setCellValue(row, 8, BigDecimal.ZERO);//TODO BILLINGS_PERIOD_CC
        setCellValue(row, 9, costGoodsSoldPeriodLC);
        setCellValue(row, 10, lossReservePeriodADJLC);
        setCellValue(row, 11, revenueInExcess);
        setCellValue(row, 12, billingsInExcess);
        setCellValue(row, 13, lossReservePeriodADJLC);
        setCellValue(row, 14, commExp);

    }

    public void generateContractSummaryReport(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-1");
            //calculationService.executeBusinessRules(contract, webSession.getCurrentPeriod());
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
            workbook.removeSheetAt(workbook.getSheetIndex("Financial Summary-2"));
            workbook.removeSheetAt(workbook.getSheetIndex("Financial Summary-3"));
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
//            worksheet = workbook.getSheet("Financial Summary-3");
//            worksheet = writeFinancialSummary2(worksheet, contract);

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
        setPercentCellValue(row, colNum, grossMarginCTD);
        grossMarginCTD = getEstimatedGrossMargin(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setPercentCellValue(row, colNum, grossMarginCTD);
        grossMarginCTD = getEstimatedGrossMargin(slPobs, period);
        row = worksheet.getRow(startRow++);
        setPercentCellValue(row, colNum, grossMarginCTD);

    }

//    public XSSFSheet writeFinancialSummary2(XSSFSheet worksheet, Contract contract) throws Exception {
//
//        Cell cell = null;
//        XSSFRow rowTitle = worksheet.getRow(1);
//        cell = rowTitle.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//        cell.setCellValue(contract.getName());
//
//        List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());
//        for (int i = 0; i < ytdPeriods.size(); i++) {
//            int colNum = i + 1;
//            printSummaryByContract(colNum, worksheet, contract, ytdPeriods.get(i));
//        }
//        return worksheet;
//    }
//    public void printSummaryByContract(int colNum, XSSFSheet worksheet, Contract contract, FinancialPeriod period) throws Exception {
//
//        XSSFRow row;
//
//        BigDecimal transactionPrice = getTransactionPrice(contract, period);
//        BigDecimal revenueToRecognize = getRevenueRecognizeCTD(contract, period);
//        BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
//        BigDecimal EAC = getEAC(contract, period);
//        BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(contract, period);
//        BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(contract, period);
//        BigDecimal localCostCTDLC = getCostOfGoodsSold(contract, period);
//        BigDecimal billToDateLC = getContractBillingsCTDLC(contract, period);
//
//        row = worksheet.getRow(6);
//        setCellValue(row, colNum, transactionPrice);
//        row = worksheet.getRow(7);
//        setCellValue(row, colNum, revenueToRecognize);
//        row = worksheet.getRow(11);
//        setCellValue(row, colNum, liquidatedDamage);
//        row = worksheet.getRow(16);
//        setCellValue(row, colNum, EAC);
//        row = worksheet.getRow(17);
//        setCellValue(row, colNum, localCostCTDLC);
//        row = worksheet.getRow(20);
//        setCellValue(row, colNum, estimatedGrossProfit);
//        row = worksheet.getRow(21);
//        //Getting NPE
//        //setCellValue(row, colNum, estimatedGrossMargin);
//
//        row = worksheet.getRow(25);
//        setCellValue(row, colNum, transactionPrice);
//        row = worksheet.getRow(26);
//        setCellValue(row, colNum, billToDateLC);
//        row = worksheet.getRow(29);
//        setCellValue(row, colNum, liquidatedDamage);
//        row = worksheet.getRow(32);
//        setCellValue(row, colNum, EAC);
//        row = worksheet.getRow(33);
//        setCellValue(row, colNum, localCostCTDLC);
//
//    }
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

    private void setCellValue(XSSFRow row, int cellNum, BigDecimal value) {
        if (value == null) {
            return;
        }
        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        //CellStyle currentStyle = cell.getCellStyle();
        CellStyle currentStyle = row.getSheet().getWorkbook().createCellStyle();
        CreationHelper ch = row.getSheet().getWorkbook().getCreationHelper();
        cell.setCellValue(value.doubleValue());
        currentStyle.setDataFormat(ch.createDataFormat().getFormat("_(* #,##0_);_(* (#,##0);_(* \"-\"??_);_(@_)"));
        cell.setCellStyle(currentStyle);

    }

    private void setPercentCellValue(XSSFRow row, int cellNum, BigDecimal value) {
        if (value == null) {
            return;
        }
        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        //CellStyle currentStyle = cell.getCellStyle();
        CellStyle currentStyle = row.getSheet().getWorkbook().createCellStyle();
        CreationHelper ch = row.getSheet().getWorkbook().getCreationHelper();
        cell.setCellValue(value.doubleValue());
        currentStyle.setDataFormat(ch.createDataFormat().getFormat("0.00%"));
        cell.setCellStyle(currentStyle);
    }

    private BigDecimal getTransactionPrice(Measurable measureable, FinancialPeriod period) throws Exception {
        BigDecimal value = calculationService.getCurrencyMetric("TRANSACTION_PRICE_CC", measureable, period).getLcValue();
        return value != null ? value : new BigDecimal(BigInteger.ZERO);
    }

    private BigDecimal getLiquidatedDamages(Measurable measureable, FinancialPeriod period) throws Exception {
        BigDecimal value = calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_CTD_CC", measureable, period).getLcValue();
        return value != null ? value : new BigDecimal(BigInteger.ZERO);
    }

    private BigDecimal getLiquidatedDamagesPeriod(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_TO_RECOGNIZE_PERIOD_CC", measureable, period).getLcValue();
    }

    private BigDecimal getTPCIncured(Measurable measureable, FinancialPeriod period) throws Exception {
        BigDecimal value = calculationService.getCurrencyMetric("THIRD_PARTY_COMMISSION_TO_RECOGNIZE_PERIOD_LC", measureable, period).getLcValue();
        return value != null ? value : new BigDecimal(BigInteger.ZERO);
    }

    private BigDecimal getAcceleratedTPC(Measurable measureable, FinancialPeriod period) throws Exception {
        BigDecimal value = calculationService.getCurrencyMetric("LOSS_RESERVE_INCL_TPC_PERIOD_LC", measureable, period).getLcValue();
        return value != null ? value : new BigDecimal(BigInteger.ZERO);
    }

    private BigDecimal getEAC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", measureable, period).getLcValue();
    }

    private BigDecimal getEstimatedGrossProfit(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("ESTIMATED_GROSS_PROFIT_LC", measureable, period).getLcValue();
    }

    private BigDecimal getEstimatedGrossMargin(Measurable measureable, FinancialPeriod period) {
        return calculationService.getDecimalMetric("ESTIMATED_GROSS_MARGIN", measureable, period).getValue();
    }

    private BigDecimal getCostOfGoodsSold(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("COST_OF_GOODS_SOLD_CTD_LC", measureable, period).getLcValue();
    }

    private BigDecimal getPercentComplete(Measurable measureable, FinancialPeriod period) {
        return calculationService.getDecimalMetric("CONTRACT_PERCENT_COMPLETE", measureable, period).getValue();
    }

    private BigDecimal getRevenueRecognizeCTD(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("REVENUE_TO_RECOGNIZE_CTD_CC", measureable, period).getLcValue();
    }

    private BigDecimal getLossReserveCTD(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("LOSS_RESERVE_CTD_LC", measureable, period).getLcValue();
    }

    private BigDecimal getContractCostToCompleteLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_COST_TO_COMPLETE_LC", measureable, period).getLcValue();
    }

    private BigDecimal getContractBillingsInExcess(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_BILLINGS_IN_EXCESS_LC", measureable, period).getLcValue();
    }

    private BigDecimal getContractRevenueInExcess(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_REVENUE_IN_EXCESS_LC", measureable, period).getLcValue();
    }

    private BigDecimal getContractBillingsCTDLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_BILLINGS_CTD_CC", measureable, period).getLcValue();
    }

    private BigDecimal getContractBillingsPeriodLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_BILLINGS_PERIOD_CC", measureable, period).getLcValue();
    }

    private BigDecimal getCThirdPartyCommissionCTDLC(Measurable measureable, FinancialPeriod period) throws Exception {
        BigDecimal value = calculationService.getCurrencyMetric("THIRD_PARTY_COMMISSION_CTD_LC", measureable, period).getLcValue();
        return value != null ? value : new BigDecimal(BigInteger.ZERO);
    }

    private BigDecimal getCThirdPartyCommRegLC(Measurable measureable, FinancialPeriod period) throws Exception {
        BigDecimal value = calculationService.getCurrencyMetric("THIRD_PARTY_COMMISSION_TO_RECOGNIZE_CTD_LC", measureable, period).getLcValue();
        return value != null ? value : new BigDecimal(BigInteger.ZERO);
    }

    private BigDecimal getCumulativeCostGoodsSoldLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CUMULATIVE_COST_OF_GOODS_SOLD_LC", measureable, period).getLcValue();
    }

    private BigDecimal getRevenueRecognizePeriodLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("REVENUE_TO_RECOGNIZE_PERIOD_CC", measureable, period).getLcValue();
    }

    private BigDecimal getLiquidatedDamagesRecognizePeriodLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_TO_RECOGNIZE_PERIOD_CC", measureable, period).getLcValue();
    }

    private BigDecimal getCostGoodsSoldPeriodLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("COST_OF_GOODS_SOLD_PERIOD_LC", measureable, period).getLcValue();
    }

    private BigDecimal getLossReservePeriodADJLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("LOSS_RESERVE_PERIOD_ADJ_LC", measureable, period).getLcValue();
    }

    private BigDecimal getAccuLossReservePeriodADJLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("LOSS_RESERVE_PERIOD_ADJ_LC", measureable, qtdPeriods).getLcValue();
    }

    private BigDecimal getAccuTPCIncured(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("THIRD_PARTY_COMMISSION_TO_RECOGNIZE_PERIOD_LC", measureable, qtdPeriods).getLcValue();
    }

    private BigDecimal getAccuAcceleratedTPC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("LOSS_RESERVE_INCL_TPC_PERIOD_LC", measureable, qtdPeriods).getLcValue();
    }

    private BigDecimal getAccuRevenueToRecognizeLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("REVENUE_TO_RECOGNIZE_CTD_CC", measureable, qtdPeriods).getLcValue();
    }

    private BigDecimal getAccuLiquidatedDamageCC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("LIQUIDATED_DAMAGES_CTD_CC", measureable, qtdPeriods).getLcValue();
    }

    private BigDecimal getAccuCumulativeCostGoodsSoldLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("CUMULATIVE_COST_OF_GOODS_SOLD_LC", measureable, qtdPeriods).getLcValue();
    }

    private BigDecimal getAccuEstimatedGrossProfitLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("ESTIMATED_GROSS_PROFIT_LC", measureable, qtdPeriods).getLcValue();
    }

    BigDecimal zeroIfNull(BigDecimal decimal) {
        if (decimal == null) {
            return BigDecimal.ZERO;
        }

        return decimal;
    }

    Date sqlDateConverter(LocalDate date) {
        if (date == null) {
            return null;
        }
        return java.sql.Date.valueOf(date);
    }
}
