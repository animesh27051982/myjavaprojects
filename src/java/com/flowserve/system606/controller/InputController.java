package com.flowserve.system606.controller;

import com.flowserve.system606.service.AdminService;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@RequestScoped
public class InputController implements Serializable {

    private static final Logger logger = Logger.getLogger("com.flowserve.system606");
    private static final long serialVersionUID = 6393550044523662986L;
    @Inject
    private AdminService adminService;

    public InputController() {
    }

    public String inputDashboard() {
        return "inputDashboard";
    }

    public String submitForApproval() {
        FacesMessage msg = new FacesMessage("Succesful", " Submitted for approval.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
        return "inputDashboard";
    }

    public String proceedToInputDownload() {
        return "inputTemplateDownload";
    }

    public String proceedToInputUpload() {
        return "inputTemplateUpload";
    }

    public String proceedToOnlineEntry() {
        return "inputOnlineEntry";
    }

}
