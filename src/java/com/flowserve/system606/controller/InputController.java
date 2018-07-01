package com.flowserve.system606.controller;

import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import java.io.Serializable;
import java.util.List;
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

    public String saveInputs(List<ReportingUnit> reportingUnits) throws Exception {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Inputs saved.", ""));
        adminService.update(reportingUnits);

        return "inputOnlineEntry";
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
