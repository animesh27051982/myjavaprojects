/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.controller;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
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
    private WebSession webSession;

//    @Inject
//    private BusinessUnitSession businessUnitSession;
//    @ManagedProperty(value = "#{reportingUnitSession}")
//    private ReportingUnitSession reportingUnitSession;
    public AdminController() {
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

    public String editUser(User u) throws Exception {
        webSession.setEditUser(u);

        return "userEdit";
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

    public String updateFinancialPeriod(FinancialPeriod financialPeriod) {
         FacesContext context = FacesContext.getCurrentInstance();
        try {
            adminService.updateFinancialPeriod(financialPeriod);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "financialPeriod saved", ""));

        return "financialPeriodList";
    }

}
