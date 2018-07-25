/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.CalculationService;
import com.flowserve.system606.service.TemplateService;
import com.flowserve.system606.web.WebSession;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;

/**
 *
 * @author kgraves
 */
@Named
@RequestScoped
public class InputTemplateUpload implements Serializable {

    @Inject
    AdminService adminService;
    @Inject
    private TemplateService templateService;
    @Inject
    private CalculationService calculationService;
    @Inject
    private WebSession webSession;
    private List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    @PostConstruct
    public void init() {
        //reportingUnits = adminService.getPreparableReportingUnits();
        reportingUnits.add(webSession.getCurrentReportingUnit());
    }

    public void handleTemplateUpload(FileUploadEvent event) {

        try {
            templateService.processTemplateUpload((InputStream) event.getFile().getInputstream(), event.getFile().getFileName());

            FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");

            FacesContext.getCurrentInstance().addMessage(null, msg);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Error handleTemplateUpload: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error handleTemplateUpload.", e);
        }
    }

    public List<ReportingUnit> getReportingUnits() {
        return reportingUnits;
    }

    public void saveInputs() throws Exception {
        Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINE, "Saving inputs.");
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Inputs saved.", ""));

        calculationService.calculateAndSave(reportingUnits, webSession.getCurrentPeriod());
    }

}
