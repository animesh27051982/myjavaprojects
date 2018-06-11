/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.User;
import com.flowserve.system606.web.UserSession;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

/**
 *
 * @author shubhamv
 */
@ManagedBean(name = "userEdit")
@ViewScoped
public class UserEdit implements Serializable {

    @ManagedProperty(value = "#{userSession}")
    private UserSession userSession;
    private User user = new User();

    /**
     * Creates a new instance of UserEdit
     */
    public UserEdit() {
    }

    @PostConstruct
    public void init() {
        user = userSession.getEditUser();
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
