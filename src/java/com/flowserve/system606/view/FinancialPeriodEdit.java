package com.flowserve.system606.view;

import com.flowserve.system606.controller.AdminController;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.PeriodStatus;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.logging.Logger;
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
public class FinancialPeriodEdit implements Serializable {

    private FinancialPeriod financialPeriod = new FinancialPeriod();

    @Inject
    private WebSession webSession;

    @Inject
    private AdminController adminController;

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

    public String addUpdateCondition() throws Exception {
        return this.financialPeriod.getId() == null ? adminController.addFinancialPeriod(this.financialPeriod) : adminController.updateFinancialPeriod(this.financialPeriod);
    }
    
    public String addOpenCondition() throws Exception {
        this.financialPeriod.setStatus(PeriodStatus.OPENED);
        return this.financialPeriod.getId() == null ? adminController.addFinancialPeriod(this.financialPeriod) : adminController.updateFinancialPeriod(this.financialPeriod);
    }
    
    public String addCloseCondition() throws Exception {
        this.financialPeriod.setStatus(PeriodStatus.CLOSED);        
        return this.financialPeriod.getId() == null ? adminController.addFinancialPeriod(this.financialPeriod) : adminController.updateFinancialPeriod(this.financialPeriod);
    }
    
    public String addFreezeCondition() throws Exception {
        this.financialPeriod.setStatus(PeriodStatus.USER_FROZEN);        
        return this.financialPeriod.getId() == null ? adminController.addFinancialPeriod(this.financialPeriod) : adminController.updateFinancialPeriod(this.financialPeriod);
    }
}
