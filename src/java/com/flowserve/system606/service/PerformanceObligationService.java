package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(PerformanceObligationService.class.getName());

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;
    @EJB
    private AdminService adminService;
    @EJB
    private OutputService outputService;
    @EJB
    private InputService inputService;
    @EJB
    private ContractService contractService;
    @EJB
    private PerformanceObligationService performanceObligationService;

    public void initializeInputs(PerformanceObligation pob) throws Exception {
        pob.initializeInputs(inputService.findActiveInputTypes());
    }

    public void initializeOutputs(PerformanceObligation pob) throws Exception {
        pob.initializeOutputs(outputService.findActiveOutputTypes());
    }

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

    public void initPOBs() throws Exception {

        //read init_contract_pob_data.txt a second time
        if (findById(10660L) == null) {
            logger.info("Initializing POBs");
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/init_contract_pob_data.txt"), "UTF-8"));

            String platform = null;
            String ru = null;
            long contractId = -1;
            String customerName = null;
            String salesOrderNumber = null;
            String pobName = null;
            long pobId = -1;
            String revRecMethod = null;
            
            int count = 0;
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                count = 0;
                String[] values = line.split("\\t");

                PerformanceObligation pob = new PerformanceObligation();

                platform = values[count++].trim();
                ru = values[count++].trim().replace("RU", "");
                contractId = Long.valueOf(values[count++].trim());
                customerName = values[count++].trim();         
                salesOrderNumber = values[count++].trim();
                pobName = values[count++].trim();
                pobId = Long.valueOf(values[count++].trim());
                revRecMethod = values[count++].trim();
                
                Contract contract = contractService.findContractById(contractId);
                if (contract == null) {
                    contract = new Contract();
                    contract.setId(contractId);
                    contract.setName(customerName + '-' + contractId);
                    contract.setSalesOrderNumber(salesOrderNumber);                    
                }
                pob.setContract(contract);
                pob.setName(pobName);
                pob.setId(pobId);
                pob.setRevRecMethod(revRecMethod);

                pob.setActive(true);
                //performanceObligationService.initializeInputs(pob);
                //performanceObligationService.initializeOutputs(pob);

                persist(pob);
            }

            reader.close();

            logger.info("Finished initializing POBs.");
        }
    }
}
