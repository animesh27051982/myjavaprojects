/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Accumulable;
import com.flowserve.system606.model.BusinessRule;
import com.flowserve.system606.model.CurrencyMetric;
import com.flowserve.system606.model.DateMetric;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Metric;
import com.flowserve.system606.model.MetricPriorPeriod;
import com.flowserve.system606.model.MetricStore;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.StringMetric;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
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
    private MetricService metricService;
    private StatelessKieSession kSession = null;
    private static final String PACKAGE_PREFIX = "com.flowserve.system606.model.";

    @PostConstruct
    public void initBusinessRulesEngine() {
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "initBusinessRulesEngine");

        try {
            KieServices ks = KieServices.Factory.get();
            KieRepository kr = ks.getRepository();
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

    private void initializeCurrenciesCurrentPeriod(Collection<Metric> metrics, MetricStore metricStore) throws Exception {
        FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();

        for (Metric metric : metrics) {
            initializeCurrencies(metric, metricStore, period);
        }
    }

    private void initializeCurrencies(Metric metric, MetricStore metricStore, FinancialPeriod period) throws Exception {
        if (metric.getValue() == null || !(metric instanceof CurrencyMetric)) {
            return;
        }
        if (metric.getMetricType().getMetricCurrencyType() == null) {
            throw new IllegalStateException("There is no currency type defined for the metric type " + metric.getMetricType().getId() + ".  Please contact a system administrator.");
        }
        if (metricStore.getLocalCurrency() == null) {
            throw new IllegalStateException("There is no local currency defined for the reporting unit.  Please contact a system administrator.");
        }
        if (metricStore.getContractCurrency() == null) {
            throw new IllegalStateException("There is no contract currency defined for the contract.  Please contact a system administrator.");
        }

        CurrencyMetric currencyMetric = (CurrencyMetric) metric;
        if (currencyMetric.isLocalCurrencyMetric()) {
            currencyMetric.setLocalCurrencyValue(currencyMetric.getValue());
            if (currencyMetric.getValue().equals(BigDecimal.ZERO)) {
                currencyMetric.setContractCurrencyValue(BigDecimal.ZERO);
            } else {
                BigDecimal contractCurrencyValue = currencyService.convert(currencyMetric.getValue(), metricStore.getLocalCurrency(), metricStore.getContractCurrency(), period);
                currencyMetric.setContractCurrencyValue(contractCurrencyValue);
            }
        } else if (currencyMetric.isContractCurrencyMetric()) {
            currencyMetric.setContractCurrencyValue(currencyMetric.getValue());
            if (currencyMetric.getValue().equals(BigDecimal.ZERO)) {
                currencyMetric.setLocalCurrencyValue(BigDecimal.ZERO);
            } else {
                BigDecimal localCurrencyValue = currencyService.convert(currencyMetric.getValue(), metricStore.getContractCurrency(), metricStore.getLocalCurrency(), period);
                currencyMetric.setLocalCurrencyValue(localCurrencyValue);
            }
        }
    }

    // intelliGet since this is no ordinary get.  Initialize any missing metrics on the fly.
    private Metric intelliGetMetric(MetricType metricType, MetricStore metricStore, FinancialPeriod period) {  // TODO - Exception type to be thrown?
        if (!metricStore.metricSetExistsForPeriod(period)) {
            metricStore.initializeMetricSetForPeriod(period);
        }
        if (!metricStore.metricExistsForPeriod(period, metricType)) {
            metricStore.initializeMetricForPeriod(period, metricType);
        }

        return metricStore.getPeriodMetric(period, metricType);
    }

    private Metric getMetric(String metricTypeId, MetricStore metricStore) {
        FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();
        return intelliGetMetric(metricService.findMetricTypeById(metricTypeId), metricStore, period);
    }

    private Metric getMetricPriorPeriod(String metricTypeId, MetricStore metricStore) {
        FinancialPeriod period = financialPeriodService.getPriorFinancialPeriod();
        return intelliGetMetric(metricService.findMetricTypeById(metricTypeId), metricStore, period);
    }

    public String getStringMetricValue(String metricTypeId, MetricStore metricStore) {
        return (String) getMetric(metricTypeId, metricStore).getValue();
    }

    public BigDecimal getDecimalMetricValue(String metricTypeId, PerformanceObligation pob) {
        return (BigDecimal) getMetric(metricTypeId, pob).getValue();
    }

    public LocalDate getDateMetricValue(String metricTypeId, PerformanceObligation pob) {
        return (LocalDate) getMetric(metricTypeId, pob).getValue();
    }

    public BigDecimal getCurrencyMetricValue(String metricTypeId, PerformanceObligation pob) {
        return (BigDecimal) getMetric(metricTypeId, pob).getValue();
    }

    public BigDecimal getCurrencyMetricValuePriorPeriod(String metricTypeId, PerformanceObligation pob) {
        return (BigDecimal) getMetricPriorPeriod(metricTypeId, pob).getValue();
    }

    public StringMetric getStringMetric(String metricTypeId, PerformanceObligation pob) {
        return (StringMetric) getMetric(metricTypeId, pob);
    }

    public DateMetric getDateMetric(String metricTypeId, PerformanceObligation pob) {
        return (DateMetric) getMetric(metricTypeId, pob);
    }

    public CurrencyMetric getCurrencyMetric(String metricTypeId, PerformanceObligation pob) {  // TODO KJG - This method should be getCurrencyMetric() and the method above should be getCurrencyMetricValue() waiting on this due to impact.
        return (CurrencyMetric) getMetric(metricTypeId, pob);
    }

    public void putCurrencyMetricValue(String metricTypeId, PerformanceObligation pob, BigDecimal value) {
        getMetric(metricTypeId, pob).setValue(value);
    }

//    public boolean isMetricRequired() {
//        for (Metric metric : periodMetricSets.values()) {
//            if (metric.getMetricType().isRequired() && metric.getValue() == null) {
//                return true;
//            }
//        }
//
//        return false;
//    }
    public void executeBusinessRules(MetricStore metricStore) throws Exception {
        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "Firing all business rules for: " + metricStore.getId());

        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "Running currency conversions");

        List<Object> facts = new ArrayList<Object>();
        facts.add(metricStore);
        Collection<Metric> currentPeriodMetrics = getAllCurrentPeriodMetrics(metricStore);
        initializeCurrenciesCurrentPeriod(currentPeriodMetrics, metricStore);
        facts.addAll(currentPeriodMetrics);
        facts.addAll(getAllPriorPeriodMetrics(metricStore));
        kSession.execute(facts);
        initializeCurrenciesCurrentPeriod(currentPeriodMetrics, metricStore);

        Logger.getLogger(CalculationService.class.getName()).log(Level.FINER, "Firing all business rules complete.");
    }

    public void executeBusinessRules(List<PerformanceObligation> pobs) throws Exception {
        for (PerformanceObligation pob : pobs) {
            executeBusinessRules(pob);
        }
    }

    private Collection<Metric> getAllCurrentPeriodMetrics(MetricStore metricStore) throws Exception {
        FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();

        List<MetricType> metricTypes = metricService.findActiveMetricTypesPob();
        List<Metric> metrics = new ArrayList<Metric>();
        for (MetricType metricType : metricTypes) {  // TODO - We are getting all pob here, this may not be good long term.
            metrics.add(intelliGetMetric(metricType, metricStore, period));
        }

        return metrics;
    }

    private Collection<MetricPriorPeriod> getAllPriorPeriodMetrics(MetricStore metricStore) {
        // TODO - change this to retrieve prior from periodService.  Use current for now.
        FinancialPeriod priorPeriod = financialPeriodService.getPriorFinancialPeriod();

        List<MetricPriorPeriod> metricsPriorPeriod = new ArrayList<MetricPriorPeriod>();
        List<MetricType> metricTypes = metricService.findActiveMetricTypesPob();

        for (MetricType metricType : metricTypes) {
            Metric priorPeriodMetric = intelliGetMetric(metricType, metricStore, priorPeriod);
            if (priorPeriodMetricDoesNotExist(priorPeriodMetric)) {
                priorPeriodMetric.setValue(BigDecimal.ZERO);
            }
            metricsPriorPeriod.add(new MetricPriorPeriod(priorPeriodMetric));
        }

        return metricsPriorPeriod;
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
    public BigDecimal getAccumulatedCurrencyMetricValue(String metricTypeId, Accumulable accumulable) {

        BigDecimal sum = new BigDecimal("0.0");

        if (accumulable.getChildAccumulables().isEmpty()) {
            // TODO - We can abstract this further instead of hard cast to pob.
            BigDecimal value = getCurrencyMetric(metricTypeId, ((PerformanceObligation) accumulable)).getValue();
            if (value != null) {
                sum = sum.add(value);
            }
            return sum;
        }

        for (Accumulable childAccumulable : accumulable.getChildAccumulables()) {
            sum = sum.add(getAccumulatedCurrencyMetricValue(metricTypeId, childAccumulable));
        }

        return sum;
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
