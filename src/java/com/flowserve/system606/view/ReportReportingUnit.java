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
 * @author shubhamc
 */
@Named
@ViewScoped
public class ReportReportingUnit implements Serializable{
     @Inject
    private WebSession webSession;
    
    ReportingUnit ru=new ReportingUnit();
    
    
     @PostConstruct
    public void init() {
        ru = webSession.getEditReportingUnit();
    }

    public ReportingUnit getRu() {
        return ru;
    }

    public void setRu(ReportingUnit ru) {
        this.ru = ru;
    }
    
}
