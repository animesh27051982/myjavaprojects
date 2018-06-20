/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.controller;

import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author shubhamv
 */
@FacesConverter(value = "userConverter")
public class UserConverter implements Converter {

    @Inject
    private AdminService adminService;

    @Override
    public Object getAsObject(FacesContext ctx, UIComponent component, String value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        //QuestionService questionService = null;
        try {
            InitialContext ic = new InitialContext();
            adminService = (AdminService) ic.lookup("java:global/service/AdminService");
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return adminService.findUserById(new Long(value));
    }

    @Override
    public String getAsString(FacesContext ctx, UIComponent component, Object value) {
        if (value instanceof String) {
            return null;
        }
        return ((User) value).getId().toString();
    }
}
