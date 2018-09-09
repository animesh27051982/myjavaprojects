package com.flowserve.system606.view;

import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.service.FinancialPeriodService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@RequestScoped
public class FinancialPeriodEdit implements Serializable {

    @Inject
    private WebSession webSession;
    @Inject
    private FinancialPeriodService financialPeriodService;
    private FinancialPeriod financialPeriod;

    public FinancialPeriodEdit() {
    }

    @PostConstruct
    public void init() {
        financialPeriod = financialPeriodService.findById(webSession.getEditFinancialPeriodId());
    }

    public FinancialPeriod getFinancialPeriod() {
        return financialPeriod;
    }

    public String openPeriod() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            financialPeriodService.openPeriod(financialPeriod, webSession.getUser());
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, financialPeriod.getId() + " period opened.", ""));
        } catch (Exception e) {
            Logger.getLogger(FinancialPeriodEdit.class.getName()).log(Level.INFO, "Error opening period: ", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error opening period: ", e.getMessage()));
        }

        return "financialPeriodEdit";
    }

    public String closePeriod() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            financialPeriodService.closePeriod(financialPeriod);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, financialPeriod.getId() + " period closed.", ""));
        } catch (Exception e) {
            Logger.getLogger(FinancialPeriodEdit.class.getName()).log(Level.INFO, "Error closing period: ", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error closing period: ", e.getMessage()));
        }

        return "financialPeriodEdit";
    }

    public String freezePeriod() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            financialPeriodService.freezePeriod(financialPeriod);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, financialPeriod.getId() + " period frozen for user input.", ""));
        } catch (Exception e) {
            Logger.getLogger(FinancialPeriodEdit.class.getName()).log(Level.INFO, "Error freezing period: ", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error freezing period: ", e.getMessage()));
        }

        return "financialPeriodEdit";
    }
}
