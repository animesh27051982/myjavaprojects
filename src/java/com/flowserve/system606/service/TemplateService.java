/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Steve
 */
@Stateless
public class TemplateService {
    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    
    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    
    public List<ReportingUnit> getReportingUnits() {
        return new ArrayList<ReportingUnit>();
    }
    
    public void populateData(InputStream inputStream, FileOutputStream outputStream, List<ReportingUnit> reportingUnits) throws Exception {
        
        List<InputType> inputTypeList = findInputTypes();
        
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet spreadsheet = workbook.getSheetAt(0);

        //Create row object
        XSSFRow row;

        //int rowid = spreadsheet.getLastRowNum();
        int rowid = 3;
        for (ReportingUnit ru : reportingUnits) {
            List<Contract> contracts = ru.getContract();
            logger.info("TemplateService.populateData ReportingUnit:" + ru.getId() + " contracts:" + contracts.size() +  " \t\t ");
            for(Contract contract : contracts) {
                logger.info("TemplateService.populateData contract:" + contract.getId() + " \t\t ");
                List<PerformanceObligation> pobs = contract.getPerformanceObligations();
                logger.info("TemplateService.populateData contract:" + contract.getId() + " pobs:" + pobs.size() +  " \t\t ");
                for (PerformanceObligation pob : pobs) {
                    row = spreadsheet.createRow(rowid++);
                    logger.info("TemplateService.populateData rowid:" + rowid + " \t\t ");
                    logger.info("TemplateService.populateData pob:" + pob + " \t\t ");

                    //int cellid = 0;
                    Cell cell = null;
                    for (InputType inputType : inputTypeList) {
                        logger.info("TemplateService.populateData inputType.getExcelCol():" + inputType.getExcelCol() + " \t\t ");
                        switch (inputType.getExcelCol() ) {
                            case "B": //RU
                                cell = row.createCell(0);
                                cell.setCellValue("AMSS");
                                cell = row.createCell(1);
                                cell.setCellValue( ru.getCode() );
                                break;
                            case "C": //C-ID
                                cell = row.createCell(2);
                                cell.setCellValue( contract.getId() );
                                break;    
                            case "D":
                                cell = row.createCell(3);
                                // cell.setCellValue( contract.getCustomer().getName() );
                                cell.setCellValue( contract.getName() );
                                break;
                            case "E":
                                cell = row.createCell(4);
                                cell.setCellValue(contract.getSalesOrderNumber());
                                break; 
                            case "F":
                                cell = row.createCell(5);
                                cell.setCellValue(pob.getName());
                                break;                     
                            case "G":
                                cell = row.createCell(6);
                                cell.setCellValue(pob.getId());
                            case "H":
                                cell = row.createCell(7);
                                cell.setCellValue(pob.getRevRecMethod());
                            case "I":
                                cell = row.createCell(8);
                                // cell.setCellValue(pob.getCurrencyInput(inputType.getInputCurrencyType()));
                            case "J":
                                cell = row.createCell(9);
                                if(contract.getTotalTransactionPrice() != null)
                                    cell.setCellValue(contract.getTotalTransactionPrice().doubleValue());
                                break;                     
                            case "K": //TRANSACTION_PRICE
                                cell = row.createCell(10);
                                cell.setCellValue( pob.getDecimalValue(inputType.getId()).doubleValue() );
                                break;                     
                        }

                    }
                }
            }
      
        }
        workbook.write(outputStream);
        workbook.close();
        inputStream.close();
        outputStream.close();
    }
    
    
    public List<InputType> findInputTypes() {
        Query query = em.createQuery("SELECT inputType FROM InputType inputType");
        return (List<InputType>) query.getResultList();
    }
}
