package com.flowserve.system606.view;

import com.flowserve.system606.controller.AdminController;
import com.flowserve.system606.model.Company;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author span
 */
@Named
@ViewScoped
public class CompanyEdit implements Serializable {

    private Company company = new Company();

    @Inject
    private WebSession webSession;

    @Inject
    private AdminService adminService;

    @Inject
    private AdminController adminController;
    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public CompanyEdit() {
    }

    @PostConstruct
    public void init() {
        company = webSession.getEditCompany();
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

}
