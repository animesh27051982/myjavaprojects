/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.InputSet;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.view.PobInput;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Stateless
public class TemplateService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    @Inject
    InputService inputService;
    @Inject
    PerformanceObligationService pobService;
    @Inject
    BusinessRuleService businessRuleService;
    private static final int HEADER_ROW_COUNT = 2;
    private InputStream inputStream;

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public List<ReportingUnit> getReportingUnits() {
        return new ArrayList<ReportingUnit>();
    }

    public void processTemplateDownload(InputStream inputStream, FileOutputStream outputStream, List<ReportingUnit> reportingUnits) throws Exception {

        List<InputType> inputTypes = inputService.findActiveInputTypesPob();
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet worksheet = workbook.getSheetAt(0);
        XSSFRow row;
        Cell cell = null;

        int rowid = HEADER_ROW_COUNT;
        for (ReportingUnit ru : reportingUnits) {
            List<Contract> contracts = ru.getContracts();
            for (Contract contract : contracts) {
                List<PerformanceObligation> pobs = contract.getPerformanceObligations();
                for (PerformanceObligation pob : pobs) {
                    row = worksheet.getRow(rowid++);

                    // Populate non-input cells
                    row.getCell(0).setCellValue("AMSS");
                    row.getCell(1).setCellValue(ru.getCode());
                    row.getCell(2).setCellValue(contract.getId());
                    row.getCell(3).setCellValue(contract.getName());  // TODO - Need customer name?
                    row.getCell(4).setCellValue(contract.getSalesOrderNumber());
                    row.getCell(5).setCellValue(pob.getName());
                    row.getCell(6).setCellValue(pob.getId());
                    row.getCell(7).setCellValue(pob.getRevRecMethod());

                    for (InputType inputType : inputTypes) {
                        cell = row.getCell(CellReference.convertColStringToIndex(inputType.getExcelCol()));
                        if ("com.flowserve.system606.model.CurrencyInput".equals(inputType.getInputClass())) {
                            //if (pob.getCurrencyInput(inputType.getName()) != null) {
                            if (pobService.getCurrencyInput(inputType.getId(), pob) != null) {
                                //cell.setCellValue(pob.getCurrencyInputValue(inputType.getName()).doubleValue());
                                cell.setCellValue(pobService.getCurrencyInputValue(inputType.getId(), pob).doubleValue());
                            }
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

    public void processTemplateUpload(InputStream fis, String filename) throws Exception {  // Need an application exception type defined.
        try {
            List<InputType> inputTypes = inputService.findActiveInputTypesPob();
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet worksheet = workbook.getSheetAt(0);
            InputSet inputSet = new InputSet();
            inputSet.setFilename(filename);
            int pobIdColNumber = CellReference.convertColStringToIndex("G");

            if (worksheet == null) {
                throw new IllegalStateException("Invalid xlsx file.  Report detail to user");
            }

            Logger.getLogger(TemplateService.class.getName()).log(Level.INFO, "Processing POB input template: " + filename);
            for (Row row : worksheet) {
                if (row.getRowNum() < HEADER_ROW_COUNT) {
                    continue;
                }
                Cell pobIdCell = row.getCell(pobIdColNumber);
                if (pobIdCell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK) {
                    Logger.getLogger(InputService.class.getName()).log(Level.FINE, "POB input template processing complete.");  // TODO - figure out if we really want to stop here.
                    break;
                }
                if (pobIdCell.getCellTypeEnum() != CellType.NUMERIC) { //  TODO - Need a mechansim to report exact error to user.
                    throw new IllegalStateException("Input file invalid.  POB ID column not a numeric");
                }

                Logger.getLogger(InputService.class.getName()).log(Level.INFO, "Processing POB: " + NumberToTextConverter.toText(pobIdCell.getNumericCellValue()));

                PerformanceObligation pob = pobService.findById((long) pobIdCell.getNumericCellValue());
                if (pob == null) {
                    throw new IllegalStateException("Input file invalid.  Invalid POB at row: " + row.getRowNum());
                }

                for (InputType inputType : inputTypes) {
                    Cell cell = row.getCell(CellReference.convertColStringToIndex(inputType.getExcelCol()));
                    try {
                        if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
                            // TODO - figure out what to do in this blank case.  It will depend on the situation.
                            Logger.getLogger(InputService.class.getName()).log(Level.INFO, "Encountered an empty cell at row: " + row.getRowNum() + " Cell: " + inputType.getExcelCol());
                            continue;
                        }
                        if ("com.flowserve.system606.model.CurrencyInput".equals(inputType.getInputClass())) {
                            Logger.getLogger(TemplateService.class.getName()).log(Level.INFO, "Upload processing row: " + row.getRowNum() + " cell: " + cell.getColumnIndex());
                            Logger.getLogger(TemplateService.class.getName()).log(Level.INFO, "Upload Processing. getCurrencyInp: " + cell.getNumericCellValue());
                            //pob.getCurrencyInput(inputType.getName()).setValue(new BigDecimal(NumberToTextConverter.toText(cell.getNumericCellValue())));
                            pobService.getCurrencyInput(inputType.getId(), pob).setValue(new BigDecimal(NumberToTextConverter.toText(cell.getNumericCellValue())));
                        }
                        if ("com.flowserve.system606.model.StringInput".equals(inputType.getInputClass())) {
                            Logger.getLogger(TemplateService.class.getName()).log(Level.INFO, "Upload processing row: " + row.getRowNum() + " cell: " + cell.getColumnIndex());
                            Logger.getLogger(TemplateService.class.getName()).log(Level.INFO, "Upload Processing. getCurrencyInp: " + cell.getStringCellValue());

                            //pob.getStringInput(inputType.getName()).setValue(cell.getStringCellValue());
                            pobService.getStringInput(inputType.getId(), pob).setValue(cell.getStringCellValue());

                        }
                        if ("com.flowserve.system606.model.DateInput".equals(inputType.getInputClass())) {
                            Logger.getLogger(TemplateService.class.getName()).log(Level.INFO, "Upload processing row: " + row.getRowNum() + " cell: " + cell.getColumnIndex());
                            Logger.getLogger(TemplateService.class.getName()).log(Level.INFO, "Upload Processing. getCurrencyInp: " + cell.getDateCellValue());

                            //pob.getDateInput(inputType.getName()).setValue(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                            pobService.getDateInput(inputType.getId(), pob).setValue(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        }
                    } catch (Exception rce) {
                        throw new Exception("processTemplateUpload row: " + row.getRowNum() + " cell:" + cell.getColumnIndex() + " " + rce.getMessage());
                    }
                }

                businessRuleService.executeBusinessRules(pob);
                pob = pobService.update(pob);
            }
        } catch (Exception e) {
            throw new Exception("processTemplateUpload: " + e.getMessage());
        } finally {
            fis.close();
        }

        // going to change inputset to be pob level history only.  removing for now.
        //inputSet.setInputs(inputList);
        //persist(inputSet);
    }

    public void reportingPreparersList() throws Exception {
        Logger.getLogger(InputService.class.getName()).log(Level.INFO, "Start: ");
        String folder = "D:/Users/shubhamv/Documents/NetBeansProjects/FlowServe/src/java/resources/app_data_init_files/";
        try {

            File fout = new File(folder + "preparers_list.txt");
            //Create the file
            if (fout.exists()) {
                fout.delete();
                fout.createNewFile();
            } else {
                fout.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(fout);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            //BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            inputStream = PobInput.class.getResourceAsStream("/resources/excel_input_templates/User Access - 02JUL2018.xlsx");
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet worksheet = workbook.getSheet("User Profiles 27JUN2018");
            Iterator<Row> rowIterator = worksheet.iterator();
            while (rowIterator.hasNext()) {
                String fsl_id = "";
                String id = "";
                String f_name = "";
                String l_name = "";
                String email = "";
                String role = "";
                String group = "";
                //Get the row object
                Row row = rowIterator.next();
                if (row.getRowNum() == 0 || row.getRowNum() == 1 || row.getRowNum() == 2) {
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
                            if (fsl_id.equalsIgnoreCase("")) {
                                fsl_id = cell.getStringCellValue().trim();
                            } else if (id.equalsIgnoreCase("")) {
                                //2nd column
                                id = cell.getStringCellValue().trim();
                            } else if (f_name.equalsIgnoreCase("")) {
                                //3rd column
                                f_name = cell.getStringCellValue().trim();
                            } else if (l_name.equalsIgnoreCase("")) {
                                //4th column
                                l_name = cell.getStringCellValue().trim();
                            } else if (email.equalsIgnoreCase("")) {
                                //5th column
                                email = cell.getStringCellValue().trim();
                            } else if (role.equalsIgnoreCase("")) {
                                //6th column
                                role = cell.getStringCellValue().trim();
                            } else if (group.equalsIgnoreCase("")) {
                                //7th column
                                String gr = cell.getStringCellValue().trim();
                                String[] splitStr = gr.split(" ");
                                int len = splitStr.length;
                                for (int i = 0; i < len; i++) {

                                    if (splitStr[i].startsWith("RU")) {
                                        if (group.equalsIgnoreCase("")) {
                                            group = splitStr[i].replace("RU", "");
                                        } else {
                                            group = group + "," + splitStr[i].replace("RU", "");
                                        }

                                    }

                                }

                            }
                            break;

                    }

                } //end of cell iterator
                //Logger.getLogger(InputService.class.getName()).log(Level.INFO, "Second: " + group);
                bw.newLine();
                bw.write(fsl_id + "\t" + id + "\t" + f_name + "\t" + l_name + "\t" + email + "\t" + role + "\t" + group);
            }
            // bw.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
