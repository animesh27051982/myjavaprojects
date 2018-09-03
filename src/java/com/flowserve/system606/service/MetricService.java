/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.CurrencyType;
import com.flowserve.system606.model.MetricDirection;
import com.flowserve.system606.model.MetricType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

@Stateless
public class MetricService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    private static Map<String, MetricType> metricCodeCache = new HashMap<String, MetricType>();

    @EJB
    private AdminService adminService;

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public List<MetricType> getAllPobExcelInputMetricTypes() {
        return findActiveMetricTypesByOwnerEntityTypeDirection("POB", MetricDirection.INPUT);
    }

    public List<MetricType> findActiveMetricTypes() {
        Query query = em.createQuery("SELECT it FROM MetricType it WHERE it.active = TRUE");
        return (List<MetricType>) query.getResultList();
    }

    public List<MetricType> findActiveCurrencyMetricTypesWithAccount() {
        Query query = em.createQuery("SELECT it FROM MetricType it WHERE it.active = TRUE AND it.inputClass = 'CurrencyType' AND it.account IS NOT NULL");
        return (List<MetricType>) query.getResultList();
    }

    private List<MetricType> findActiveMetricTypesByOwnerEntityType(String ownerEntityType) {
        Query query = em.createQuery("SELECT it FROM MetricType it WHERE (it.ownerEntityType = :OET OR it.ownerEntityType = 'ALL') AND it.active = TRUE");
        query.setParameter("OET", ownerEntityType);
        return (List<MetricType>) query.getResultList();
    }

    private List<MetricType> findActiveMetricTypesByOwnerEntityTypeDirection(String ownerEntityType, MetricDirection direction) {
        Query query = em.createQuery("SELECT it FROM MetricType it WHERE it.ownerEntityType = :OET AND it.direction = :DIR AND it.active = TRUE AND it.excelCol != NULL");
        query.setParameter("OET", ownerEntityType);
        query.setParameter("DIR", direction);
        return (List<MetricType>) query.getResultList();
    }

    private List<MetricType> findAllMetricTypes() {
        Query query = em.createQuery("SELECT metricType FROM MetricType metricType");
        return (List<MetricType>) query.getResultList();
    }

    public MetricType findMetricTypeById(Long id) {
        return em.find(MetricType.class, id);
    }

    public MetricType findMetricTypeByName(String metricName) {
        Query query = em.createQuery("SELECT it FROM MetricType it WHERE it.name = :IN");
        query.setParameter("IN", metricName);
        return (MetricType) query.getSingleResult();  // we want an exception if not one and only one.
    }

    public MetricType getMetricTypeByCode(String metricCode) {
        if (metricCodeCache.get(metricCode) != null) {
            return metricCodeCache.get(metricCode);
        }

        return findMetricTypeByCode(metricCode);
    }

    private MetricType findMetricTypeByCode(String metricCode) {
        Query query = em.createQuery("SELECT it FROM MetricType it WHERE it.code = :IN");
        query.setParameter("IN", metricCode);
        MetricType metricType = (MetricType) query.getSingleResult();
        metricCodeCache.put(metricCode, metricType);

        return metricType;
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
            String[] values = line.split("\\|", 16);

            MetricType metricType = new MetricType();
            metricType.setDirection(MetricDirection.valueOf(values[count++]));
            metricType.setCode(values[count++]);
            try {
                if (getMetricTypeByCode(metricType.getCode()) != null) {
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
            metricType.setConvertible("Convertible".equals(values[count++]));
            metricType.setName(values[count++]);
            metricType.setDescription(values[count++]);
            metricType.setExcelSheet(values[count++]);
            metricType.setExcelCol(values[count++]);
            metricType.setGroupName(values[count++]);
            metricType.setGroupPosition(Integer.parseInt(values[count++]));
            metricType.setEffectiveFrom(LocalDate.now());
            metricType.setActive(true);
            //metricType.setAccount(adminService.findAccountById(values[count++]));
            logger.info("Creating MetricType: " + metricType.getName());
            adminService.persist(metricType);
        }

        reader.close();
        logger.info("Finished initializing MetricTypes.");
    }

    public List<MetricType> findMetricType() throws Exception {  // Need an application exception type defined.

        TypedQuery<MetricType> query = em.createQuery("SELECT b FROM MetricType b", MetricType.class);
        return (List<MetricType>) query.getResultList();
    }

    public MetricType update(MetricType m) throws Exception {
        metricCodeCache.clear();

        return em.merge(m);
    }
}
