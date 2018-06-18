package com.flowserve.system606.service;

import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.User;
import java.time.LocalDateTime;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author kgraves
 */
@Stateless
public class PerformanceObligationService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    public PerformanceObligation findById(Long id) {
        return em.find(PerformanceObligation.class, id);
    }

    public void persist(PerformanceObligation pob, User createdBy) throws Exception {
        pob.setCreatedBy(createdBy);
        pob.setCreationDate(LocalDateTime.now());
        pob.setLastUpdatedBy(createdBy);
        pob.setLastUpdateDate(LocalDateTime.now());
        em.persist(pob);
    }

    public PerformanceObligation update(PerformanceObligation pob, User updatedBy) throws Exception {
        pob.setLastUpdatedBy(updatedBy);
        pob.setLastUpdateDate(LocalDateTime.now());
        return em.merge(pob);
    }
}
