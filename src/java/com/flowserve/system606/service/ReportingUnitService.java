/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author kgraves
 */
@Stateless
public class ReportingUnitService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    @Inject
    private AdminService adminService;

    public List<ReportingUnit> getPreparableReportingUnits(User user) {
        Query query = em.createQuery("SELECT ru FROM ReportingUnit ru where :USER MEMBER OF ru.preparers ORDER BY ru.code");
        query.setParameter("USER", user);
        return (List<ReportingUnit>) query.getResultList();
    }

    public List<ReportingUnit> getApprovableReportingUnits(User user) {
        Query query = em.createQuery("SELECT ru FROM ReportingUnit ru where :USER MEMBER OF ru.approvers ORDER BY ru.code");
        query.setParameter("USER", user);
        return (List<ReportingUnit>) query.getResultList();
    }

    public boolean isUpdatable(ReportingUnit ru, FinancialPeriod period, User user) {
        if (period.isOpen() && ru.isDraft(period)) {
            return true;
        }

        return false;
    }

    /**
     * KJG Enhance the methods below to check user role levels.
     */
    public void submitForReview(ReportingUnit reportingUnit, FinancialPeriod period) throws Exception {
        if (period.isOpen() && reportingUnit.isDraft(period)) {
            reportingUnit.setPeriodPendingReview(period);
            adminService.update(reportingUnit);
        }
    }

    public void submitForApproval(ReportingUnit reportingUnit, FinancialPeriod period) throws Exception {
        if (period.isOpen() && reportingUnit.isPendingReview(period)) {
            reportingUnit.setPeriodPendingApproval(period);
            adminService.update(reportingUnit);
        }
    }

    public void approve(ReportingUnit reportingUnit, FinancialPeriod period) throws Exception {
        if (period.isOpen() && reportingUnit.isPendingApproval(period)) {
            reportingUnit.setPeriodApproved(period);
            adminService.update(reportingUnit);
        }
    }
}
