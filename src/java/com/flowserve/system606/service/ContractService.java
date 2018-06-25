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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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
        InputStream fis = AppInitializeService.class.getResourceAsStream("/resources/excel_input_templates/POCI_Template_DRAFT_v2 (version 1).xlsb - Copy - Copy");

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
            String contractId = null;
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
//                                    if(contract == null) {
//                                        contract = new Contract();
//                                    }
                                    
                                    switch (input.getInputType().getId()) {
                                        case "REPORTING_UNIT": 
                                            reportingUnit = (String)input.getValue();
                                            break;
                                        case "C_ID":
                                            contractId = (String)input.getValue();
                                            break;
                                        case "CUSTOMER_NAME":
                                            customerName = (String)input.getValue();
                                            break;
                                        case "SALES_ORDER_NUMBER":
                                            salesOrderNum = (String)input.getValue();
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

            // update/save the pob
            logger.info("pobService.update POB ID:" + pob.getId() + " Name:" + pob.getName() + " \t\t ");
            pob = pobService.update(pob);
            pobService.initializeOutputs(pob);
            businessRuleService.executeBusinessRules(pob);
            if(totalTransactionPrice != null) {
                saveContract(reportingUnit, contractId, customerName, salesOrderNum, totalTransactionPrice);
            }
            logger.log(Level.INFO, "pob.PERCENT_COMPLETE: " + pob.getOutput(OutputTypeId.PERCENT_COMPLETE).getValue().toString());
            logger.log(Level.INFO, "pob.REVENUE_EARNED_TO_DATE: " + pob.getOutput(OutputTypeId.REVENUE_EARNED_TO_DATE).getValue().toString());
        }
        fis.close();
        inputSet.setInputs(inputList);
        persist(inputSet);

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
    
    private void saveContract(String reportingUnit, String contractId, String customerName, String salesOrderNum, BigDecimal totalTransactionPrice) {
        Contract contract = new Contract();
        contract.setName( customerName + "-" + contractId );
        // contract.setReportingUnit(reportingUnit);
        contract.setSalesOrderNumber(salesOrderNum);
//        contract.setTotalTransactionPrice(totalTransactionPrice);
        em.merge( contract );
    }
}


//Contract fields... set:
//Contract ID - B
//Contract Name (Customer name dash contract ID) - C
//Sales order number - D
//Contract currency - use a Currency.getInstance("XXX") - I
//Total transaction price - use the first occurrence of this TTP to set the contract level amount. - J
//BookingDate - Please add this field to contract and set using first occurrence - L
//est comp date - Please add this field to contract and set using first occurrence - M
//
//After persisting contract, add it to the proper reporting unit object, which should already exist.
//
//REPORTING_UNIT|Contract|com.flowserve.system606.model.StringInput|Reporting Unit|Reporting Unit|POC POB Inputs|Group1|B
//C_ID|Contract|com.flowserve.system606.model.StringInput|C-ID|C-ID|POC POB Inputs|Group1|C
//CUSTOMER_NAME|Contract|com.flowserve.system606.model.StringInput|Customer Name|Customer Name|POC POB Inputs|Group1|D
//SALES_ORDER_NUMBER|Contract|com.flowserve.system606.model.StringInput|Sales Order #|Sales Order #|POC POB Inputs|Group1|E
//CONTRACT_CURRENCY|Contract|com.flowserve.system606.model.StringInput|Contract Currency|Contract Currency|POC POB Inputs|Group1|I
//TOTAL_TRANS_PRICE_CONTRACT_CURR|Contract|com.flowserve.system606.model.DecimalInput|Total Trans Price Contract Curr|Total Trans Price Contract Curr|POC POB Inputs|Group1|J
//BOOKING_DATE|Contract|com.flowserve.system606.model.DateInput|Booking Date (LoA)|Booking Date (LoA)|POC POB Inputs|Group1|L
//ESTIMATED_COMPLETION_DATE|Contract|com.flowserve.system606.model.DateInput|Estimated Completion Date|Estimated Completion Date|POC POB Inputs|Group1|M