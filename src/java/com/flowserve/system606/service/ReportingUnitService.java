/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

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
    private CalculationService calculationService;

}
