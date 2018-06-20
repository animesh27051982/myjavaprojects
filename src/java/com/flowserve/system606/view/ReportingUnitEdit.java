/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author shubhamv
 */
@Named
@ViewScoped
public class ReportingUnitEdit implements Serializable {

    private ReportingUnit reporintgUnit = new ReportingUnit();
    @Inject
    private WebSession webSession;

    public ReportingUnitEdit() {
    }

    @PostConstruct
    public void init() {
        reporintgUnit = webSession.getEditReportingUnit();
    }

    public ReportingUnit getReporintgUnit() {
        return reporintgUnit;
    }
}
