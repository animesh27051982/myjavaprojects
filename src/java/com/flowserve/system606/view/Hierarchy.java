/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author shubhamc
 */
@Named
@ViewScoped
public class Hierarchy implements Serializable {

    List<BusinessUnit> bu = new ArrayList<BusinessUnit>();
    private TreeNode root;
    @Inject
    private AdminService adminService;
    @Inject
    private ViewSupport viewSupport;

    @PostConstruct
    public void init() {
        try {
            bu = adminService.findBusinessUnits();
        } catch (Exception ex) {
            Logger.getLogger(Hierarchy.class.getName()).log(Level.SEVERE, null, ex);
        }
        root = new DefaultTreeNode("root", null);
        TreeNode fls = new DefaultTreeNode("Flowserve", root);
        for (BusinessUnit businessUnit : bu) {
            TreeNode bunits = new DefaultTreeNode(businessUnit.getName(), fls);
            for (ReportingUnit reportingUnit : businessUnit.getReportingUnit()) {
                TreeNode runits = new DefaultTreeNode(reportingUnit.getName(), bunits);
                for (Contract contract : reportingUnit.getContracts()) {
                    TreeNode con = new DefaultTreeNode(contract.getName(), runits);

                }
            }

        }

    }

    public TreeNode getRoot() {
        return root;
    }

}
