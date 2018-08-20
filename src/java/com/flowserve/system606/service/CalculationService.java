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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
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
    private PerformanceObligationService performanceObligationService;
    @Inject
    private ContractService contractService;
    @Inject
    private CurrencyService currencyService;
    @Inject
    private AdminService adminService;
    @Inject
    private MetricService metricService;
    private StatelessKieSession kSession = null;
    private static final String PACKAGE_PREFIX = "com.flowserve.system606.model.";
    private List<MetricType> metricTypes = null;

    @PostConstruct
    public void init() {
        metricTypes = metricService.findActiveMetricTypes();
    }

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
            currencyService.convertCurrency(metric, measurable, period);
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

    private Metric getMetric(String metricCode, Measurable measurable, FinancialPeriod period) {
        //FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();
        return intelliGetMetric(metricService.findMetricTypeByCode(metricCode), measurable, period);
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

    public CurrencyMetric getAccumulatedCurrencyMetricAcrossPeriods(String metricCode, Measurable measurable, List<FinancialPeriod> periods) throws Exception {
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "getAccumulatedCurrencyMetricAcrossPeriods() " + metricCode + " measurable: " + measurable.getClass().getName());
        CurrencyMetric metric = new CurrencyMetric();
        metric.setMetricType(metricService.findMetricTypeByCode(metricCode));
        metric.setValue(new BigDecimal("0.0"));

        if (isEmptyTransientMesaurable(measurable)) {
            return metric;
        }

        for (FinancialPeriod period : periods) {
            Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "getAccumulatedCurrencyMetricAcrossPeriods: " + metricCode + " " + period.getId());
            if (isRootMeasurable(measurable)) {
                CurrencyMetric rootCurrencyMetric = getCurrencyMetric(metricCode, measurable, period);
                if (rootCurrencyMetric != null) {
                    BigDecimal rootValue = rootCurrencyMetric.getValue();
                    if (rootValue != null) {
                        metric.setValue(metric.getValue().add(rootValue));
                    }
                }
                continue;
            }

            for (Measurable childMeasurable : measurable.getChildMeasurables()) {
                BigDecimal childMetricValue = getAccumulatedCurrencyMetric(metricCode, childMeasurable, period).getValue();
                Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Adding: " + childMetricValue.toPlainString());
                metric.setValue(metric.getValue().add(childMetricValue));
            }

            currencyService.convertCurrency(metric, measurable, period);
        }

        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Final sum: " + metric.getValue().toPlainString());

        return metric;
    }

    public CurrencyMetric getCurrencyMetric(String metricCode, Measurable measurable, FinancialPeriod period) throws Exception {
        if (measurable instanceof MetricStore) {
            CurrencyMetric currencyMetric = (CurrencyMetric) getMetric(metricCode, measurable, period);
            if (isMetricAvailableAtThisLevel(currencyMetric) || measurable instanceof PerformanceObligation) {
                return currencyMetric;
            }
        }
        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "Metric not directly available at level. " + measurable.getClass() + " Returnig accumulated version: " + metricCode);

        return getAccumulatedCurrencyMetric(metricCode, measurable, period);
    }

    private boolean isMetricAvailableAtThisLevel(Metric metric) {
        return metric != null;
    }

    public void calculateAndSave(ReportingUnit reportingUnit, FinancialPeriod period) throws Exception {

        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Period passed to CalculationService.calculateAndSave: " + period.getId());

        if (reportingUnit.isParent()) {
            Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "RU is parent, skipping calcs: " + reportingUnit.getCode());
            return;
        }
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Calculating RU: " + reportingUnit.getCode() + "...");

        FinancialPeriod calculationPeriod = period;
        do {
            Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Recalcing POBs for period: " + calculationPeriod.getId() + " RU: " + reportingUnit.getCode());
            executeBusinessRulesAndSave((new ArrayList<Measurable>(reportingUnit.getPerformanceObligations())), calculationPeriod);
        } while ((calculationPeriod = financialPeriodService.calculateNextPeriodUntilCurrent(calculationPeriod)) != null);

        calculationPeriod = period;
        do {
            Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Recalcing Contracts for period: " + calculationPeriod.getId());
            executeBusinessRulesAndSave((new ArrayList<Measurable>(reportingUnit.getContracts())), calculationPeriod);
        } while ((calculationPeriod = financialPeriodService.calculateNextPeriodUntilCurrent(calculationPeriod)) != null);
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Completed calcs RU: " + reportingUnit.getCode());
    }

    // KJG TODO - More validity work needed.  Just checking TP not good enough.
    private boolean isValidForCalculations(Measurable measurable, FinancialPeriod period) throws Exception {
        if (measurable instanceof PerformanceObligation) {
            if (isValid((PerformanceObligation) measurable, period)) {
                return true;
            }
            return false;
        }
        if (measurable instanceof Contract) {
            for (PerformanceObligation pob : ((Contract) measurable).getPerformanceObligations()) {
                if (isValidForCalculations(pob, period) == true) {
                    return true;
                }
            }
            return false;
        }

        // All other types are valid since they will be rollups.
        return true;
    }

    private boolean isValid(PerformanceObligation pob, FinancialPeriod period) throws Exception {
        if (getCurrencyMetric("TRANSACTION_PRICE_CC", pob, period).getCcValue() != null) {
            return true;
        }
        return false;
    }

    public boolean isSubmittableForReview(ReportingUnit ru, FinancialPeriod period) throws Exception {
        if (ru.getPerformanceObligations().size() == 0) {
            return false;
        }
        for (PerformanceObligation pob : ru.getPerformanceObligations()) {
            if (isValid(pob, period) == false) {
                return false;
            }
        }

        return true;
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public void executeBusinessRules(Measurable measurable, FinancialPeriod period) throws Exception {
        //Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "Firing all business rules: " + period.getId() + " ts: " + measurable.toString());

        if (kSession == null) {
            initBusinessRulesEngine();
        }

        List<Object> facts = new ArrayList<Object>();
        facts.add(measurable);
        Collection<Metric> periodMetrics = getAllCurrentPeriodMetrics(measurable, period);
        convertCurrencies(periodMetrics, measurable, period);
        facts.addAll(periodMetrics);
        facts.add(period);
        facts.add(currencyService);
        facts.addAll(getAllPriorPeriodMetrics(measurable, period));
        kSession.execute(facts);
        convertCurrencies(periodMetrics, measurable, period);
    }

    public void executeBusinessRulesAndSave(List<Measurable> measurables, FinancialPeriod period) throws Exception {
        for (Measurable measurable : measurables) {
            if (isValidForCalculations(measurable, period)) {
                executeBusinessRules(measurable, period);
                adminService.update(measurable);
            }
        }
    }

    public void executeBusinessRulesAndSave(Measurable measurable, FinancialPeriod period) throws Exception {
        if (isValidForCalculations(measurable, period)) {
            executeBusinessRules(measurable, period);
            adminService.update(measurable);
        }
    }

    private Collection<Metric> getAllCurrentPeriodMetrics(Measurable measurable, FinancialPeriod period) throws Exception {
        return getAllMetrics(measurable, period);
    }

    private Collection<Metric> getAllMetrics(Measurable measurable, FinancialPeriod period) throws Exception {

        List<Metric> metrics = new ArrayList<Metric>();
        for (MetricType metricType : metricTypes) {

            if (metricType.isCurrency()) {
                Metric currencyMetric = getCurrencyMetric(metricType.getCode(), measurable, period);
                if (currencyMetric != null) {
                    metrics.add(currencyMetric);
                }
            } else {
                // Quick hack to test getting decimal metric at all levels.
                if (metricType.isDecimal()) {
                    Metric metric = intelliGetMetric(metricType, measurable, period);
                    if (metric != null) {
                        metrics.add(metric);
                    }
                }
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
        //FinancialPeriod priorPeriod = financialPeriodService.calculatePriorPeriod(currentPeriod);
        FinancialPeriod priorPeriod = currentPeriod.getPriorPeriod();

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

    private CurrencyMetric getAccumulatedCurrencyMetric(String metricCode, Measurable measurable, FinancialPeriod period) throws Exception {
        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "getAccumulatedCurrencyMetric() " + metricCode + " measurable: " + measurable.getClass().getName());
        BigDecimal sum = new BigDecimal("0.0");
        CurrencyMetric metric = new CurrencyMetric();
        metric.setFinancialPeriod(period);
        metric.setMetricType(metricService.findMetricTypeByCode(metricCode));
        metric.setValue(sum);
        if (isEmptyTransientMesaurable(measurable)) {
            return metric;
        }
        if (isRootMeasurable(measurable)) {
            CurrencyMetric rootCurrencyMetric = getCurrencyMetric(metricCode, measurable, period);
            if (rootCurrencyMetric != null) {
                BigDecimal value = rootCurrencyMetric.getValue();
                if (value != null) {
                    sum = sum.add(value);
                }
            }
            metric.setValue(sum);
            return metric;
        }

        for (Measurable childMeasurable : measurable.getChildMeasurables()) {
            sum = sum.add(getAccumulatedCurrencyMetric(metricCode, childMeasurable, period).getValue());
        }

        metric.setValue(sum);
        currencyService.convertCurrency(metric, measurable, period);

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
