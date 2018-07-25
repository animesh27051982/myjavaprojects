/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.Company;
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
 * @author shubhamc
 */

@Named
@ViewScoped
public class CompanyReports implements Serializable {
    
    private StreamedContent file;
    private InputStream inputStream;
    private FileOutputStream outputStream;
    Company comp=new Company();
    
    @Inject
    AdminService adminService;
    @Inject
    ReportsService reportsService;
    @Inject
    private WebSession webSession;
    
     @PostConstruct
    public void init() {
      comp=webSession.getEditCompany();
    }

    public Company getComp() {
        return comp;
    }

    public void setComp(Company comp) {
        this.comp = comp;
    }

  
    
    public StreamedContent getFileFinancialSummary() throws Exception {
        try {

           inputStream =CompanyReports.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2_ORIGINAL.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2_ORIGINAL.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateReportFinancialSummary(inputStream, outputStream);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2_ORIGINAL.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "FinancialSummary.xlsx");
        return file;
    }
    
    
     public StreamedContent getFileDisclosures() throws Exception {
        try {

           inputStream =CompanyReports.class.getResourceAsStream("/resources/excel_input_templates/Outputs_Summary_v2_ORIGINAL.xlsx");
            outputStream = new FileOutputStream(new File("Outputs_Summary_v2_ORIGINAL.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        reportsService.generateReportDisclosures(inputStream, outputStream);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("Outputs_Summary_v2_ORIGINAL.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Disclosures.xlsx");
        return file;
    }
     
     
    
}
