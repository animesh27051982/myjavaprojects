/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.BusinessRule;
import com.flowserve.system606.model.PerformanceObligation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
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
public class BusinessRuleService {

    private static final Logger LOG = Logger.getLogger(BusinessRuleService.class.getName());

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    StatelessKieSession kSession = null;

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

    public void initBusinessRules() throws Exception {
        LOG.info("initBusinessRules");
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

        LOG.info("Finished initBusinessRules");
    }

    public void initBusinessRulesEngine() throws Exception {
        LOG.info("initBusinessRulesEngine");
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

        LOG.info("Finished initBusinessRulesEngine");
    }

    public void executePOBCalculations(PerformanceObligation pob) throws Exception {
        LOG.info("Firing all business rules");
        kSession.execute(pob);
        //kSession.fireAllRules();
        LOG.info("Business rule fire complete.");
    }

//    @PreDestroy
//    public void destruct() {
//        LOG.info("BusinessRuleService.destruct");
//        kSession.dispose();
//    }
}
