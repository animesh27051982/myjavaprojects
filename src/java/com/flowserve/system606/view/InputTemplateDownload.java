/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.TemplateService;
import com.flowserve.system606.web.WebSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author kgraves
 */
@Named
@ViewScoped
public class InputTemplateDownload implements Serializable {

    private List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();
    private List<ReportingUnit> selectedReportingUnits;
    private StreamedContent file;
    private InputStream inputStream;
    private FileOutputStream outputStream;
    @Inject
    AdminService adminService;
    @Inject
    TemplateService templateService;
    @Inject
    private WebSession webSession;

    @PostConstruct
    public void init() {
        //reportingUnits = adminService.getPreparableReportingUnits();
        reportingUnits.add(webSession.getCurrentReportingUnit());
        selectedReportingUnits = reportingUnits;
    }

    public StreamedContent getFile() throws Exception {
        try {
            //reportingUnits = createReportingUnitTree();
            inputStream = PobInput.class.getResourceAsStream("/resources/excel_input_templates/POCI_Template.xlsx");
            outputStream = new FileOutputStream(new File("poci_input_template.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        templateService.processTemplateDownload(inputStream, outputStream, selectedReportingUnits);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("poci_input_template.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "poci_input_template.xlsx");
        return file;
    }

    public List<ReportingUnit> getSelectedReportingUnits() {
        return selectedReportingUnits;
    }

    public void setSelectedReportingUnits(List<ReportingUnit> selectedReportingUnits) {
        this.selectedReportingUnits = selectedReportingUnits;
    }

    public List<ReportingUnit> getReportingUnits() {
        return reportingUnits;
    }

}
