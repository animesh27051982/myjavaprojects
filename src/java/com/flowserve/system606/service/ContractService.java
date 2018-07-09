/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.Metric;
import com.flowserve.system606.model.MetricSet;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.ReportingUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

/**
 *
 * @author span
 */
@Stateless
public class ContractService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    @EJB
    private PerformanceObligationService pobService;
    @EJB
    private AdminService adminService;

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public long getContractCount() {
        return (long) em.createQuery("SELECT COUNT(c.id) FROM Contract c").getSingleResult();
    }

    public void initContracts() throws Exception {

        // change to read init_contract_pob_data.txt a first time
        if (getContractCount() == 0) {
            logger.info("Initializing Contracts");
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

                platform = values[count++].trim();
                ru = values[count++].trim().replace("RU", "");
                contractId = Long.valueOf(values[count++].trim());
                if (findContractById(contractId) != null) {
                    continue;  // we've already processed this contract.  dont' process the repeated lines.
                }
                customerName = values[count++].trim();
                salesOrderNumber = values[count++].trim();
                pobName = values[count++].trim();
                pobId = Long.valueOf(values[count++].trim());
                revRecMethod = values[count++].trim();

                Contract contract = new Contract();
                contract.setId(contractId);
                contract.setName(customerName + '-' + contractId);
                contract.setSalesOrderNumber(salesOrderNumber);

                ReportingUnit reportingUnit = adminService.findReportingUnitByCode(ru);

                if (contract == null) {
                    throw new IllegalStateException("Countract refers to a non-existent RU.  Invalid.");
                }
                // KJG removing this code, we should never have a reporting unit that we are not prepared for.
                // We don't want to create here.  We'll have users create via front end and then reload.

//                if (reportingUnit == null) {
//                    reportingUnit = new ReportingUnit();
//                    reportingUnit.setCode(ru);
//                }
                contract.setReportingUnit(reportingUnit);
                //persist(contract);

                //update(contract);
                // KJG Adding code
                contract = update(contract);   // this gives us the JPA managed object.
                reportingUnit.getContracts().add(contract);
                adminService.update(reportingUnit);

            }

            reader.close();

            logger.info("Finished initializing Contracts.");
        }
    }

    public Contract findContractById(Long id) {
        return em.find(Contract.class, id);
    }

//    // We are now initializing contract from data files.  Removing for now.
//    public boolean initContractsFromExcel() throws Exception {  // Need an application exception type defined.
//        String filename = "POCI_Template_DRAFT_v2 (version 1).xlsb - Copy - Copy";
//        InputStream fis = AppInitializeService.class.getResourceAsStream("/resources/excel_input_templates/POCI_Template_DRAFT_v2.xlsb.xlsx");
//
//        logger.info("readFeed:" + fis.toString());
//
//        List<InputType> inputTypeList = findInputTypes();
//        HashMap<String, InputType> inputTypeMap = new HashMap();
//        for (InputType inputType : inputTypeList) {
//            inputTypeMap.put(inputType.getExcelCol(), inputType);
//        }
//
//        XSSFRow row;
//        XSSFWorkbook workbook = new XSSFWorkbook(fis);
//        XSSFSheet spreadsheet = workbook.getSheetAt(0);
//        Iterator< Row> rowIterator = spreadsheet.iterator();
//        InputSet inputSet = new InputSet();
//        inputSet.setFilename(filename);
//
//        List<Input> inputList = new ArrayList<>();
//        // when do we call contract.setExchange - when we save a new contract or we see a new C-ID and customer name
//        List<PerformanceObligation> exchange = null;
//        Contract contract = null;
//
//        while (rowIterator.hasNext()) {
//            row = (XSSFRow) rowIterator.next();
//            // skip first row as header
//            if (row.getRowNum() <= 1) {
//                continue;
//            } else if (row.getRowNum() > 3) //test only 2 rows
//            {
//                break;
//            }
//
//            PerformanceObligation pob = null;
//            String reportingUnit = null;
//            long contractId = -1;
//            String customerName = null;
//            String salesOrderNum = null;
//            BigDecimal totalTransactionPrice = null;
//
//            Long pobID = null;
//            Iterator< Cell> cellIterator = row.cellIterator();
//            while (cellIterator.hasNext()) {
//                Cell cell = cellIterator.next();
//
//                // only process the excel_cols that are in the map
//                String excelCol = CellReference.convertNumToColString(cell.getColumnIndex());
//                if (inputTypeMap.get(excelCol) != null) {
//
//                    InputType inputType = inputTypeMap.get(excelCol);
//                    Class<?> clazz = Class.forName(inputType.getInputClass());
//                    Input input = (Input) clazz.newInstance();
//                    input.setInputType(inputType);
//
//                    switch (cell.getCellType()) {
//                        case Cell.CELL_TYPE_NUMERIC:
//                            boolean bDate = false;
//                            CellStyle style = cell.getCellStyle();
//                            if (style != null) {
//                                int i = style.getDataFormat();
//                                String f = style.getDataFormatString();
//                                bDate = DateUtil.isADateFormat(i, f);
//                                if (bDate) {
//                                    DataFormatter formatter = new DataFormatter();
//                                    String formattedValue = formatter.formatCellValue(cell);
//                                    Date d = cell.getDateCellValue();
//                                    input.setValue(d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
//                                    inputList.add(input);
//                                    pob.putInput(input);
//                                }
//                            }
//                            if (!bDate) {
//                                BigDecimal bd = BigDecimal.valueOf(cell.getNumericCellValue());
//                                input.setValue(bd);
//                                if (input.getInputType().getOwnerEntityType().equalsIgnoreCase("Contract")) {
//                                    switch (input.getInputType().getId()) {
//                                        case "C_ID":
//                                            contractId = (long) cell.getNumericCellValue();
//                                            break;
//                                        case "TOTAL_TRANS_PRICE_CONTRACT_CURR":
//                                            totalTransactionPrice = (BigDecimal) input.getValue();
//                                            break;
//                                    }
//                                } else {
//                                    inputList.add(input);
//                                    pob.putInput(input);
//                                }
//                            }
//                            break;
//
//                        case Cell.CELL_TYPE_STRING:
//                            if (!cell.getStringCellValue().trim().isEmpty()) {
//                                input.setValue(cell.getStringCellValue());
//                                if (input.getInputType().getOwnerEntityType().equalsIgnoreCase("Contract")) {
//
//                                    switch (input.getInputType().getId()) {
//                                        case "REPORTING_UNIT":
//                                            reportingUnit = (String) input.getValue();
//                                            break;
//                                        case "CUSTOMER_NAME":
//                                            customerName = (String) input.getValue();
//                                            break;
//                                        case "SALES_ORDER_NUMBER":
//                                            salesOrderNum = (String) input.getValue();
//                                            break;
//                                    }
//
//                                } else {
//                                    inputList.add(input);
//                                    pob.putInput(input);
//                                }
//                            }
//                            break;
//
//                    }
//                } else if (excelCol.equalsIgnoreCase("G")) { //check the POB ID column to get POB_ID
//                    switch (cell.getCellType()) {
//                        case Cell.CELL_TYPE_NUMERIC:
//                            pobID = (long) cell.getNumericCellValue();
//                            pob = pobService.findById(pobID);
//                            break;
//                        default:
//                            logger.info("Invalid POB ID \t\t ");
//                    }
//                }
//            }
//
//            logger.info("pobService.update POB ID:" + pob.getId() + " Name:" + pob.getName() + " \t\t ");
//            //this indicates are are in a new contract, or use C-ID and Customer Name to check
//            if ((contract != null && contractId != contract.getId()) || totalTransactionPrice != null) {
//                // if there are existing old contract, save exchange list to it and finish the existing old contract
//                if (contract != null) {
//                    contract.setPerformanceObligations(exchange);
//                    //em.merge( contract );
//                    //persist(contract);
//                    logger.log(Level.INFO, "contract.getExchanges().size(): " + contract.getPerformanceObligations().size());
//                    logger.log(Level.INFO, "contract.getExchanges().get(0).getId(): " + contract.getPerformanceObligations().get(0).getId());
//                    contract = update(contract);
//                }
//                //now create a new contract for the new pob and add its pob
//                contract = createContract(reportingUnit, contractId, customerName, salesOrderNum, totalTransactionPrice);
//                exchange = contract.getPerformanceObligations();
//                pob.setContract(contract);
//                exchange.add(pob);
//            } else {
//                // we are in existing contract with a new pob, add pob to existing contract
//                pob.setContract(contract);
//                exchange.add(pob);
//            }
//            pob = pobService.update(pob);
//            pobService.initializeOutputs(pob);
//            businessRuleService.executeBusinessRules(pob);
//            logger.log(Level.INFO, "pob.PERCENT_COMPLETE: " + pob.getOutput(OutputTypeId.PERCENT_COMPLETE).getValue().toString());
//            logger.log(Level.INFO, "pob.REVENUE_EARNED_TO_DATE: " + pob.getOutput(OutputTypeId.REVENUE_EARNED_TO_DATE).getValue().toString());
//        }
//        fis.close();
//        inputSet.setInputs(inputList);
//        persist(inputSet);
//        // need commit last contract
//        contract.setPerformanceObligations(exchange);
//        logger.log(Level.INFO, "contract.getExchanges().size(): " + contract.getPerformanceObligations().size());
//        logger.log(Level.INFO, "contract.getExchanges().get(0).getId(): " + contract.getPerformanceObligations().get(0).getId());
//        contract = update(contract);
//        return true;
//    }
    public List<MetricType> findInputTypes() {
        Query query = em.createQuery("SELECT inputType FROM InputType inputType");
        return (List<MetricType>) query.getResultList();
    }

    public void persist(Metric input) throws Exception {
        em.persist(input);
    }

    @Transactional
    public void persist(MetricSet inputSet) throws Exception {
        em.persist(inputSet);
    }

    public MetricType findInputTypeById(String id) {
        return em.find(MetricType.class, id);
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
}
