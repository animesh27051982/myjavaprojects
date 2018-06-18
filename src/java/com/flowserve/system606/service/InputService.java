/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Input;
import com.flowserve.system606.model.InputSet;
import com.flowserve.system606.model.InputType;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    public boolean readFeed(InputStream fis, String filename) throws Exception {  // Need an application exception type defined.
       
        System.out.println("readFeed:" + fis.toString());
        
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

        while (rowIterator.hasNext()) {
           row = (XSSFRow) rowIterator.next();
           // skip first row as header
           if(row.getRowNum() <= 1)
               continue;
           else if(row.getRowNum() > 3) //test only 2 rows
               break;
           
           Iterator < Cell >  cellIterator = row.cellIterator();
           while ( cellIterator.hasNext()) {
              Cell cell = cellIterator.next();
              
              // only process the excel_cols that are in the map
              String excelCol = CellReference.convertNumToColString(cell.getColumnIndex());              
              if(inputTypeMap.get(excelCol) != null) {             
                System.out.print( CellReference.convertNumToColString(cell.getColumnIndex())  + " \t\t ");

                InputType inputType = inputTypeMap.get(excelCol);
                Class<?> clazz = Class.forName( inputType.getInputClass() );
                Input input = (Input)clazz.newInstance();
                
                switch (cell.getCellType()) {
                   case Cell.CELL_TYPE_NUMERIC:
                      System.out.print(cell.getNumericCellValue() + " \t\t ");
                      boolean bDate = false;
                      CellStyle style = cell.getCellStyle(); 
                      if(style != null) {
                        int i = style.getDataFormat(); 
                        String f = style.getDataFormatString(); 
                        bDate = DateUtil.isADateFormat(i, f); 
                        if(bDate) {
                            DataFormatter formatter = new DataFormatter();
                            String formattedValue = formatter.formatCellValue(cell);
                            System.out.print("formattedValue:" + formattedValue + " \t\t ");
//                            input.setValue( formattedValue );
                            Date d = cell.getDateCellValue();
                            input.setValue( d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() );
                            inputList.add(input);                          
                        }
                      } 
                      if(!bDate) {
                        BigDecimal bd = BigDecimal.valueOf(cell.getNumericCellValue());
                        input.setValue( bd );
                        inputList.add(input);
                      }
                      break;

                   case Cell.CELL_TYPE_STRING:
                      System.out.print("cell.getStringCellValue():" + cell.getStringCellValue() + " \t\t ");
                      if(!cell.getStringCellValue().trim().isEmpty()) {
                        input.setValue(cell.getStringCellValue());
                        inputList.add(input);
                      }                      
                      break; 
                      
                }
              }
           }
           System.out.println();
        }
        fis.close();          
        inputSet.setInputs(inputList);
        persist(inputSet);
        
        return true;
    }
    
    public List<InputType> findInputTypes() {
        System.out.println("findInputTypes");
        Query query = em.createQuery("SELECT inputType FROM InputType inputType");
        return (List<InputType>) query.getResultList();
    }
    
    // persist in INPUT table
    public void persist(Input input) throws Exception {
       em.persist(input);
    }
    
    // persist in INPUT_SETS table
    @Transactional
    public void persist(InputSet inputSet) throws Exception {
       System.out.println("inputSet.getInputs().size():" + inputSet.getInputs().size());
       em.persist(inputSet);
    }

}
