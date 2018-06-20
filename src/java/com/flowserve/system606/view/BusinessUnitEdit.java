package com.flowserve.system606.view;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author span
 */
@ManagedBean(name = "businessUnitEdit")
@ViewScoped
public class BusinessUnitEdit implements Serializable {

    private BusinessUnit businessUnit = new BusinessUnit();

    @Inject
    private WebSession webSession;

    @Inject
    private AdminService adminService;
    
    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    public BusinessUnitEdit() {
    }

    @PostConstruct
    public void init() {
        businessUnit = webSession.getEditBusinessUnit();
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
