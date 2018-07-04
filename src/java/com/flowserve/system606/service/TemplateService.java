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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
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
    PerformanceObligationService performanceObligationService;
    @Inject
    BusinessRuleService businessRuleService;
    private static final int HEADER_ROW_COUNT = 3;

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
                            if (pob.getCurrencyInput(inputType.getName()) != null) {
                                cell.setCellValue(pob.getCurrencyInputValue(inputType.getName()).doubleValue());
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

            Logger.getLogger(InputService.class.getName()).log(Level.FINER, "Processing POB: " + NumberToTextConverter.toText(pobIdCell.getNumericCellValue()));

            PerformanceObligation pob = performanceObligationService.findById((long) pobIdCell.getNumericCellValue());
            if (pob == null) {
                throw new IllegalStateException("Input file invalid.  Invalid POB at row: " + row.getRowNum());
            }

            for (InputType inputType : inputTypes) {
                Cell cell = row.getCell(CellReference.convertColStringToIndex(inputType.getExcelCol()));
                if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
                    // TODO - figure out what to do in this blank case.  It will depend on the situation.
                    Logger.getLogger(InputService.class.getName()).log(Level.FINER, "Encountered an empty cell at row: " + row.getRowNum() + " Cell: " + inputType.getExcelCol());
                    continue;
                }
                if ("com.flowserve.system606.model.CurrencyInput".equals(inputType.getInputClass())) {
                    Logger.getLogger(TemplateService.class.getName()).log(Level.FINER, "Upload processing row: " + row.getRowNum() + " cell: " + cell.getColumnIndex());
                    Logger.getLogger(TemplateService.class.getName()).log(Level.FINER, "Upload Processing. getCurrencyInp: " + cell.getNumericCellValue());
                    pob.getCurrencyInput(inputType.getName()).setValue(new BigDecimal(NumberToTextConverter.toText(cell.getNumericCellValue())));
                }
//                if ("com.flowserve.system606.model.StringInput".equals(inputType.getInputClass())) {
//                    pob.getStringInput(inputType.getId()).setValue(cell.getStringCellValue());
//                }
//                if ("com.flowserve.system606.model.DateInput".equals(inputType.getInputClass())) {
//                    pob.getDateInput(inputType.getId()).setValue(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
//                }
            }

            businessRuleService.executeBusinessRules(pob);
            pob = performanceObligationService.update(pob);
        }

        fis.close();

        // going to change inputset to be pob level history only.  removing for now.
        //inputSet.setInputs(inputList);
        //persist(inputSet);
    }

}
