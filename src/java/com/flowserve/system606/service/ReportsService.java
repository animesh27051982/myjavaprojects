/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.view.ViewSupport;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.logging.Level;
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
    private CalculationService calculationService;

    @Inject
    private MetricService metricService;
    @Inject
    private ContractService contractService;
    @Inject
    private ViewSupport viewSupport;
    private static final int HEADER_ROW_COUNT = 10;

    public void generateContractEsimatesReport(InputStream inputStream, FileOutputStream outputStream, Contract contract) throws Exception {

        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet worksheet = workbook.getSheet("Contract Summary-1");
        XSSFRow row;
        Cell cell = null;
        int rowid = HEADER_ROW_COUNT;
        XSSFRow rowTitle = worksheet.getRow(1);
        cell = rowTitle.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(contract.getName());

        BigDecimal trancationPrice = viewSupport.getAccumulatedCurrencyMetricValue("TRANSACTION_PRICE", contract);
        BigDecimal loquidatedDamage = viewSupport.getAccumulatedCurrencyMetricValue("LIQUIDATED_DAMAGES", contract);
        BigDecimal EAC = viewSupport.getAccumulatedCurrencyMetricValue("ESTIMATED_COST_AT_COMPLETION", contract);
        BigDecimal estimatedGrossProfit = viewSupport.getAccumulatedCurrencyMetricValue("ESTIMATED_GROSS_PROFIT", contract);
        //BigDecimal estimatedGrossMargin = viewSupport.getAccumulatedCurrencyMetricValue("ESTIMATED_GROSS_MARGIN", contract);
        Logger.getLogger(ReportsService.class.getName()).log(Level.INFO, "message" + estimatedGrossProfit);
        BigDecimal estimatedGrossMargin = new BigDecimal(0);
        if (estimatedGrossProfit.compareTo(BigDecimal.ZERO) > 0) {
            estimatedGrossMargin = estimatedGrossProfit.divide(trancationPrice, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
        }

        row = worksheet.getRow(rowid++);
        cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(trancationPrice.doubleValue());
        cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(loquidatedDamage.doubleValue());
        cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(EAC.doubleValue());
        cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossProfit.doubleValue());
        cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(estimatedGrossMargin.doubleValue());

        workbook.write(outputStream);
        workbook.close();
        inputStream.close();
        outputStream.close();

    }

}
