/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.web;

import com.flowserve.system606.model.BusinessUnit;
//import com.flowserve.system606.model.User;
import java.io.Serializable;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author span
 */
@ManagedBean
@SessionScoped
public class BusinessUnitSession implements Serializable {

    /**
     * Creates a new instance of BusinessUnitSession
     */
    private BusinessUnit editBusinessUnit;

    public BusinessUnitSession() {
    }

    public BusinessUnit getEditBusinessUnit() {
        return editBusinessUnit;
    }

    public void setEditBusinessUnit(BusinessUnit editBusinessUnit) {
        this.editBusinessUnit = editBusinessUnit;
    }

}
