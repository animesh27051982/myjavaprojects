/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.ReportsService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
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

    private List<ReportingUnit> reportingUnits;
    private List<ReportingUnit> selectedReportingUnits;

    @Inject
    AdminService adminService;
    @Inject
    ReportsService reportsService;

    @PostConstruct
    public void init() {
        reportingUnits = adminService.getPreparableReportingUnits();
        selectedReportingUnits = reportingUnits;
    }

    public StreamedContent getFile() throws Exception {
        try {

            inputStream = RceInput.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateContractEsimatesReport(inputStream, outputStream, selectedReportingUnits);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Outputs_Summary_v2.xlsx");
        return file;
    }

}
