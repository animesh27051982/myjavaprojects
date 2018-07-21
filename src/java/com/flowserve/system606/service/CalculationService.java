/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.BillingEvent;
import com.flowserve.system606.model.BusinessRule;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.CurrencyMetric;
import com.flowserve.system606.model.DateMetric;
import com.flowserve.system606.model.DecimalMetric;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.Metric;
import com.flowserve.system606.model.MetricPriorPeriod;
import com.flowserve.system606.model.MetricStore;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.StringMetric;
import com.flowserve.system606.model.TransientMeasurable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.StatelessKieSession;

/**
 *
 * @author kgraves
 */
@Stateless
public class CalculationService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    @Inject
    private FinancialPeriodService financialPeriodService;
    @Inject
    private CurrencyService currencyService;
    @Inject
    private AdminService adminService;
    @Inject
    private MetricService metricService;
    private StatelessKieSession kSession = null;
    private static final String PACKAGE_PREFIX = "com.flowserve.system606.model.";

    //@PostConstruct
    public void initBusinessRulesEngine() {
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "initBusinessRulesEngine");

        try {
            KieServices ks = KieServices.Factory.get();
            //KieRepository kr = ks.getRepository();
            KieFileSystem kfs = ks.newKieFileSystem();

            //kfs.write("src/main/resources/DroolsTest.drl", "");
            for (BusinessRule rule : findAllBusinessRules()) {
                String drl = rule.getContent();
                kfs.write("src/main/resources/" + rule.getRuleKey() + ".drl", drl);
            }

            KieBuilder kb = ks.newKieBuilder(kfs).buildAll();

            if (kb.getResults().hasMessages(Message.Level.ERROR)) {
                throw new RuntimeException("Business Rule Build Errors:\n" + kb.getResults().toString());
            }

            kSession = ks.newKieContainer(ks.getRepository().getDefaultReleaseId()).newStatelessKieSession();
            kSession.setGlobal("logger", Logger.getLogger(CalculationService.class.getName()));
            //kSession.setGlobal("calcService", calculationService);

        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }

        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Finished initBusinessRulesEngine");
    }

    private void initializeCurrenciesCurrentPeriod(Collection<Metric> metrics, Measurable measurable) throws Exception {
        FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();

        for (Metric metric : metrics) {
            initializeCurrencies(metric, measurable, period);
        }
    }

    private void initializeCurrencies(Metric metric, Measurable measurable, FinancialPeriod period) throws Exception {
        if (metric.getValue() == null || !(metric instanceof CurrencyMetric)) {
            return;
        }
        if (metric.getMetricType().getMetricCurrencyType() == null) {
            throw new IllegalStateException("There is no currency type defined for the metric type " + metric.getMetricType().getId() + ".  Please contact a system administrator.");
        }
        if (measurable.getLocalCurrency() == null) {
            throw new IllegalStateException("There is no local currency defined for the reporting unit.  Please contact a system administrator.");
        }
        if (measurable.getContractCurrency() == null) {
            throw new IllegalStateException("There is no contract currency defined for the contract.  Please contact a system administrator.");
        }

        CurrencyMetric currencyMetric = (CurrencyMetric) metric;
        if (currencyMetric.isLocalCurrencyMetric()) {
            currencyMetric.setLocalCurrencyValue(currencyMetric.getValue());
            if (currencyMetric.getValue().equals(BigDecimal.ZERO)) {
                currencyMetric.setContractCurrencyValue(BigDecimal.ZERO);
            } else {
                BigDecimal contractCurrencyValue = currencyService.convert(currencyMetric.getValue(), measurable.getLocalCurrency(), measurable.getContractCurrency(), period);
                currencyMetric.setContractCurrencyValue(contractCurrencyValue);
            }
        } else if (currencyMetric.isContractCurrencyMetric()) {
            currencyMetric.setContractCurrencyValue(currencyMetric.getValue());
            if (currencyMetric.getValue().equals(BigDecimal.ZERO)) {
                currencyMetric.setLocalCurrencyValue(BigDecimal.ZERO);
            } else {
                BigDecimal localCurrencyValue = currencyService.convert(currencyMetric.getValue(), measurable.getContractCurrency(), measurable.getLocalCurrency(), period);
                currencyMetric.setLocalCurrencyValue(localCurrencyValue);
            }
        }
    }

    // intelliGet since this is no ordinary get.  Initialize any missing metrics on the fly.
    private Metric intelliGetMetric(MetricType metricType, Measurable measurable, FinancialPeriod period) {  // TODO - Exception type to be thrown?
        if (!measurable.metricSetExistsForPeriod(period)) {
            measurable.initializeMetricSetForPeriod(period);
        }
        if (!measurable.metricExistsForPeriod(period, metricType)) {
            measurable.initializeMetricForPeriod(period, metricType);
        }

        return measurable.getPeriodMetric(period, metricType);
    }

    private Metric getMetric(String metricTypeId, Measurable measurable, FinancialPeriod period) {
        //FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();
        return intelliGetMetric(metricService.findMetricTypeById(metricTypeId), measurable, period);
    }

//    private Metric getMetricPriorPeriod(String metricTypeId, Measurable measurable) {
//        FinancialPeriod period = financialPeriodService.getPriorFinancialPeriod();
//        return intelliGetMetric(metricService.findMetricTypeById(metricTypeId), measurable, period);
//    }
//    public String getStringMetricValue(String metricTypeId, Measurable measurable) {
//        return (String) getMetricCurrentPeriod(metricTypeId, measurable).getValue();
//    }
//    public LocalDate getDateMetricValue(String metricTypeId, Measurable measurable) {
//        return (LocalDate) getMetricCurrentPeriod(metricTypeId, measurable).getValue();
//    }
//    public BigDecimal getCurrencyMetricValue(String metricTypeId, Measurable measurable) {
//        return (BigDecimal) getMetricCurrentPeriod(metricTypeId, measurable).getValue();
//    }
//    public BigDecimal getCurrencyMetricValuePriorPeriod(String metricTypeId, PerformanceObligation pob) {
//        return (BigDecimal) getMetricPriorPeriod(metricTypeId, pob).getValue();
//    }
    public StringMetric getStringMetric(String metricTypeId, PerformanceObligation pob, FinancialPeriod period) {
        return (StringMetric) getMetric(metricTypeId, pob, period);
    }

    public DecimalMetric getDecimalMetric(String metricTypeId, Measurable measurable, FinancialPeriod period) {
        return (DecimalMetric) getMetric(metricTypeId, measurable, period);
    }

//    public BigDecimal getDecimalMetricValue(String metricTypeId, Measurable measurable) {
//        return (BigDecimal) getMetricCurrentPeriod(metricTypeId, measurable).getValue();
//    }
    public DateMetric getDateMetric(String metricTypeId, Measurable measurable, FinancialPeriod period) {
        return (DateMetric) getMetric(metricTypeId, measurable, period);
    }

    private FinancialPeriod getCurrentPeriod() {
        return financialPeriodService.getCurrentFinancialPeriod();
    }

    public CurrencyMetric getCurrencyMetric(String metricTypeId, Measurable measurable, FinancialPeriod period) throws Exception {
        if (measurable instanceof MetricStore) {
            CurrencyMetric currencyMetric = (CurrencyMetric) getMetric(metricTypeId, measurable, period);
            // TODO The getChildMeasurable needs work.  Could be an empty RU etc.
            if (isMetricAvailableAtThisLevel(currencyMetric) || measurable instanceof PerformanceObligation) {
                return currencyMetric;
            }
        }
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Metric not available at level. " + measurable.getClass() + " Returnig accumulated version: " + metricTypeId);
        return getAccumulatedCurrencyMetric(metricTypeId, measurable, period);
    }

    private boolean isMetricAvailableAtThisLevel(Metric metric) {
        return metric != null;
    }

//    public void putCurrencyMetricValue(String metricTypeId, Measurable measurable, BigDecimal value) {
//        if (isMetricAvailableAtThisLevel(getMetricCurrentPeriod(metricTypeId, measurable))) {
//            getMetricCurrentPeriod(metricTypeId, measurable).setValue(value);
//        } else {
//            Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Metric not available at contract level: " + metricTypeId);
//        }
//
//    }
//    public boolean isMetricRequired() {
//        for (Metric metric : periodMetricSets.values()) {
//            if (metric.getMetricType().isRequired() && metric.getValue() == null) {
//                return true;
//            }
//        }
//
//        return false;
//    }
    public void executeBusinessRules(Measurable measurable) throws Exception {
        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "Firing all business rules");
        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "Running currency conversions");

        if (kSession == null) {
            initBusinessRulesEngine();
        }

        List<Object> facts = new ArrayList<Object>();
        facts.add(measurable);
        Collection<Metric> currentPeriodMetrics = getAllCurrentPeriodMetrics(measurable);
        initializeCurrenciesCurrentPeriod(currentPeriodMetrics, measurable);
        facts.addAll(currentPeriodMetrics);
        facts.addAll(getAllPriorPeriodMetrics(measurable));
        kSession.execute(facts);
        initializeCurrenciesCurrentPeriod(currentPeriodMetrics, measurable);

        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "Firing all business rules complete.");
    }

    public void executeBusinessRules(List<PerformanceObligation> pobs) throws Exception {
        for (PerformanceObligation pob : pobs) {
            executeBusinessRules(pob);
        }
    }

//    public void executeBusinessRulesForAccumulable(Accumulable measurable) throws Exception {   // Pass in the contract as the measurable.
//
//        if (kSession == null) {
//            initBusinessRulesEngine();
//        }
//
//        List<Object> facts = new ArrayList<Object>();
//
//        facts.add(measurable);
//        Collection<Metric> currentPeriodMetrics = getAccumulated(measurable);
//        facts.addAll(currentPeriodMetrics);
//
//        kSession.execute(facts);
//
//    }
//    public Collection<Metric> getAccumulated(Measurable measurable) throws Exception {
//
//        // Init the input Metrics.  KG TODO - Needs work to make generic.
//        CurrencyMetric accPrice = getAccumulatedCurrencyMetric("TRANSACTION_PRICE_CC", measurable);
//        CurrencyMetric accLiquidatedDamages = getAccumulatedCurrencyMetric("LIQUIDATED_DAMAGES_ITD_CC", measurable);
//        CurrencyMetric accEAC = getAccumulatedCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", measurable);
//
//        // Init the output Metrics.  KG TODO - Needs work to make generic.
//        CurrencyMetric egp = new CurrencyMetric();
//        egp.setMetricType(metricService.findMetricTypeById("ESTIMATED_GROSS_PROFIT_LC"));
//        CurrencyMetric egm = new CurrencyMetric();
//        egm.setMetricType(metricService.findMetricTypeById("ESTIMATED_GROSS_MARGIN_LC"));
//
//        List<Metric> metrics = new ArrayList<Metric>();
//        metrics.add(accPrice);
//        metrics.add(accLiquidatedDamages);
//        metrics.add(accEAC);
//        metrics.add(egp);
//        metrics.add(egm);
//        return metrics;
//
//    }
    private Collection<Metric> getAllCurrentPeriodMetrics(Measurable measurable) throws Exception {
        FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();

        return getAllMetrics(measurable, period);
    }

    private Collection<Metric> getAllMetrics(Measurable measurable, FinancialPeriod period) throws Exception {

        List<MetricType> metricTypes = metricService.findActiveMetricTypes();
        List<Metric> metrics = new ArrayList<Metric>();
        for (MetricType metricType : metricTypes) {

            if (metricType.isCurrency()) {
                Metric currencyMetric = getCurrencyMetric(metricType.getId(), measurable, period);
                if (currencyMetric != null) {
                    metrics.add(currencyMetric);
                }
            } else {
                if (measurable instanceof MetricStore) {
                    Metric metric = intelliGetMetric(metricType, measurable, period);
                    if (metric != null) {
                        metrics.add(metric);
                    }
                }
            }
        }

        return metrics;
    }

    private Collection<MetricPriorPeriod> getAllPriorPeriodMetrics(Measurable measurable) throws Exception {
        FinancialPeriod period = financialPeriodService.getPriorFinancialPeriod();
        Collection<Metric> metrics = getAllMetrics(measurable, period);
        List<MetricPriorPeriod> priorPeriodMetrics = new ArrayList<MetricPriorPeriod>();
        for (Metric metric : metrics) {
            priorPeriodMetrics.add(new MetricPriorPeriod((metric)));
        }

        return priorPeriodMetrics;
    }

    private static boolean priorPeriodMetricDoesNotExist(Metric priorPeriodMetric) {
        return priorPeriodMetric.getValue() == null && priorPeriodMetric instanceof CurrencyMetric;
    }

    public BusinessRule findByRuleKey(String ruleKey) {
        Query query = em.createQuery("SELECT bu FROM BusinessRule bu WHERE bu.ruleKey = :RULE_KEY");
        query.setParameter("RULE_KEY", ruleKey);
        return (BusinessRule) query.getSingleResult();  // we want an exception if not one and only one.
    }

    public void persist(BusinessRule bu) throws Exception {
        em.persist(bu);
    }

    public BusinessRule update(BusinessRule bu) throws Exception {
        return em.merge(bu);
    }

    public List<BusinessRule> findAllBusinessRules() throws Exception {
        Query query = em.createQuery("SELECT bu FROM BusinessRule bu", BusinessRule.class);

        return (List<BusinessRule>) query.getResultList();
    }

    /**
     * This method will throw an exception if the underlying type does not support BigDecimal summation. We want the exception in the invalid case as it
     * indicates either a programming error or a business rules error. Neither of which is recoverable.
     */
//    public BigDecimal getAccumulatedCurrencyMetricValue(String metricTypeId, Accumulable measurable) {
//
//        BigDecimal sum = new BigDecimal("0.0");
//
//        if (measurable.getChildAccumulables().isEmpty() && measurable instanceof PerformanceObligation) {
//            // TODO - We can abstract this further instead of hard cast to pob.
//            BigDecimal value = getCurrencyMetric(metricTypeId, ((PerformanceObligation) measurable)).getValue();
//            if (value != null) {
//                sum = sum.add(value);
//            }
//            return sum;
//        }
//
//        for (Accumulable childAccumulable : measurable.getChildAccumulables()) {
//            sum = sum.add(getAccumulatedCurrencyMetricValue(metricTypeId, childAccumulable));
//        }
//
//        return sum;
//    }
    public BigDecimal getAccumulatedBilledValue(Measurable measurable) {

        BigDecimal sum = new BigDecimal("0.0");
        List<BillingEvent> bEvents = adminService.findBillingEventsByContract((Contract) measurable);
        for (BillingEvent be : bEvents) {
            sum = sum.add(be.getAmountContractCurrency());
        }
        return sum;
    }

    private CurrencyMetric getAccumulatedCurrencyMetric(String metricTypeId, Measurable measurable, FinancialPeriod period) throws Exception {
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "getAccumulatedCurrencyMetric()");
        BigDecimal sum = new BigDecimal("0.0");
        CurrencyMetric metric = new CurrencyMetric();
        metric.setMetricType(metricService.findMetricTypeById(metricTypeId));
        metric.setValue(sum);
        if (isEmptyTransientMesaurable(measurable)) {
            return metric;
        }
        if (isRootMeasurable(measurable)) {
            BigDecimal value = getCurrencyMetric(metricTypeId, measurable, period).getValue();
            if (value != null) {
                sum = sum.add(value);
            }
            metric.setValue(sum);
            return metric;
        }

        for (Measurable childMeasurable : measurable.getChildMeasurables()) {
            sum = sum.add(getAccumulatedCurrencyMetric(metricTypeId, childMeasurable, period).getValue());
        }

        metric.setValue(sum);

        //FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();
        initializeCurrencies(metric, measurable, period);

        return metric;
    }

    private boolean isRootMeasurable(Measurable measurable) {
        if (measurable instanceof PerformanceObligation) {
            return true;
        }

        return false;
    }

    private boolean isEmptyTransientMesaurable(Measurable measurable) {
        if (measurable.getChildMeasurables().isEmpty() && measurable instanceof TransientMeasurable) {
            return true;
        }

        return false;
    }

    public void initBusinessRules() throws Exception {
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "initBusinessRules");
        String content = new String(Files.readAllBytes(Paths.get(getClass().getResource("/resources/business_rules/massive_rule.drl").toURI())));

        if (findAllBusinessRules().isEmpty()) {
            BusinessRule businessRule = new BusinessRule();
            businessRule.setRuleKey("massive.rule");
            businessRule.setVersionNumber(1L);
            businessRule.setContent(content);
            persist(businessRule);
        } else {
            BusinessRule businessRule = findByRuleKey("massive.rule");
            businessRule.setContent(content);
            update(businessRule);
        }

        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Finished initBusinessRules");
    }
}
