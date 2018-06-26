/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Input;
import com.flowserve.system606.model.InputSet;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.OutputTypeId;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.CurrencyType;
import com.flowserve.system606.model.InputTypeId;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
    @EJB
    private BusinessRuleService businessRuleService;

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public boolean initContracts() throws Exception {  // Need an application exception type defined.
        String filename = "POCI_Template_DRAFT_v2 (version 1).xlsb - Copy - Copy";
        InputStream fis = AppInitializeService.class.getResourceAsStream("/resources/excel_input_templates/POCI_Template_DRAFT_v2.xlsb.xlsx");

        logger.info("readFeed:" + fis.toString());

        List<InputType> inputTypeList = findInputTypes();
        HashMap<String, InputType> inputTypeMap = new HashMap();
        for (InputType inputType : inputTypeList) {
            inputTypeMap.put(inputType.getExcelCol(), inputType);
        }

        XSSFRow row;
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        XSSFSheet spreadsheet = workbook.getSheetAt(0);
        Iterator< Row> rowIterator = spreadsheet.iterator();
        InputSet inputSet = new InputSet();
        inputSet.setFilename(filename);

        List<Input> inputList = new ArrayList<>();
        // when do we call contract.setExchange - when we save a new contract or we see a new C-ID and customer name
        List<PerformanceObligation> exchange = null;
        Contract contract = null;
        
        while (rowIterator.hasNext()) {
            row = (XSSFRow) rowIterator.next();
            // skip first row as header
            if (row.getRowNum() <= 1) {
                continue;
            } else if (row.getRowNum() > 3) //test only 2 rows
            {
                break;
            }

            PerformanceObligation pob = null;
            String reportingUnit = null;
            long contractId = -1;
            String customerName = null;
            String salesOrderNum = null;
            BigDecimal totalTransactionPrice = null;        

            Long pobID = null;
            Iterator< Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();

                // only process the excel_cols that are in the map
                String excelCol = CellReference.convertNumToColString(cell.getColumnIndex());
                if (inputTypeMap.get(excelCol) != null) {

                    InputType inputType = inputTypeMap.get(excelCol);
                    Class<?> clazz = Class.forName(inputType.getInputClass());
                    Input input = (Input) clazz.newInstance();
                    input.setInputType(inputType);

                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_NUMERIC:
                            boolean bDate = false;
                            CellStyle style = cell.getCellStyle();
                            if (style != null) {
                                int i = style.getDataFormat();
                                String f = style.getDataFormatString();
                                bDate = DateUtil.isADateFormat(i, f);
                                if (bDate) {
                                    DataFormatter formatter = new DataFormatter();
                                    String formattedValue = formatter.formatCellValue(cell);
                                    Date d = cell.getDateCellValue();
                                    input.setValue(d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                                    inputList.add(input);
                                    pob.putInput(input);
                                }
                            }
                            if (!bDate) {
                                BigDecimal bd = BigDecimal.valueOf(cell.getNumericCellValue());
                                input.setValue(bd);
                                if(input.getInputType().getOwnerEntityType().equalsIgnoreCase("Contract")) {                                  
                                    switch (input.getInputType().getId()) {
                                        case "C_ID":
                                            contractId = (long) cell.getNumericCellValue();
                                            break;
                                        case "TOTAL_TRANS_PRICE_CONTRACT_CURR": 
                                            totalTransactionPrice = (BigDecimal)input.getValue();
                                            break;
                                    }                                    
                                } else {
                                    inputList.add(input);
                                    pob.putInput(input);
                                }
                            }
                            break;

                        case Cell.CELL_TYPE_STRING:
                            if (!cell.getStringCellValue().trim().isEmpty()) {
                                input.setValue(cell.getStringCellValue());
                                if(input.getInputType().getOwnerEntityType().equalsIgnoreCase("Contract")) {

                                    switch (input.getInputType().getId()) {
                                        case "REPORTING_UNIT": 
                                            reportingUnit = (String)input.getValue();
                                            break;
                                        case "CUSTOMER_NAME":
                                            customerName = (String)input.getValue();
                                            break;
                                        case "SALES_ORDER_NUMBER":
                                            salesOrderNum = (String)input.getValue();
                                            break;
                                    }                                   
                                    
                                } else {
                                    inputList.add(input);
                                    pob.putInput(input);
                                }
                            }
                            break;

                    }
                } else if (excelCol.equalsIgnoreCase("G")) { //check the POB ID column to get POB_ID
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_NUMERIC:
                            pobID = (long) cell.getNumericCellValue();
                            pob = pobService.findById(pobID);
                            break;
                        default:
                            logger.info("Invalid POB ID \t\t ");
                    }
                }
            }

            logger.info("pobService.update POB ID:" + pob.getId() + " Name:" + pob.getName() + " \t\t ");
            //this indicates are are in a new contract, or use C-ID and Customer Name to check
            if( (contract != null && contractId != contract.getId()) || totalTransactionPrice != null) { 
                // if there are existing old contract, save exchange list to it and finish the existing old contract
                if(contract != null) {
                    contract.setExchanges(exchange);
                    //em.merge( contract );
                    //persist(contract);
                    logger.log(Level.INFO, "contract.getExchanges().size(): " + contract.getExchanges().size());
                    logger.log(Level.INFO, "contract.getExchanges().get(0).getId(): " + contract.getExchanges().get(0).getId());                    
                    contract = update(contract);
                }
                //now create a new contract for the new pob and add its pob
                contract = createContract(reportingUnit, contractId, customerName, salesOrderNum, totalTransactionPrice); 
                exchange = contract.getExchanges();
                pob.setContract(contract);
                exchange.add( pob );
            } else {
                // we are in existing contract with a new pob, add pob to existing contract
                pob.setContract(contract);
                exchange.add( pob );
            }
            pob = pobService.update(pob);
            pobService.initializeOutputs(pob);
            businessRuleService.executeBusinessRules(pob);            
            logger.log(Level.INFO, "pob.PERCENT_COMPLETE: " + pob.getOutput(OutputTypeId.PERCENT_COMPLETE).getValue().toString());
            logger.log(Level.INFO, "pob.REVENUE_EARNED_TO_DATE: " + pob.getOutput(OutputTypeId.REVENUE_EARNED_TO_DATE).getValue().toString());
        }
        fis.close();
        inputSet.setInputs(inputList);
        persist(inputSet);
        // need commit last contract
        contract.setExchanges(exchange);
        logger.log(Level.INFO, "contract.getExchanges().size(): " + contract.getExchanges().size());
        logger.log(Level.INFO, "contract.getExchanges().get(0).getId(): " + contract.getExchanges().get(0).getId());             
        contract = update(contract);
        return true;
    }

    public List<InputType> findInputTypes() {
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
    
    private Contract createContract(String reportingUnit, long contractId, String customerName, String salesOrderNum, BigDecimal totalTransactionPrice) {
        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setName( customerName + "-" + contractId );
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

        //admin = adminService.findUserByFlsId("admin");
        if (findInputTypes().isEmpty()) {
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
                logger.info(line);
                String[] values = line.split("\\|");

                InputType inputType = new InputType();
                inputType.setId(values[count++]);
                inputType.setOwnerEntityType(values[count++]);
                inputType.setInputClass(values[count++]);
                inputCurrencyType = values[count++];
                inputType.setInputCurrencyType(inputCurrencyType == null || "".equals(inputCurrencyType) ? null : CurrencyType.fromShortName(inputCurrencyType));
                inputType.setName(values[count++]);
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

        logger.info("Input type name for " + InputTypeId.TRANSACTION_PRICE + " = " + findInputTypeById(InputTypeId.TRANSACTION_PRICE).getName());
    }
}