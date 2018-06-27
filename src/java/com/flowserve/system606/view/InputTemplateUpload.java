/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;

/**
 *
 * @author kgraves
 */
@Named
@RequestScoped
public class InputTemplateUpload implements Serializable {

    private TreeNode pobs;
    private StreamedContent file;
    @Inject
    AdminService adminService;

    @PostConstruct
    public void init() {
        pobs = createPobs();
        InputStream stream = PobInput.class.getResourceAsStream("/resources/excel_input_templates/Alonso_PIQ_POCI_IMPORT_TEMPLATE_v1.xlsx");
        file = new DefaultStreamedContent(stream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "poci_input_template.xlsx");

    }

    public TreeNode getPobs() {
        return pobs;
    }

    public TreeNode createPobs() {
        TreeNode root = new DefaultTreeNode(new BusinessUnit(), null);

        // find reporting units assigned to user.  for now get all.
        //List<ReportingUnit> reportingUnits = adminService.findAllReportingUnits();
        List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();
        reportingUnits.add(adminService.findReportingUnitByCode("1015"));
        reportingUnits.add(adminService.findReportingUnitByCode("1100"));

        for (ReportingUnit reportingUnit : reportingUnits) {
            TreeNode reportingUnitNode = new DefaultTreeNode(reportingUnit, root);
            for (Contract contract : reportingUnit.getContract()) {
                TreeNode contractNode = new DefaultTreeNode(contract, reportingUnitNode);
                for (PerformanceObligation pob : contract.getPerformanceObligations()) {
                    TreeNode pobNode = new DefaultTreeNode(pob, contractNode);
                }
            }
        }

        return root;
    }

    public StreamedContent getFile() {
        return file;
    }
}
