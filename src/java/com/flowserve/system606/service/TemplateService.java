/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.CurrencyEvent;
import com.flowserve.system606.model.Event;
import com.flowserve.system606.model.EventType;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.MetricSet;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.PerformanceObligation;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.view.PobInput;
import com.flowserve.system606.view.ViewSupport;
import com.flowserve.system606.web.WebSession;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
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
    private MetricService metricService;
    @Inject
    private EventService eventService;
    @Inject
    private CalculationService calculationService;
    @Inject
    private PerformanceObligationService pobService;
    @Inject
    private ContractService contractService;
    @Inject
    FinancialPeriodService financialPeriodService;
    @Inject
    CurrencyService currencyService;
    @Inject
    AdminService adminService;
    @Inject
    ViewSupport viewSupport;
    private static final int HEADER_ROW_COUNT = 2;
    private InputStream inputStream;
    @Inject
    private WebSession webSession;
    private EventType billingEventType;

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    @PostConstruct
    public void init() {
        billingEventType = eventService.getEventTypeByCode("BILLING_EVENT_CC");
    }

    public List<ReportingUnit> getReportingUnits() {
        return new ArrayList<ReportingUnit>();
    }

    public void processTemplateDownload(InputStream inputStream, FileOutputStream outputStream, ReportingUnit reportingUnit) throws Exception {

        List<MetricType> metricTypes = metricService.getAllPobExcelInputMetricTypes();
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet worksheet = workbook.getSheetAt(0);
        XSSFRow row;
        Cell cell = null;
        // TODO - This needs to be read from the file and then checked to make sure it's open.
        FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();

        int rowid = 3;
        List<Contract> contracts = reportingUnit.getContracts();
        for (Contract contract : contracts) {
            List<PerformanceObligation> pobs = contract.getPerformanceObligations();
            int pobCount = 1;
            for (PerformanceObligation pob : pobs) {
                row = worksheet.getRow(rowid);
                if (row == null) {
                    row = worksheet.createRow(rowid);
                }

                // Populate non-input cells
                cell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(reportingUnit.getCode());
                cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(contract.getId());
                //TODO
//                cell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//                cell.setCellValue(contract.getCustomer().getName());
                cell = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(contract.getSalesOrderNumber());
                cell = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getDescription());
                cell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getId());
                cell = row.getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(pob.getRevenueMethod().getShortName() == null ? "" : pob.getRevenueMethod().getShortName());
                cell = row.getCell(7, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(contract.getContractCurrency().getCurrencyCode());

                BigDecimal value = BigDecimal.ZERO;
                value = calculationService.getCurrencyMetric("TRANSACTION_PRICE_CC", pob, period).getCcValue();
                setCellValue(row, 9, value);
                value = calculationService.getCurrencyMetric("LIQUIDATED_DAMAGES_CTD_CC", pob, period).getCcValue();
                setCellValue(row, 10, value);
                setStringCellValue(row, 11, contract.getLocalCurrency().getCurrencyCode());
                value = calculationService.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", pob, period).getLcValue();
                setCellValue(row, 12, value);
                value = calculationService.getCurrencyMetric("ESTIMATED_COST_AT_COMPLETION_LC", pob, webSession.getPriorPeriod()).getLcValue();
                setCellValue(row, 13, value);

                value = calculationService.getCurrencyMetric("LOCAL_COSTS_CTD_LC", pob, period).getLcValue();
                setCellValue(row, 15, value);
                value = calculationService.getCurrencyMetric("THIRD_PARTY_COSTS_CTD_LC", pob, period).getLcValue();
                setCellValue(row, 16, value);
                value = calculationService.getCurrencyMetric("INTERCOMPANY_COSTS_CTD_LC", pob, period).getLcValue();
                setCellValue(row, 17, value);

                value = calculationService.getCurrencyMetric("COSTS_INCURRED_CTD_LC", pob, period).getLcValue();
                setCellValue(row, 18, value);
                LocalDate dDate = calculationService.getDateMetric("DELIVERY_DATE", pob, period).getValue();
                setDateCellValue(row, 19, dDate);
                value = calculationService.getCurrencyMetric("PARTIAL_SHIPMENT_COSTS_LC", pob, period).getLcValue();
                setCellValue(row, 20, value);
                if (pobCount == 1) {
                    value = calculationService.getCurrencyMetric("THIRD_PARTY_COMMISSION_CTD_LC", contract, period).getLcValue();
                    setCellValue(row, 31, value);
                }
                String val = calculationService.getStringMetric("SALES_DESTINATION", pob, period).getValue();
                setStringCellValue(row, 32, val);
                val = calculationService.getStringMetric("OEAM_DISAGG", pob, period).getValue();
                setStringCellValue(row, 33, val);
                dDate = calculationService.getDateMetric("SL_START_DATE", pob, period).getValue();
                setDateCellValue(row, 34, dDate);
                dDate = calculationService.getDateMetric("SL_END_DATE", pob, period).getValue();
                setDateCellValue(row, 35, dDate);

                EventType billingEventType = eventService.getEventTypeByCode("BILLING_EVENT_CC");
                List<Event> bEv = calculationService.getAllEventsByPeriodAndEventType(contract, billingEventType, period);
                if (pobCount == 1) {
                    int cnt = 1;
                    for (Event bv : bEv) {
                        CurrencyEvent b = (CurrencyEvent) bv;
                        //BigDecimal billValue = BigDecimal.ZERO;
                        if (cnt == 1) {
                            String invoiceNum = b.getNumber();
                            if (invoiceNum != null) {
                                cell = row.getCell(22, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                cell.setCellValue(invoiceNum);
                            }
                            if (b.getEventDate() != null) {
                                String date = b.getEventDate().toString();

                                cell = row.getCell(23, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                cell.setCellValue(date);
                            }

                            BigDecimal billValue = b.getCcValue();
                            setCellValue(row, 24, billValue);
                            billValue = b.getLcValue();
                            setCellValue(row, 25, billValue);

                        } else if (cnt == 2) {
                            String invoiceNum = b.getNumber();
                            if (invoiceNum != null) {
                                cell = row.getCell(26, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                cell.setCellValue(invoiceNum);
                            }
                            if (b.getEventDate() != null) {
                                String date = b.getEventDate().toString();
                                cell = row.getCell(27, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                cell.setCellValue(date);
                            }

                            BigDecimal billValue = b.getCcValue();
                            setCellValue(row, 28, billValue);
                            billValue = b.getLcValue();
                            setCellValue(row, 29, billValue);

                        }

                        cnt++;
                    }
                }

                if (bEv.size() > 2) {
                    cell = row.getCell(CellReference.convertColStringToIndex("W"));
                    setCellComment(cell, "More than 2 Billinngs, use Billings UI");
                    cell = row.getCell(CellReference.convertColStringToIndex("AA"));
                    setCellComment(cell, "More than 2 Billinngs, use Billings UI");
                }
                rowid++;
                pobCount++;
            }
        }
        ((XSSFSheet) worksheet).getCTWorksheet().getSheetViews().getSheetViewArray(0).setTopLeftCell("A1");
        ((XSSFSheet) worksheet).setActiveCell(new CellAddress("A4"));
        workbook.write(outputStream);
        workbook.close();
        inputStream.close();
        outputStream.close();
    }

    protected void setCellComment(Cell cell, String message) {
        Drawing drawing = cell.getSheet().createDrawingPatriarch();
        CreationHelper factory = cell.getSheet().getWorkbook()
                .getCreationHelper();
        // When the comment box is visible, have it show in a 1x3 space
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 2);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 2);
        anchor.setDx1(100);
        anchor.setDx2(100);
        anchor.setDy1(100);
        anchor.setDy2(100);

        // Create the comment and set the text+author
        Comment comment = drawing.createCellComment(anchor);
        RichTextString str = factory.createRichTextString(message);
        comment.setString(str);
        comment.setAuthor("POCI_Template_RU");
        // Assign the comment to the cell
        cell.setCellComment(comment);
    }

    public void processTemplateUpload(InputStream fis, String filename) throws Exception {  // Need an application exception type defined.
        try {

            // TODO - This needs to be read from the file and then checked to make sure it's open.
            FinancialPeriod period = financialPeriodService.getCurrentFinancialPeriod();

            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet worksheet = workbook.getSheetAt(0);
            MetricSet inputSet = new MetricSet();
            inputSet.setFilename(filename);
            int pobIdColNumber = CellReference.convertColStringToIndex("F");

            if (worksheet == null) {
                throw new IllegalStateException("Invalid xlsx file.  Report detail to user");
            }

            Logger.getLogger(TemplateService.class.getName()).log(Level.INFO, "Processing POB input template: " + filename);
            for (Row row : worksheet) {
                if (row.getRowNum() < 3) {
                    continue;
                }
                Cell pobIdCell = row.getCell(pobIdColNumber);
                if (pobIdCell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK) {
                    Logger.getLogger(MetricService.class.getName()).log(Level.FINE, "POB input template processing complete.");  // TODO - figure out if we really want to stop here.
                    break;
                }
                if (pobIdCell.getCellTypeEnum() != CellType.NUMERIC) { //  TODO - Need a mechansim to report exact error to user.
                    throw new IllegalStateException("Input file invalid.  POB ID column not a numeric");
                }

                Logger.getLogger(MetricService.class.getName()).log(Level.INFO, "Processing POB: " + NumberToTextConverter.toText(pobIdCell.getNumericCellValue()));

                PerformanceObligation pob = pobService.findById((long) pobIdCell.getNumericCellValue());
                if (pob == null) {
                    throw new IllegalStateException("Input file invalid.  Invalid POB at row: " + row.getRowNum());
                }
                Cell cell = null;
                try {
                    cell = row.getCell(CellReference.convertColStringToIndex("J"));
                    setValueInMetricType(cell, pobIdCell, "TRANSACTION_PRICE_CC", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("K"));
                    setValueInMetricType(cell, pobIdCell, "LIQUIDATED_DAMAGES_CTD_CC", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("M"));
                    setValueInMetricType(cell, pobIdCell, "ESTIMATED_COST_AT_COMPLETION_LC", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("N"));
                    setValueInMetricType(cell, pobIdCell, "ESTIMATED_COST_AT_COMPLETION_LC", pob, webSession.getPriorPeriod());
                    cell = row.getCell(CellReference.convertColStringToIndex("P"));
                    setValueInMetricType(cell, pobIdCell, "LOCAL_COSTS_CTD_LC", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("Q"));
                    setValueInMetricType(cell, pobIdCell, "THIRD_PARTY_COSTS_CTD_LC", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("R"));
                    setValueInMetricType(cell, pobIdCell, "INTERCOMPANY_COSTS_CTD_LC", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("S"));
                    setValueInMetricType(cell, pobIdCell, "COSTS_INCURRED_CTD_LC", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("T"));
                    setValueInDate(cell, pobIdCell, "DELIVERY_DATE", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("U"));
                    setValueInMetricType(cell, pobIdCell, "PARTIAL_SHIPMENT_COSTS_LC", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("AF"));
                    setValueInMetricType(cell, pobIdCell, "THIRD_PARTY_COMMISSION_CTD_LC", pob.getContract(), period);
                    cell = row.getCell(CellReference.convertColStringToIndex("AG"));
                    setValueInStringMetricType(cell, pobIdCell, "SALES_DESTINATION", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("AH"));
                    setValueInStringMetricType(cell, pobIdCell, "OEAM_DISAGG", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("AI"));
                    setValueInDate(cell, pobIdCell, "SL_START_DATE", pob, period);
                    cell = row.getCell(CellReference.convertColStringToIndex("AJ"));
                    setValueInDate(cell, pobIdCell, "SL_END_DATE", pob, period);
                } catch (Exception rce) {
                    Logger.getLogger(TemplateService.class.getName()).log(Level.SEVERE, "Error processing ");
                    throw new Exception("processTemplateUpload row: " + row.getRowNum() + " cell: " + (cell.getColumnIndex() + 1) + " " + rce.getMessage());
                }
                try {
                    // Process first billing event
                    CurrencyEvent billingEvent = new CurrencyEvent();
                    billingEvent.setEventType(billingEventType);
                    billingEvent.setFinancialPeriod(period);
                    billingEvent.setContract(pob.getContract());

                    cell = row.getCell(CellReference.convertColStringToIndex("W"));
                    if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
                        // TODO - figure out what to do in this blank case.  It will depend on the situation.
                    } else {
                        if (cell.getCellTypeEnum().toString().equalsIgnoreCase("STRING")) {
                            billingEvent.setNumber(cell.getStringCellValue());
                        } else if (cell.getCellTypeEnum().toString().equalsIgnoreCase("NUMERIC")) {
                            billingEvent.setNumber(NumberToTextConverter.toText(cell.getNumericCellValue()));
                        }

                        cell = row.getCell(CellReference.convertColStringToIndex("X"));
                        if (cell.getCellTypeEnum().toString().equalsIgnoreCase("STRING")) {
                            billingEvent.setEventDate(LocalDate.parse(cell.getStringCellValue()));
                        } else if (cell.getCellTypeEnum().toString().equalsIgnoreCase("NUMERIC")) {
                            billingEvent.setEventDate(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        }

                        cell = row.getCell(CellReference.convertColStringToIndex("Y"));
                        if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
                            // TODO - figure out what to do in this blank case.  It will depend on the situation.
                        } else {
                            billingEvent.setCcValue(new BigDecimal(NumberToTextConverter.toText(cell.getNumericCellValue())));
                        }

                        cell = row.getCell(CellReference.convertColStringToIndex("Z"));
                        if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
                            // TODO - figure out what to do in this blank case.  It will depend on the situation.
                        } else {
                            billingEvent.setLcValue(new BigDecimal(NumberToTextConverter.toText(cell.getNumericCellValue())));
                        }

                        if (billingEventDoesNotExistInContract(pob.getContract(), billingEvent.getNumber())) {
                            billingEvent.setName("Billing " + billingEvent.getNumber());
                            billingEvent = (CurrencyEvent) eventService.update(billingEvent);
                            calculationService.addEvent(pob.getContract(), period, billingEvent);
                            contractService.update(pob.getContract());
                        }
                    }

                    // Process Second billing event
                    billingEvent = new CurrencyEvent();
                    billingEvent.setEventType(billingEventType);
                    billingEvent.setFinancialPeriod(period);
                    billingEvent.setContract(pob.getContract());

                    cell = row.getCell(CellReference.convertColStringToIndex("AA"));
                    if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
                        // TODO - figure out what to do in this blank case.  It will depend on the situation.
                    } else {
                        if (cell.getCellTypeEnum().toString().equalsIgnoreCase("STRING")) {
                            billingEvent.setNumber(cell.getStringCellValue());
                        } else if (cell.getCellTypeEnum().toString().equalsIgnoreCase("NUMERIC")) {
                            billingEvent.setNumber(NumberToTextConverter.toText(cell.getNumericCellValue()));
                        }

                        cell = row.getCell(CellReference.convertColStringToIndex("AB"));
                        if (cell.getCellTypeEnum().toString().equalsIgnoreCase("STRING")) {
                            billingEvent.setEventDate(LocalDate.parse(cell.getStringCellValue()));
                        } else if (cell.getCellTypeEnum().toString().equalsIgnoreCase("NUMERIC")) {
                            billingEvent.setEventDate(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        }
                        cell = row.getCell(CellReference.convertColStringToIndex("AC"));
                        if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
                            // TODO - figure out what to do in this blank case.  It will depend on the situation.
                        } else {
                            billingEvent.setCcValue(new BigDecimal(NumberToTextConverter.toText(cell.getNumericCellValue())));
                        }
                        cell = row.getCell(CellReference.convertColStringToIndex("AD"));
                        if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
                            // TODO - figure out what to do in this blank case.  It will depend on the situation.
                        } else {
                            billingEvent.setLcValue(new BigDecimal(NumberToTextConverter.toText(cell.getNumericCellValue())));
                        }

                        if (billingEventDoesNotExistInContract(pob.getContract(), billingEvent.getNumber())) {//billingEvent.getName()
                            billingEvent.setName("Billing " + billingEvent.getNumber());
                            billingEvent = (CurrencyEvent) eventService.update(billingEvent);
                            calculationService.addEvent(pob.getContract(), period, billingEvent);
                            contractService.update(pob.getContract());
                        }
                    }

                } catch (Exception rce) {
                    throw new Exception("processTemplateUpload row: " + row.getRowNum() + " cell:" + cell.getColumnIndex() + " " + rce.getMessage());
                }

                calculationService.executeBusinessRules(pob, webSession.getCurrentPeriod());
                pob.setLastUpdatedBy(webSession.getUser());
                pob.getContract().setLastUpdatedBy(webSession.getUser());
                pob.setLastUpdateDate(LocalDateTime.now());
                pob.getContract().setLastUpdateDate(LocalDateTime.now());

                pob = pobService.update(pob);
            }
        } catch (Exception e) {
            throw new Exception("processTemplateUpload: " + e.getMessage());
        } finally {
            fis.close();
        }
    }

    private boolean billingEventDoesNotExistInContract(Contract contract, String invoiceNumber) {
        return contract.getAllEventsByEventTypeAndNumber(billingEventType, invoiceNumber).isEmpty();
    }

    public void reportingPreparersList() throws Exception {
        Logger.getLogger(MetricService.class.getName()).log(Level.INFO, "Start: ");
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

    private boolean currencyMetricIsNotNull(MetricType metricType, Measurable measurable, FinancialPeriod period) throws Exception {
        return calculationService.getCurrencyMetric(metricType.getCode(), measurable, period) != null
                && calculationService.getCurrencyMetric(metricType.getCode(), measurable, period).getValue() != null;
    }

    private void setCellValue(XSSFRow row, int cellNum, BigDecimal value) {
        if (value != null) {
            Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            CellStyle currentStyle = cell.getCellStyle();
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(currentStyle);
        }

    }

    private void setStringCellValue(XSSFRow row, int cellNum, String value) {
        if (value != null) {
            Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            CellStyle currentStyle = cell.getCellStyle();
            cell.setCellValue(value);
            cell.setCellStyle(currentStyle);
        }

    }

    private void setDateCellValue(XSSFRow row, int cellNum, LocalDate value) {
        if (value != null) {
            Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            CellStyle currentStyle = cell.getCellStyle();
            cell.setCellValue(value.toString());
            cell.setCellStyle(currentStyle);
        }

    }

    private void setValueInMetricType(Cell cell, Cell pobIdCell, String type, Measurable measurable, FinancialPeriod period) throws Exception {
        if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
            // TODO - figure out what to do in this blank case.  It will depend on the situation.
        } else {
            calculationService.getCurrencyMetric(type, measurable, period).setValue(new BigDecimal(NumberToTextConverter.toText(cell.getNumericCellValue())));

        }

    }

    private void setValueInStringMetricType(Cell cell, Cell pobIdCell, String type, PerformanceObligation pob, FinancialPeriod period) throws Exception {
        if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
            // TODO - figure out what to do in this blank case.  It will depend on the situation.
        } else {
            if (cell.getCellTypeEnum().toString().equalsIgnoreCase("STRING")) {
                calculationService.getStringMetric(type, pob, period).setValue(cell.getStringCellValue());
            }

        }

    }

    private void setValueInDate(Cell cell, Cell pobIdCell, String type, Measurable measurable, FinancialPeriod period) throws Exception {
        if (cell == null || pobIdCell.getCellTypeEnum() == CellType.BLANK || ((XSSFCell) cell).getRawValue() == null) {
            // TODO - figure out what to do in this blank case.  It will depend on the situation.
        } else {
            if (cell.getCellTypeEnum().toString().equalsIgnoreCase("STRING")) {
                calculationService.getDateMetric(type, measurable, period).setValue(LocalDate.parse(cell.getStringCellValue()));
            } else if (cell.getCellTypeEnum().toString().equalsIgnoreCase("NUMERIC")) {
                calculationService.getDateMetric(type, measurable, period).setValue(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

            }
        }
    }
}
