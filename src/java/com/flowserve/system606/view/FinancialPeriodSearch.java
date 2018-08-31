package com.flowserve.system606.view;

import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.service.FinancialPeriodService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@ViewScoped
public class FinancialPeriodSearch implements Serializable {

    List<FinancialPeriod> financialPeriods = new ArrayList<FinancialPeriod>();
    @Inject
    private FinancialPeriodService financialPeriodService;
    private String searchString = "";

    public void search() throws Exception {
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public List<FinancialPeriod> getFinancialPeriods() throws Exception {
        financialPeriods = financialPeriodService.findFinancialPeriods();
        Collections.sort(financialPeriods);

        return financialPeriods;
    }

    public void setFinancialPeriods(List<FinancialPeriod> financialPeriods) {
        this.financialPeriods = financialPeriods;
    }
}
