/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.service.ReportsService;
import java.io.Serializable;
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
public class PobOutput implements Serializable {

    @Inject
    ReportsService reportsService;

    public void downloadFile() throws Exception {

        try {

            reportsService.generatePobOutput();
            FacesMessage msg = new FacesMessage("Succesful", "File write completed.");

            FacesContext.getCurrentInstance().addMessage(null, msg);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "POCI Data Upload: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);

        }
    }

}
