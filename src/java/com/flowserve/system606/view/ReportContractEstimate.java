/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.JournalService;
import com.flowserve.system606.service.ReportsService;
import com.flowserve.system606.web.WebSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author shubhamv
 */
@Named
@ViewScoped
public class ReportContractEstimate implements Serializable {

    private StreamedContent file;
    private InputStream inputStream;
    private FileOutputStream outputStream;
    private Contract contract = new Contract();

    @Inject
    AdminService adminService;
    @Inject
    ReportsService reportsService;
    @Inject
    private WebSession webSession;
    @Inject
    private JournalService journalService;

    @PostConstruct
    public void init() {
        contract = webSession.getEditContract();
    }

    public void generateJournal(ReportingUnit reortingUnit) throws Exception {
        journalService.generateJournal(reortingUnit, webSession.getCurrentPeriod());
    }

    public StreamedContent getContractSummaryReport(Contract contract) {
        try {
            String filename = "Contract_Sum_Report_" + contract.getName() + ".xlsx";
            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            File tempFile = File.createTempFile(filename, ".xlsx");
            outputStream = new FileOutputStream(tempFile);
            reportsService.generateContractSummaryReport(inputStream, outputStream, contract);
            InputStream inputStreamFromOutputStream = new FileInputStream(tempFile);
            file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filename);
        } catch (Exception e) {
            Logger.getLogger(ReportContractEstimate.class.getName()).log(Level.INFO, "Error generating report: ", e);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error generating contract summary report: " + e.getMessage(), e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
        return file;
    }

    public StreamedContent getRUSummaryReport(ReportingUnit ru) {
        try {
            String filename = "RU_Sum_Report_" + ru.getCode() + ".xlsx";
            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            File tempFile = File.createTempFile(filename, ".xlsx");
            outputStream = new FileOutputStream(tempFile);
            reportsService.generateRUSummaryReport(inputStream, outputStream, ru);
            InputStream inputStreamFromOutputStream = new FileInputStream(tempFile);
            file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filename);
        } catch (Exception e) {
            Logger.getLogger(ReportContractEstimate.class.getName()).log(Level.INFO, "Error generating report: ", e);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error generating RU_summary report: " + e.getMessage(), e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
        return file;
    }

    public StreamedContent getFinancialSummaryReport(Contract contract) {
        try {
            String filename = "FS_Report_" + contract.getName() + ".xlsx";
            inputStream = ReportContractEstimate.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2_ORIGINAL.xlsx");
            File tempFile = File.createTempFile(filename, ".xlsx");
            outputStream = new FileOutputStream(tempFile);
            reportsService.generateReportFinancialSummary(inputStream, outputStream, contract);
            InputStream inputStreamFromOutputStream = new FileInputStream(tempFile);
            file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filename);
        } catch (Exception e) {
            Logger.getLogger(ReportContractEstimate.class.getName()).log(Level.INFO, "Error generating report: ", e);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error generating Financial summary report: " + e.getMessage(), e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
        return file;
    }

    public StreamedContent getJournalEntryReport(ReportingUnit reportingUnit) {
        try {
            String filename = "JE_Report_" + reportingUnit.getCode() + ".xlsx";
            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Journal_Entry_RU_Template.xlsx");
            File tempFile = File.createTempFile(filename, ".xlsx");
            outputStream = new FileOutputStream(tempFile);
            journalService.generateJournalEntryReport(inputStream, outputStream, reportingUnit, webSession.getCurrentPeriod());
            InputStream inputStreamFromOutputStream = new FileInputStream(tempFile);
            file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filename);
        } catch (Exception e) {
            Logger.getLogger(ReportContractEstimate.class.getName()).log(Level.INFO, "Error generating report: ", e);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error generating journal entry report: " + e.getMessage(), e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }

        return file;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

}
