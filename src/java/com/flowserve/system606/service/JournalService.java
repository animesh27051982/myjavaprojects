package com.flowserve.system606.service;

import com.flowserve.system606.model.Account;
import com.flowserve.system606.model.AccountMapping;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.ContractJournal;
import com.flowserve.system606.model.CurrencyMetric;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.JournalEntryHeader;
import com.flowserve.system606.model.JournalEntryLine;
import com.flowserve.system606.model.JournalEntryType;
import com.flowserve.system606.model.JournalStatus;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.PerformanceObligationGroup;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.ReportingUnitJournal;
import com.flowserve.system606.model.RevenueMethod;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Stateless
public class JournalService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    @Inject
    private AdminService adminService;
    @Inject
    private MetricService metricService;
    @Inject
    private CalculationService calculationService;
    @Inject
    private FinancialPeriodService financialService;

    public ReportingUnitJournal generateJournal(ReportingUnit ru, FinancialPeriod period) throws Exception {
        Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "checkpoint 1");
        ReportingUnitJournal reportingUnitJournal = new ReportingUnitJournal(ru, period);
        reportingUnitJournal.setJournalStatus(JournalStatus.NEW);

        Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "generateJournal iterating through Contracts..");
        for (Contract contract : ru.getContracts()) {
            ContractJournal contractJournal = new ContractJournal(reportingUnitJournal, period, contract);
            contractJournal.setJournalStatus(JournalStatus.NEW);

            //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "checkpoint 3");
            for (AccountMapping accountMappingContract : findAllAccountMappingsByOwnerEntityType(MetricType.OWNER_ENTITY_TYPE_CONTRACT)) {
                CurrencyMetric contractMetric = calculationService.getCurrencyMetric(accountMappingContract.getMetricType().getCode(), contract, period);
                JournalEntryHeader journalEntryHeader = new JournalEntryHeader(contractJournal, contractMetric.getMetricType(), accountMappingContract.getAccount(), contract.getLocalCurrency(), LocalDate.now(), JournalEntryType.NORMAL);
                generateJournalEntryLines(journalEntryHeader, contractMetric);
                contractJournal.addJournalEntryHeader(journalEntryHeader);
            }

            //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "checkpoint 4");
            List<PerformanceObligationGroup> pobGroupsByRevenueMethods = new ArrayList<PerformanceObligationGroup>();

            pobGroupsByRevenueMethods.add(new PerformanceObligationGroup("pocPobs", contract, RevenueMethod.PERC_OF_COMP, contract.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP)));
            pobGroupsByRevenueMethods.add(new PerformanceObligationGroup("pitPobs", contract, RevenueMethod.POINT_IN_TIME, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME)));
            pobGroupsByRevenueMethods.add(new PerformanceObligationGroup("rtiPobs", contract, RevenueMethod.RIGHT_TO_INVOICE, contract.getPobsByRevenueMethod(RevenueMethod.RIGHT_TO_INVOICE)));
            pobGroupsByRevenueMethods.add(new PerformanceObligationGroup("slPobs", contract, RevenueMethod.STRAIGHT_LINE, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE)));

            for (AccountMapping accountMappingPOB : findAllAccountMappingsByOwnerEntityType(MetricType.OWNER_ENTITY_TYPE_POB)) {
                //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "checkpoint 5");
                for (PerformanceObligationGroup pobGroup : pobGroupsByRevenueMethods) {
                    //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "checkpoint 6");
                    CurrencyMetric pobGroupMetric = calculationService.getCurrencyMetric(accountMappingPOB.getMetricType().getCode(), pobGroup, period);
                    JournalEntryHeader journalEntryHeader = new JournalEntryHeader(contractJournal, pobGroupMetric.getMetricType(), accountMappingPOB.getAccount(), contract.getLocalCurrency(), LocalDate.now(), JournalEntryType.NORMAL);
                    journalEntryHeader.setRevenueMethod(pobGroup.getRevenueMethod());
                    generateJournalEntryLines(journalEntryHeader, pobGroupMetric);
                    contractJournal.addJournalEntryHeader(journalEntryHeader);
                }
            }

            reportingUnitJournal.addContractJournal(contractJournal);
        }

        persist(reportingUnitJournal);

        return reportingUnitJournal;
    }

    public void persist(ReportingUnitJournal reportingUnitJournal) throws Exception {
        em.persist(reportingUnitJournal);
    }

    public List<AccountMapping> findAllAccountMappings() {
        Query query = em.createQuery("SELECT am FROM AccountMapping am");
        return (List<AccountMapping>) query.getResultList();
    }

    public List<AccountMapping> findAllAccountMappingsByOwnerEntityType(String ownerEntityType) {
        Query query = em.createQuery("SELECT am FROM AccountMapping am WHERE am.ownerEntityType = :OET");
        query.setParameter("OET", ownerEntityType);
        return (List<AccountMapping>) query.getResultList();
    }

    private void generateJournalEntryLines(JournalEntryHeader journalEntryHeader, CurrencyMetric metric) {
        Account account = journalEntryHeader.getAccount();
        if (account != null) {
            //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "Generating JE Line for Account: " + account.getName());
            JournalEntryLine line = new JournalEntryLine(journalEntryHeader, account, journalEntryHeader.getCurrency(), journalEntryHeader.getRevenueMethod());
            setJournalEntryLineAmounts(account, metric.getLcValue(), line);
            journalEntryHeader.addJournalEntryLine(line);

            Account offsetAccount = account.getOffsetAccount();
            if (offsetAccount != null) {
                //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "Generating JE Line for Offset Account: " + offsetAccount.getName());
                JournalEntryLine offsetLine = new JournalEntryLine(journalEntryHeader, offsetAccount, journalEntryHeader.getCurrency(), journalEntryHeader.getRevenueMethod());
                setJournalEntryLineAmounts(offsetAccount, metric.getLcValue(), line);
                journalEntryHeader.addJournalEntryLine(offsetLine);
            }
        }
    }

    private void setJournalEntryLineAmounts(Account account, BigDecimal amount, JournalEntryLine line) {
        if (account.isDebitAccount()) {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                line.setDebitAmount(BigDecimal.ZERO);
                line.setCreditAmount(BigDecimal.ZERO);
                return;
            }
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                line.setDebitAmount(amount);
                line.setCreditAmount(BigDecimal.ZERO);
                return;
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                line.setDebitAmount(BigDecimal.ZERO);
                line.setCreditAmount(amount.abs());
                return;
            }
        }
        if (account.isCreditAccount()) {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                line.setDebitAmount(BigDecimal.ZERO);
                line.setCreditAmount(BigDecimal.ZERO);
                return;
            }
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                line.setDebitAmount(BigDecimal.ZERO);
                line.setCreditAmount(amount);
                return;
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                line.setDebitAmount(amount.abs());
                line.setCreditAmount(BigDecimal.ZERO);
                return;
            }
        }
    }

    public void generateJournalEntryReport(InputStream inputStream, FileOutputStream outputStream, ReportingUnit reportingUnit, FinancialPeriod period) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet worksheet = workbook.getSheet("JournalEntryRU");

        Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "Generating JE worksheet.");
        worksheet = writeJournalEntryReport(worksheet, reportingUnit, period);
        Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "Finished generating JE worksheet.");

        ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
        ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));

        workbook.write(outputStream);
        inputStream.close();
        outputStream.close();
    }

    public XSSFSheet writeJournalEntryReport(XSSFSheet worksheet, ReportingUnit reportingUnit, FinancialPeriod period) throws Exception {
        XSSFRow row;
        Cell cell = null;
        XSSFRow ru_name = worksheet.getRow(2);
        cell = ru_name.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(reportingUnit.getName());
        row = worksheet.getRow(6);
        row.getCell(0).setCellValue(Date.valueOf(period.getEndDate()));

        ReportingUnitJournal reportingUnitJournal = generateJournal(reportingUnit, period);

        BigDecimal revenueToRecognizePeriod = getRevenueRecognizePeriodLC(reportingUnit, period);

        BigDecimal liquidatedDamageRecognizePeriodLC = getLiquidatedDamagesRecognizePeriodLC(reportingUnit, period);
        BigDecimal costGoodsSoldPeriodLC = getCostGoodsSoldPeriodLC(reportingUnit, period);
        BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(reportingUnit, period);
        BigDecimal revenueInExcess = getContractRevenueInExcess(reportingUnit, period);
        BigDecimal billingsInExcess = getContractBillingsInExcess(reportingUnit, period);
        BigDecimal bilingsPeriodLC = getContractBillingsPeriodLC(reportingUnit, period);
        BigDecimal commExp = getTPCIncured(reportingUnit, period);

        Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "revenueToRecognizePeriod: " + revenueToRecognizePeriod.toPlainString());

//        row = worksheet.getRow(10);
//        setCellValue(row, 2, revenueToRecognizePeriod);
//        setCellValue(row, 3, liquidatedDamageRecognizePeriodLC);
//        setCellValue(row, 4, costGoodsSoldPeriodLC);
//        setCellValue(row, 5, lossReservePeriodADJLC);
//        setCellValue(row, 6, commExp);
//        setCellValue(row, 7, BigDecimal.ZERO);//TODO FX_GAIN_LOSS
//        setCellValue(row, 8, bilingsPeriodLC);
//        setCellValue(row, 9, costGoodsSoldPeriodLC);
//        setCellValue(row, 10, lossReservePeriodADJLC);
//        setCellValue(row, 11, revenueInExcess);
//        setCellValue(row, 12, billingsInExcess);
//        setCellValue(row, 13, lossReservePeriodADJLC);
//        setCellValue(row, 14, commExp);
        //For Contract Billings row
        //row = worksheet.getRow(13);
        //setCellValue(row, 8, bilingsPeriodLC);
        // Split the RU into groups.  We need totals per type of POB, so create 3 groups.  The PerformanceObligationGroup is just a shell Measurable non-entity class used for grouping.
        PerformanceObligationGroup pocPobs = new PerformanceObligationGroup("pocPobs", reportingUnit, RevenueMethod.PERC_OF_COMP, reportingUnit.getPobsByRevenueMethod(RevenueMethod.PERC_OF_COMP));
        PerformanceObligationGroup pitPobs = new PerformanceObligationGroup("pitPobs", reportingUnit, RevenueMethod.POINT_IN_TIME, reportingUnit.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME));
        PerformanceObligationGroup rtiPobs = new PerformanceObligationGroup("rtiPobs", reportingUnit, RevenueMethod.RIGHT_TO_INVOICE, reportingUnit.getPobsByRevenueMethod(RevenueMethod.RIGHT_TO_INVOICE));
        PerformanceObligationGroup slPobs = new PerformanceObligationGroup("slPobs", reportingUnit, RevenueMethod.STRAIGHT_LINE, reportingUnit.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE));

        PerformanceObligationGroup slAndRTIPobs = new PerformanceObligationGroup("slAndRTIPobs", reportingUnit);
        slAndRTIPobs.addPerformanceObligations(rtiPobs.getPerformanceObligations());
        slAndRTIPobs.addPerformanceObligations(slPobs.getPerformanceObligations());

        printJournalEntryPobsGroups(10, worksheet, pocPobs, period);
        printJournalEntryPobsGroups(11, worksheet, pitPobs, period);
        printJournalEntryPobsGroups(12, worksheet, slAndRTIPobs, period);

        return worksheet;
    }

    public void printJournalEntryPobsGroups(int single, XSSFSheet worksheet, PerformanceObligationGroup pobGroup, FinancialPeriod period) throws Exception {

        XSSFRow row;
        BigDecimal revenueToRecognizePeriod = getRevenueRecognizePeriodLC(pobGroup, period);
        BigDecimal liquidatedDamageRecognizePeriodLC = getLiquidatedDamagesRecognizePeriodLC(pobGroup, period);
        BigDecimal costGoodsSoldPeriodLC = getCostGoodsSoldPeriodLC(pobGroup, period);
        BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(pobGroup, period);
        BigDecimal revenueInExcess = getContractRevenueInExcess(pobGroup, period);
        BigDecimal billingsInExcess = getContractBillingsInExcess(pobGroup, period);
        BigDecimal commExp = getTPCIncured(pobGroup, period);
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

    private BigDecimal getContractRevenueInExcess(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_REVENUE_IN_EXCESS_LC", measureable, period).getLcValue();
    }

    private BigDecimal getContractBillingsCTDLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_BILLINGS_CTD_CC", measureable, period).getLcValue();
    }

    private BigDecimal getContractBillingsPeriodLC(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_BILLINGS_PERIOD_CC", measureable, period).getLcValue();
    }

    private BigDecimal getContractBillingsInExcess(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("CONTRACT_BILLINGS_IN_EXCESS_LC", measureable, period).getLcValue();
    }

    private BigDecimal getTPCIncured(Measurable measureable, FinancialPeriod period) throws Exception {
        BigDecimal value = calculationService.getCurrencyMetric("THIRD_PARTY_COMMISSION_TO_RECOGNIZE_PERIOD_LC", measureable, period).getLcValue();
        return value != null ? value : BigDecimal.ZERO;
    }

}
