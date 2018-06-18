/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Input;
import com.flowserve.system606.model.InputSet;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.User;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
public class InputService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    
    @EJB
    private PerformanceObligationService pobService;
    @EJB
    private AdminService adminService;
    
    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public boolean readFeed(InputStream fis, String filename) throws Exception {  // Need an application exception type defined.
       
        logger.info("readFeed:" + fis.toString());
        
        List<InputType> inputTypeList = findInputTypes();
        HashMap<String, InputType> inputTypeMap = new HashMap();
        for(InputType inputType: inputTypeList) {
            inputTypeMap.put(inputType.getExcelCol(), inputType);
        }
               
        XSSFRow row;
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        XSSFSheet spreadsheet = workbook.getSheetAt(0);
        Iterator < Row >  rowIterator = spreadsheet.iterator();
        InputSet inputSet = new InputSet();
        inputSet.setFilename( filename );

        List<Input> inputList = new ArrayList<>();
        User admin = adminService.findUserByFlsId("admin").get(0);

        while (rowIterator.hasNext()) {
           row = (XSSFRow) rowIterator.next();
           // skip first row as header
           if(row.getRowNum() <= 1)
               continue;
           else if(row.getRowNum() > 3) //test only 2 rows
               break;
           
           PerformanceObligation pob = null;
           Long pobID = null;
           Iterator < Cell >  cellIterator = row.cellIterator();
           while ( cellIterator.hasNext()) {
              Cell cell = cellIterator.next();
              
              // only process the excel_cols that are in the map
              String excelCol = CellReference.convertNumToColString(cell.getColumnIndex());              
              if(inputTypeMap.get(excelCol) != null) {             

                InputType inputType = inputTypeMap.get(excelCol);
                Class<?> clazz = Class.forName( inputType.getInputClass() );
                Input input = (Input)clazz.newInstance();
                input.setInputType(inputType);
                
                switch (cell.getCellType()) {
                   case Cell.CELL_TYPE_NUMERIC:
                      boolean bDate = false;
                      CellStyle style = cell.getCellStyle(); 
                      if(style != null) {
                        int i = style.getDataFormat(); 
                        String f = style.getDataFormatString(); 
                        bDate = DateUtil.isADateFormat(i, f); 
                        if(bDate) {
                            DataFormatter formatter = new DataFormatter();
                            String formattedValue = formatter.formatCellValue(cell);
                            Date d = cell.getDateCellValue();
                            input.setValue( d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() );
                            inputList.add(input);
                            pob.putInput(input);
                        }
                      } 
                      if(!bDate) {
                        BigDecimal bd = BigDecimal.valueOf(cell.getNumericCellValue());
                        input.setValue( bd );
                        inputList.add(input);
                        pob.putInput(input);
                      }
                      break;

                   case Cell.CELL_TYPE_STRING:
                      if(!cell.getStringCellValue().trim().isEmpty()) {
                        input.setValue(cell.getStringCellValue());
                        inputList.add(input);
                        pob.putInput(input);
                      }                      
                      break; 
                      
                }
              } else if(excelCol.equalsIgnoreCase("G")) { //check the POB ID column to get POB_ID
                switch (cell.getCellType()) {
                   case Cell.CELL_TYPE_NUMERIC:
                      pobID = (long)cell.getNumericCellValue();
                      pob = pobService.findById(pobID);
                      break;
                   default: 
                      logger.info("Invalid POB ID \t\t ");
                }
              }
           }

           // update/save the pob
           logger.info("pobService.update POB ID:" + pob.getId() + " Name:" +pob.getName() + " \t\t ");
           pobService.update(pob, admin);
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

}
