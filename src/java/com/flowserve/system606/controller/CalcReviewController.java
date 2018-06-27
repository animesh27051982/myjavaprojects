package com.flowserve.system606.controller;

import java.io.Serializable;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@Named
@RequestScoped
public class CalcReviewController implements Serializable {

    private static final Logger logger = Logger.getLogger("com.flowserve.system606");
    private static final long serialVersionUID = 6393550544523662986L;

    public CalcReviewController() {
    }

    public String returnToInputs() {
        return "inputOnlineEntry";
    }

    public String reviewCalculations() {
        return "pobCalculationReview";
    }
}
