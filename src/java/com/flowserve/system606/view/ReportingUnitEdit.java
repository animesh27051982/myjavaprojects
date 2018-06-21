/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author shubhamv
 */
@Named
@ViewScoped
public class ReportingUnitEdit implements Serializable {

    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    private ReportingUnit editReporintgUnit = new ReportingUnit();
    private Country country = new Country();
    List<Country> countries = new ArrayList<Country>();
    private User completedUser;
    private User completedPUser;
    @Inject
    private WebSession webSession;
    @Inject
    private AdminService adminService;

    public ReportingUnitEdit() {
    }

    @PostConstruct
    public void init() {
        editReporintgUnit = webSession.getEditReportingUnit();

        try {
            countries = adminService.AllCountry();
        } catch (Exception ex) {
            Logger.getLogger(ReportingUnitEdit.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void addApprover(User approver) {
        try {
            if (approver == null || editReporintgUnit.getApprovers().contains(approver)) {
                return;
            }
            editReporintgUnit.getApprovers().add(approver);
            editReporintgUnit = adminService.update(editReporintgUnit);
            completedUser = null;
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Site save error " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error site save.", e);
        }
    }

    public void removeApprover(User approver) {

        try {
            editReporintgUnit.getApprovers().remove(approver);
            editReporintgUnit = adminService.update(editReporintgUnit);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Site save error " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error site save.", e);
        }
    }

    public void addPreparer(User preparer) {

        try {
            if (preparer == null || editReporintgUnit.getPreparers().contains(preparer)) {
                return;
            }
            editReporintgUnit.getPreparers().add(preparer);
            editReporintgUnit = adminService.update(editReporintgUnit);
            completedPUser = null;
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Site save error " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error site save.", e);
        }
    }

    public void removePreparer(User preparer) {

        try {
            editReporintgUnit.getPreparers().remove(preparer);
            editReporintgUnit = adminService.update(editReporintgUnit);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Site save error " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error site save.", e);
        }
    }

    public ReportingUnit getEditReporintgUnit() {
        return editReporintgUnit;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public List<Country> getCountries() {
        return countries;
    }

    public void setCountries(List<Country> countries) {
        this.countries = countries;
    }

    public User getCompletedUser() {
        return completedUser;
    }

    public void setCompletedUser(User completedUser) {
        this.completedUser = completedUser;
    }

    public User getCompletedPUser() {
        return completedPUser;
    }

    public void setCompletedPUser(User completedPUser) {
        this.completedPUser = completedPUser;
    }

}
