package com.flowserve.system606.web;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 * @author kgraves
 */
@Named
@SessionScoped
public class WebSession implements Serializable {

    private BusinessUnit editBusinessUnit;
    private ReportingUnit editReportingUnit;
    private User editUser;
    private Country country;

    public BusinessUnit getEditBusinessUnit() {
        return editBusinessUnit;
    }

    public void setEditBusinessUnit(BusinessUnit editBusinessUnit) {
        this.editBusinessUnit = editBusinessUnit;
    }

    public ReportingUnit getEditReportingUnit() {
        return editReportingUnit;
    }

    public void setEditReportingUnit(ReportingUnit editReportingUnit) {
        this.editReportingUnit = editReportingUnit;
    }

    public User getEditUser() {
        return editUser;
    }

    public void setEditUser(User editUser) {
        this.editUser = editUser;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }
}
