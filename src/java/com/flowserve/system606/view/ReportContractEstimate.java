/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.ReportsService;
import com.flowserve.system606.web.WebSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import javax.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        contract = webSession.getEditContract();
    }

    public StreamedContent getFile() throws Exception {
        try {

            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateContractEsimatesReport(inputStream, outputStream, contract);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Total_Contract_Estimate.xlsx");
        return file;
    }

    public StreamedContent getFileContractInception() throws Exception {
        try {

            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateReportfromInceptiontoDate(inputStream, outputStream, contract);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Contract_From_Inception_to_Date.xlsx");
        return file;
    }

    public StreamedContent getFileMonthlyIncomeImpact() throws Exception {
        try {

            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateReportMonthlyIncomeImpact(inputStream, outputStream, contract);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Monthly_Income_Statement_Impact.xlsx");
        return file;
    }

    public StreamedContent getFileQuarterlyIncomeImpact() throws Exception {
        try {

            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateReportQuarterlyIncomeImpact(inputStream, outputStream, contract);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Quarterly_Income_Statement_Impact.xlsx");
        return file;
    }

    public StreamedContent getFileAnnualIncomeImpact() throws Exception {
        try {

            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateReportAnnualIncomeImpact(inputStream, outputStream, contract);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Annual_Income_Statement_Impact.xlsx");
        return file;
    }

    public StreamedContent getJournalEntryReport() throws Exception {
        try {

            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Journal_Entry.xlsx");
            outputStream = new FileOutputStream(new File("Journal Entry Report.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateJournalEntryReport(inputStream, outputStream, contract);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Journal Entry Report.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Journal Entry.xlsx");
        return file;
    }

    public StreamedContent getCombineReport() throws Exception {
        try {

            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateCombineReport(inputStream, outputStream, contract);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Combine Contract Report.xlsx");
        return file;
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

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

}
