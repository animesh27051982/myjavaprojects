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
import com.flowserve.system606.model.DateMetricPriorPeriod;
import com.flowserve.system606.model.DecimalMetric;
import com.flowserve.system606.model.Event;
import com.flowserve.system606.model.EventList;
import com.flowserve.system606.model.EventStore;
import com.flowserve.system606.model.EventType;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.Metric;
import com.flowserve.system606.model.MetricStore;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.StringMetric;
import com.flowserve.system606.model.StringMetricPriorPeriod;
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
    @Inject
    private EventService eventService;
    @Inject
    private DroolsInitService droolsInitService;
    private StatelessKieSession kSession = null;
    private static final String PACKAGE_PREFIX = "com.flowserve.system606.model.";
    private List<MetricType> metricTypes = null;

    @PostConstruct
    public void init() {
        metricTypes = metricService.findActiveMetricTypes();
    }

    public void initBusinessRulesSession() {
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "getKieSession");
        kSession = droolsInitService.getKieSession();
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Finished getKieSession");
    }

    private void convertCurrencies(Collection<Metric> metrics, Measurable measurable, FinancialPeriod period) throws Exception {
        for (Metric metric : metrics) {
            currencyService.convertCurrency(metric, measurable, period);
        }
    }

    // intelliGet since this is no ordinary get.  Initialize any missing metrics on the fly.
    private Metric intelliGetMetric(MetricType metricType, Measurable measurable, FinancialPeriod period) {
        if (!measurable.metricSetExistsForPeriod(period)) {
            measurable.initializeMetricSetForPeriod(period);
        }
        if (!measurable.metricExistsForPeriod(period, metricType)) {
            measurable.initializeMetricForPeriod(period, metricType);
        }

        return measurable.getPeriodMetric(period, metricType);
    }

    private Metric getMetric(String metricCode, Measurable measurable, FinancialPeriod period) {
        return intelliGetMetric(metricService.getMetricTypeByCode(metricCode), measurable, period);
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
        metric.setMetricType(metricService.getMetricTypeByCode(metricCode));
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

        return getAccumulatedCurrencyMetric(metricCode, measurable, period);
    }

    private EventList intelliGetEventList(EventStore eventStore, FinancialPeriod period) {
        if (!eventStore.eventListExistsForPeriod(period)) {
            eventStore.initializeEventListForPeriod(period);
        }

        return eventStore.getPeriodEventList(period);
    }

    private EventList getEventList(EventStore eventStore, FinancialPeriod period) {
        return intelliGetEventList(eventStore, period);
    }

    private List<Event> getPeriodEvents(Measurable measurable, FinancialPeriod period) {
        List<Event> events = new ArrayList<Event>();

        if (measurable instanceof EventStore) {
            events.addAll(getEventList((EventStore) measurable, period).getEventList());
        }

        return events;
    }

    public void addEvent(EventStore eventStore, FinancialPeriod period, Event event) throws Exception {
        if (eventStore instanceof EventStore) {
            EventList eventList = getEventList(eventStore, period);
            event.setEventList(eventList);
            eventList.addEvent(event);
        }
    }

    public void removeEvent(EventStore eventStore, FinancialPeriod period, Event event) throws Exception {
        if (eventStore instanceof EventStore) {
            EventList eventList = getEventList(eventStore, period);
            eventList.removeEvent(event);
        }
    }

    private boolean isMetricAvailableAtThisLevel(Metric metric) {
        return metric != null;
    }

    public void calculateAndSave(ReportingUnit reportingUnit, FinancialPeriod period) throws Exception {

//        if (reportingUnit.isParent()) {
//            Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "RU is parent, skipping calcs: " + reportingUnit.getCode());
//            return;
//        }
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
        if (kSession == null) {
            initBusinessRulesSession();
        }

        List<Object> facts = new ArrayList<Object>();
        facts.add(measurable);
        Collection<Metric> periodMetrics = getAllCurrentPeriodMetrics(measurable, period);
        convertCurrencies(periodMetrics, measurable, period);
        facts.addAll(periodMetrics);
        facts.add(period);
        facts.add(currencyService);
        facts.addAll(getAllPriorPeriodMetrics(measurable, period));
        facts.addAll(getPeriodEvents(measurable, period));
        kSession.execute(facts);
        convertCurrencies(periodMetrics, measurable, period);
    }

    public void executeBusinessRulesAndSave(List<Measurable> measurables, FinancialPeriod period) throws Exception {
        for (Measurable measurable : measurables) {
            if (isValidForCalculations(measurable, period)) {
                executeBusinessRules(measurable, period);
                update(measurable);
            }
        }
    }

    public void executeBusinessRulesAndSave(Measurable measurable, FinancialPeriod period) throws Exception {
        if (isValidForCalculations(measurable, period)) {
            executeBusinessRules(measurable, period);
            update(measurable);
        }
    }

    public Measurable update(Measurable measurable) throws Exception {
        return em.merge(measurable);
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

    public List<Event> getAllEventsByPeriodAndEventType(EventStore eventStore, EventType eventType, FinancialPeriod period) {
        return eventStore.getAllEventsByPeriodAndEventType(period, eventType);
    }

    public List<Event> getAllEventsByEventType(EventStore eventStore, EventType eventType) {
        return eventStore.getAllEventsByEventType(eventType);
    }

    private Collection<Object> getAllPriorPeriodMetrics(Measurable measurable, FinancialPeriod currentPeriod) throws Exception {
        FinancialPeriod priorPeriod = currentPeriod.getPriorPeriod();

        Collection<Metric> metrics = getAllMetrics(measurable, priorPeriod);
        List<Object> priorPeriodMetrics = new ArrayList<Object>();
        for (Metric metric : metrics) {
            if (metric instanceof CurrencyMetric) {
                priorPeriodMetrics.add(new CurrencyMetricPriorPeriod((CurrencyMetric) metric));
            }
            if (metric instanceof DateMetric) {
                priorPeriodMetrics.add(new DateMetricPriorPeriod((DateMetric) metric));
            }
            if (metric instanceof StringMetric) {
                priorPeriodMetrics.add(new StringMetricPriorPeriod((StringMetric) metric));
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

    /**
     * For metrics eligible for automatic system currency conversion, we can just convert the base 'value' and convert to the other currencies. If not then we
     * have to accumulate each currency type separately. Splitting this into two methods as the non-convertible is 4x slower.
     */
    private CurrencyMetric getAccumulatedCurrencyMetric(String metricCode, Measurable measurable, FinancialPeriod period) throws Exception {
        MetricType metricType = metricService.getMetricTypeByCode(metricCode);

        if (measurable.getContractCurrency() == null) {
            //Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Calculating non-CC accumulation.");
            CurrencyMetric metric = getAccumulatedNonContractCurrencyMetric(metricType, measurable, period);
            Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "non-CC metric value: " + metric.getLcValue().toPlainString());
            return metric;
        }

        if (metricType.isConvertible()) {
            return getAccumulatedConvertibleCurrencyMetric(metricType, measurable, period);
        } else {
            return getAccumulatedNonConvertibleCurrencyMetric(metricType, measurable, period);
        }
    }

    /**
     * Contract currency is not valid for RU level accumulation, etc. Calculate local and reporting currency only. CC set to zero.
     */
    private CurrencyMetric getAccumulatedNonContractCurrencyMetric(MetricType metricType, Measurable measurable, FinancialPeriod period) throws Exception {

        BigDecimal lcValueSum = BigDecimal.ZERO;
        BigDecimal rcValueSum = BigDecimal.ZERO;

        CurrencyMetric metric = new CurrencyMetric();
        metric.setFinancialPeriod(period);
        metric.setMetricType(metricType);
        metric.setValue(BigDecimal.ZERO);
        metric.setCcValue(BigDecimal.ZERO);
        metric.setLcValue(lcValueSum);
        metric.setRcValue(rcValueSum);

        if (isEmptyTransientMesaurable(measurable)) {
            return metric;
        }

        if (isRootMeasurable(measurable)) {
            CurrencyMetric rootCurrencyMetric = getCurrencyMetric(metricType.getCode(), measurable, period);
            if (rootCurrencyMetric != null) {
                BigDecimal lcValue = rootCurrencyMetric.getLcValue();
                if (lcValue != null) {
                    lcValueSum = lcValueSum.add(lcValue);
                }
                BigDecimal rcValue = rootCurrencyMetric.getRcValue();
                if (rcValue != null) {
                    rcValueSum = rcValueSum.add(rcValue);
                }
            }
            metric.setLcValue(lcValueSum);
            metric.setRcValue(rcValueSum);

            return metric;
        }

        for (Measurable childMeasurable : measurable.getChildMeasurables()) {
            lcValueSum = lcValueSum.add(getAccumulatedNonContractCurrencyMetric(metricType, childMeasurable, period).getLcValue());
            rcValueSum = rcValueSum.add(getAccumulatedNonContractCurrencyMetric(metricType, childMeasurable, period).getRcValue());
        }

        metric.setLcValue(lcValueSum);
        metric.setRcValue(rcValueSum);

        return metric;
    }

    private CurrencyMetric getAccumulatedNonConvertibleCurrencyMetric(MetricType metricType, Measurable measurable, FinancialPeriod period) throws Exception {
        BigDecimal valueSum = BigDecimal.ZERO;
        BigDecimal ccValueSum = BigDecimal.ZERO;
        BigDecimal lcValueSum = BigDecimal.ZERO;
        BigDecimal rcValueSum = BigDecimal.ZERO;
        CurrencyMetric metric = new CurrencyMetric();
        metric.setFinancialPeriod(period);
        metric.setMetricType(metricType);
        metric.setValue(valueSum);
        metric.setCcValue(ccValueSum);
        metric.setLcValue(lcValueSum);
        metric.setRcValue(rcValueSum);
        if (isEmptyTransientMesaurable(measurable)) {
            return metric;
        }
        if (isRootMeasurable(measurable)) {
            CurrencyMetric rootCurrencyMetric = getCurrencyMetric(metricType.getCode(), measurable, period);
            if (rootCurrencyMetric != null) {
                BigDecimal value = rootCurrencyMetric.getValue();
                if (value != null) {
                    valueSum = valueSum.add(value);
                }
                BigDecimal ccValue = rootCurrencyMetric.getCcValue();
                if (ccValue != null) {
                    ccValueSum = ccValueSum.add(ccValue);
                }
                BigDecimal lcValue = rootCurrencyMetric.getLcValue();
                if (lcValue != null) {
                    lcValueSum = lcValueSum.add(lcValue);
                }
                BigDecimal rcValue = rootCurrencyMetric.getRcValue();
                if (rcValue != null) {
                    rcValueSum = rcValueSum.add(rcValue);
                }
            }
            metric.setValue(valueSum);
            metric.setCcValue(ccValueSum);
            metric.setLcValue(lcValueSum);
            metric.setRcValue(rcValueSum);

            return metric;
        }

        for (Measurable childMeasurable : measurable.getChildMeasurables()) {
            valueSum = valueSum.add(getAccumulatedNonConvertibleCurrencyMetric(metricType, childMeasurable, period).getValue());
            ccValueSum = ccValueSum.add(getAccumulatedNonConvertibleCurrencyMetric(metricType, childMeasurable, period).getCcValue());
            lcValueSum = lcValueSum.add(getAccumulatedNonConvertibleCurrencyMetric(metricType, childMeasurable, period).getLcValue());
            rcValueSum = rcValueSum.add(getAccumulatedNonConvertibleCurrencyMetric(metricType, childMeasurable, period).getRcValue());
        }

        metric.setValue(valueSum);
        metric.setCcValue(ccValueSum);
        metric.setLcValue(lcValueSum);
        metric.setRcValue(rcValueSum);
        //currencyService.convertCurrency(metric, measurable, period);

        return metric;
    }

    private CurrencyMetric getAccumulatedConvertibleCurrencyMetric(MetricType metricType, Measurable measurable, FinancialPeriod period) throws Exception {
        BigDecimal valueSum = BigDecimal.ZERO;
        CurrencyMetric metric = new CurrencyMetric();
        metric.setFinancialPeriod(period);
        metric.setMetricType(metricType);
        metric.setValue(valueSum);
        if (isEmptyTransientMesaurable(measurable)) {
            return metric;
        }
        if (isRootMeasurable(measurable)) {
            CurrencyMetric rootCurrencyMetric = getCurrencyMetric(metricType.getCode(), measurable, period);
            if (rootCurrencyMetric != null) {
                BigDecimal value = rootCurrencyMetric.getValue();
                if (value != null) {
                    valueSum = valueSum.add(value);
                }
            }
            metric.setValue(valueSum);

            return metric;
        }

        for (Measurable childMeasurable : measurable.getChildMeasurables()) {
            valueSum = valueSum.add(getAccumulatedConvertibleCurrencyMetric(metricType, childMeasurable, period).getValue());
        }

        metric.setValue(valueSum);
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

        if (adminService.findAllBusinessRules().isEmpty()) {
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
