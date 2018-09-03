/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.SubledgerLine;
import com.flowserve.system606.service.JournalService;
import com.flowserve.system606.service.AdminService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class SubledgerLineList implements Serializable{
    
    private List<SubledgerLine> list=new ArrayList<SubledgerLine>();
   
    @Inject
    private AdminService adminService;
    @Inject
    private JournalService accountingService;
    @PostConstruct
    public void init() 
    {
        try { 
            accountingService.createAccounting(adminService.findReportingUnitByCode("1100"));
        } catch (Exception ex) {
            Logger.getLogger(SubledgerLineList.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    public SubledgerLineList() 
    {
    }
    public List<SubledgerLine> getList() {
        
        list=adminService.findSubledgerLines();
        return list;
    }

    public void setList(List<SubledgerLine> list) {
        this.list = list;
    }
    
    
    
}
