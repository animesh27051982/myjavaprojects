/*
 * Copyright 2009-2014 PrimeTek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flowserve.system606.convert;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.service.AdminService;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import javax.naming.InitialContext;

@FacesConverter("businessUnitConverter")
public class BusinessUnitConverter implements Converter {
    
    public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
        if(value != null && value.trim().length() > 0) {
            try {

                InitialContext ic = new InitialContext();
                AdminService service = (AdminService) ic.lookup("java:global/FlowServe/AdminService"); //this works

                Logger.getLogger(BusinessUnitConverter.class.getName()).log(Level.INFO, "service:", service);

                List<BusinessUnit> businessUnits = service.getBusinessUnit(value);
                return businessUnits.get(0);

            } catch (Exception ex) {               
                Logger.getLogger(BusinessUnitConverter.class.getName()).log(Level.SEVERE, null, ex);
                throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Conversion Error", "Not a valid Business Unit."));
            }
        }
        else {
            return null;
        }
    }

    public String getAsString(FacesContext fc, UIComponent uic, Object object) {
        if(object != null) {
            return String.valueOf(((BusinessUnit) object).getName());
        }
        else {
            return null;
        }
    }   
}
