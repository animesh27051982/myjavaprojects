/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import java.io.InputStream;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.primefaces.california.domain.Document;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.DefaultStreamedContent;
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

    @PostConstruct
    public void init() {
        reportingUnits = createCheckboxDocuments();
        InputStream stream = PobInput.class.getResourceAsStream("/resources/excel_input_templates/Alonso_PIQ_POCI_IMPORT_TEMPLATE_v1.xlsx");
        file = new DefaultStreamedContent(stream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "poci_input_template.xlsx");

    }

    public TreeNode getReportingUnits() {
        return reportingUnits;
    }

    public TreeNode createCheckboxDocuments() {
        CheckboxTreeNode root = new CheckboxTreeNode(new Document("Files", "-", "Folder"), null);

        CheckboxTreeNode ru1015 = new CheckboxTreeNode(new Document("RU1015 FPD ENG'D MOOSIC PARTS DISTR", "-", "Folder", "AMSS", "-", "-", "-"), root);
        ru1015.setPartialSelected(true);
        CheckboxTreeNode ru1100 = new CheckboxTreeNode(new Document("RU1100 FPD ENG'D TANEYTOWN MD", "-", "Folder", "IPO", "-", "-", "-"), root);
        ru1100.setPartialSelected(true);
        CheckboxTreeNode ru1200 = new CheckboxTreeNode(new Document("RU1200 FPD ENG'D CPO VERNON", "-", "Folder", "EPO", "-", "-", "-"), root);
        ru1200.setPartialSelected(true);

        /**
         * TreeNode chevron = new DefaultTreeNode(new Document("Chevron Products - 1072381", "-", "Pages Document", "AMSS", "-", "-", "-"), ru1015);
         * chevron.setExpanded(true); TreeNode chevron1 = new DefaultTreeNode(new Document("POC-11261", "-", "Pages Document", "AMSS", "15", "10", "8"),
         * chevron); TreeNode chevron2 = new DefaultTreeNode(new Document("POC-13361", "-", "Pages Document", "AMSS", "30", "12", "7"), chevron);
         *
         * TreeNode phillips = new DefaultTreeNode(new Document("Phillips 66 - 07771205", "-", "Pages Document", "AMSS", "-", "-", "-"), ru1015);
         * phillips.setExpanded(true); TreeNode phillips1 = new DefaultTreeNode(new Document("POC-88776", "-", "Pages Document", "AMSS", "5", "2", "4"),
         * phillips); TreeNode phillips2 = new DefaultTreeNode(new Document("POC-88996", "-", "Pages Document", "AMSS", "21", "7", "6"), phillips);
         *
         * //Documents //TreeNode expenses = new DefaultTreeNode("document", new Document("Expenses.doc", "30 KB", "Word Document"), work); //TreeNode resume =
         * new DefaultTreeNode("document", new Document("Resume.doc", "10 KB", "Word Document"), work); //TreeNode refdoc = new DefaultTreeNode("document", new
         * Document("RefDoc.pages", "40 KB", "Pages Document"), primefaces);
         *
         *
         */
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
