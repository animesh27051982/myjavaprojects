/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.CurrencyType;
import com.flowserve.system606.model.Input;
import com.flowserve.system606.model.InputSet;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.InputTypeName;
import com.flowserve.system606.model.OutputType;
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
public class InputService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    @EJB
    private PerformanceObligationService pobService;
    @EJB
    private AdminService adminService;
    @EJB
    private BusinessRuleService businessRuleService;

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public List<InputType> findActiveInputTypesPob() {
        return findActiveInputTypesByOwnerEntityType("POB");
    }

    public List<InputType> findActiveInputTypesContract() {
        return findActiveInputTypesByOwnerEntityType("Contract");
    }

    private List<InputType> findActiveInputTypesByOwnerEntityType(String ownerEntityType) {
        Query query = em.createQuery("SELECT it FROM InputType it WHERE it.ownerEntityType = :OET AND it.active = TRUE");
        query.setParameter("OET", ownerEntityType);
        return (List<InputType>) query.getResultList();
    }

    private List<InputType> findAllInputTypes() {
        Query query = em.createQuery("SELECT inputType FROM InputType inputType");
        return (List<InputType>) query.getResultList();
    }

    public void persist(Input input) throws Exception {
        em.persist(input);
    }

    @Transactional
    public void persist(InputSet inputSet) throws Exception {
        em.persist(inputSet);
    }

    public InputType findInputTypeById(String id) {
        return em.find(InputType.class, id);
    }

    public InputType findInputTypeByName(InputTypeName inputName) {
        Query query = em.createQuery("SELECT it FROM InputType it WHERE it.name = :IN");
        query.setParameter("IN", inputName);
        return (InputType) query.getSingleResult();  // we want an exception if not one and only one.
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

    public void initInputTypes() throws Exception {

        logger.info("Initializing InputTypes");
        BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/init_input_types.txt"), "UTF-8"));
        String inputCurrencyType = null;
        int count = 0;
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }

            count = 0;
            String[] values = line.split("\\|");

            InputType inputType = new InputType();
            inputType.setName(InputTypeName.valueOf(values[count++]));
            try {
                findInputTypeByName(inputType.getName());
                continue;
            } catch (Exception e) {
                Logger.getLogger(InputService.class.getName()).log(Level.FINE, "Adding InputType: " + line);
            }

            inputType.setOwnerEntityType(values[count++]);
            inputType.setRequired("REQUIRED".equals(values[count++]));
            inputType.setInputClass(values[count++]);
            inputCurrencyType = values[count++];
            inputType.setInputCurrencyType(inputCurrencyType == null || "".equals(inputCurrencyType) ? null : CurrencyType.fromShortName(inputCurrencyType));
            //inputType.setName(values[count++]);
            count++;
            inputType.setDescription(values[count++]);
            inputType.setExcelSheet(values[count++]);
            inputType.setExcelCol(values[count++]);
            inputType.setGroupName(values[count++]);
            inputType.setGroupPosition(Integer.parseInt(values[count++]));
            inputType.setEffectiveFrom(LocalDate.now());
            //inputType.setEffectiveTo(LocalDate.now());
            inputType.setActive(true);

            logger.info("Creating InputType: " + inputType.getName());

            adminService.persist(inputType);

        }

        reader.close();

        logger.info("Finished initializing InputTypes.");

    }

    public void initInputTypeMap() {
        List<InputType> inputTypes = findAllInputTypes();
        inputTypes.forEach(inputType -> InputTypeName.putInputType(inputType.getName(), inputType));
    }

    public List<InputType> findInputType() throws Exception {  // Need an application exception type defined.

        TypedQuery<InputType> query = em.createQuery("SELECT b FROM InputType b", InputType.class);
        return (List<InputType>) query.getResultList();
    }

    public List<OutputType> findOutputType() throws Exception {  // Need an application exception type defined.

        TypedQuery<OutputType> query = em.createQuery("SELECT b FROM OutputType b", OutputType.class);
        return (List<OutputType>) query.getResultList();
    }

}
