/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.InputTypeName;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author shubhamc
 */
@Stateless
public class ReportsService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    private static Logger logger = Logger.getLogger("com.flowserve.system606");
    @Inject
    PerformanceObligationService performanceObligationService;

    @Inject
    private InputService inputService;
    private static final int HEADER_ROW_COUNT = 10;

    public void generateContractEsimatesReport(InputStream inputStream, FileOutputStream outputStream, List<ReportingUnit> reportingUnits) throws Exception {

        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet worksheet = workbook.getSheet("Contract Summary-1");
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        List<Contract> contracts = reportingUnits.get(0).getContracts();
        for (Contract contract : contracts) {
            List<PerformanceObligation> pobs = contract.getPerformanceObligations();
            for (PerformanceObligation pob : pobs) {
                row = worksheet.getRow(rowid++);

                // Populate non-input cells
                row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(contract.getName());
                InputType inputType = inputService.findInputTypeByName(InputTypeName.TRANSACTION_PRICE);
                cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                if (pob.getCurrencyInput(inputType.getName()).getValue() != null) {
                    cell.setCellValue(pob.getCurrencyInputValue(inputType.getName()).doubleValue());
                }
                inputType = inputService.findInputTypeByName(InputTypeName.LIQUIDATED_DAMAGES);
                cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                if (pob.getCurrencyInput(inputType.getName()).getValue() != null) {
                    cell.setCellValue(pob.getCurrencyInputValue(inputType.getName()).doubleValue());
                }
                inputType = inputService.findInputTypeByName(InputTypeName.ESTIMATED_COST_AT_COMPLETION);
                cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                if (pob.getCurrencyInput(inputType.getName()).getValue() != null) {
                    cell.setCellValue(pob.getCurrencyInputValue(inputType.getName()).doubleValue());
                }

            }
        }

        workbook.write(outputStream);
        workbook.close();
        inputStream.close();
        outputStream.close();

    }

}
