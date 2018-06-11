/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.web;

import com.flowserve.system606.model.User;
import java.io.Serializable;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

/**
 *
 * @author shubhamv
 */
@ManagedBean
@ApplicationScoped
public class UserSession implements Serializable {

    /**
     * Creates a new instance of UserSession
     */
    private User editUser;

    public UserSession() {
    }

    public User getEditUser() {
        return editUser;
    }

    public void setEditUser(User editUser) {
        this.editUser = editUser;
    }

}
