/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.ExchangeRate;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author shubhamv
 */
@Stateless
public class CurrencyService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    public void persist(Object object) {
        em.persist(object);
    }

    public void copyFile(String destination, String fileName, InputStream in) {
        try {
            // write the inputStream to a FileOutputStream
            OutputStream out = new FileOutputStream(new File(destination + fileName));

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            in.close();
            out.flush();
            out.close();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void ReadFile(String folder, String fileName) {
        try {
            //Create the input stream from the xlsx/xls file
            FileInputStream fis = new FileInputStream(folder + fileName);

            File fout = new File(folder + "currency.txt");
            //Create the file
            if (fout.exists()) {
                fout.delete();
                fout.createNewFile();
            } else {
                fout.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(fout);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            //Create Workbook instance for xlsx/xls file input stream
            Workbook workbook = null;
            if (fileName.toLowerCase().endsWith("xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (fileName.toLowerCase().endsWith("xls")) {
                workbook = new HSSFWorkbook(fis);
            }

            //Get the Specifc sheet in the xlsx file
            Sheet sheet = workbook.getSheet("Oracle Load Rates");

            this.ReadEachRecord(sheet, bw);

            File file = new File(folder + fileName);

            if (file.delete()) {
                //System.out.println("File deleted successfully");
            } else {
                System.out.println("Failed to delete the file");
            }

            bw.close();
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ReadEachRecord(Sheet sheet, BufferedWriter bw) throws IOException {
        //every sheet has rows, iterate over them
        Iterator<Row> rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {
            String name = "";
            String cnt_name = "";
            String type = "";
            String code = "";
            Double usd = null;
            //Get the row object
            Row row = rowIterator.next();
            if (row.getRowNum() == 0) {
                continue;
            }
            //Every row has columns, get the column iterator and iterate over them
            Iterator<Cell> cellIterator = row.cellIterator();

            while (cellIterator.hasNext()) {
                //Get the Cell object
                Cell cell = cellIterator.next();

                //check the cell type and process accordingly
                switch (cell.getCellType()) {
                    case Cell.CELL_TYPE_STRING:
                        if (cnt_name.equalsIgnoreCase("")) {
                            cnt_name = cell.getStringCellValue().trim();
                        } else if (name.equalsIgnoreCase("")) {
                            //2nd column
                            name = cell.getStringCellValue().trim();
                        } else if (type.equalsIgnoreCase("")) {
                            //3rd column
                            type = cell.getStringCellValue().trim();
                        } else if (code.equalsIgnoreCase("")) {
                            //4th column
                            code = cell.getStringCellValue().trim();
                        } else {

                        }
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        //5th column
                        usd = cell.getNumericCellValue();
                }
            } //end of cell iterator

            bw.newLine();
            bw.write(name + "\t" + cnt_name + "\t" + type + "\t" + code + "\t" + usd);
        } //end of rows iterator

    }

    public List<ExchangeRate> findRatesByNextDate() throws Exception {
        Query query = em.createQuery("SELECT er FROM ExchangeRate er WHERE er.effectiveDate <= :EFFECTIVE_DATE ");
        LocalDate ld = LocalDate.now();
        query.setParameter("EFFECTIVE_DATE", ld);
        return (List<ExchangeRate>) query.getResultList();
    }

    public void deleteExchangeRate() throws Exception {
        em.createQuery("DELETE FROM ExchangeRate e").executeUpdate();

    }

    public void updater(ExchangeRate eRate) throws Exception {

        em.persist(eRate);

    }
}
