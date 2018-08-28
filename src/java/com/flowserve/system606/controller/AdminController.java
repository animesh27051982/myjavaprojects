/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.controller;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.FinancialPeriodService;
import com.flowserve.system606.service.MetricService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.Currency;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author span
 */
@Named
@RequestScoped
public class AdminController implements Serializable {

    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    @Inject
    private AdminService adminService;
    @Inject
    private MetricService metricService;
    @Inject
    private FinancialPeriodService financialPeriodService;
    @Inject
    private WebSession webSession;

    public AdminController() {
    }

    public String detailMessage(DataImportFile d) throws Exception {
        webSession.setDataImportFile(d);
        return "importMessageList";
    }

    public String editBusinessUnit(BusinessUnit u) throws Exception {

        webSession.setEditBusinessUnit(u);
        return "businessUnitEdit";
    }

    public String newBusinessUnit(BusinessUnit u) throws Exception {

        webSession.setEditBusinessUnit(new BusinessUnit());
        return "businessUnitEdit";
    }

    public String updateBusinessUnit(BusinessUnit u) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            adminService.updateBusinessUnit(u);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "businessUnit saved", ""));

        return "businessUnitList";
    }

    public String addBusinessUnit(BusinessUnit u) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            adminService.persist(u);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "businessUnit saved", ""));

        return "businessUnitList";
    }

    public String editReportingUnit(ReportingUnit u) throws Exception {

        webSession.setEditReportingUnit(u);
        return "reportingUnitEdit";
    }

    public String newReportingUnit(ReportingUnit u) throws Exception {

        webSession.setEditReportingUnit(new ReportingUnit());
        return "reportingUnitEdit";
    }

    public String updateReportingUnit(ReportingUnit u) throws Exception {

        FacesContext context = FacesContext.getCurrentInstance();

        try {
            u.setLocalCurrency(Currency.getInstance(new Locale("en", u.getCountry().getCode())));
            adminService.update(u);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Reporting Unit saved", ""));

        return "reportingUnitSearch";
    }

    public String addReportingUnit(ReportingUnit u) throws Exception {
        System.out.println("addReportingUnit()");
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            u.setLocalCurrency(Currency.getInstance(new Locale("en", u.getCountry().getCode())));
            adminService.persist(u);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Reporting Unit saved", ""));

        return "reportingUnitSearch";
    }

    public String generateReportReportingUnit(ReportingUnit u) throws Exception {
        webSession.setEditReportingUnit(u);

        return "reportReportingUnit";
    }

    public String generateReportBusinessUnit(BusinessUnit u) throws Exception {
        webSession.setEditBusinessUnit(u);

        return "reportBusinessUnit";
    }

    public String editUser(User u) throws Exception {
        webSession.setEditUser(u);

        return "userEdit";
    }

    public String assumeIdentity(User u) throws Exception {
        webSession.setUser(u);
        webSession.init();

        return "dashboard";
    }

    public String updateUser(User u) {
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            adminService.updateUser(u);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage("Successful", "User saved"));

        return "userSearch";
    }

    public String updateUserProfile(User u) {
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            adminService.updateUser(u);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage("Successful", "User saved"));

        return "userProfile";
    }

    public String editFinancialPeriod(FinancialPeriod f) throws Exception {

        webSession.setEditFinancialPeriod(f);
        return "financialPeriodEdit";
    }

    public String addFinancialPeriod(FinancialPeriod financialPeriod) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            adminService.persist(financialPeriod);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "FinancialPeriod saved", ""));

        return "financialPeriodList";
    }

    public String addHoliday(Holiday h) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            adminService.persist(h);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "businessUnit saved", ""));

        return "holidaysList";
    }

    public String editHoliday(Holiday h) throws Exception {

        webSession.setEditHolidays(h);
        return "holidaysEdit";
    }

    public String newHoliday(Holiday h) throws Exception {

        webSession.setEditHolidays(new Holiday());
        return "holidaysEdit";
    }

    public String updateHoliday(Holiday h) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            adminService.updateHoliday(h);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "businessUnit saved", ""));

        return "holidaysList";
    }

    public String deleteHoliday(Holiday h) throws Exception {

        adminService.deleteHoliday(h);
        return "holidaysList";
    }

    public String editCompany(Company c) throws Exception {

        webSession.setEditCompany(c);
        return "companyEdit";
    }

    public String updateCompany(Company c) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            adminService.update(c);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Company saved", ""));

        return "companyList";
    }

    public String updateMetricType(MetricType m) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            metricService.update(m);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "MetricType saved", ""));
        return "metricTypeList";
    }

    public String editMetricType(MetricType m) {
        webSession.setMetricType(m);

        return "metricTypeEdit";
    }

    public String generateReport(Contract c) throws Exception {
        webSession.setEditContract(c);

        return "reportContractEstimate";
    }

    public String generateReportCompany(Company c) throws Exception {
        webSession.setEditCompany(c);

        return "companyReports";
    }
}
