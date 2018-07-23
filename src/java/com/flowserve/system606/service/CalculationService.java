/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.BusinessRule;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.CurrencyMetric;
import com.flowserve.system606.model.CurrencyMetricPriorPeriod;
import com.flowserve.system606.model.DateMetric;
import com.flowserve.system606.model.DecimalMetric;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.Metric;
import com.flowserve.system606.model.MetricStore;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.StringMetric;
import com.flowserve.system606.model.TransientMeasurable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }

        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Finished initBusinessRulesEngine");
    }

    private void convertCurrencies(Collection<Metric> metrics, Measurable measurable, FinancialPeriod period) throws Exception {
        for (Metric metric : metrics) {
            convertCurrency(metric, measurable, period);
        }
    }

    private void convertCurrency(Metric metric, Measurable measurable, FinancialPeriod period) throws Exception {
        if (!(metric instanceof CurrencyMetric)) {
            return;
        }
        CurrencyMetric currencyMetric = (CurrencyMetric) metric;

        if (currencyMetric.getLcValue() == null && currencyMetric.getCcValue() == null) {
            return;
        }

        if (metric.getMetricType().getMetricCurrencyType() == null) {
            throw new IllegalStateException("There is no currency type defined for the metric type " + metric.getMetricType().getId() + ".  Please contact a system administrator.");
        }
        if (measurable.getLocalCurrency() == null) {
            throw new IllegalStateException("There is no local currency defined.  Please contact a system administrator.");
        }
        if (measurable.getContractCurrency() == null) {
            throw new IllegalStateException("There is no contract currency defined.  Please contact a system administrator.");
        }
        if (measurable.getReportingCurrency() == null) {
            throw new IllegalStateException("There is no reporting currency defined.  Please contact a system administrator.");
        }

        if (currencyMetric.isLocalCurrencyMetric()) {
            if (BigDecimal.ZERO.equals(currencyMetric.getLcValue())) {
                currencyMetric.setCcValue(BigDecimal.ZERO);
                currencyMetric.setRcValue(BigDecimal.ZERO);
            } else {
                currencyMetric.setCcValue(currencyService.convert(currencyMetric.getLcValue(), measurable.getLocalCurrency(), measurable.getContractCurrency(), period));
                currencyMetric.setRcValue(currencyService.convert(currencyMetric.getLcValue(), measurable.getLocalCurrency(), measurable.getReportingCurrency(), period));
            }
        } else if (currencyMetric.isContractCurrencyMetric()) {
            if (BigDecimal.ZERO.equals(currencyMetric.getCcValue())) {
                currencyMetric.setLcValue(BigDecimal.ZERO);
                currencyMetric.setRcValue(BigDecimal.ZERO);
            } else {
                currencyMetric.setLcValue(currencyService.convert(currencyMetric.getCcValue(), measurable.getContractCurrency(), measurable.getLocalCurrency(), period));
                currencyMetric.setRcValue(currencyService.convert(currencyMetric.getCcValue(), measurable.getContractCurrency(), measurable.getReportingCurrency(), period));
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

    public StringMetric getStringMetric(String metricTypeId, PerformanceObligation pob, FinancialPeriod period) {
        return (StringMetric) getMetric(metricTypeId, pob, period);
    }

    public DecimalMetric getDecimalMetric(String metricTypeId, Measurable measurable, FinancialPeriod period) {
        return (DecimalMetric) getMetric(metricTypeId, measurable, period);
    }

    public DateMetric getDateMetric(String metricTypeId, Measurable measurable, FinancialPeriod period) {
        return (DateMetric) getMetric(metricTypeId, measurable, period);
    }

    public CurrencyMetric getCurrencyMetric(String metricTypeId, Measurable measurable, FinancialPeriod period) throws Exception {
        if (measurable instanceof MetricStore) {
            CurrencyMetric currencyMetric = (CurrencyMetric) getMetric(metricTypeId, measurable, period);
            // TODO The getChildMeasurable needs work.  Could be an empty RU etc.
            if (isMetricAvailableAtThisLevel(currencyMetric) || measurable instanceof PerformanceObligation) {
                return currencyMetric;
            }
        }
        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "Metric not available at level. " + measurable.getClass() + " Returnig accumulated version: " + metricTypeId);
        return getAccumulatedCurrencyMetric(metricTypeId, measurable, period);
    }

    private boolean isMetricAvailableAtThisLevel(Metric metric) {
        return metric != null;
    }

    public void calculateAndSave(List<ReportingUnit> reportingUnits, FinancialPeriod period) throws Exception {

        Set<Contract> contractsToCalc = new HashSet<Contract>();

        for (ReportingUnit reportingUnit : reportingUnits) {

            FinancialPeriod calculationPeriod = period;
            List<PerformanceObligation> pobs = reportingUnit.getPerformanceObligations();
            List<PerformanceObligation> validPobs = new ArrayList<PerformanceObligation>();
            for (PerformanceObligation pob : pobs) {
                if (!isInputRequired(pob, period)) {
                    validPobs.add(pob);
                    contractsToCalc.add(pob.getContract());
                }
            }

            executeBusinessRules((new ArrayList<Measurable>(validPobs)), calculationPeriod);
            while ((calculationPeriod = financialPeriodService.calculateNextPeriodUntilCurrent(calculationPeriod)) != null) {
                executeBusinessRules((new ArrayList<Measurable>(validPobs)), calculationPeriod);
            }
        }

        for (Contract contract : contractsToCalc) {
            FinancialPeriod calculationPeriod = period;
            executeBusinessRules(contract, calculationPeriod);
            while ((calculationPeriod = financialPeriodService.calculateNextPeriodUntilCurrent(calculationPeriod)) != null) {
                executeBusinessRules(contract, calculationPeriod);
            }
        }
        adminService.update(reportingUnits);
    }

    // More validity work needed.
    private boolean isInputRequired(PerformanceObligation pob, FinancialPeriod period) throws Exception {
        if (getCurrencyMetric("TRANSACTION_PRICE_CC", pob, period).getCcValue() == null) {
            return true;
        }

        return false;
    }

    public void executeBusinessRules(Measurable measurable, FinancialPeriod period) throws Exception {
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Firing all business rules: " + period.getId() + " " + measurable.getClass().getName());

        if (kSession == null) {
            initBusinessRulesEngine();
        }

        List<Object> facts = new ArrayList<Object>();
        facts.add(measurable);
        Collection<Metric> periodMetrics = getAllCurrentPeriodMetrics(measurable, period);
        convertCurrencies(periodMetrics, measurable, period);
        facts.addAll(periodMetrics);
        facts.addAll(getAllPriorPeriodMetrics(measurable, period));
        kSession.execute(facts);
        convertCurrencies(periodMetrics, measurable, period);

        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "Firing all business rules complete.");
    }

    public void executeBusinessRules(List<Measurable> measurables, FinancialPeriod period) throws Exception {
        for (Measurable measurable : measurables) {
            executeBusinessRules(measurable, period);
        }
    }

    private Collection<Metric> getAllCurrentPeriodMetrics(Measurable measurable, FinancialPeriod period) throws Exception {
        //FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();

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

    private Collection<CurrencyMetricPriorPeriod> getAllPriorPeriodMetrics(Measurable measurable, FinancialPeriod currentPeriod) throws Exception {
        FinancialPeriod priorPeriod = financialPeriodService.calculatePriorPeriod(currentPeriod);
        Collection<Metric> metrics = getAllMetrics(measurable, priorPeriod);
        List<CurrencyMetricPriorPeriod> priorPeriodMetrics = new ArrayList<CurrencyMetricPriorPeriod>();
        for (Metric metric : metrics) {
            if (metric instanceof CurrencyMetric) {
                priorPeriodMetrics.add(new CurrencyMetricPriorPeriod((CurrencyMetric) metric));
            }
        }

        return priorPeriodMetrics;
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

    private CurrencyMetric getAccumulatedCurrencyMetric(String metricTypeId, Measurable measurable, FinancialPeriod period) throws Exception {
        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "getAccumulatedCurrencyMetric() " + metricTypeId + " measurable: " + measurable.getClass().getName());
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
        convertCurrency(metric, measurable, period);

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
