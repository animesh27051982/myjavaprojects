package com.flowserve.system606.service;

import com.flowserve.system606.model.FinancialPeriod;
import java.time.format.DateTimeFormatter;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author kgraves
 */
@Stateless
public class FinancialPeriodService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    private DateTimeFormatter periodNameFormatter = DateTimeFormatter.ofPattern("MMM-yy");

    @PostConstruct
    public void init() {
    }

//    public String getPeriodIdByDate(LocalDate date) {
//        return periodNameFormatter.format(date);
//    }
    public FinancialPeriod findById(String id) {
        return em.find(FinancialPeriod.class, id);
    }

    public void persist(FinancialPeriod fp) throws Exception {
        em.persist(fp);
    }

}
