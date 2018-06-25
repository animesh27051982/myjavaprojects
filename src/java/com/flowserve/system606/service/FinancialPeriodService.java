package com.flowserve.system606.service;

import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.PeriodStatus;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(FinancialPeriodService.class.getName());

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

    public void initFinancialPeriods() throws Exception {
        if (findById("MAY-18") == null) {
            logger.info("Initializing FinancialPeriods");
            FinancialPeriod period = new FinancialPeriod("MAY-18", "MAY-18", LocalDate.of(2018, Month.MAY, 1), LocalDate.of(2018, Month.MAY, 31), 2018, 5, PeriodStatus.OPENED);
            persist(period);
            logger.info("Finished initializing FinancialPeriods.");
        }

    }

}
