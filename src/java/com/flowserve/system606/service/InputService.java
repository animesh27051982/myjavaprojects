/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import java.io.InputStream;
import java.util.Iterator;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
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

    public boolean readFeed(InputStream fis) throws Exception {  // Need an application exception type defined.
       
        System.out.println("readFeed:" + fis.toString());
               
        XSSFRow row;
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        XSSFSheet spreadsheet = workbook.getSheetAt(0);
        Iterator < Row >  rowIterator = spreadsheet.iterator();

        while (rowIterator.hasNext()) {
           row = (XSSFRow) rowIterator.next();
           Iterator < Cell >  cellIterator = row.cellIterator();

           while ( cellIterator.hasNext()) {
              Cell cell = cellIterator.next();

              switch (cell.getCellType()) {
                 case Cell.CELL_TYPE_NUMERIC:
                    System.out.print(cell.getNumericCellValue() + " \t\t ");
                    break;

                 case Cell.CELL_TYPE_STRING:
                    System.out.print(
                    cell.getStringCellValue() + " \t\t ");
                    break;
              }
           }
           System.out.println();
        }
        fis.close();           
        
        return true;
    }
    
    // TODO: persist in INPUT table
//    public void persist(Input input) throws Exception {
//       em.persist(input);
//    }

}
