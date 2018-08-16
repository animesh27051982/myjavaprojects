package com.flowserve.system606.view;

import com.flowserve.system606.controller.AdminController;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.service.FinancialPeriodService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author span
 */
@Named
@ViewScoped
public class FinancialPeriodEdit implements Serializable {

    private FinancialPeriod financialPeriod = new FinancialPeriod();

    @Inject
    private WebSession webSession;

    @Inject
    private AdminController adminController;
    @Inject
    private FinancialPeriodService financialPeriodService;

    public FinancialPeriodEdit() {
    }

    @PostConstruct
    public void init() {
        financialPeriod = webSession.getEditFinancialPeriod();
    }

    public FinancialPeriod getFinancialPeriod() {
        return financialPeriod;
    }

    public void setFinancialPeriod(FinancialPeriod financialPeriod) {
        this.financialPeriod = financialPeriod;
    }

    public String openPeriod() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            financialPeriodService.openPeriod(financialPeriod);
        } catch (Exception e) {
            Logger.getLogger(FinancialPeriodEdit.class.getName()).log(Level.INFO, "Error opening period: ", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error opening period: ", e.getMessage()));
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Period opened.", ""));

        return "financialPeriodEdit";
    }

    public String closePeriod() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            financialPeriodService.closePeriod(financialPeriod);
        } catch (Exception e) {
            Logger.getLogger(FinancialPeriodEdit.class.getName()).log(Level.INFO, "Error closing period: ", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error closing period: ", e.getMessage()));
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Period closed.", ""));

        return "financialPeriodEdit";
    }

    public String freezePeriod() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            financialPeriodService.freezePeriod(financialPeriod);
        } catch (Exception e) {
            Logger.getLogger(FinancialPeriodEdit.class.getName()).log(Level.INFO, "Error freezing period: ", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Error freezing period: ", e.getMessage()));
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Period freezed.", ""));

        return "financialPeriodEdit";
    }

//    public String addCloseCondition() throws Exception {
//        this.financialPeriod.setStatus(PeriodStatus.CLOSED);
//        return this.financialPeriod.getId() == null ? adminController.addFinancialPeriod(this.financialPeriod) : adminController.updateFinancialPeriod(this.financialPeriod);
//    }
//
//    public String addFreezeCondition() throws Exception {
//        this.financialPeriod.setStatus(PeriodStatus.USER_FROZEN);
//        return this.financialPeriod.getId() == null ? adminController.addFinancialPeriod(this.financialPeriod) : adminController.updateFinancialPeriod(this.financialPeriod);
//    }
}
