/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Output;
import com.flowserve.system606.model.OutputType;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author kgraves
 */
@Stateless
public class OutputService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    @EJB
    private PerformanceObligationService pobService;
    @EJB
    private BusinessRuleService businessRuleService;

    public List<OutputType> findOutputTypes() {
        Query query = em.createQuery("SELECT ot FROM OutputType ot");
        return (List<OutputType>) query.getResultList();
    }

    public List<OutputType> findActiveOutputTypes() {
        Query query = em.createQuery("SELECT ot FROM OutputType ot WHERE ot.active = TRUE");
        return (List<OutputType>) query.getResultList();
    }

    public void persist(Output output) throws Exception {
        em.persist(output);
    }

    public OutputType findOutputTypeById(String id) {
        return em.find(OutputType.class, id);
    }
}
