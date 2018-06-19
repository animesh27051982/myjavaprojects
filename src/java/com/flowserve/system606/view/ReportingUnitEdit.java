/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.web.ReportingUnitSession;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

/**
 *
 * @author shubhamv
 */
@ManagedBean(name = "reportingUnitEdit")
@ViewScoped
public class ReportingUnitEdit implements Serializable {

    /**
     * Creates a new instance of ReportingUnitEdit
     */
    @ManagedProperty(value = "#{reportingUnitSession}")
    private ReportingUnitSession reportingUnitSession;
    private ReportingUnit reporintgUnit = new ReportingUnit();

    public ReportingUnitEdit() {
    }

    @PostConstruct
    public void init() {
        reporintgUnit = reportingUnitSession.getEditReportingUnit();
        System.out.println("com." + reporintgUnit);
    }

    public ReportingUnit getReporintgUnit() {
        return reporintgUnit;
    }

    public void setReporintgUnit(ReportingUnit reporintgUnit) {
        this.reporintgUnit = reporintgUnit;
    }

    public ReportingUnitSession getReportingUnitSession() {
        return reportingUnitSession;
    }

    public void setReportingUnitSession(ReportingUnitSession reportingUnitSession) {
        this.reportingUnitSession = reportingUnitSession;
    }

}
