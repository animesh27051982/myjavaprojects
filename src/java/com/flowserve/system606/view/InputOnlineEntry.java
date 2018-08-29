/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.CurrencyEvent;
import com.flowserve.system606.model.Event;
import com.flowserve.system606.model.EventType;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.service.CalculationService;
import com.flowserve.system606.service.ContractService;
import com.flowserve.system606.service.EventService;
import com.flowserve.system606.service.PerformanceObligationService;
import com.flowserve.system606.service.ReportingUnitService;
import com.flowserve.system606.service.TemplateService;
import com.flowserve.system606.web.WebSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.CellEditEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;

/**
 *
 * @author kgraves
 */
@Named
@ViewScoped
public class InputOnlineEntry implements Serializable {

    private static final Logger logger = Logger.getLogger(InputOnlineEntry.class.getName());

    private StreamedContent file;
    private InputStream inputStream;
    private FileOutputStream outputStream;
    @Inject
    TemplateService templateService;
    private TreeNode rootTreeNode;
    private TreeNode billingTreeNode;
    @Inject
    private EventService eventService;
    @Inject
    private AdminService adminService;
    @Inject
    private CalculationService calculationService;
    @Inject
    private ReportingUnitService reportingUnitService;
    @Inject
    private PerformanceObligationService performanceObligationService;
    private BigDecimal eacValue;
    @Inject
    private ViewSupport viewSupport;
    @Inject
    private ContractService contractService;
    @Inject
    private WebSession webSession;
    private TreeNode selectedNode;
    private List<Contract> contracts;
    private String activeTabIndex;
    private ReportingUnit reportingUnit;

    List<String> salesDestination = Arrays.asList("ASIA", "CHINA", "INDIA", "JAPAN",
            "EUROPE", "RUSCIS", "LA", "CANADA", "US",
            "ANTARCTICA", "OTHER", "N AFRICA", "OTHER AFRICA",
            "S AFRICA", "IRAQ", "MIDEAST");
    List<String> oeamDisagg = Arrays.asList("OE", "AM");

    @PostConstruct
    public void init() {
        reportingUnit = adminService.findReportingUnitById(webSession.getCurrentReportingUnit().getId());
        rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
        initContracts(reportingUnit);

        if (webSession.getFilterText() != null) {
            filterByContractText();
        }
        if (webSession.getSelectedContracts() != null && webSession.getSelectedContracts().length > 0) {
            filterByContracts();
        }
    }

    public String getTextStyle(Measurable measurable) {
        if (measurable instanceof ReportingUnit || measurable instanceof Contract) {
            return "color: grey; font-style: italic;";
        }

        return "";
    }

    public void switchPeriod(FinancialPeriod period) {
        webSession.switchPeriod(period);
        init();
    }

    public void onTabChange(TabChangeEvent event) {
        activeTabIndex = event.getTab().getId();

    }

    public void onReportingUnitSelect(SelectEvent event) {
        Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.INFO, "RU Selected");
        webSession.setFilterText(null);
        init();
    }

    public void initContracts(ReportingUnit reportingUnit) {
        contracts = reportingUnit.getContracts();
    }

    public void filterByContractText() {
        if (isEmpty(webSession.getFilterText())) {
            //rootTreeNode = viewSupport.generateNodeTree(reportingUnits);
            rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
            billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
        } else {
            viewSupport.filterNodeTree(rootTreeNode, webSession.getFilterText());
            viewSupport.filterBillingNodeTree(billingTreeNode, webSession.getFilterText());
        }
    }

    public void filterByContracts() {
        rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
        if (webSession.getSelectedContracts().length == 0) {
            rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
            billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
        } else {
            viewSupport.filterNodeTreeContracts(rootTreeNode, Arrays.asList(webSession.getSelectedContracts()));
            viewSupport.filterNodeTreeContracts(billingTreeNode, Arrays.asList(webSession.getSelectedContracts()));
        }
    }

    public void addBillingEvent(Contract contract) throws Exception {
        EventType billingEventType = eventService.getEventTypeByCode("BILLING_EVENT_CC");
        CurrencyEvent billingEvent = new CurrencyEvent();
        billingEvent.setCreatedBy(webSession.getUser());
        billingEvent.setCreationDate(LocalDateTime.now());
        billingEvent.setEventType(billingEventType);
        billingEvent.setContract(contract);
        billingEvent = (CurrencyEvent) eventService.update(billingEvent);
        billingEvent.setName("BillingEvent " + billingEvent.getId());
        contract.setNodeExpanded(true);
        calculationService.addEvent(contract, webSession.getCurrentPeriod(), billingEvent);
        contractService.update(contract);

        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
    }

    public void setBillingName(Event billingEvent, String billingNumber) {
        billingEvent.setName("Billing " + billingNumber);
    }

    public void removeBillingEvent(CurrencyEvent billingEvent) throws Exception {
        Contract contract = billingEvent.getContract();
        contract.setNodeExpanded(true);
        calculationService.removeEvent(contract, webSession.getCurrentPeriod(), billingEvent);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
    }

    public void clearFilterByContractText() {
        webSession.setFilterText(null);
        webSession.setSelectedContracts(null);
        rootTreeNode = viewSupport.generateNodeTree(reportingUnit);
        billingTreeNode = viewSupport.generateNodeTreeForBilling(reportingUnit);
    }

    private boolean isEmpty(String text) {
        if (text == null || "".equals(webSession.getFilterText().trim())) {
            return true;
        }

        return false;
    }

    public void onCellEdit(CellEditEvent event) {
        Object oldValue = event.getOldValue();
        Object newValue = event.getNewValue();

        if (newValue != null && !newValue.equals(oldValue)) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Cell Changed", "Old: " + oldValue + ", New:" + newValue);
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public BigDecimal getEacValue() {
        return eacValue;
    }

    public void setEacValue(BigDecimal eacValue) {
        this.eacValue = eacValue;
    }

    public void calculateOutputs(PerformanceObligation pob) throws Exception {
        try {
            Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.INFO, "Calcing outputs...");
            updateAuditInfo(pob);
            calculationService.executeBusinessRules(pob, webSession.getCurrentPeriod());

        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getCause().getCause().getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error calculateOutputs.", e);
        }
    }

    public void calculateEventOutputs(Event event) throws Exception {
        try {
            Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.INFO, "Calcing outputs...");
            updateAuditInfo(event.getContract());
            calculationService.executeBusinessRules(event.getContract(), webSession.getCurrentPeriod());
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getCause().getCause().getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error calculateOutputs.", e);
        }
    }

    public TreeNode getRootTreeNode() {
        return rootTreeNode;
    }

    public void saveInputs() throws Exception {
        if (viewSupport.isEditable()) {
            Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINE, "Saving inputs.");
            adminService.update(reportingUnit);
            for (Contract contract : reportingUnit.getContracts()) {
                Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINER, "Updating Contract: " + contract.getName());
                contractService.update(contract);
            }
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Inputs saved.", ""));
            Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINE, "Inputs saved.");
        }
    }

//    public boolean isUpdatable() {
//        return reportingUnitService.isUpdatable(reportingUnit, webSession.getCurrentPeriod(), webSession.getUser());
//    }
    public void cancelEdits() throws Exception {
        Logger.getLogger(InputOnlineEntry.class.getName()).log(Level.FINE, "Current edits canceled.");
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Current edits canceled.", ""));
        init();
    }

    public TreeNode getBillingTreeNode() {
        return billingTreeNode;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public int getProgress() {
        return 50;
    }

    public String getActiveTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(String activeTabIndex) {
        this.activeTabIndex = activeTabIndex;
    }

    public List<Contract> getContracts() {
        return contracts;
    }

    public void setContracts(List<Contract> contracts) {
        this.contracts = contracts;
    }

    public ReportingUnit getReportingUnit() {
        return reportingUnit;
    }

    private void updateAuditInfo(PerformanceObligation pob) {
        pob.setLastUpdatedBy(webSession.getUser());
        pob.setLastUpdateDate(LocalDateTime.now());
        pob.getContract().setLastUpdatedBy(webSession.getUser());
        pob.getContract().setLastUpdateDate(LocalDateTime.now());
    }

    private void updateAuditInfo(Contract contract) {
        contract.setLastUpdatedBy(webSession.getUser());
        contract.setLastUpdateDate(LocalDateTime.now());
    }

    public StreamedContent getFile() throws Exception {
        try {
            //reportingUnits = createReportingUnitTree();
            inputStream = PobInput.class.getResourceAsStream("/resources/excel_input_templates/POCI_Template_FINAL.xlsx");
            outputStream = new FileOutputStream(new File("POCI_Template_FINAL.xlsx"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        templateService.processTemplateDownload(inputStream, outputStream, reportingUnit);

        InputStream inputStreamFromOutputStream = new FileInputStream(new File("POCI_Template_FINAL.xlsx"));
        file = new DefaultStreamedContent(inputStreamFromOutputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "POCI_Template_FINAL.xlsx");
        return file;
    }

    public void handleTemplateUpload(FileUploadEvent event) {

        try {
            templateService.processTemplateUpload((InputStream) event.getFile().getInputstream(), event.getFile().getFileName());

            FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            FacesContext.getCurrentInstance().getExternalContext().redirect("inputOnlineEntry.xhtml");
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Error handleTemplateUpload: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
            logger.log(Level.SEVERE, "Error handleTemplateUpload.", e);
        }
    }

    public List<String> getSalesDestination() {
        return salesDestination;
    }

    public List<String> getOeamDisagg() {
        return oeamDisagg;
    }
}
