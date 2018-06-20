/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.BusinessUnitSession;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 *
 * @author span
 */
@ManagedBean(name = "businessUnitEdit")
@ViewScoped
public class BusinessUnitEdit implements Serializable {

    @ManagedProperty(value = "#{businessUnitSession}")
    private BusinessUnitSession businessUnitSession;
    private BusinessUnit businessUnit = new BusinessUnit();
    @EJB
    private AdminService adminService;
    
    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    /**
     * Creates a new instance of UserEdit
     */
    public BusinessUnitEdit() {
    }

    @PostConstruct
    public void init() {
        businessUnit = businessUnitSession.getEditBusinessUnit();
    }

    public BusinessUnitSession getBusinessUnitSession() {
        return businessUnitSession;
    }

    public void setBusinessUnitSession(BusinessUnitSession businessUnitSession) {
        this.businessUnitSession = businessUnitSession;
    }

    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(BusinessUnit businessUnit) {
        this.businessUnit = businessUnit;
    }
    
    public List<BusinessUnit> completeSite(String searchString) {
        List<BusinessUnit> sites = null;

        try {
            sites = adminService.searchSites(searchString);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", " site location error  " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error siteLocations.", e);
        }
        return sites;
    }


}
