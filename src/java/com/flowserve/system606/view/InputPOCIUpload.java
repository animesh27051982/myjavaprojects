/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.CalculationService;
import com.flowserve.system606.service.DataUploadService;
import com.flowserve.system606.service.ReportingUnitService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
public class InputPOCIUpload implements Serializable {

    @Inject
    private DataUploadService dataUploadService;
    @Inject
    private AdminService adminService;
    @Inject
    private ReportingUnitService reportingUnitService;
    @Inject
    private CalculationService calculationService;

    List<DataImportFile> dataImportFile = new ArrayList<DataImportFile>();

    public static final String PREFIX = "msaccess";
    public static final String SUFFIX = ".tmp";

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public void handleInputPOCIUpload(FileUploadEvent event) {

        try {
            File accessFile = stream2file((InputStream) event.getFile().getInputstream());
            String fileName = accessFile.getAbsolutePath();
            dataUploadService.processUploadedCalculationData(fileName, event.getFile().getFileName());
            calculationService.calcAllPobsApr2018(adminService.findBUByReportingUnitCode("1015"));
            //calculationService.calcAllPobsApr2018(adminService.findBUByReportingUnitCode("1100"));
            //calculationService.calcAllPobsApr2018(adminService.findBUByReportingUnitCode("8225"));
            //calculationService.calcAllPobsApr2018(adminService.findBUByReportingUnitCode("5200"));

            //get reporting unit to calculate business rules on the POBs
            //List<ReportingUnit> reportingUnits = adminService.getPreparableReportingUnits();
            // KJG - TODO - Need to calculate all periods.
            //reportingUnitService.calculateAndSave(reportingUnits);
            FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");

            FacesContext.getCurrentInstance().addMessage(null, msg);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "POCI Data Upload: " + e.getMessage());
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

    public List<DataImportFile> getDataImportFile() throws Exception {
        dataImportFile = adminService.findDataImportFileByType("POCI DATA");
        Collections.sort(dataImportFile);
        return dataImportFile;
    }

    public void setDataImportFile(List<DataImportFile> dataImportFile) {
        this.dataImportFile = dataImportFile;
    }

}
