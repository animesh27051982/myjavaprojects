package com.flowserve.system606.service;

import com.flowserve.system606.model.Account;
import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.ContractJournal;
import com.flowserve.system606.model.CurrencyMetric;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.JournalEntryHeader;
import com.flowserve.system606.model.JournalEntryLine;
import com.flowserve.system606.model.JournalEntryType;
import com.flowserve.system606.model.JournalStatus;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.ReportingUnitJournal;
import com.flowserve.system606.model.SubledgerBatch;
import com.flowserve.system606.model.SubledgerLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
        ReportingUnitJournal reportingUnitJournal = new ReportingUnitJournal();
        reportingUnitJournal.setJournalStatus(JournalStatus.NEW);

        for (Contract contract : ru.getContracts()) {
            ContractJournal contractJournal = new ContractJournal(contract);
            contractJournal.setJournalStatus(JournalStatus.NEW);

            for (MetricType metricType : metricService.findActiveCurrencyMetricTypesWithAccount()) {
                CurrencyMetric metric = calculationService.getCurrencyMetric(metricType.getCode(), contract, period);
                JournalEntryHeader journalEntryHeader = new JournalEntryHeader(metric.getMetricType(), contract.getLocalCurrency(), LocalDate.now(), JournalEntryType.NORMAL);
                generateJournalEntryLines(journalEntryHeader, metric);
                contractJournal.addJournalEntryHeader(journalEntryHeader);
            }
            reportingUnitJournal.addContractJournal(contractJournal);
        }

        return reportingUnitJournal;
    }

    private void generateJournalEntryLines(JournalEntryHeader journalEntryHeader, CurrencyMetric metric) {
        MetricType metricType = journalEntryHeader.getMetricType();
        Account account = metricType.getAccount();
        if (account != null) {
            Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "Generating JE Line for Account: " + account.getName());
            JournalEntryLine line = new JournalEntryLine(account, journalEntryHeader.getCurrency());
            setJournalEntryLineAmounts(account, metric.getLcValue(), line);
            journalEntryHeader.addJournalEntryLine(line);

            Account offsetAccount = metricType.getAccount().getOffsetAccount();
            if (offsetAccount != null) {
                Logger.getLogger(JournalService.class.getName()).log(Level.INFO, "Generating JE Line for Offset Account: " + offsetAccount.getName());
                JournalEntryLine offsetLine = new JournalEntryLine(offsetAccount, journalEntryHeader.getCurrency());
                setJournalEntryLineAmounts(offsetAccount, metric.getLcValue(), line);
                journalEntryHeader.addJournalEntryLine(offsetLine);
            }
        }
    }

    private void setJournalEntryLineAmounts(Account account, BigDecimal amount, JournalEntryLine line) {
        if (account.isDebitAccount()) {
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                line.setDebitAmount(BigDecimal.ZERO);
                line.setCreditAmount(BigDecimal.ZERO);
            }
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                line.setDebitAmount(amount);
                line.setCreditAmount(BigDecimal.ZERO);
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                line.setDebitAmount(BigDecimal.ZERO);
                line.setCreditAmount(amount.abs());
            }
        }
        if (account.isCreditAccount()) {
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                line.setDebitAmount(BigDecimal.ZERO);
                line.setCreditAmount(BigDecimal.ZERO);
            }
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                line.setDebitAmount(BigDecimal.ZERO);
                line.setCreditAmount(amount);
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                line.setDebitAmount(amount.abs());
                line.setCreditAmount(BigDecimal.ZERO);
            }
        }
    }

    public List<PerformanceObligation> createAccounting(ReportingUnit reportingUnit) throws Exception {
        SubledgerBatch batch = new SubledgerBatch();
        batch.setBatchDate(LocalDate.now());
        adminService.persist(batch);
        List<PerformanceObligation> list = reportingUnit.getPerformanceObligations();
        List<SubledgerLine> sl = adminService.findSubledgerLines();
        if (sl.isEmpty() == true) {
            for (PerformanceObligation po : list) {

                try {
                    CurrencyMetric currencyMetric = calculationService.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", po, financialService.getCurrentFinancialPeriod());

                    if (currencyMetric.getValue() != null) {
                        //Line 1 in SubledgerLine for AccountedDr
                        SubledgerLine sub = new SubledgerLine();
                        Account subledger;
                        subledger = adminService.findAccountById("DummyDR");
                        Company com = adminService.findCompanyById("FLS");
                        sub.setAccountedDr(currencyMetric.getLcValue());
                        sub.setAccountingDate(LocalDate.now());
                        sub.setFinancialPeriod(financialService.getCurrentFinancialPeriod());
                        sub.setCompany(com);

                        sub.setSubledgerAccount(subledger);
                        adminService.persist(sub);

                        //Line 2 in SubledgerLine for AccountedCr
                        SubledgerLine subCredit = new SubledgerLine();
                        Account subAcc;
                        subAcc = adminService.findAccountById("DummyCR");
                        Company com2 = adminService.findCompanyById("FLS");
                        subCredit.setAccountedCr(currencyMetric.getLcValue().negate());
                        subCredit.setAccountingDate(LocalDate.now());
                        subCredit.setFinancialPeriod(financialService.getCurrentFinancialPeriod());
                        subCredit.setCompany(com2);
                        subCredit.setSubledgerAccount(subAcc);
                        adminService.persist(subCredit);

                    }

                } catch (Exception ex) {
                    Logger.getLogger(JournalService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return list;
    }
}
