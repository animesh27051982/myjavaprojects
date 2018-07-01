/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.TemplateService;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
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
    private List<ReportingUnit> reportingUnits;

    @PostConstruct
    public void init() {
        reportingUnits = adminService.getPreparableReportingUnits();
    }

    public void handleTemplateUpload(FileUploadEvent event) throws Exception {
        templateService.processTemplateUpload((InputStream) event.getFile().getInputstream(), event.getFile().getFileName());

        FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public List<ReportingUnit> getReportingUnits() {
        return reportingUnits;
    }
}
