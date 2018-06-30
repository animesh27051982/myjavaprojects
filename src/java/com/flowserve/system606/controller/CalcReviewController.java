package com.flowserve.system606.controller;

import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@RequestScoped
public class CalcReviewController implements Serializable {

    private static final Logger logger = Logger.getLogger("com.flowserve.system606");
    private static final long serialVersionUID = 6393550544523662986L;
    @Inject
    private WebSession webSession;
    @Inject
    private AdminService adminService;

    public CalcReviewController() {
    }

    public String returnToInputs() {
        return "inputOnlineEntry";
    }

    public String reviewCalculations() throws Exception {
        // TODO - If problem here, then return back and show validations, etc.
        adminService.update(webSession.getReportingUnits());

        return "pobCalculationReview";
    }
}
