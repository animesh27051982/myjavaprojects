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
import java.io.IOException;
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

//    public StreamedContent getFile() throws Exception {
//        try {
//
//            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
//            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        reportsService.generateContractEsimatesReport(inputStream, outputStream, contract);
//
//        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
//        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Total_Contract_Estimate.xlsx");
//        return file;
//    }
//    public StreamedContent getFileMonthlyIncomeImpact() throws Exception {
//        try {
//
//            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
//            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        reportsService.generateReportByFinancialPeriod(inputStream, outputStream, contract);
//
//        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
//        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Monthly_Income_Statement_Impact.xlsx");
//        return file;
//    }
    public void generateJournal(ReportingUnit reortingUnit) throws Exception {
        journalService.generateJournal(reortingUnit, webSession.getCurrentPeriod());
    }

    public StreamedContent getFinancialSummaryReport() throws Exception {
        try {

            inputStream = ReportContractEstimate.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2_ORIGINAL.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2_ORIGINAL.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateReportFinancialSummary(inputStream, outputStream, contract);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2_ORIGINAL.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "FinancialSummary.xlsx");
        return file;
    }

    public StreamedContent getContractSummaryReport() throws Exception {
        try {

            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateContractSummaryReport(inputStream, outputStream, contract);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Combine Contract Report.xlsx");
        return file;
    }

    public StreamedContent getContractSummaryReport(Contract contract) throws Exception {
        this.contract = contract;

        return getContractSummaryReport();
    }

//    public StreamedContent getRUSummaryReport() throws Exception {
//        try {
//
//            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
//            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        reportsService.generateRUSummaryReport(inputStream, outputStream, contract);
//
//        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
//        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Contract Summary RU Level.xlsx");
//        return file;
//    }
    public StreamedContent getRUSummaryReport(ReportingUnit ru) throws Exception {
        try {

            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateRUSummaryReport(inputStream, outputStream, ru);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Contract Summary RU Level.xlsx");
        return file;
    }

    public StreamedContent getFinancialSummaryReport(Contract contract) throws Exception {
        this.contract = contract;

        return getFinancialSummaryReport();
    }

//    public StreamedContent getFileMonthlyIncomeImpact(Contract contract) throws Exception {
//        this.contract = contract;
//
//        return getFileMonthlyIncomeImpact();
//    }
//    public StreamedContent getFile(Contract contract) throws Exception {
//        this.contract = contract;
//
//        return getFile();
//    }
    public StreamedContent getJournalEntryReport(ReportingUnit reportingUnit) {
        try {
            String filename = "JE_Report_" + reportingUnit.getCode() + ".xlsx";
            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Journal_Entry_RU_Template.xlsx");
            outputStream = new FileOutputStream(new File("/tmp/" + filename));
            journalService.generateJournalEntryReport(inputStream, outputStream, reportingUnit, webSession.getCurrentPeriod());
            InputStream inputStreamFromOutputStream = new FileInputStream(new File("/tmp/" + filename));
            file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filename);
        } catch (Exception e) {
            Logger.getLogger(ReportContractEstimate.class.getName()).log(Level.INFO, "Error generating report: ", e);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error generating journal entry report: " + e.getMessage(), e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }

        return file;
    }

    public StreamedContent getContractSummaryReports(Contract contract) {
        try {
            String filename = "Cont_Sum_Report_" + contract.getName() + ".xlsx";
            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            outputStream = new FileOutputStream(new File("/tmp/" + filename));
            reportsService.generateContractSummaryReport(inputStream, outputStream, contract);
            InputStream inputStreamFromOutputStream = new FileInputStream(new File("/tmp/" + filename));
            file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filename);
        } catch (Exception e) {
            Logger.getLogger(ReportContractEstimate.class.getName()).log(Level.INFO, "Error generating report: ", e);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error generating contract summary report: " + e.getMessage(), e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
        return file;

    }

    public StreamedContent getFinancialSummaryReports(Contract contract) {
        try {
            String filename = "FS_Report_" + contract.getName() + ".xlsx";
            inputStream = ReportContractEstimate.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2_ORIGINAL.xlsx");
            outputStream = new FileOutputStream(new File("/tmp/" + filename));
            reportsService.generateReportFinancialSummary(inputStream, outputStream, contract);
            InputStream inputStreamFromOutputStream = new FileInputStream(new File("/tmp/" + filename));
            file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filename);
        } catch (Exception e) {
            Logger.getLogger(ReportContractEstimate.class.getName()).log(Level.INFO, "Error generating report: ", e);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error generating Financial summary report: " + e.getMessage(), e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
        return file;
    }

//    public StreamedContent getRUSummaryReports(Contract contract) {
//        try {
//            String filename = "RU_Sum_Report_" + contract.getName() + ".xlsx";
//            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
//            outputStream = new FileOutputStream(new File("/tmp/" + filename));
//            reportsService.generateRUSummaryReport(inputStream, outputStream, contract);
//            InputStream inputStreamFromOutputStream = new FileInputStream(new File("/tmp/" + filename));
//            file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filename);
//        } catch (Exception e) {
//            Logger.getLogger(ReportContractEstimate.class.getName()).log(Level.INFO, "Error generating report: ", e);
//            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error generating RU_summary report: " + e.getMessage(), e.getMessage());
//            FacesContext.getCurrentInstance().addMessage(null, msg);
//        }
//        return file;
//    }
    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

}
