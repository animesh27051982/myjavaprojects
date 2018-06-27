/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import java.io.InputStream;
import java.io.Serializable;
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
    @Inject
    AdminService adminService;

    @PostConstruct
    public void init() {
        reportingUnits = createReportingUnitTree();
        InputStream stream = PobInput.class.getResourceAsStream("/resources/excel_input_templates/Alonso_PIQ_POCI_IMPORT_TEMPLATE_v1.xlsx");
        file = new DefaultStreamedContent(stream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "poci_input_template.xlsx");

    }

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

    public StreamedContent getFile() {
        return file;
    }

    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }
}
