/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.BatchProcessingService;
import com.flowserve.system606.service.CalculationService;
import com.flowserve.system606.service.FinancialPeriodService;
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

@Named
@RequestScoped
public class InputPOCIUpload implements Serializable {

    @Inject
    private AdminService adminService;
    @Inject
    private ReportingUnitService reportingUnitService;
    @Inject
    private BatchProcessingService calculationService;
    @Inject
    private FinancialPeriodService financialPeriodService;

    List<DataImportFile> dataImportFile = new ArrayList<DataImportFile>();

    public static final String PREFIX = "msaccess";
    public static final String SUFFIX = ".tmp";

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public void handleInputPOCIUpload(FileUploadEvent event) {

        try {
            File accessFile = stream2file((InputStream) event.getFile().getInputstream());
            String fileName = accessFile.getAbsolutePath();
            calculationService.processUploadedCalculationData(fileName, event.getFile().getFileName());

            // KJG For local testing of specific RUs.
            //calculationService.calcAllPobsApr2018(adminService.findBUByReportingUnitCode("1015"));
            //calculationService.calcAllPobsApr2018(adminService.findBUByReportingUnitCode("1100"));
            //calculationService.calcAllPobsApr2018(adminService.findBUByReportingUnitCode("8225"));
            //calculationService.calcAllPobsApr2018(adminService.findBUByReportingUnitCode("5200"));
            FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");

            FacesContext.getCurrentInstance().addMessage(null, msg);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "POCI Data Upload: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error handleInputPOCIUpload.", e);
        }
    }

//    public void calcAllPobsApr2018(ReportingUnit ru) throws Exception {
//        FinancialPeriod period = financialPeriodService.findById("DEC-17");
//
//        //calculateAndSave(adminService.findAllReportingUnits(), period);
//        List<ReportingUnit> rus = new ArrayList<ReportingUnit>();
//        rus.add(ru);
//        calculateAndSave(rus, period);
//
//    }
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

    public String calcAll() throws Exception {
        FinancialPeriod period = financialPeriodService.findById("DEC-17");

        // KJG Full load of all RUs.  Comment this out and use specific RUs for local testing.
        Logger.getLogger(InputPOCIUpload.class.getName()).log(Level.INFO, "Calculating all RUs...");
//            List<Long> ruIds = adminService.findAllReportingUnitIds();
//            for (Long ruId : ruIds) {

        Logger.getLogger(InputPOCIUpload.class.getName()).log(Level.INFO, "Calling calc and save from poci upload.");
        calculationService.calculateAllFinancials(adminService.findAllReportingUnits(), period);
//            }

        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Calcs complete.");

        return "inputPOCIUpload";
    }

}
