/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.service.CurrencyService;
import com.flowserve.system606.service.InputService;
import java.io.IOException;
import java.io.InputStream;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author shubhamv
 */
@ManagedBean
public class FileUploadView {

    private UploadedFile file;
    @EJB
    private CurrencyService currencyService;
    private String destination = "D:/Users/shubhamv/Documents/NetBeansProjects/FlowServe/src/java/Flat_file/";

    @EJB
    private InputService inputService;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void upload() {
        if (file != null) {
            FacesMessage message = new FacesMessage("Succesful", file.getFileName() + " is uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }

    public void handleFileUpload(FileUploadEvent event) {

        try {
            currencyService.copyFile(destination, event.getFile().getFileName(), event.getFile().getInputstream());
            currencyService.ReadFile(destination, event.getFile().getFileName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void handlePobFileUpload(FileUploadEvent event) throws Exception {
        // handle POI file read process here
        FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
        FacesContext.getCurrentInstance().addMessage(null, msg);

        inputService.readFeed((InputStream) event.getFile().getInputstream(), event.getFile().getFileName());
    }
}
