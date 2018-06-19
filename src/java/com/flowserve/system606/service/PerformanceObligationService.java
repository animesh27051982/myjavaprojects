package com.flowserve.system606.service;

import com.flowserve.system606.model.PerformanceObligation;
import java.time.LocalDateTime;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
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

    @Resource
    private SessionContext sessionContext;
    @EJB
    private AdminService adminService;

    public PerformanceObligation findById(Long id) {
        return em.find(PerformanceObligation.class, id);
    }

    public void persist(PerformanceObligation pob) throws Exception {
        //pob.setCreatedBy(createdBy);
        pob.setCreationDate(LocalDateTime.now());
        //pob.setLastUpdatedBy(createdBy);
        pob.setLastUpdateDate(LocalDateTime.now());
        em.persist(pob);
    }

    public PerformanceObligation update(PerformanceObligation pob) throws Exception {
        //User user = adminService.findUserByFlsId(sessionContext.getCallerPrincipal().getName().toLowerCase());
        //pob.setLastUpdatedBy(updatedBy);
        pob.setLastUpdateDate(LocalDateTime.now());
        return em.merge(pob);
    }

    public void persist1(Object object) {
        em.persist(object);
    }
}
