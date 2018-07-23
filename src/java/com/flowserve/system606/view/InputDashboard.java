/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.ReportingUnitService;
import com.flowserve.system606.web.WebSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import org.glassfish.soteria.identitystores.annotation.Credentials;
import org.glassfish.soteria.identitystores.annotation.EmbeddedIdentityStoreDefinition;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.TreeNode;

/**
 *
 * @author kgraves
 */
@CustomFormAuthenticationMechanismDefinition(
        loginToContinue = @LoginToContinue(
                loginPage = "/login.xhtml",
                errorPage = ""
        )
)
@EmbeddedIdentityStoreDefinition({
    @Credentials(callerName = "kg", password = "kg", groups = {"users"})
    ,
        @Credentials(callerName = "arjan", password = "secret3", groups = {"foo"})}
)
@Named
@ViewScoped
public class InputDashboard implements Serializable {

    private static final Logger logger = Logger.getLogger(InputDashboard.class.getName());

    private TreeNode rootTreeNode;
    @Inject
    private AdminService adminService;
    @Inject
    private WebSession webSession;
    @Inject
    private ReportingUnitService reportingUnitService;
    @Inject
    private ViewSupport viewSupport;

    private List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();

    @PostConstruct
    public void init() {
        if (webSession.getCurrentReportingUnit() != null) {
            reportingUnits.clear();
            reportingUnits.add(webSession.getCurrentReportingUnit());
        }
    }

    public List<ReportingUnit> getReportingUnits() {
        return reportingUnits;
    }

    public void onReportingUnitSelect(SelectEvent event) {
        webSession.setCurrentReportingUnit((ReportingUnit) event.getObject());
        init();
    }

    public int getReportingUnitCount() {
        return reportingUnits.size();
    }

    public int getContractCount() {
        List<Contract> contracts = new ArrayList<Contract>();
        if (reportingUnits.size() > 0) {
            for (ReportingUnit reportingUnit : reportingUnits) {
                contracts.addAll(reportingUnit.getContracts());
            }
        }
        return contracts.size();
    }

    public long getPobCount() {
        long pobCount = 0;
        for (ReportingUnit reportingUnit : reportingUnits) {
            pobCount += reportingUnit.getPobCount();
        }

        return pobCount;
    }

    public long getPobInputRequiredCount() {
        long pobCount = 0;
        for (ReportingUnit reportingUnit : reportingUnits) {
            pobCount += reportingUnit.getPobInputRequiredCount();
        }

        return pobCount;

    }
}
