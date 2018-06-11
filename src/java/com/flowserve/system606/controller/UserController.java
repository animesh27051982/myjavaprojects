/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.controller;

import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.UserSession;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;

/**
 *
 * @author shubhamv
 */
@ManagedBean(name = "userController")
@RequestScoped
public class UserController implements Serializable {

    /**
     * Creates a new instance of UserController
     */
    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    @EJB
    private AdminService adminService;

    @ManagedProperty(value = "#{userSession}")
    private UserSession userSession;

    public UserController() {
    }

    public String edit(User u) throws Exception {
        userSession.setEditUser(u);
        return "editUser";
    }

    public String update(User u) {
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            adminService.updateUser(u);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error saving", e.getMessage()));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "User saved", ""));

        return "userSearch";
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }

}
