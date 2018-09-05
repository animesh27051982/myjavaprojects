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
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
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

    public void removeExistingJournals(ReportingUnit ru, FinancialPeriod period) throws Exception {
        for (ReportingUnitJournal ruj : findAllReportingUnitJournals(ru, period)) {
            remove(ruj);
        }
    }

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
            //pobGroupsByRevenueMethods.add(new PerformanceObligationGroup("pitPobs", contract, RevenueMethod.POINT_IN_TIME, contract.getPobsByRevenueMethod(RevenueMethod.POINT_IN_TIME)));
            //pobGroupsByRevenueMethods.add(new PerformanceObligationGroup("rtiPobs", contract, RevenueMethod.RIGHT_TO_INVOICE, contract.getPobsByRevenueMethod(RevenueMethod.RIGHT_TO_INVOICE)));
            //pobGroupsByRevenueMethods.add(new PerformanceObligationGroup("slPobs", contract, RevenueMethod.STRAIGHT_LINE, contract.getPobsByRevenueMethod(RevenueMethod.STRAIGHT_LINE)));

            for (AccountMapping accountMappingPOB : findAllAccountMappingsByOwnerEntityType(MetricType.OWNER_ENTITY_TYPE_POB)) {
                //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "checkpoint 5");
                for (PerformanceObligationGroup pobGroup : pobGroupsByRevenueMethods) {
                    if (pobGroup.getRevenueMethod().equals(accountMappingPOB.getRevenueMethod())) {
                        //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "checkpoint 6");
                        CurrencyMetric pobGroupMetric = calculationService.getCurrencyMetric(accountMappingPOB.getMetricType().getCode(), pobGroup, period);
                        JournalEntryHeader journalEntryHeader = new JournalEntryHeader(contractJournal, pobGroupMetric.getMetricType(), accountMappingPOB.getAccount(), contract.getLocalCurrency(), LocalDate.now(), JournalEntryType.NORMAL);
                        journalEntryHeader.setRevenueMethod(pobGroup.getRevenueMethod());
                        generateJournalEntryLines(journalEntryHeader, pobGroupMetric);
                        contractJournal.addJournalEntryHeader(journalEntryHeader);
                    }
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

    public void remove(ReportingUnitJournal reportingUnitJournal) throws Exception {
        em.remove(reportingUnitJournal);
    }

    public List<Account> findAllAccounts() {
        Query query = em.createQuery("SELECT a FROM Account a");
        return (List<Account>) query.getResultList();
    }

    public List<AccountMapping> findAllAccountMappings() {
        Query query = em.createQuery("SELECT am FROM AccountMapping am");
        return (List<AccountMapping>) query.getResultList();
    }

    public Account findAccountById(String id) {
        return em.find(Account.class, id);
    }

    public List<AccountMapping> findAllAccountMappingsByOwnerEntityType(String ownerEntityType) {
        Query query = em.createQuery("SELECT am FROM AccountMapping am WHERE am.ownerEntityType = :OET");
        query.setParameter("OET", ownerEntityType);
        return (List<AccountMapping>) query.getResultList();
    }

    public List<ReportingUnitJournal> findAllReportingUnitJournals(ReportingUnit ru, FinancialPeriod period) {
        Query query = em.createQuery("SELECT ruj FROM ReportingUnitJournal ruj WHERE ruj.reportingUnit.id = :RU_ID AND ruj.period = :PERIOD");
        query.setParameter("RU_ID", ru.getId());
        query.setParameter("PERIOD", period);
        return (List<ReportingUnitJournal>) query.getResultList();
    }

    private void generateJournalEntryLines(JournalEntryHeader journalEntryHeader, CurrencyMetric metric) {
        Account account = journalEntryHeader.getAccount();
        if (account != null) {
            //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "Generating JE Line for Account: " + account.getName());
            JournalEntryLine line = new JournalEntryLine(journalEntryHeader, account, journalEntryHeader.getCurrency(), journalEntryHeader.getRevenueMethod());
            setJournalEntryLineAmounts(metric.getLcValue(), line);
            journalEntryHeader.addJournalEntryLine(line);

            Account offsetAccount = account.getOffsetAccount();
            if (offsetAccount != null) {
                //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "Generating JE Line for Offset Account: " + offsetAccount.getName());
                JournalEntryLine offsetLine = new JournalEntryLine(journalEntryHeader, offsetAccount, journalEntryHeader.getCurrency(), journalEntryHeader.getRevenueMethod());
                if (account.isCredit() && offsetAccount.isCredit() && metric.getLcValue() != null) {
                    setJournalEntryLineAmounts(metric.getLcValue().negate(), offsetLine);
                } else {
                    setJournalEntryLineAmounts(metric.getLcValue(), offsetLine);
                }
                journalEntryHeader.addJournalEntryLine(offsetLine);
            }
        }
    }

    private void setJournalEntryLineAmounts(BigDecimal amount, JournalEntryLine line) {
        if (amount == null) {
            line.setAmount(BigDecimal.ZERO);
            return;
        }
        line.setAmount(amount);
    }

    public void generateJournalEntryReport(InputStream inputStream, FileOutputStream outputStream, ReportingUnit reportingUnit, FinancialPeriod period) throws Exception {

        removeExistingJournals(reportingUnit, period);

        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet worksheet = workbook.getSheet("JournalEntryRU");

        Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "Generating JE worksheet.");
        worksheet = writeJournalEntryReport(worksheet, reportingUnit, period);
        Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "Finished generating JE worksheet.");

        ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
        ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A2"));

        XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);

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

        //BigDecimal revenueToRecognizePeriod = getRevenueRecognizePeriodLC(reportingUnit, period);
        //BigDecimal liquidatedDamageRecognizePeriodLC = getLiquidatedDamagesRecognizePeriodLC(reportingUnit, period);
        //BigDecimal costGoodsSoldPeriodLC = getCostGoodsSoldPeriodLC(reportingUnit, period);
        BigDecimal lossReservePeriodADJLC = getLossReservePeriodADJLC(reportingUnit, period);
        BigDecimal commExp = getThirdPartyCommissionsPeriod(reportingUnit, period);
        BigDecimal bilingsPeriodLC = getContractBillingsPeriodLC(reportingUnit, period);

        Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "commExp: " + commExp.toPlainString());
        //BigDecimal revenueInExcess = getContractRevenueInExcess(reportingUnit, period);
        //BigDecimal billingsInExcess = getContractBillingsInExcess(reportingUnit, period);
        //Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "revenueToRecognizePeriod: " + revenueToRecognizePeriod.toPlainString());
        row = worksheet.getRow(13);
        setCellValue(row, 5, lossReservePeriodADJLC);
        row = worksheet.getRow(14);
        setCellValue(row, 6, commExp);
        setCellValue(row, 14, commExp.negate());
        row = worksheet.getRow(15);
        setCellValue(row, 8, bilingsPeriodLC);

//        setCellValue(row, 2, revenueToRecognizePeriod);
//        setCellValue(row, 3, liquidatedDamageRecognizePeriodLC);
//        setCellValue(row, 4, costGoodsSoldPeriodLC);
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

        final int debitCol = 5;
        final int creditCol = 6;

        row = worksheet.getRow(41);
        JournalEntryLine salesPocInLinePOC = reportingUnitJournal.getReportingUnitSummaryJournalEntryLine(findAccountById("SALESPOC.IN"), RevenueMethod.PERC_OF_COMP);
        setCellValue(row, debitCol, salesPocInLinePOC.getDebitAmount());
        setCellValue(row, creditCol, salesPocInLinePOC.getCreditAmount());

        row = worksheet.getRow(42);
        JournalEntryLine salesLDPenaltyPOC = reportingUnitJournal.getReportingUnitSummaryJournalEntryLine(findAccountById("SALESLDPENALTY"), RevenueMethod.PERC_OF_COMP);
        setCellValue(row, debitCol, salesLDPenaltyPOC.getDebitAmount());
        setCellValue(row, creditCol, salesLDPenaltyPOC.getCreditAmount());

        row = worksheet.getRow(43);
        JournalEntryLine cosPocInPOC = reportingUnitJournal.getReportingUnitSummaryJournalEntryLine(findAccountById("COSPOC.IN"), RevenueMethod.PERC_OF_COMP);
        setCellValue(row, debitCol, cosPocInPOC.getDebitAmount());
        setCellValue(row, creditCol, cosPocInPOC.getCreditAmount());

        row = worksheet.getRow(44);
        JournalEntryLine invWipPOC = reportingUnitJournal.getReportingUnitSummaryJournalEntryLine(findAccountById("INV.WIP"), RevenueMethod.PERC_OF_COMP);
        setCellValue(row, debitCol, invWipPOC.getDebitAmount());
        setCellValue(row, creditCol, invWipPOC.getCreditAmount());

        row = worksheet.getRow(45);
        JournalEntryLine contrLiabPOC = reportingUnitJournal.getReportingUnitSummaryJournalEntryLine(findAccountById("STCONTLIAB"), RevenueMethod.PERC_OF_COMP);
        setCellValue(row, debitCol, contrLiabPOC.getDebitAmount());
        setCellValue(row, creditCol, contrLiabPOC.getCreditAmount());

        row = worksheet.getRow(48);
        JournalEntryLine lossReservePeriodAdjContract = reportingUnitJournal.getReportingUnitSummaryJournalEntryLine(findAccountById("GLVAR.INVADJ"));
        setCellValue(row, debitCol, lossReservePeriodAdjContract.getDebitAmount());
        setCellValue(row, creditCol, lossReservePeriodAdjContract.getCreditAmount());

        row = worksheet.getRow(50);
        JournalEntryLine lossReservePeriodAdjContractLiab = reportingUnitJournal.getReportingUnitSummaryJournalEntryLine(findAccountById("ACCLOSSRES"));
        setCellValue(row, debitCol, lossReservePeriodAdjContractLiab.getDebitAmount());
        setCellValue(row, creditCol, lossReservePeriodAdjContractLiab.getCreditAmount());

        row = worksheet.getRow(61);
        JournalEntryLine tpcExpense = reportingUnitJournal.getReportingUnitSummaryJournalEntryLine(findAccountById("COMMISH.EXP"));
        setCellValue(row, debitCol, tpcExpense.getDebitAmount());
        setCellValue(row, creditCol, tpcExpense.getCreditAmount());

        row = worksheet.getRow(62);
        JournalEntryLine tpcLiab = reportingUnitJournal.getReportingUnitSummaryJournalEntryLine(findAccountById("ACCOTHEXP.ROYALTY"));
        setCellValue(row, debitCol, tpcLiab.getDebitAmount());
        setCellValue(row, creditCol, tpcLiab.getCreditAmount());

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
        //BigDecimal commExp = getThirdPartyCommissionsPeriod(pobGroup, period);
        // Percentage of completion Pobs.  Set total row for POC POBs
        row = worksheet.getRow(single);
        setCellValue(row, 2, revenueToRecognizePeriod);
        setCellValue(row, 3, liquidatedDamageRecognizePeriodLC);
        setCellValue(row, 4, costGoodsSoldPeriodLC);
        setCellValue(row, 5, lossReservePeriodADJLC);
        //setCellValue(row, 6, commExp);
        setCellValue(row, 7, BigDecimal.ZERO);//TODO FX_GAIN_LOSS
        setCellValue(row, 8, BigDecimal.ZERO);//TODO BILLINGS_PERIOD_CC
        setCellValue(row, 9, costGoodsSoldPeriodLC.negate());
        if ("pitPobs".equals(pobGroup.getId()) || "slAndRTIPobs".equals(pobGroup.getId())) {
            setCellValue(row, 10, lossReservePeriodADJLC);
        }
        setCellValue(row, 11, revenueInExcess);
        setCellValue(row, 12, billingsInExcess);
        if ("pocPobs".equals(pobGroup.getId())) {
            setCellValue(row, 13, lossReservePeriodADJLC);
        }
        //setCellValue(row, 14, commExp.negate());

    }

    private void setCellValue(XSSFRow row, int cellNum, BigDecimal value) {
        if (value == null) {
            return;
        }
        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        CellStyle currentStyle = cell.getCellStyle();
        //CellStyle currentStyle = row.getSheet().getWorkbook().createCellStyle();
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

    private BigDecimal getThirdPartyCommissionsPeriod(Measurable measureable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric("THIRD_PARTY_COMMISSION_TO_RECOGNIZE_PERIOD_LC", measureable, period).getLcValue();
    }
}
