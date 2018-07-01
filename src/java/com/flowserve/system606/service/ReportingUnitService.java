/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.ReportingUnit;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author kgraves
 */
@Stateless
public class ReportingUnitService {

    @Inject
    private AdminService adminService;
    @Inject
    private BusinessRuleService businessRuleService;
    @Inject
    private PerformanceObligationService performanceObligationService;

    public void calculateAndSave(List<ReportingUnit> reportingUnits) throws Exception {
        for (ReportingUnit reportingUnit : reportingUnits) {
            businessRuleService.executeBusinessRules(reportingUnit.getPerformanceObligations());
        }

        adminService.update(reportingUnits);
    }

}
