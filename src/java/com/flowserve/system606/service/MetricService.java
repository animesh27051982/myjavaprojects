/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.CurrencyType;
import com.flowserve.system606.model.Metric;
import com.flowserve.system606.model.MetricDirection;
import com.flowserve.system606.model.MetricSet;
import com.flowserve.system606.model.MetricType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

/**
 *
 * @author span
 */
@Stateless
public class MetricService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    @EJB
    private PerformanceObligationService pobService;
    @EJB
    private AdminService adminService;

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public List<MetricType> findActiveMetricTypesPob() {
        return findActiveMetricTypesByOwnerEntityType("POB");
    }

    public List<MetricType> findActiveMetricTypesContract() {
        return findActiveMetricTypesByOwnerEntityType("Contract");
    }

    private List<MetricType> findActiveMetricTypesByOwnerEntityType(String ownerEntityType) {
        Query query = em.createQuery("SELECT it FROM MetricType it WHERE it.ownerEntityType = :OET AND it.active = TRUE");
        query.setParameter("OET", ownerEntityType);
        return (List<MetricType>) query.getResultList();
    }

    private List<MetricType> findAllMetricTypes() {
        Query query = em.createQuery("SELECT metricType FROM MetricType metricType");
        return (List<MetricType>) query.getResultList();
    }

    public void persist(Metric metric) throws Exception {
        em.persist(metric);
    }

    @Transactional
    public void persist(MetricSet metricSet) throws Exception {
        em.persist(metricSet);
    }

    public MetricType findMetricTypeById(String id) {
        return em.find(MetricType.class, id);
    }

    public MetricType findMetricTypeByName(String metricName) {
        Query query = em.createQuery("SELECT it FROM MetricType it WHERE it.name = :IN");
        query.setParameter("IN", metricName);
        return (MetricType) query.getSingleResult();  // we want an exception if not one and only one.
    }

    private Contract createContract(String reportingUnit, long contractId, String customerName, String salesOrderNum, BigDecimal totalTransactionPrice) {
        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setName(customerName + "-" + contractId);
        // contract.setReportingUnit(reportingUnit);
        contract.setSalesOrderNumber(salesOrderNum);
        contract.setTotalTransactionPrice(totalTransactionPrice);
        return contract;
    }

    public void persist(Contract contract) throws Exception {
        em.persist(contract);
    }

    public Contract update(Contract contract) throws Exception {
        // contract.setLastUpdateDate(LocalDateTime.now());
        return em.merge(contract);
    }

    public void initMetricTypes() throws Exception {

        logger.info("Initializing MetricTypes");
        BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/init_metric_types.txt"), "UTF-8"));
        String metricCurrencyType = null;
        int count = 0;
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }

            count = 0;
            String[] values = line.split("\\|");

            MetricType metricType = new MetricType();
            metricType.setDirection(MetricDirection.valueOf(values[count++]));
            metricType.setId(values[count++]);
            try {
                if (findMetricTypeById(metricType.getId()) != null) {
                    continue;
                }
            } catch (Exception e) {
                Logger.getLogger(MetricService.class.getName()).log(Level.FINE, "Adding MetricType: " + line);
            }

            metricType.setOwnerEntityType(values[count++]);
            metricType.setRequired("REQUIRED".equals(values[count++]));
            metricType.setMetricClass(values[count++]);
            metricCurrencyType = values[count++];
            metricType.setMetricCurrencyType(metricCurrencyType == null || "".equals(metricCurrencyType) ? null : CurrencyType.fromShortName(metricCurrencyType));
            metricType.setName(values[count++]);
            metricType.setDescription(values[count++]);
            metricType.setExcelSheet(values[count++]);
            metricType.setExcelCol(values[count++]);
            metricType.setGroupName(values[count++]);
            metricType.setGroupPosition(Integer.parseInt(values[count++]));
            metricType.setEffectiveFrom(LocalDate.now());
            //metricType.setEffectiveTo(LocalDate.now());
            metricType.setActive(true);

            logger.info("Creating MetricType: " + metricType.getName());

            adminService.persist(metricType);

        }

        reader.close();

        logger.info("Finished initializing MetricTypes.");

    }

//    public void initMetricTypeMap() {
//        List<MetricType> metricTypes = findAllMetricTypes();
//        metricTypes.forEach(metricType -> MetricTypeKey.putMetricType(metricType.getName(), metricType));
//    }
    public List<MetricType> findMetricType() throws Exception {  // Need an application exception type defined.

        TypedQuery<MetricType> query = em.createQuery("SELECT b FROM MetricType b", MetricType.class);
        return (List<MetricType>) query.getResultList();
    }
}
