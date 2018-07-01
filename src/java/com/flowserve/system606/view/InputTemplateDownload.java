/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.TemplateService;
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
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;

/**
 *
 * @author kgraves
 */
@Named
@ViewScoped
public class InputTemplateDownload implements Serializable {

    private TreeNode reportingUnits;
    private TreeNode[] selectedNodes;
    private StreamedContent file;
    private InputStream inputStream;
    private FileOutputStream outputStream;
    @Inject
    AdminService adminService;
    @Inject
    TemplateService templateService;

    @PostConstruct
    public void init() {
        reportingUnits = createReportingUnitTree();
    }

    //    <p:treeTable value="#{inputTemplateDownload.reportingUnits}" from inputTemplateDownload.xhtml
    public TreeNode getReportingUnits() {
        return reportingUnits;
    }

    public TreeNode createReportingUnitTree() {
        TreeNode root = new DefaultTreeNode(new ReportingUnit(), null);
        TreeNode ru1015 = new CheckboxTreeNode(adminService.findReportingUnitByCode("1015"), root);
        ru1015.setPartialSelected(true);
        TreeNode ru1100 = new CheckboxTreeNode(adminService.findReportingUnitByCode("1100"), root);
        ru1100.setPartialSelected(true);

        return root;
    }

    public StreamedContent getFile() throws Exception {

        try {
            //reportingUnits = createReportingUnitTree();
            inputStream = PobInput.class.getResourceAsStream("/resources/excel_input_templates/POCI_Template.xlsx");
            outputStream = new FileOutputStream(new File("poci_input_template.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();
        reportingUnits.add(adminService.findReportingUnitByCode("1015"));
        reportingUnits.add(adminService.findReportingUnitByCode("1100"));
        templateService.processTemplateDownload(inputStream, outputStream, reportingUnits);

        //InputStream inputStreamFromUutputStream = new ByteArrayInputStream(outputStream.toByteArray());
        InputStream inputStreamFromOutputStream = new FileInputStream(new File("poci_input_template.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "poci_input_template.xlsx");
        return file;
    }

    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }
}
