/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.model.WorkflowAction;
import com.flowserve.system606.model.WorkflowActionType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * @author kgraves
 */
@Stateless
public class ReportingUnitService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    @Inject
    private AdminService adminService;

    public List<ReportingUnit> getViewableReportingUnits(User user) {
        Query query = em.createQuery("SELECT ru FROM ReportingUnit ru where :USER MEMBER OF ru.viewers ORDER BY ru.code");
        query.setParameter("USER", user);
        return (List<ReportingUnit>) query.getResultList();
    }

    public List<ReportingUnit> getPreparableReportingUnits(User user) {
        Query query = em.createQuery("SELECT ru FROM ReportingUnit ru where :USER MEMBER OF ru.preparers ORDER BY ru.code");
        query.setParameter("USER", user);
        return (List<ReportingUnit>) query.getResultList();
    }

    public List<ReportingUnit> getReviewableReportingUnits(User user) {
        Query query = em.createQuery("SELECT ru FROM ReportingUnit ru where :USER MEMBER OF ru.reviewers ORDER BY ru.code");
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
     * KJG Enhance the methods below to check user role levels. Deferring that check due to lack of time. We'll handle that check in the UI for now.
     */
    public void submitForReview(ReportingUnit reportingUnit, FinancialPeriod period, User user) throws Exception {
        if (period.isOpen() && reportingUnit.isDraft(period)) {
            WorkflowAction action = new WorkflowAction(WorkflowActionType.REQUEST_REVIEW, user);
            adminService.persist(action);
            reportingUnit.addWorkflowAction(period, action);
            reportingUnit.setPrepared(period);
            adminService.update(reportingUnit);
        }
    }

    public void submitForApproval(ReportingUnit reportingUnit, FinancialPeriod period, User user) throws Exception {
        if (period.isOpen() && reportingUnit.isReviewable(period, user)) {
            WorkflowAction action = new WorkflowAction(WorkflowActionType.REQUEST_APPROVAL, user);
            adminService.persist(action);
            reportingUnit.addWorkflowAction(period, action);
            reportingUnit.setReviewed(period);
            adminService.update(reportingUnit);
        }
    }

    public void approve(ReportingUnit reportingUnit, FinancialPeriod period, User user) throws Exception {
        if (period.isOpen() && reportingUnit.isApprovable(period, user)) {
            WorkflowAction action = new WorkflowAction(WorkflowActionType.APPROVE, user);
            adminService.persist(action);
            reportingUnit.addWorkflowAction(period, action);
            reportingUnit.setApproved(period);
            adminService.update(reportingUnit);
        }
    }

    public void review(ReportingUnit reportingUnit, FinancialPeriod period, User user) throws Exception {
        if (period.isOpen() && reportingUnit.isReviewable(period, user)) {
            WorkflowAction action = new WorkflowAction(WorkflowActionType.REVIEW, user);
            adminService.persist(action);
            reportingUnit.addWorkflowAction(period, action);
            reportingUnit.setReviewed(period);
            adminService.update(reportingUnit);
        }
    }

    public void reject(ReportingUnit reportingUnit, FinancialPeriod period, User user) throws Exception {
        if (period.isOpen() && !reportingUnit.isDraft(period)) {
            WorkflowAction action = new WorkflowAction(WorkflowActionType.REJECT, user);
            adminService.persist(action);
            reportingUnit.addWorkflowAction(period, action);
            reportingUnit.setRejected(period);
            adminService.update(reportingUnit);
        }
    }

    public void initializeDraft(ReportingUnit reportingUnit, FinancialPeriod period, User user) throws Exception {
        Logger.getLogger(ReportingUnitService.class.getName()).log(Level.INFO, "initializeDraft()");
        if (period.isOpen()) {
            //Logger.getLogger(ReportingUnitService.class.getName()).log(Level.INFO, "initializeDraft() it's open.");
            WorkflowAction action = new WorkflowAction(WorkflowActionType.INITIALIZE, user);
            adminService.persist(action);
            reportingUnit.addWorkflowAction(period, action);
            reportingUnit.setDraft(period);
            adminService.update(reportingUnit);
        }
    }

}
