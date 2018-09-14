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
import com.flowserve.system606.model.ReportingUnit;
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
import java.util.logging.Level;
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
    public XSSFSheet writeContractSummaryReport(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = 0;
        XSSFRow rowContract = worksheet.getRow(1);
        cell = rowContract.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());
        XSSFRow rowRU = worksheet.getRow(2);
        cell = rowRU.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getReportingUnit().getName());
        // For now, get the period from the user's session.  Later we will be passing in the period from the reports generation page.
        FinancialPeriod period = webSession.getCurrentPeriod();
        row = worksheet.getRow(4);
        row.getCell(0).setCellValue(Date.valueOf(period.getEndDate()));

        // Get the contract level values for the contract total row on the "Contract Summary Totals" row.
        row = worksheet.getRow(12);
        setCellValue(row, 11, getLossReservePeriodADJLC(contract, period));

        row = worksheet.getRow(13);
        setCellValue(row, 6, getThirdPartyCommissionCTDLC(contract, period));
        setCellValue(row, 14, getThirdPartyCommRegLC(contract, period));

        row = worksheet.getRow(14);
        setCellValue(row, 15, getContractBillingsCTDLC(contract, period));

        row = worksheet.getRow(15);
        setCellValue(row, 1, getTransactionPrice(contract, period));
        setCellValue(row, 2, getLiquidatedDamages(contract, period));
        setCellValue(row, 3, getEAC(contract, period));
        setCellValue(row, 4, getEstimatedGrossProfit(contract, period));
        setPercentCellValue(row, 5, getEstimatedGrossMargin(contract, period));
        setCellValue(row, 6, getThirdPartyCommissionCTDLC(contract, period));

        setPercentCellValue(row, 7, getContractPercentComplete(contract, period));
        setCellValue(row, 8, getRevenueRecognizeCTD(contract, period));
        setCellValue(row, 9, getLiquidatedDamagesPeriod(contract, period));
        setCellValue(row, 10, getCostOfGoodsSold(contract, period));
        setCellValue(row, 11, getLossReserveCTD(contract, period));
        setCellValue(row, 12, getGrossProfitCTD(contract, period));
        setPercentCellValue(row, 13, getGrossMarginCTD(contract, period));
        setCellValue(row, 14, getThirdPartyCommRegLC(contract, period));
        setCellValue(row, 15, getContractBillingsCTDLC(contract, period));
        setCellValue(row, 16, getContractCostToCompleteLC(contract, period));
        setCellValue(row, 17, getContractAsset(contract, period));
        setCellValue(row, 18, getContractLiability(contract, period));

        // Split the contract into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, RevenueMethod.PERC_OF_COMP, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, RevenueMethod.POINT_IN_TIME, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, RevenueMethod.STRAIGHT_LINE, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        printContractSummaryPobsGroups(9, 18, worksheet, pocPobs, period);
        printContractSummaryPobsGroups(10, 21, worksheet, pitPobs, period);
        printContractSummaryPobsGroups(11, 24, worksheet, slPobs, period);

        int shiftInRow = printContractSummaryPobsDetailLines(18, 24, worksheet, pocPobs, period);
        shiftInRow = printContractSummaryPobsDetailLines(shiftInRow + 3, shiftInRow + 6, worksheet, pitPobs, period);
        shiftInRow = printContractSummaryPobsDetailLines(shiftInRow + 3, shiftInRow + 6, worksheet, slPobs, period);

        return worksheet;
    }

    public void printContractSummaryPobsGroups(int single, int total, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {

        XSSFRow row;
        BigDecimal transactionPrice = getTransactionPrice(pGroup, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(pGroup, period);
        BigDecimal EAC = getEAC(pGroup, period);
        BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(pGroup, period);
        BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(pGroup, period);

        //BigDecimal liquidatedDamagePeriod = getLiquidatedDamagesPeriod(pGroup, period);
        BigDecimal localCostCTDLC = getCostOfGoodsSold(pGroup, period);
        BigDecimal percentComplete = getPercentComplete(pGroup, period);
        BigDecimal revenueCTD = getRevenueRecognizeCTD(pGroup, period);
        BigDecimal lossReserveCTD = getLossReserveCTD(pGroup, period);
        BigDecimal grossProfitCTD = getGrossProfitCTD(pGroup, period);
        BigDecimal grossMarginCTD = getGrossMarginCTD(pGroup, period);
        BigDecimal costToComplete = getContractCostToCompleteLC(pGroup, period);
        BigDecimal billingsInExcess = getContractLiability(pGroup, period);
        BigDecimal revenueInExcess = getContractAsset(pGroup, period);
        BigDecimal billToDate = new BigDecimal(BigInteger.ZERO);

        // Percentage of completion Pobs.  Set total row for POC POBs
        row = worksheet.getRow(single);
        setCellValue(row, 1, transactionPrice);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setPercentCellValue(row, 5, estimatedGrossMargin);

        setCellValue(row, 7, percentComplete);
        setCellValue(row, 8, revenueCTD);
        setCellValue(row, 9, liquidatedDamage);
        setCellValue(row, 10, localCostCTDLC);
        setCellValue(row, 12, grossProfitCTD);
        setPercentCellValue(row, 13, grossMarginCTD);
        setCellValue(row, 15, billToDate);
        setCellValue(row, 16, costToComplete);
        setCellValue(row, 17, revenueInExcess);
        setCellValue(row, 18, billingsInExcess);

        // We have the same total 8 rows down
        row = worksheet.getRow(total);
        setCellValue(row, 1, transactionPrice);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setPercentCellValue(row, 5, estimatedGrossMargin);

        //setCellValue(row, 7, percentComplete);
        setCellValue(row, 8, revenueCTD);
        setCellValue(row, 9, liquidatedDamage);
        setCellValue(row, 10, localCostCTDLC);
        setCellValue(row, 12, grossProfitCTD);
        setPercentCellValue(row, 13, grossMarginCTD);
        setCellValue(row, 15, billToDate);
        setCellValue(row, 16, costToComplete);
        setCellValue(row, 17, revenueInExcess);
        setCellValue(row, 18, billingsInExcess);
    }

    public int printContractSummaryPobsDetailLines(int insertRow, int shiftRow, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {
        XSSFRow row;
        Cell cell = null;
        if (pGroup.getPerformanceObligations().size() > 0) {
            worksheet.shiftRows(insertRow, shiftRow, pGroup.getPerformanceObligations().size(), true, false);

            for (PerformanceObligation pob : pGroup.getPerformanceObligations()) {
                row = worksheet.createRow(insertRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getName());
                setCellValue(row, 1, getTransactionPrice(pob, period));
                setCellValue(row, 2, getLiquidatedDamages(pob, period));
                setCellValue(row, 3, getEAC(pob, period));
                setCellValue(row, 4, getEstimatedGrossProfit(pob, period));
                setPercentCellValue(row, 5, getEstimatedGrossMargin(pob, period));

                setPercentCellValue(row, 7, getPercentComplete(pob, period));
                setCellValue(row, 8, getRevenueRecognizeCTD(pob, period));
                setCellValue(row, 9, getLiquidatedDamagesPeriod(pob, period));
                setCellValue(row, 10, getCostOfGoodsSold(pob, period));
                setCellValue(row, 12, getEstimatedGrossProfit(pob, period));
                setPercentCellValue(row, 13, getEstimatedGrossMargin(pob, period));

                insertRow++;
            }
        }
        return insertRow;
    }

//    public void generateReportByFinancialPeriod(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
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
    public XSSFSheet writeContractSummaryReportByFinancialPeriod(XSSFSheet worksheet, Contract contract) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        XSSFRow contract_name = worksheet.getRow(1);
        cell = contract_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());
        XSSFRow ru_name = worksheet.getRow(2);
        cell = ru_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getReportingUnit().getName());

        FinancialPeriod period = webSession.getCurrentPeriod();
        row = worksheet.getRow(4);
        row.getCell(0).setCellValue(Date.valueOf(period.getEndDate()));

        BigDecimal revenueToRecognizePeriod = getRevenueRecognizePeriodLC(contract, period);
        BigDecimal liquidatedDamagePeriod = getLiquidatedDamagesPeriod(contract, period);
        BigDecimal costGoodsSoldPeriodLC = getCostGoodsSoldPeriodLC(contract, period);
        BigDecimal EstimatedGrossProfitLC = getEstimatedGrossProfit(contract, period);
        BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(contract, period);

        BigDecimal tcpIncured = getTPCIncured(contract, period);
        BigDecimal acceleratedTCP = getAcceleratedTPC(contract, period);

        row = worksheet.getRow(12);
        setCellValue(row, 4, lossReservePeriodADJLC);

        row = worksheet.getRow(13);
        setCellValue(row, 6, tcpIncured);
        setCellValue(row, 7, acceleratedTCP);

        row = worksheet.getRow(15);
        setCellValue(row, 1, revenueToRecognizePeriod);
        setCellValue(row, 2, liquidatedDamagePeriod);
        setCellValue(row, 3, costGoodsSoldPeriodLC);
        setCellValue(row, 5, EstimatedGrossProfitLC);

        List<FinancialPeriod> qtdPeriods = financialPeriodService.getQTDFinancialPeriods(viewSupport.getCurrentPeriod());
        revenueToRecognizePeriod = getAccuRevenueToRecognizeLC(contract, qtdPeriods);
        liquidatedDamagePeriod = getAccuLiquidatedDamageCC(contract, qtdPeriods);
        costGoodsSoldPeriodLC = getAccuCumulativeCostGoodsSoldLC(contract, qtdPeriods);
        EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(contract, qtdPeriods);
        lossReservePeriodADJLC = getAccuLossReservePeriodADJLC(contract, qtdPeriods);
        tcpIncured = getAccuTPCIncured(contract, qtdPeriods);
        acceleratedTCP = getAccuAcceleratedTPC(contract, qtdPeriods);

        row = worksheet.getRow(12);
        setCellValue(row, 12, lossReservePeriodADJLC);

        row = worksheet.getRow(13);
        setCellValue(row, 14, tcpIncured);
        setCellValue(row, 15, acceleratedTCP);

        row = worksheet.getRow(15);
        setCellValue(row, 9, revenueToRecognizePeriod);
        setCellValue(row, 10, liquidatedDamagePeriod);
        setCellValue(row, 11, costGoodsSoldPeriodLC);
        setCellValue(row, 13, EstimatedGrossProfitLC);

        List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());
        revenueToRecognizePeriod = getAccuRevenueToRecognizeLC(contract, ytdPeriods);
        liquidatedDamagePeriod = getAccuLiquidatedDamageCC(contract, ytdPeriods);
        costGoodsSoldPeriodLC = getAccuCumulativeCostGoodsSoldLC(contract, ytdPeriods);
        EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(contract, ytdPeriods);
        lossReservePeriodADJLC = getAccuLossReservePeriodADJLC(contract, ytdPeriods);
        tcpIncured = getAccuTPCIncured(contract, ytdPeriods);
        acceleratedTCP = getAccuAcceleratedTPC(contract, ytdPeriods);

        row = worksheet.getRow(12);
        setCellValue(row, 20, lossReservePeriodADJLC);

        row = worksheet.getRow(13);
        setCellValue(row, 22, tcpIncured);
        setCellValue(row, 23, acceleratedTCP);

        row = worksheet.getRow(15);
        setCellValue(row, 17, revenueToRecognizePeriod);
        setCellValue(row, 18, liquidatedDamagePeriod);
        setCellValue(row, 19, costGoodsSoldPeriodLC);
        setCellValue(row, 21, EstimatedGrossProfitLC);

        // Split the contract into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, RevenueMethod.PERC_OF_COMP, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, RevenueMethod.POINT_IN_TIME, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, RevenueMethod.STRAIGHT_LINE, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        printFinancialPobsGroups(9, 18, worksheet, pocPobs, period, qtdPeriods, ytdPeriods);
        printFinancialPobsGroups(10, 21, worksheet, pitPobs, period, qtdPeriods, ytdPeriods);
        printFinancialPobsGroups(11, 24, worksheet, slPobs, period, qtdPeriods, ytdPeriods);

        int shiftInRow = printFinancialPobsDetailLines(18, 24, worksheet, pocPobs, period, qtdPeriods, ytdPeriods);
        shiftInRow = printFinancialPobsDetailLines(shiftInRow + 3, shiftInRow + 6, worksheet, pitPobs, period, qtdPeriods, ytdPeriods);
        shiftInRow = printFinancialPobsDetailLines(shiftInRow + 3, shiftInRow + 6, worksheet, slPobs, period, qtdPeriods, ytdPeriods);
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

    public int printFinancialPobsDetailLines(int insertRow, int shiftRow, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period, List<FinancialPeriod> qtdPeriods, List<FinancialPeriod> ytdPeriods) throws Exception {
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
        return insertRow;
    }

    public void generateContractSummaryReport(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Reporting Unit Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Reporting Unit Summary-2"));
            XSSFSheet worksheet = workbook.getSheet("Contract Summary-1");
            worksheet = writeContractSummaryReport(worksheet, contract);
            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));

            worksheet = workbook.getSheet("Contract Summary-2");
            worksheet = writeContractSummaryReportByFinancialPeriod(worksheet, contract);
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

            worksheet = writeFinancialSummary(worksheet, contract);
            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writeFinancialSummary(XSSFSheet worksheet, Contract contract) throws Exception {
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
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", contract, RevenueMethod.PERC_OF_COMP, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", contract, RevenueMethod.POINT_IN_TIME, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", contract, RevenueMethod.STRAIGHT_LINE, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        BigDecimal revenueToRecognize = getRevenueRecognizePeriodLC(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);
        revenueToRecognize = getRevenueRecognizePeriodLC(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);
        revenueToRecognize = getRevenueRecognizePeriodLC(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);

        startRow = startRow + 2;
        BigDecimal liquidatedDamagePeriod = getLiquidatedDamagesPeriod(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, liquidatedDamagePeriod);
        liquidatedDamagePeriod = getLiquidatedDamagesPeriod(pitPobs, period);
        row = worksheet.getRow(startRow++);
        //getting NPE
        setCellValue(row, colNum, liquidatedDamagePeriod);
        liquidatedDamagePeriod = getLiquidatedDamagesPeriod(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, liquidatedDamagePeriod);

        startRow = startRow + 2;
        BigDecimal netRevenue = getRevenueRecognizeCTD(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, netRevenue);
        netRevenue = getRevenueRecognizeCTD(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, netRevenue);
        netRevenue = getRevenueRecognizeCTD(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, netRevenue);

        startRow = startRow + 2;
        BigDecimal costsIncurredCOGS = getCostGoodsSoldPeriodLC(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, costsIncurredCOGS);
        costsIncurredCOGS = getCostGoodsSoldPeriodLC(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, costsIncurredCOGS);
        costsIncurredCOGS = getCostGoodsSoldPeriodLC(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, costsIncurredCOGS);

        startRow = startRow + 2;
        BigDecimal reserveLCM_COGS = getLossReservePeriodADJLC(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, reserveLCM_COGS);
        reserveLCM_COGS = getLossReservePeriodADJLC(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, reserveLCM_COGS);
        reserveLCM_COGS = getLossReservePeriodADJLC(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, reserveLCM_COGS);

        startRow = startRow + 2;
        BigDecimal totalCOGS = getCumulativeCostGoodsSoldLC(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, totalCOGS);
        totalCOGS = getCumulativeCostGoodsSoldLC(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, totalCOGS);
        totalCOGS = getCumulativeCostGoodsSoldLC(slPobs, period);
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

        startRow = startRow + 2;
        BigDecimal tcp = getTPCIncured(contract, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, tcp);
        BigDecimal acceleratedTPC = getAcceleratedTPC(contract, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, acceleratedTPC);
        BigDecimal thirdPartyCommRegLC = getThirdPartyCommRegLC(contract, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, thirdPartyCommRegLC);
    }

    public void generateRUReportFinancialSummary(InputStream inputStream, FileOutputStream outputStream, ReportingUnit ru) throws Exception {
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

            worksheet = writeRUFinancialSummary(worksheet, ru);
            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));

            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writeRUFinancialSummary(XSSFSheet worksheet, ReportingUnit ru) throws Exception {
        Cell cell = null;
        XSSFRow rowTitle = worksheet.getRow(1);
        cell = rowTitle.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(ru.getName());

        List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());
        for (int i = 0; i < ytdPeriods.size(); i++) {
            int colNum = i + 1;
            printRUSummaryByPobs(colNum, worksheet, ru, ytdPeriods.get(i));
        }
        return worksheet;
    }

    public void printRUSummaryByPobs(int colNum, XSSFSheet worksheet, ReportingUnit ru, FinancialPeriod period) throws Exception {

        XSSFRow row;
        int startRow = 7;
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", ru, RevenueMethod.PERC_OF_COMP, ru.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", ru, RevenueMethod.POINT_IN_TIME, ru.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", ru, RevenueMethod.STRAIGHT_LINE, ru.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        calculationService.executeBusinessRules(slPobs, period);

        BigDecimal revenueToRecognize = getRevenueRecognizePeriodLC(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);
        revenueToRecognize = getRevenueRecognizePeriodLC(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);
        revenueToRecognize = getRevenueRecognizePeriodLC(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, revenueToRecognize);

        startRow = startRow + 2;
        BigDecimal liquidatedDamagePeriod = getLiquidatedDamagesPeriod(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, liquidatedDamagePeriod);
        liquidatedDamagePeriod = getLiquidatedDamagesPeriod(pitPobs, period);
        row = worksheet.getRow(startRow++);
        //getting NPE
        setCellValue(row, colNum, liquidatedDamagePeriod);
        liquidatedDamagePeriod = getLiquidatedDamagesPeriod(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, liquidatedDamagePeriod);

        startRow = startRow + 2;
        BigDecimal netRevenue = getRevenueRecognizeCTD(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, netRevenue);
        netRevenue = getRevenueRecognizeCTD(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, netRevenue);
        netRevenue = getRevenueRecognizeCTD(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, netRevenue);

        startRow = startRow + 2;
        BigDecimal costsIncurredCOGS = getCostGoodsSoldPeriodLC(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, costsIncurredCOGS);
        costsIncurredCOGS = getCostGoodsSoldPeriodLC(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, costsIncurredCOGS);
        costsIncurredCOGS = getCostGoodsSoldPeriodLC(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, costsIncurredCOGS);

        startRow = startRow + 2;
        BigDecimal reserveLCM_COGS = getLossReservePeriodADJLC(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, reserveLCM_COGS);
        reserveLCM_COGS = getLossReservePeriodADJLC(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, reserveLCM_COGS);
        reserveLCM_COGS = getLossReservePeriodADJLC(slPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, reserveLCM_COGS);

        startRow = startRow + 2;
        BigDecimal totalCOGS = getCumulativeCostGoodsSoldLC(pocPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, totalCOGS);
        totalCOGS = getCumulativeCostGoodsSoldLC(pitPobs, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, totalCOGS);
        totalCOGS = getCumulativeCostGoodsSoldLC(slPobs, period);
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

        startRow = startRow + 2;
        BigDecimal tcp = getTPCIncured(ru, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, tcp);
        BigDecimal acceleratedTPC = getAcceleratedTPC(ru, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, acceleratedTPC);
        BigDecimal thirdPartyCommRegLC = getThirdPartyCommRegLC(ru, period);
        row = worksheet.getRow(startRow++);
        setCellValue(row, colNum, thirdPartyCommRegLC);
    }

    public void generateContractSummaryReportingUnitLevelReport(InputStream inputStream, FileOutputStream outputStream, ReportingUnit reportingUnit) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-1"));
            workbook.removeSheetAt(workbook.getSheetIndex("Contract Summary-2"));
            XSSFSheet worksheet = workbook.getSheet("Reporting Unit Summary-1");

            worksheet = writeContractSummaryReportingUnitLevelReport(worksheet, reportingUnit);
            ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
            ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));

            worksheet = workbook.getSheet("Reporting Unit Summary-2");
            worksheet = writeRUReportByFinancialPeriod(worksheet, reportingUnit);
            workbook.write(outputStream);
        }
        inputStream.close();
        outputStream.close();

    }

    public XSSFSheet writeContractSummaryReportingUnitLevelReport(XSSFSheet worksheet, ReportingUnit ru) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = 0;
        //ReportingUnit ru = contract.getReportingUnit();
        XSSFRow rowContract = worksheet.getRow(1);
        cell = rowContract.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(ru.getName());
        // For now, get the period from the user's session.  Later we will be passing in the period from the reports generation page.
        FinancialPeriod period = webSession.getCurrentPeriod();
        row = worksheet.getRow(3);
        row.getCell(0).setCellValue(Date.valueOf(period.getEndDate()));

        // Get the contract level values for the contract total row on the "Contract Summary Totals" row.
        BigDecimal transactionPrice = getTransactionPrice(ru, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(ru, period);
        BigDecimal EAC = getEAC(ru, period);
        BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(ru, period);
        BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(ru, period);

        BigDecimal liquidatedDamagePeriod = getLiquidatedDamagesPeriod(ru, period);
        BigDecimal localCostCTDLC = getCostOfGoodsSold(ru, period);
        BigDecimal percentComplete = getContractPercentComplete(ru, period);
        BigDecimal revenueCTD = getRevenueRecognizeCTD(ru, period);
        BigDecimal lossReserveCTD = getLossReserveCTD(ru, period);
        BigDecimal grossProfitCTD = getEstimatedGrossProfit(ru, period);
        BigDecimal grossMarginCTD = getEstimatedGrossMargin(ru, period);
        BigDecimal costToComplete = getContractCostToCompleteLC(ru, period);
        BigDecimal contractLiability = getContractLiability(ru, period);
        BigDecimal contractAsset = getContractAsset(ru, period);
        BigDecimal billToDate = getContractBillingsCTDLC(ru, period);
        BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(ru, period);
        BigDecimal thirdPartyCommCTD = getThirdPartyCommissionCTDLC(ru, period);
        BigDecimal thirdPartyRecogLC = getThirdPartyCommRegLC(ru, period);

        row = worksheet.getRow(11);
        setCellValue(row, 11, lossReservePeriodADJLC);

        row = worksheet.getRow(12);
        Logger.getLogger(ReportsService.class.getName()).log(Level.INFO, "thirdPartyCommCTD: " + thirdPartyCommCTD.toPlainString());
        setCellValue(row, 6, thirdPartyCommCTD);
        setCellValue(row, 14, thirdPartyRecogLC);

        row = worksheet.getRow(14);

        // TODO - Please change all setCell lines to the same approach below.
        setCellValue(row, 1, transactionPrice);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, EAC);
        setCellValue(row, 4, estimatedGrossProfit);
        setPercentCellValue(row, 5, estimatedGrossMargin);
        Logger.getLogger(ReportsService.class.getName()).log(Level.INFO, "thirdPartyCommCTD: " + thirdPartyCommCTD.toPlainString());
        setCellValue(row, 6, thirdPartyCommCTD);
        setPercentCellValue(row, 7, percentComplete);
        setCellValue(row, 8, revenueCTD);
        setCellValue(row, 9, liquidatedDamagePeriod);
        setCellValue(row, 10, localCostCTDLC);
        setCellValue(row, 11, lossReserveCTD);
        setCellValue(row, 12, grossProfitCTD);
        setPercentCellValue(row, 13, grossMarginCTD);
        setCellValue(row, 14, thirdPartyRecogLC);
        setCellValue(row, 15, billToDate);
        setCellValue(row, 16, costToComplete);
        setCellValue(row, 17, contractAsset);
        setCellValue(row, 18, contractLiability);

        // Split the contract into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", ru, RevenueMethod.PERC_OF_COMP, ru.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        /**
         * For an RU level report, we don't have a contract currency, so pass the local currency in its place. No rules fire for POBGroup that use the contract
         * currency, so this is safe. Contract currency does have to be populated however for calc execution, but we need to revisit this. Country level?
         * company level, etc?
         */
        pocPobs.setContractCurrency(ru.getLocalCurrency());
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", ru, RevenueMethod.POINT_IN_TIME, ru.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        pitPobs.setContractCurrency(ru.getLocalCurrency());
        calculationService.executeBusinessRules(pitPobs, period);
        //PerformanceObligationGroup rtiPobs = new PerformanceObligationGroup("rtiPobs", ru, RevenueMethod.RIGHT_TO_INVOICE, ru.getPobsByRevenueMethod(RevenueMethod.RIGHT_TO_INVOICE));
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", ru, RevenueMethod.STRAIGHT_LINE, ru.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        slPobs.setContractCurrency(ru.getLocalCurrency());
        calculationService.executeBusinessRules(slPobs, period);

        printRUEsimatesPobsGroups(8, worksheet, pocPobs, period);
        printRUEsimatesPobsGroups(9, worksheet, pitPobs, period);
        printRUEsimatesPobsGroups(10, worksheet, slPobs, period);
        printRUTotalContractLevel(18, worksheet, ru, period);

        return worksheet;
    }

    public void printRUEsimatesPobsGroups(int single, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period) throws Exception {

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
        BigDecimal billingsInExcess = getContractLiability(pGroup, period);
        BigDecimal revenueInExcess = getContractAsset(pGroup, period);
        BigDecimal billToDate = new BigDecimal(BigInteger.ZERO);
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
        setCellValue(row, 17, revenueInExcess);
        setCellValue(row, 18, billingsInExcess);
    }

    public void printRUTotalContractLevel(int insertRow, XSSFSheet worksheet, ReportingUnit ru, FinancialPeriod period) throws Exception {
        XSSFRow row;
        Cell cell = null;
        if (ru.getContracts().size() > 0) {

            for (Contract contract : ru.getContracts()) {
                BigDecimal transactionPrice = getTransactionPrice(contract, period);
                BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
                BigDecimal EAC = getEAC(contract, period);
                BigDecimal estimatedGrossProfit = getEstimatedGrossProfit(contract, period);
                BigDecimal estimatedGrossMargin = getEstimatedGrossMargin(contract, period);

                BigDecimal liquidatedDamagePeriod = getLiquidatedDamagesPeriod(contract, period);
                BigDecimal localCostCTDLC = getCostOfGoodsSold(contract, period);
                BigDecimal percentComplete = getContractPercentComplete(contract, period);
                BigDecimal revenueCTD = getRevenueRecognizeCTD(contract, period);
                BigDecimal lossReserveCTD = getLossReserveCTD(contract, period);
                BigDecimal grossProfitCTD = getEstimatedGrossProfit(contract, period);
                BigDecimal grossMarginCTD = getEstimatedGrossMargin(contract, period);
                BigDecimal costToComplete = getContractCostToCompleteLC(contract, period);
                BigDecimal contractLiability = getContractLiability(contract, period);
                BigDecimal contractAsset = getContractAsset(contract, period);
                BigDecimal billToDate = getContractBillingsCTDLC(contract, period);
                BigDecimal thirdPartyCommCTD = getThirdPartyCommissionCTDLC(contract, period);
                BigDecimal thirdPartyRecogLC = getThirdPartyCommRegLC(contract, period);

                row = worksheet.createRow(insertRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(contract.getName());
                setCellValue(row, 1, transactionPrice);
                setCellValue(row, 2, liquidatedDamage);
                setCellValue(row, 3, EAC);
                setCellValue(row, 4, estimatedGrossProfit);
                setPercentCellValue(row, 5, estimatedGrossMargin);
                setCellValue(row, 6, thirdPartyCommCTD);
                setPercentCellValue(row, 7, percentComplete);
                setCellValue(row, 8, revenueCTD);
                setCellValue(row, 9, liquidatedDamagePeriod);
                setCellValue(row, 10, localCostCTDLC);
                setCellValue(row, 11, lossReserveCTD);
                setCellValue(row, 12, grossProfitCTD);
                setPercentCellValue(row, 13, grossMarginCTD);
                setCellValue(row, 14, thirdPartyRecogLC);
                setCellValue(row, 15, billToDate);
                setCellValue(row, 16, costToComplete);
                setCellValue(row, 17, contractAsset);
                setCellValue(row, 18, contractLiability);

                insertRow++;
            }
        }
    }

    public XSSFSheet writeRUReportByFinancialPeriod(XSSFSheet worksheet, ReportingUnit ru) throws Exception {
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        //ReportingUnit ru = contract.getReportingUnit();
        XSSFRow ru_name = worksheet.getRow(1);
        cell = ru_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(ru.getName());

        FinancialPeriod period = webSession.getCurrentPeriod();
        row = worksheet.getRow(3);
        row.getCell(0).setCellValue(Date.valueOf(period.getEndDate()));

        BigDecimal revenueToRecognize = getRevenueRecognizeCTD(ru, period);
        BigDecimal liquidatedDamage = getLiquidatedDamages(ru, period);
        BigDecimal cumulativeCostGoodsSoldLC = getCumulativeCostGoodsSoldLC(ru, period);
        BigDecimal EstimatedGrossProfitLC = getEstimatedGrossProfit(ru, period);
        BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(ru, period);

        BigDecimal tcpIncured = getTPCIncured(ru, period);
        BigDecimal acceleratedTCP = getAcceleratedTPC(ru, period);

        row = worksheet.getRow(11);
        setCellValue(row, 4, lossReservePeriodADJLC);

        row = worksheet.getRow(12);
        setCellValue(row, 6, tcpIncured);
        setCellValue(row, 7, acceleratedTCP);

        row = worksheet.getRow(14);
        setCellValue(row, 1, revenueToRecognize);
        setCellValue(row, 2, liquidatedDamage);
        setCellValue(row, 3, cumulativeCostGoodsSoldLC);
        setCellValue(row, 5, EstimatedGrossProfitLC);

        List<FinancialPeriod> qtdPeriods = financialPeriodService.getQTDFinancialPeriods(viewSupport.getCurrentPeriod());
        revenueToRecognize = getAccuRevenueToRecognizeLC(ru, qtdPeriods);
        liquidatedDamage = getAccuLiquidatedDamageCC(ru, qtdPeriods);
        cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(ru, qtdPeriods);
        EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(ru, qtdPeriods);
        lossReservePeriodADJLC = getAccuLossReservePeriodADJLC(ru, qtdPeriods);
        tcpIncured = getAccuTPCIncured(ru, qtdPeriods);
        acceleratedTCP = getAccuAcceleratedTPC(ru, qtdPeriods);

        row = worksheet.getRow(11);
        setCellValue(row, 12, lossReservePeriodADJLC);

        row = worksheet.getRow(12);
        setCellValue(row, 14, tcpIncured);
        setCellValue(row, 15, acceleratedTCP);

        row = worksheet.getRow(14);
        setCellValue(row, 9, revenueToRecognize);
        setCellValue(row, 10, liquidatedDamage);
        setCellValue(row, 11, cumulativeCostGoodsSoldLC);
        setCellValue(row, 13, EstimatedGrossProfitLC);

        List<FinancialPeriod> ytdPeriods = financialPeriodService.getYTDFinancialPeriods(viewSupport.getCurrentPeriod());
        revenueToRecognize = getAccuRevenueToRecognizeLC(ru, ytdPeriods);
        liquidatedDamage = getAccuLiquidatedDamageCC(ru, ytdPeriods);
        cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(ru, ytdPeriods);
        EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(ru, ytdPeriods);
        lossReservePeriodADJLC = getAccuLossReservePeriodADJLC(ru, ytdPeriods);
        tcpIncured = getAccuTPCIncured(ru, ytdPeriods);
        acceleratedTCP = getAccuAcceleratedTPC(ru, ytdPeriods);

        row = worksheet.getRow(11);
        setCellValue(row, 20, lossReservePeriodADJLC);

        row = worksheet.getRow(12);
        setCellValue(row, 22, tcpIncured);
        setCellValue(row, 23, acceleratedTCP);

        row = worksheet.getRow(14);
        setCellValue(row, 17, revenueToRecognize);
        setCellValue(row, 18, liquidatedDamage);
        setCellValue(row, 19, cumulativeCostGoodsSoldLC);
        setCellValue(row, 21, EstimatedGrossProfitLC);

        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", ru, RevenueMethod.PERC_OF_COMP, ru.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        pocPobs.setContractCurrency(ru.getLocalCurrency());
        calculationService.executeBusinessRules(pocPobs, period);
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", ru, RevenueMethod.POINT_IN_TIME, ru.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        pitPobs.setContractCurrency(ru.getLocalCurrency());
        calculationService.executeBusinessRules(pitPobs, period);
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", ru, RevenueMethod.STRAIGHT_LINE, ru.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));
        slPobs.setContractCurrency(ru.getLocalCurrency());
        calculationService.executeBusinessRules(slPobs, period);

        printRUFinancialPobsGroups(8, worksheet, pocPobs, period, qtdPeriods, ytdPeriods);
        printRUFinancialPobsGroups(9, worksheet, pitPobs, period, qtdPeriods, ytdPeriods);
        printRUFinancialPobsGroups(10, worksheet, slPobs, period, qtdPeriods, ytdPeriods);
        printRUFinancialContract(18, worksheet, ru, period, qtdPeriods, ytdPeriods);
        return worksheet;
    }

    public void printRUFinancialPobsGroups(int single, XSSFSheet worksheet, PerformanceObligationGroup pGroup, FinancialPeriod period, List<FinancialPeriod> qtdPeriods, List<FinancialPeriod> ytdPeriods) throws Exception {

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

    }

    public void printRUFinancialContract(int insertRow, XSSFSheet worksheet, ReportingUnit ru, FinancialPeriod period, List<FinancialPeriod> qtdPeriods, List<FinancialPeriod> ytdPeriods) throws Exception {
        XSSFRow row;
        Cell cell = null;
        if (ru.getContracts().size() > 0) {

            for (Contract contract : ru.getContracts()) {
                //for monthly report
                BigDecimal revenueToRecognize = getRevenueRecognizeCTD(contract, period);
                BigDecimal liquidatedDamage = getLiquidatedDamages(contract, period);
                BigDecimal cumulativeCostGoodsSoldLC = getCumulativeCostGoodsSoldLC(contract, period);
                BigDecimal EstimatedGrossProfitLC = getEstimatedGrossProfit(contract, period);
                BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(contract, period);
                BigDecimal tcpIncured = getTPCIncured(contract, period);
                BigDecimal acceleratedTCP = getAcceleratedTPC(contract, period);

                row = worksheet.createRow(insertRow);
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(contract.getName());
                setCellValue(row, 1, revenueToRecognize);
                setCellValue(row, 2, liquidatedDamage);
                //setCellValue(row, 3, cumulativeCostGoodsSoldLC);
                setCellValue(row, 4, lossReservePeriodADJLC);
                setCellValue(row, 5, EstimatedGrossProfitLC);
                setCellValue(row, 6, tcpIncured);
                setCellValue(row, 7, acceleratedTCP);

                //for quaterly report
                revenueToRecognize = getAccuRevenueToRecognizeLC(contract, qtdPeriods);
                liquidatedDamage = getAccuLiquidatedDamageCC(contract, qtdPeriods);
                cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(contract, qtdPeriods);
                EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(contract, qtdPeriods);
                lossReservePeriodADJLC = getAccuLossReservePeriodADJLC(contract, qtdPeriods);
                tcpIncured = getAccuTPCIncured(contract, qtdPeriods);
                acceleratedTCP = getAccuAcceleratedTPC(contract, qtdPeriods);

                setCellValue(row, 9, revenueToRecognize);
                setCellValue(row, 10, liquidatedDamage);
                setCellValue(row, 11, cumulativeCostGoodsSoldLC);
                setCellValue(row, 12, lossReservePeriodADJLC);
                setCellValue(row, 13, EstimatedGrossProfitLC);
                setCellValue(row, 14, tcpIncured);
                setCellValue(row, 15, acceleratedTCP);

                //for annually report
                revenueToRecognize = getAccuRevenueToRecognizeLC(contract, ytdPeriods);
                liquidatedDamage = getAccuLiquidatedDamageCC(contract, ytdPeriods);
                cumulativeCostGoodsSoldLC = getAccuCumulativeCostGoodsSoldLC(contract, ytdPeriods);
                EstimatedGrossProfitLC = getAccuEstimatedGrossProfitLC(contract, ytdPeriods);
                lossReservePeriodADJLC = getAccuLossReservePeriodADJLC(contract, ytdPeriods);
                tcpIncured = getAccuTPCIncured(contract, ytdPeriods);
                acceleratedTCP = getAccuAcceleratedTPC(contract, ytdPeriods);

                setCellValue(row, 17, revenueToRecognize);
                setCellValue(row, 18, liquidatedDamage);
                setCellValue(row, 19, cumulativeCostGoodsSoldLC);
                setCellValue(row, 20, lossReservePeriodADJLC);
                setCellValue(row, 21, EstimatedGrossProfitLC);
                setCellValue(row, 22, tcpIncured);
                setCellValue(row, 23, acceleratedTCP);
                insertRow++;
            }
        }
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
        BigDecimal value = calculationService.getCurrencyMetric("TPC_TO_ACCEL_PERIOD_LC", measureable, period).getLcValue();
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

    private BigDecimal getGrossProfitCTD(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("GROSS_PROFIT_CTD_LC", measureable, period).getLcValue();
    }

    private BigDecimal getGrossMarginCTD(Measurable measureable, FinancialPeriod period) {
        return calculationService.getDecimalMetric("GROSS_MARGIN_CTD", measureable, period).getValue();
    }

    private BigDecimal getCostOfGoodsSold(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("COST_OF_GOODS_SOLD_CTD_LC", measureable, period).getLcValue();
    }

    private BigDecimal getContractPercentComplete(Measurable measureable, FinancialPeriod period) {
        return calculationService.getDecimalMetric("CONTRACT_PERCENT_COMPLETE", measureable, period).getValue();
    }

    private BigDecimal getPercentComplete(Measurable measureable, FinancialPeriod period) {
        return calculationService.getDecimalMetric("PERCENT_COMPLETE", measureable, period).getValue();
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

    private BigDecimal getContractLiability(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_LIABILITY_LC", measureable, period).getLcValue();
    }

    private BigDecimal getContractAsset(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_ASSET_LC", measureable, period).getLcValue();
    }

    private BigDecimal getContractBillingsCTDLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_BILLINGS_CTD_CC", measureable, period).getLcValue();
    }

    private BigDecimal getContractBillingsPeriodLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_BILLINGS_PERIOD_CC", measureable, period).getLcValue();
    }

    private BigDecimal getThirdPartyCommissionCTDLC(Measurable measureable, FinancialPeriod period) throws Exception {
        BigDecimal value = calculationService.getCurrencyMetric("THIRD_PARTY_COMMISSION_CTD_LC", measureable, period).getLcValue();
        return value != null ? value : new BigDecimal(BigInteger.ZERO);
    }

    private BigDecimal getThirdPartyCommRegLC(Measurable measureable, FinancialPeriod period) throws Exception {
        BigDecimal value = calculationService.getCurrencyMetric("THIRD_PARTY_COMMISSION_TO_RECOGNIZE_CTD_LC", measureable, period).getLcValue();
        return value != null ? value : new BigDecimal(BigInteger.ZERO);
    }

    private BigDecimal getCumulativeCostGoodsSoldLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("COST_OF_GOODS_SOLD_CTD_LC", measureable, period).getLcValue();
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
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("TPC_TO_ACCEL_PERIOD_LC", measureable, qtdPeriods).getLcValue();
    }

    private BigDecimal getAccuRevenueToRecognizeLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("REVENUE_TO_RECOGNIZE_CTD_CC", measureable, qtdPeriods).getLcValue();
    }

    private BigDecimal getAccuLiquidatedDamageCC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("LIQUIDATED_DAMAGES_CTD_CC", measureable, qtdPeriods).getLcValue();
    }

    private BigDecimal getAccuCumulativeCostGoodsSoldLC(Measurable measureable, List<FinancialPeriod> qtdPeriods) throws Exception {
        return calculationService.getAccumulatedCurrencyMetricAcrossPeriods("COST_OF_GOODS_SOLD_CTD_LC", measureable, qtdPeriods).getLcValue();
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
