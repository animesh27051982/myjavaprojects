/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.TemplateService;
import java.io.InputStream;
import java.io.Serializable;
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
 * @author shubhamv
 */
@Named
@RequestScoped
public class inputExchangeRate implements Serializable {

    @Inject
    AdminService adminService;
    @Inject
    private TemplateService templateService;
    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    @PostConstruct
    public void init() {

    }

    public void handleExchangeRates(FileUploadEvent event) {

        try {
            templateService.processExchangeRates((InputStream) event.getFile().getInputstream(), event.getFile().getFileName());

            FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");

            FacesContext.getCurrentInstance().addMessage(null, msg);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Exchange Rate Upload: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error handleExchangeRates.", e);
        }
    }

}
