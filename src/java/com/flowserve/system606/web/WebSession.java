package com.flowserve.system606.web;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.PerformanceObligationService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 * @author kgraves
 */
@Named
@SessionScoped
public class WebSession implements Serializable {

    private BusinessUnit editBusinessUnit;
    //private List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();  // this is temp for calculatin testing.
    private TreeNode reportingUnitTreeNode = new DefaultTreeNode();  // this is temp for calculatin testing.
    private List<ReportingUnit> reportingUnits = new ArrayList<ReportingUnit>();
    private ReportingUnit editReportingUnit;
    private User editUser;
    private Country country;
    @Inject
    private PerformanceObligationService performanceObligationService;
    @Inject
    private AdminService adminService;

    @PostConstruct
    public void init() {   // this is temp for calculatin testing.
        try {

            reportingUnits.add(adminService.findReportingUnitByCode("1015"));
            reportingUnits.add(adminService.findReportingUnitByCode("1100"));

//            ReportingUnit ru1015 = new ReportingUnit();
//            ru1015.setId(12341L);
//            ru1015.setCode("1015");
//            ru1015.setName("1015 USA, Moosic - PMC, PA");
//
//            Contract ru1015Contract = new Contract();
//            ru1015Contract.setId(1234L);
//            ru1015Contract.setName("Chevron Proucts - " + ru1015Contract.getId());
//            ru1015Contract.setReportingUnit(ru1015);
//            ru1015.getContract().add(ru1015Contract);
//
//            PerformanceObligation po1015 = new PerformanceObligation();
//            po1015.setContract(ru1015Contract);
//            po1015.setId(11261L);
//            po1015.setName("POC-11261");
//            po1015.setRevRecMethod("POC");
//            performanceObligationService.initializeInputs(po1015);
//            performanceObligationService.initializeOutputs(po1015);
//            ru1015Contract.getPerformanceObligations().add(po1015);
//
//            reportingUnits.add(ru1015);
//
//            ReportingUnit ru1100 = new ReportingUnit();
//            ru1100.setId(12441L);
//            ru1100.setCode("1100");
//            ru1100.setName("1100 USA, Taneytown, MD");
//            //ReportingUnit ru1100 = adminService.findReportingUnitByCode("1100");
//            Contract ru1100Contract = new Contract();
//            ru1100Contract.setId(1235L);
//            ru1100Contract.setName("Phillips 66 - " + ru1100Contract.getId());
//            ru1100Contract.setReportingUnit(ru1100);
//            ru1100.getContract().add(ru1100Contract);
//
//            PerformanceObligation po1100 = new PerformanceObligation();
//            po1100.setContract(ru1100Contract);
//            po1100.setId(88776L);
//            po1100.setName("POC-88776");
//            po1100.setRevRecMethod("POC");
//            performanceObligationService.initializeInputs(po1100);
//            performanceObligationService.initializeOutputs(po1100);
//            ru1100Contract.getPerformanceObligations().add(po1100);
//            reportingUnits.add(ru1100);
            reportingUnitTreeNode = new DefaultTreeNode(new BusinessUnit(), null);

            // find reporting units assigned to user.  for now set two from websession
            for (ReportingUnit reportingUnit : reportingUnits) {
                Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "RU Name: " + reportingUnit.getName());
                TreeNode reportingUnitNode = new DefaultTreeNode(reportingUnit, reportingUnitTreeNode);
                reportingUnitNode.setExpanded(true);
                for (Contract contract : reportingUnit.getContract()) {
                    Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "Contract Name: " + contract.getName());
                    TreeNode contractNode = new DefaultTreeNode(contract, reportingUnitNode);
                    contractNode.setExpanded(true);
                    for (PerformanceObligation pob : contract.getPerformanceObligations()) {
                        Logger.getLogger(WebSession.class.getName()).log(Level.FINER, "POB ID: " + pob.getId());
                        performanceObligationService.initializeInputs(pob);
                        performanceObligationService.initializeOutputs(pob);

                        TreeNode pobNode = new DefaultTreeNode(pob, contractNode);
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public BusinessUnit getEditBusinessUnit() {
        return editBusinessUnit;
    }

    public void setEditBusinessUnit(BusinessUnit editBusinessUnit) {
        this.editBusinessUnit = editBusinessUnit;
    }

    public ReportingUnit getEditReportingUnit() {
        return editReportingUnit;
    }

    public void setEditReportingUnit(ReportingUnit editReportingUnit) {
        this.editReportingUnit = editReportingUnit;
    }

    public User getEditUser() {
        return editUser;
    }

    public void setEditUser(User editUser) {
        this.editUser = editUser;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public TreeNode getReportingUnitTreeNode() {
        return reportingUnitTreeNode;
    }

    public List<ReportingUnit> getReportingUnits() {
        return reportingUnits;
    }

}
