/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.DataUploadService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
public class InitialContractUpload implements Serializable {

    @Inject
    private DataUploadService dataUploadService;
    @Inject
    AdminService adminService;
    List<DataImportFile> dataImportFile = new ArrayList<DataImportFile>();
    public static final String PREFIX = "msaccess";
    public static final String SUFFIX = ".tmp";

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public void handleInitialContractUpload(FileUploadEvent event) {

        try {
            File accessFile = stream2file((InputStream) event.getFile().getInputstream());
            String fileName = accessFile.getAbsolutePath();
            dataUploadService.initContract(fileName);

            FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");

            FacesContext.getCurrentInstance().addMessage(null, msg);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Error handleInitialContractUpload: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error handleInitialContractUpload.", e);
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

    public List<DataImportFile> getDataImportFile() throws Exception {
        dataImportFile = adminService.findDataImportFileByType("Contract and Pobs");
        Logger.getLogger(inputExchangeRate.class.getName()).log(Level.INFO, "message" + dataImportFile);
        //Collections.sort(dataImportFile);
        return dataImportFile;
    }

    public void setDataImportFile(List<DataImportFile> dataImportFile) {
        this.dataImportFile = dataImportFile;
    }

}
