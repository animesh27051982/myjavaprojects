/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.web.BusinessUnitSession;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

/**
 *
 * @author span
 */
@ManagedBean(name = "businessUnitEdit")
@ViewScoped
public class BusinessUnitEdit implements Serializable {

    @ManagedProperty(value = "#{businessUnitSession}")
    private BusinessUnitSession businessUnitSession;
    private BusinessUnit businessUnit = new BusinessUnit();

    /**
     * Creates a new instance of UserEdit
     */
    public BusinessUnitEdit() {
    }

    @PostConstruct
    public void init() {
        businessUnit = businessUnitSession.getEditBusinessUnit();
    }

    public BusinessUnitSession getBusinessUnitSession() {
        return businessUnitSession;
    }

    public void setBusinessUnitSession(BusinessUnitSession businessUnitSession) {
        this.businessUnitSession = businessUnitSession;
    }

    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(BusinessUnit businessUnit) {
        this.businessUnit = businessUnit;
    }

}
