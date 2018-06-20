package com.flowserve.system606.view;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author span
 */
@Named
@ViewScoped
public class BusinessUnitEdit implements Serializable {

    private BusinessUnit businessUnit = new BusinessUnit();

    @Inject
    private WebSession webSession;

    public BusinessUnitEdit() {
    }

    @PostConstruct
    public void init() {
        businessUnit = webSession.getEditBusinessUnit();
    }

    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }
}
