/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.InputTypeName;
import com.flowserve.system606.model.OutputTypeName;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.CurrencyService;
import com.flowserve.system606.service.InputService;
import com.flowserve.system606.service.OutputService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author shubhamv
 */
@Named(value = "viewSupport")
@ApplicationScoped
public class ViewSupport implements Serializable {

    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    List<User> users = new ArrayList<User>();
    @Inject
    private AdminService adminService;
    @Inject
    private CurrencyService currencyService;
    @Inject
    private InputService inputService;
    @Inject
    private OutputService outputService;
    private String searchString = "";

    /**
     * Creates a new instance of ViewSupport
     */
    public ViewSupport() {
    }

    public List<User> completeUser(String searchString) {
        List<User> users = null;
        try {
            users = adminService.findByStartsWithLastName(searchString);

        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", " user search error  " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error search users.", e);
        }

        return users;

    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public List<Currency> getAllCurrencies() {
        List<Currency> currencyList = new ArrayList<Currency>();
        currencyList.addAll(Currency.getAvailableCurrencies());
        Collections.sort(currencyList, (c1, c2) -> c1.getCurrencyCode().compareTo(c2.getCurrencyCode()));

        return currencyList;
    }

    public TreeNode generateNodeTree(List<ReportingUnit> reportingUnits) {
        TreeNode root = new DefaultTreeNode(new BusinessUnit(), null);

        for (ReportingUnit reportingUnit : reportingUnits) {
            Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree RU Name: " + reportingUnit.getName());
            TreeNode reportingUnitNode = new DefaultTreeNode(reportingUnit, root);
            reportingUnitNode.setExpanded(true);
            for (Contract contract : reportingUnit.getContract()) {
                Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree Contract Name: " + contract.getName());
                TreeNode contractNode = new DefaultTreeNode(contract, reportingUnitNode);
                contractNode.setExpanded(true);
                for (PerformanceObligation pob : contract.getPerformanceObligations()) {
                    Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Adding to tree POB ID: " + pob.getId());
                    new DefaultTreeNode(pob, contractNode);
                }
            }
        }

        return root;
    }

    public String getInputTypeDescription(InputTypeName inputName) {
        return inputService.findInputTypeByName(inputName).getDescription();
    }

    public String getOutputTypeDescription(OutputTypeName outputName) {
        return outputService.findOutputTypeByName(outputName).getDescription();
    }

}
