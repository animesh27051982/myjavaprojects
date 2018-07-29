/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.ArrayList;
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
public class UserProfile implements Serializable {
    
//User currentuser= new User();
User sessionUser=new User();
List<Country> allCountries=new ArrayList<Country>();

   @Inject
   AdminService adminService;
   @Inject
   private WebSession webSession;
   
   @PostConstruct
    public void init() {
        try {
            
           webSession.setEditUser(adminService.findUserByFlsIdType("bga_admin"));
           sessionUser=webSession.getEditUser();
        
            allCountries = adminService.AllCountry();
        } catch (Exception ex) {
            Logger.getLogger(ReportingUnitEdit.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public User getSessionUser() {
        return sessionUser;
    }

    public void setSessionUser(User sessionUser) {
        this.sessionUser = sessionUser;
    }

    public List<Country> getAllCountries() {
        return allCountries;
    }

    public void setAllCountries(List<Country> allCountries) {
        this.allCountries = allCountries;
    }
   

}
