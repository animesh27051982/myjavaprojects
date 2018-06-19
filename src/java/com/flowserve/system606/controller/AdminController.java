/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.controller;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.BusinessUnitSession;
import com.flowserve.system606.web.ReportingUnitSession;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;

/**
 *
 * @author span
 */
@ManagedBean(name = "adminController")
@RequestScoped
public class AdminController implements Serializable {

    /**
     * Creates a new instance of AdminController
     */
    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    @EJB
    private AdminService adminService;

    @ManagedProperty(value = "#{businessUnitSession}")
    private BusinessUnitSession businessUnitSession;

    @ManagedProperty(value = "#{reportingUnitSession}")
    private ReportingUnitSession reportingUnitSession;

    public AdminController() {
    }

    public String editBusinessUnit(BusinessUnit u) throws Exception {
        System.out.println("businessUnitSession:" + businessUnitSession);
        System.out.println("BusinessUnit:" + u);

        businessUnitSession.setEditBusinessUnit(u);
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

    public BusinessUnitSession getBusinessUnitSession() {
        return businessUnitSession;
    }

    public void setBusinessUnitSession(BusinessUnitSession businessUnitSession) {
        this.businessUnitSession = businessUnitSession;
    }

    public String editReportingUnit(ReportingUnit u) throws Exception {

        reportingUnitSession.setEditReportingUnit(u);
        return "reportingUnitEdit";
    }

    public ReportingUnitSession getReportingUnitSession() {
        return reportingUnitSession;
    }

    public void setReportingUnitSession(ReportingUnitSession reportingUnitSession) {
        this.reportingUnitSession = reportingUnitSession;
    }

}
