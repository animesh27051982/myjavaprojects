/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.BusinessRuleService;
import com.flowserve.system606.service.InputService;
import com.flowserve.system606.service.OutputService;
import com.flowserve.system606.service.PerformanceObligationService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.CellEditEvent;
import org.primefaces.model.TreeNode;

/**
 *
 * @author kgraves
 */
@Named
@ViewScoped
public class InputOnlineEntry implements Serializable {

    private static final Logger logger = Logger.getLogger(InputOnlineEntry.class.getName());

    private TreeNode rootTreeNode;
    @Inject
    private AdminService adminService;
    @Inject
    private PerformanceObligationService performanceObligationService;
    @Inject
    BusinessRuleService businessRuleService;
    private BigDecimal eacValue;
    @Inject
    private WebSession webSession;
    @Inject
    private InputService inputService;
    @Inject
    private OutputService outputService;
    @Inject
    private ViewSupport viewSupport;

    private List<ReportingUnit> reportingUnits;

    @PostConstruct
    public void init() {
        reportingUnits = adminService.getPreparableReportingUnits();
        rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
    }

    public void onCellEdit(CellEditEvent event) {
        Object oldValue = event.getOldValue();
        Object newValue = event.getNewValue();

        if (newValue != null && !newValue.equals(oldValue)) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Cell Changed", "Old: " + oldValue + ", New:" + newValue);
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public BigDecimal getEacValue() {
        return eacValue;
    }

    public void setEacValue(BigDecimal eacValue) {
        this.eacValue = eacValue;
    }

    public void calculateOutputs(PerformanceObligation pob) throws Exception {
        pob.printInputs();
        businessRuleService.executeBusinessRules(pob);
        pob.printOutputs();
    }

    public List<ReportingUnit> getReportingUnits() {
        return reportingUnits;
    }

    public TreeNode getRootTreeNode() {
        return rootTreeNode;
    }
}
