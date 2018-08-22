package com.flowserve.system606.service;

import com.flowserve.system606.model.BusinessRule;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.StatelessKieSession;

@Singleton
public class DroolsInitService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    private KieServices ks = null;
    @Inject
    private AdminService adminService;

    @PostConstruct
    public void initDroolsEngine() {
        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "initBusinessRulesEngine");

        try {
            ks = KieServices.Factory.get();
            KieFileSystem kfs = ks.newKieFileSystem();

            for (BusinessRule rule : adminService.findAllBusinessRules()) {
                String drl = rule.getContent();
                kfs.write("src/main/resources/" + rule.getRuleKey() + ".drl", drl);
            }

            KieBuilder kb = ks.newKieBuilder(kfs).buildAll();

            if (kb.getResults().hasMessages(Message.Level.ERROR)) {
                throw new RuntimeException("Business Rule Build Errors:\n" + kb.getResults().toString());
            }

        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }

        Logger.getLogger(CalculationService.class.getName()).log(Level.INFO, "Finished initBusinessRulesEngine");
    }

    public StatelessKieSession getKieSession() {
        StatelessKieSession kSession = ks.newKieContainer(ks.getRepository().getDefaultReleaseId()).newStatelessKieSession();
        kSession.setGlobal("logger", Logger.getLogger(CalculationService.class.getName()));

        return kSession;
    }

}
