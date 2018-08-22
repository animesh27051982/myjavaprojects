/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.CurrencyMetric;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.SubledgerAccount;
import com.flowserve.system606.model.SubledgerBatch;
import com.flowserve.system606.model.SubledgerLine;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author shubhamc
 */
@Stateless
public class AccountingService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    @Inject
    private AdminService adminService;
    @Inject
    private CalculationService calculationService;
    @Inject
    private FinancialPeriodService financialService;

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
                        SubledgerAccount subledger;
                        subledger = adminService.findSubledgerAccountById("DummyDR");
                        Company com = adminService.findCompanyById("FLS");
                        sub.setAccountedDr(currencyMetric.getLcValue());
                        sub.setAccountingDate(LocalDate.now());
                        sub.setFinancialPeriod(financialService.getCurrentFinancialPeriod());
                        sub.setCompany(com);

                        sub.setSubledgerAccount(subledger);
                        adminService.persist(sub);

                        //Line 2 in SubledgerLine for AccountedCr
                        SubledgerLine subCredit = new SubledgerLine();
                        SubledgerAccount subAcc;
                        subAcc = adminService.findSubledgerAccountById("DummyCR");
                        Company com2 = adminService.findCompanyById("FLS");
                        subCredit.setAccountedCr(currencyMetric.getLcValue().negate());
                        subCredit.setAccountingDate(LocalDate.now());
                        subCredit.setFinancialPeriod(financialService.getCurrentFinancialPeriod());
                        subCredit.setCompany(com2);
                        subCredit.setSubledgerAccount(subAcc);
                        adminService.persist(subCredit);

                    }

                } catch (Exception ex) {
                    Logger.getLogger(AccountingService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return list;
    }

}
