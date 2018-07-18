/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.service.DataUploadService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.poi.util.IOUtils;
import org.primefaces.event.FileUploadEvent;

/**
 *
 * @author span
 */
@Named
@RequestScoped
public class InputPOCIUpload implements Serializable {

    @Inject
    private DataUploadService dataUploadService;

    public static final String PREFIX = "msaccess";
    public static final String SUFFIX = ".tmp";

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public void handleInputPOCIUpload(FileUploadEvent event) {

        try {
            File accessFile = stream2file((InputStream) event.getFile().getInputstream());
            String fileName = accessFile.getAbsolutePath();
            dataUploadService.processUploadedCalculationData(fileName);

            FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");

            FacesContext.getCurrentInstance().addMessage(null, msg);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Error handleInputPOCIUpload: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error handleInputPOCIUpload.", e);
        }
    }

    public File stream2file(InputStream in) throws IOException {
        final File tempFile = File.createTempFile(PREFIX, SUFFIX);
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }

}
