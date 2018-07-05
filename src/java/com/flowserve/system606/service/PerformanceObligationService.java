package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.CurrencyInput;
import com.flowserve.system606.model.DateInput;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Input;
import com.flowserve.system606.model.InputSet;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.Output;
import com.flowserve.system606.model.OutputSet;
import com.flowserve.system606.model.OutputType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.StringInput;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author kgraves
 */
@Stateless
@Named
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
    @EJB
    private FinancialPeriodService financialPeriodService;

    private Input getInput(String inputTypeId, PerformanceObligation pob) {  // TODO - Exception type to be thrown?
        FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();
        InputType inputType = inputService.findInputTypeById(inputTypeId);

        if (!inputSetExistsForPeriod(period, pob)) {
            initializeInputSetForPeriod(period, pob);
        }
        if (!inputExistsForPeriod(period, inputType, pob)) {
            initializeInputForPeriod(period, inputType, pob);
        }

        return pob.getPeriodInputSetMap().get(period).getTypeInputMap().get(inputType);
    }

    private boolean inputSetExistsForPeriod(FinancialPeriod period, PerformanceObligation pob) {
        return pob.getPeriodInputSetMap().get(period) != null;
    }

    private void initializeInputSetForPeriod(FinancialPeriod period, PerformanceObligation pob) {
        InputSet inputSet = new InputSet();
        inputSet.setPerformanceObligation(pob);
        pob.getPeriodInputSetMap().put(period, inputSet);
    }

    private boolean inputExistsForPeriod(FinancialPeriod period, InputType inputType, PerformanceObligation pob) {
        return pob.getPeriodInputSetMap().get(period).getTypeInputMap().get(inputType) != null;
    }

    private void initializeInputForPeriod(FinancialPeriod period, InputType inputType, PerformanceObligation pob) {
        try {
            Class<?> clazz = Class.forName(inputType.getInputClass());
            Input input = (Input) clazz.newInstance();
            input.setInputType(inputType);
            input.setInputSet(pob.getPeriodInputSetMap().get(period));
            pob.getPeriodInputSetMap().get(period).getTypeInputMap().put(inputType, input);
        } catch (Exception e) {
            Logger.getLogger(PerformanceObligationService.class.getName()).log(Level.SEVERE, "Severe exception initializing inputTypeId: " + inputType.getId(), e);
            throw new IllegalStateException("Severe exception initializing inputTypeId: " + inputType.getId(), e);
        }
    }

    public String getStringInputValue(String inputTypeId, PerformanceObligation pob) {
        return (String) getInput(inputTypeId, pob).getValue();
    }

    public BigDecimal getDecimalInputValue(String inputTypeId, PerformanceObligation pob) {
        return (BigDecimal) getInput(inputTypeId, pob).getValue();
    }

    public LocalDate getDateInputValue(String inputTypeId, PerformanceObligation pob) {
        return (LocalDate) getInput(inputTypeId, pob).getValue();
    }

    public BigDecimal getCurrencyInputValue(String inputTypeId, PerformanceObligation pob) {
        return (BigDecimal) getInput(inputTypeId, pob).getValue();
    }

    public BigDecimal getCurrencyInputValuePriorPeriod(String inputTypeId, PerformanceObligation pob) {
        return getCurrencyInputValue(inputTypeId, pob);   // KJG TODO - Hack for now to just return this period's value for testing calcs.
    }

    public StringInput getStringInput(String inputTypeId, PerformanceObligation pob) {
        return (StringInput) getInput(inputTypeId, pob);
    }

    public DateInput getDateInput(String inputTypeId, PerformanceObligation pob) {
        return (DateInput) getInput(inputTypeId, pob);
    }

    public CurrencyInput getCurrencyInput(String inputTypeId, PerformanceObligation pob) {  // TODO KJG - This method should be getCurrencyInput() and the method above should be getCurrencyInputValue() waiting on this due to impact.
        return (CurrencyInput) getInput(inputTypeId, pob);
    }

    // Init missing output if needed.
    private Output getOutput(String outputTypeId, PerformanceObligation pob) {  // TODO - Exception type to be thrown?

        FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();
        OutputType outputType = outputService.findOutputTypeById(outputTypeId);

        try {
            if (pob.getPeriodOutputSetMap().get(period) == null) {
                OutputSet outputSet = new OutputSet();
                outputSet.setPerformanceObligation(pob);
                pob.getPeriodOutputSetMap().put(period, outputSet);

            }
            if (pob.getPeriodOutputSetMap().get(period).getIdOutputMap().get(outputType) == null) {
                Class<?> clazz = Class.forName(outputType.getOutputClass());
                Output output = (Output) clazz.newInstance();
                output.setOutputType(outputType);
                output.setOutputSet(pob.getPeriodOutputSetMap().get(period));
                pob.getPeriodOutputSetMap().get(period).getIdOutputMap().put(outputType, output);
            }
        } catch (Exception e) {
            Logger.getLogger(PerformanceObligationService.class.getName()).log(Level.SEVERE, "Severe exception initializing outputTypeId: " + outputTypeId, e);
            throw new IllegalStateException("Severe exception initializing outputTypeId: " + outputTypeId, e);
        }

        return pob.getPeriodOutputSetMap().get(period).getIdOutputMap().get(outputType);
    }

//    public void putOutputMessage(String outputTypeId, String message) {
//        getOutput(outputTypeId).setMessage(message);
//    }
    public void putCurrencyOutputValue(String outputTypeId, PerformanceObligation pob, BigDecimal value) {
        getOutput(outputTypeId, pob).setValue(value);
    }

    public BigDecimal getCurrencyOutputValuePriorPeriod(String outputTypeId, PerformanceObligation pob) {
        return new BigDecimal("10.0");
        //return getCurrencyOutputValue(outputTypeId);  // KJG TODO - Hack for now to just return this period's value for testing calcs.
    }

    public BigDecimal getCurrencyOutputValue(String outputTypeId, PerformanceObligation pob) {
        return (BigDecimal) getOutput(outputTypeId, pob).getValue();
    }

    public Output getCurrencyOutput(String outputTypeId, PerformanceObligation pob) {
        return getOutput(outputTypeId, pob);
    }

//    public boolean isInputRequired() {
//        for (Input input : periodInputSets.values()) {
//            if (input.getInputType().isRequired() && input.getValue() == null) {
//                return true;
//            }
//        }
//
//        return false;
//    }
    public PerformanceObligation
            findById(Long id) {
        return em.find(PerformanceObligation.class,
                id);
    }

    public PerformanceObligation update(PerformanceObligation pob) throws Exception {
        //User user = adminService.findUserByFlsId(sessionContext.getCallerPrincipal().getName().toLowerCase());
        //pob.setLastUpdatedBy(updatedBy);
        if (pob.getCreationDate() == null) {
            pob.setCreationDate(LocalDateTime.now());
        }
        pob.setLastUpdateDate(LocalDateTime.now());

        return em.merge(pob);
    }

    public void persist1(Object object) {
        em.persist(object);
    }

    public void testFromDrools(PerformanceObligation pob) {
        Logger.getLogger(PerformanceObligationService.class
                .getName()).log(Level.INFO, "Test log from Drools call. Pob ID: " + pob.getId());
    }

    public PerformanceObligation
            findPerformanceObligationById(Long id) {
        return em.find(PerformanceObligation.class,
                id);
    }

    public long getPobCount() {
        return (long) em.createQuery("SELECT COUNT(pob.id) FROM PerformanceObligation pob").getSingleResult();
    }

    public void initPOBs() throws Exception {

        //read init_contract_pob_data.txt a second time
        if (getPobCount() == 0) {
            logger.info("Initializing POBs");
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class
                    .getResourceAsStream("/resources/app_data_init_files/init_contract_pob_data.txt"), "UTF-8"));

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
                pobName = pobName + " " + pobId;
                if (findPerformanceObligationById(contractId) != null) {
                    throw new IllegalStateException("Duplicte POBs in the file.  This should never happen.  POB ID: " + pobId);
                }
                revRecMethod = values[count++].trim();

                Contract contract = contractService.findContractById(contractId);

                if (contract == null) {
                    throw new IllegalStateException("POB refers to non-existent contract.  Invalid.  POB ID: " + pobId);
                }
//                if (contract == null) {
//                    contract = new Contract();
//                    contract.setId(contractId);
//                    contract.setName(customerName + '-' + contractId);
//                    contract.setSalesOrderNumber(salesOrderNumber);
//                }

                pob.setContract(contract);
                pob.setName(pobName);
                pob.setId(pobId);
                pob.setRevRecMethod(revRecMethod);

                pob.setActive(true);
                //performanceObligationService.initializeInputs(pob);
                //performanceObligationService.initializeOutputs(pob);

                //persist(pob);
                //KJG
                pob = update(pob);
                contract.getPerformanceObligations().add(pob);
                contractService.update(contract);

            }

            reader.close();

            logger.info("Finished initializing POBs.");
        }
    }
}
