/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.web;

import com.flowserve.system606.model.ReportingUnit;
import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author shubhamv
 */
@ManagedBean
@SessionScoped
public class ReportingUnitSession implements Serializable {

    /**
     * Creates a new instance of ReportingUnitSession
     */
    private ReportingUnit editReportingUnit;

    public ReportingUnitSession() {
    }

    public ReportingUnit getEditReportingUnit() {
        return editReportingUnit;
    }

    public void setEditReportingUnit(ReportingUnit editReportingUnit) {
        this.editReportingUnit = editReportingUnit;
    }

}
