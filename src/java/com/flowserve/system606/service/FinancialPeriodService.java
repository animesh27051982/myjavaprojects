package com.flowserve.system606.service;

import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.model.PeriodStatus;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import com.flowserve.system606.model.WorkflowContext;
import com.flowserve.system606.model.WorkflowStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author kgraves
 */
@Stateless
public class FinancialPeriodService {

    @Inject
    private AdminService adminService;
    @Inject
    private CalculationService calculationService;
    @Inject
    private ReportingUnitService reportingUnitService;
    private Company company;

    private static final Logger logger = Logger.getLogger(FinancialPeriodService.class.getName());

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    private DateTimeFormatter periodNameFormatter = DateTimeFormatter.ofPattern("MMM-yy");
    private String[] monthNames = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

    @PostConstruct
    public void init() {
        company = adminService.findCompanyById("FLS");
    }

    public FinancialPeriod findById(String id) {
        return em.find(FinancialPeriod.class, id);
    }

    public void persist(FinancialPeriod fp) throws Exception {
        em.persist(fp);
    }

    public void update(FinancialPeriod fp) throws Exception {
        em.merge(fp);
    }

    public List<FinancialPeriod> findAllPeriods() {
        Query query = em.createQuery("SELECT p FROM FinancialPeriod p ORDER BY p.sequence DESC");
        return (List<FinancialPeriod>) query.getResultList();
    }

    public FinancialPeriod findByNumericString(String numericString) {
        String[] tokens = numericString.split("-");
        String year = tokens[0];
        int month = Integer.parseInt(tokens[1]);
        String finalYear = year.substring(year.length() - 2);
        String alphaNumbericPeriod = monthNames[month - 1] + "-" + finalYear;

        return findById(alphaNumbericPeriod);
    }

    public void initFinancialPeriods(User user) throws Exception {
        logger.info("Initializing FinancialPeriods");
        String[] shortMonth = {"JAN", "FEB",
            "MAR", "APR", "MAY", "JUN", "JUL",
            "AUG", "SEP", "OCT", "NOV",
            "DEC"};
        Integer[] totalYear = {2017, 2018};
        FinancialPeriod priorPeriod = null;
        for (int i = 0; i < totalYear.length; i++) {
            for (int j = 1; j <= 12; j++) {
                String yrStr = Integer.toString(totalYear[i]);
                String shortYear = yrStr.substring(yrStr.length() - 2);
                String exPeriod = shortMonth[j - 1] + "-" + shortYear;
                if (findById(exPeriod) == null) {
                    LocalDate date = LocalDate.of(totalYear[i], Month.of(j), 1);
                    if (date.getYear() == 2018 && date.getMonthValue() > 8) {
                        continue;  // KJG Tempoararily only create up to JUN-18
                    }
                    LocalDate lastOfMonth = date.with(TemporalAdjusters.lastDayOfMonth());
                    FinancialPeriod thisPeriod = new FinancialPeriod(exPeriod, exPeriod, LocalDate.of(totalYear[i], Month.of(j), 1), lastOfMonth, totalYear[i], j, PeriodStatus.NEVER_OPENED);
                    thisPeriod.setCreationDate(LocalDateTime.now());
                    thisPeriod.setLastUpdateDate(LocalDateTime.now());
                    logger.info("Creating period: " + thisPeriod.getId());
                    thisPeriod.setReportingCurrencyRatePeriod(thisPeriod);
                    if (priorPeriod != null) {
                        thisPeriod.setLocalCurrencyRatePeriod(priorPeriod);
                        thisPeriod.setPriorPeriod(priorPeriod);
                        priorPeriod.setNextPeriod(thisPeriod);
                    }
                    persist(thisPeriod);
                    priorPeriod = thisPeriod;
                }
            }
        }

        logger.info("Finished initializing FinancialPeriods.");
    }

    public FinancialPeriod getCurrentFinancialPeriod() {
        return company.getCurrentPeriod();
    }

    public FinancialPeriod calculateNextPeriodUntilCurrent(FinancialPeriod period) {
        FinancialPeriod nextPeriod = period.getNextPeriod();
        if (nextPeriod == null || nextPeriod.isAfter(getCurrentFinancialPeriod())) {
            return null;
        }

        return nextPeriod;
    }

    public FinancialPeriod getPriorFinancialPeriod() {
        return company.getPriorPeriod();
    }

    private static List<Integer> months = null;
//private static Logger logger = Logger.getLogger(FlowcastCalendar.class);

    public static List<Integer> getAllMonths() {
        if (months == null) {
            months = new ArrayList<Integer>();
            months.add((Month.JANUARY.getValue()));
            months.add((Month.FEBRUARY.getValue()));
            months.add((Month.MARCH.getValue()));
            months.add((Month.APRIL.getValue()));
            months.add((Month.MAY.getValue()));
            months.add((Month.JUNE.getValue()));
            months.add((Month.JULY.getValue()));
            months.add((Month.AUGUST.getValue()));
            months.add((Month.SEPTEMBER.getValue()));
            months.add((Month.OCTOBER.getValue()));
            months.add((Month.NOVEMBER.getValue()));
            months.add((Month.DECEMBER.getValue()));
        }

        return Collections.unmodifiableList(months);
    }

//    public FinancialPeriod calculatePriorPeriod(FinancialPeriod currentPeriod) {
//        Logger.getLogger(FinancialPeriodService.class.getName()).log(Level.FINER, "current period: " + currentPeriod.getId());
//        LocalDate date = LocalDate.of(currentPeriod.getPeriodYear(), currentPeriod.getPeriodMonth(), 2);
//        date = date.minusMonths(1);
//
//        return findPeriodByLocalDate(date);
//    }
//
//    public FinancialPeriod calculateNextPeriod(FinancialPeriod currentPeriod) {
//        Logger.getLogger(FinancialPeriodService.class.getName()).log(Level.FINER, "current period: " + currentPeriod.getId());
//        LocalDate date = LocalDate.of(currentPeriod.getPeriodYear(), currentPeriod.getPeriodMonth(), 2);
//        date = date.plusMonths(1);
//
//        return findPeriodByLocalDate(date);
//    }
    public FinancialPeriod findPeriodByLocalDate(LocalDate date) {
        Logger.getLogger(FinancialPeriodService.class.getName()).log(Level.FINER, "findPeriodByLocalDate: " + date);
        Query query = em.createQuery("SELECT p FROM FinancialPeriod p WHERE :DT BETWEEN p.startDate and p.endDate");
        query.setParameter("DT", date);
        // KJG If we do not get one and only one result, getSingleResult will throw exception, which is desired.  Should never happen.
        return (FinancialPeriod) query.getSingleResult();
//        if (resultList.size() != 1) {
//            Logger.getLogger(FinancialPeriodService.class.getName()).log(Level.SEVERE, "Period not found by date: " + date.toString());
//            throw new IllegalStateException("Period not found by date: " + date.toString());
//        }
//
//        return resultList.get(0);
    }

//    public static LocalDate getLastPeriod(int versionYear, int versionMonth) throws Exception {
//
//        LocalDate datetime = LocalDate.of(versionYear, versionMonth, 1);
//        if (versionMonth != (Month.JANUARY.getValue())) {
//            datetime = datetime.minusMonths(1);
//        }
//
//        return datetime;
//    }
    public static int getTodaysYear() {
        return LocalDate.now().getYear();
    }

    public static int getTodaysMonth() {
        return LocalDate.now().getMonthValue();
    }

    public static List getMonthStringsYTD(int year, int month) {
        List periods = new ArrayList();

        for (int i = month; i >= 1; i--) {
//			String calString = Forecast.monthStrings[i] + "-" + new Integer(year).toString().substring(2);
            periods.add("");
        }

        return periods;
    }

    public static List getMonthStringsPriorThirteenMonths(int year, int month) {

        LocalDate date = LocalDate.of(year, month, 1);
        date = date.minusMonths(13);
        List periods = new ArrayList();

        for (int i = 0; i <= 12; i++) {
            date = date.plusMonths(1);
            String calString = date.getMonthValue() + "-" + String.valueOf(date.getYear()).substring(2);
            periods.add(calString);
        }

        return periods;
    }

    public static List getMonthStringsYear(int year) {
        List periods = new ArrayList();

        for (int i = 1; i <= 12; i++) {
            //		String calString = Forecast.monthStrings[i] + "-" + new Integer(year).toString().substring(2);
            periods.add("");
        }

        return periods;
    }

    public static String getMonthString(int year, int month) {

        LocalDate date = LocalDate.of(year, month, 1);

        return date.getMonthValue() + "-" + String.valueOf(date.getYear()).substring(2);
    }

    public static int getPreviousQuarterYear() throws Exception {
        if (getCurrentClosePeriodMonth() < Month.MARCH.getValue()) {
            return getCurrentClosePeriodYear() - 1;
        }

        return getCurrentClosePeriodYear();

    }

    public static int getLastMonthOfPreviousQuarter() throws Exception {
        if (getCurrentClosePeriodMonth() == Month.DECEMBER.getValue() || getCurrentClosePeriodMonth() == Month.JANUARY.getValue()
                || getCurrentClosePeriodMonth() == Month.FEBRUARY.getValue()) {
            return Month.DECEMBER.getValue();
        }
        if (getCurrentClosePeriodMonth() == Month.MARCH.getValue() || getCurrentClosePeriodMonth() == Month.APRIL.getValue()
                || getCurrentClosePeriodMonth() == Month.MAY.getValue()) {
            return Month.MARCH.getValue();
        }
        if (getCurrentClosePeriodMonth() == Month.JUNE.getValue() || getCurrentClosePeriodMonth() == Month.JULY.getValue()
                || getCurrentClosePeriodMonth() == Month.AUGUST.getValue()) {
            return Month.JUNE.getValue();
        }

        return Month.SEPTEMBER.getValue();
    }

    public List<Holiday> getFutureHolidays() throws Exception {
        // Relevant is defined as those holidays existing in the current month or beyond
        LocalDate thisMonth = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 1);
        List<Holiday> futureHolidays = new ArrayList<Holiday>();
        for (Holiday holiday : adminService.findHolidayList()) {
            if (holiday.getHolidayDate().isAfter(thisMonth)) {
                futureHolidays.add(holiday);
            }
        }
        return futureHolidays;
    }

    public static int getForecastEndWorkday(String divisionId) {
        return 5;
    }

    public boolean isForecastingDue(String divisionId) throws Exception {
        LocalDate today = LocalDate.now();

        return isXWorkday(today, getForecastEndWorkday(divisionId), adminService.findHolidayList());

    }

    public boolean isXWorkday(LocalDate date, int workday, List<Holiday> holidays) throws Exception {

        LocalDate date1 = LocalDate.of(date.getYear(), date.getMonthValue(), 1);
        int workdayCount = isWorkday(date1, holidays) ? 1 : 0;

        while (workdayCount < workday) {
            date1 = date1.plusDays(1);
            if (isWorkday(date1, holidays)) {
                workdayCount++;
            }
        }

        if (date1.compareTo(date) == 0) {
            return true;
        }

        return false;
    }

    public int getWorkday(LocalDate date) throws Exception {

        List<Holiday> holidays = adminService.findHolidayList();

        LocalDate iteratorWorkday = LocalDate.of(date.getYear(), date.getMonthValue(), 1);
        LocalDate targetWorkday = date;
        int workdayCount = isWorkday(iteratorWorkday, holidays) ? 1 : 0;

        while (iteratorWorkday.compareTo(targetWorkday) < 0) {
            iteratorWorkday = iteratorWorkday.plusDays(1);
            if (isWorkday(iteratorWorkday, holidays)) {
                workdayCount++;
            }
        }

        return workdayCount;
    }

    public boolean isWorkday(LocalDate date, List<Holiday> holidays) throws Exception {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }

        for (Holiday holiday : holidays) {
            if (holiday.getHolidayDate().equals(date)) {
                return false;
            }
        }

        return true;
    }

    public LocalDate calcInputFreezeWorkday(LocalDate date, List<Holiday> holidays, int workday) throws Exception {
        LocalDate temp = null;
        int count = 0;
        LocalDate freezeDay = LocalDate.of(date.getYear(), date.getMonthValue(), 1);
        count = isWorkday(freezeDay, holidays) ? 1 : 0;
        for (int i = 0; i <= 31; i++) {
            freezeDay = freezeDay.plusDays(1);
            if (isWorkday(freezeDay, holidays) == true) {
                count++;
                if (count == workday) {
                    return freezeDay;
                }
                temp = freezeDay;
            }

        }
        return temp;
    }

    public static int getCurrentClosePeriodYear() {
        return 2018;
    }

    public static int getCurrentClosePeriodMonth() {
        return 7;
    }

    public static int getCurrentClosePeriodMonthPlusOneMonth() throws Exception {
        LocalDate date = LocalDate.of(getCurrentClosePeriodYear(), getCurrentClosePeriodMonth(), 1);
        date = date.plusMonths(1);

        return date.getMonthValue();
    }

    public static int getCurrentClosePeriodYearPlusOneMonth() throws Exception {
        LocalDate date = LocalDate.of(getCurrentClosePeriodYear(), getCurrentClosePeriodMonth(), 1);
        date = date.plusMonths(1);
        return date.getYear();
    }

    public static int getCurrentClosePeriodMonthMinusOneMonth() throws Exception {
        LocalDate date = LocalDate.of(getCurrentClosePeriodYear(), getCurrentClosePeriodMonth(), 1);
        date = date.minusMonths(1);
        return date.getMonthValue();
    }

    public static int getCurrentClosePeriodYearMinusOneMonth() throws Exception {
        LocalDate date = LocalDate.of(getCurrentClosePeriodYear(), getCurrentClosePeriodMonth(), 1);
        date = date.minusMonths(1);

        return date.getYear();
    }

    public static int getPresentYear() throws Exception {
        return LocalDate.now().getYear();
    }

    public static int getPresentMonth() throws Exception {
        return LocalDate.now().getMonthValue();
    }

    public static int getCurrentWorkday() throws Exception {
        //	return JdbcTemplateManager.getJdbcTemplate().queryForInt("select current_workday from xxfc_general_status where group_id = '0'");
        return 0;
    }

    public List<FinancialPeriod> findFinancialPeriods() {
        TypedQuery<FinancialPeriod> query = em.createQuery("SELECT b FROM FinancialPeriod b", FinancialPeriod.class);
        return (List<FinancialPeriod>) query.getResultList();
    }

    public FinancialPeriod updateFinancialPeriod(FinancialPeriod financialPeriod) {
        return em.merge(financialPeriod);
    }

    public List<FinancialPeriod> getQTDFinancialPeriods(FinancialPeriod period) {
        FinancialPeriod temp, temp2, temp3;
        LocalDate date = LocalDate.of(period.getPeriodYear(), period.getPeriodMonth(), 1);
        List<FinancialPeriod> finacialPeriod = new ArrayList<FinancialPeriod>();

        if (period.getPeriodMonth() == 1 || period.getPeriodMonth() == 4 || period.getPeriodMonth() == 7 || period.getPeriodMonth() == 10) {
            temp = findPeriodByLocalDate(date);
            finacialPeriod.add(temp);
            return finacialPeriod;
        }

        if (period.getPeriodMonth() == 2 || period.getPeriodMonth() == 5 || period.getPeriodMonth() == 8 || period.getPeriodMonth() == 11) {
            temp = findPeriodByLocalDate(date);
            //temp2 = calculatePriorPeriod(temp);
            temp2 = temp.getPriorPeriod();
            Collections.addAll(finacialPeriod, temp2, temp);
            return finacialPeriod;
        }

        if (period.getPeriodMonth() == 3 || period.getPeriodMonth() == 6 || period.getPeriodMonth() == 9 || period.getPeriodMonth() == 12) {
            temp = findPeriodByLocalDate(date);
//            temp2 = calculatePriorPeriod(temp);
//            temp3 = calculatePriorPeriod(temp2);
            temp2 = temp.getPriorPeriod();
            temp3 = temp2.getPriorPeriod();
            Collections.addAll(finacialPeriod, temp3, temp2, temp);
            return finacialPeriod;
        }

        return finacialPeriod;
    }

    public List<FinancialPeriod> getYTDFinancialPeriods(FinancialPeriod period) {
        FinancialPeriod temp;
        LocalDate date = LocalDate.of(period.getPeriodYear(), period.getPeriodMonth(), 1);
        List<FinancialPeriod> finacialPeriod = new ArrayList<FinancialPeriod>();
        for (int i = 1; i <= period.getPeriodMonth(); i++) {
            LocalDate tempdate = LocalDate.of(period.getPeriodYear(), i, 1);
            temp = findPeriodByLocalDate(tempdate);
            finacialPeriod.add(temp);

            if (i == period.getPeriodMonth()) {
                return finacialPeriod;
            }
        }
        return finacialPeriod;
    }

    public void openPeriod(FinancialPeriod period, User user) throws Exception {

        if (period.isNeverOpened()) {
            period.setStatus(PeriodStatus.OPENED);
            for (ReportingUnit ru : adminService.findAllReportingUnits()) {
                if (ru.isActive()) {
                    //Logger.getLogger(ReportingUnit.class.getName()).log(Level.INFO, "Opening RU Period: " + ru.getName());
                    if (ru.getWorkflowContext(period) == null) {
                        WorkflowContext workflowContext = new WorkflowContext();
                        adminService.persist(workflowContext);
                        ru.putPeriodWorkflowContext(period, workflowContext);
                        reportingUnitService.initialize(ru, period, user);
                    }

//                    for (Contract contract : ru.getContracts()) {
//                        if (contract.isActive() && contract.getPeriodApprovalRequest(period) == null) {
//                            WorkflowContext contractApprovalRequest = new WorkflowContext();
//                            contractApprovalRequest.setWorkflowStatus(WorkflowStatus.DRAFT);
//                            adminService.persist(contractApprovalRequest);
//                            contract.putPeriodApprovalRequest(period, contractApprovalRequest);
//                            calculationService.update(contract);
//                        }
//                    }
                }
            }
        }

        if (period.isClosed() || period.isUserFreeze()) {
            period.setStatus(PeriodStatus.OPENED);
        }

        updateFinancialPeriod(period);
    }

    public FinancialPeriod closePeriod(FinancialPeriod period) throws Exception {
        if (period.isOpen() || period.isUserFreeze()) {
            period.setStatus(PeriodStatus.CLOSED);
        }
        return updateFinancialPeriod(period);
    }

    public void freezePeriod(FinancialPeriod period) throws Exception {
        try {
            if (period.isOpen() || period.isClosed()) {
                period.setStatus(PeriodStatus.USER_FREEZE);
                updateFinancialPeriod(period);
            } else if (period.isNeverOpened()) {
                throw new IllegalStateException("This Period Never Opened.");
            }
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    public void initWorkflowContext(FinancialPeriod period, Contract contract) throws Exception {
        WorkflowContext contractApprovalRequest = new WorkflowContext();
        contractApprovalRequest.setWorkflowStatus(WorkflowStatus.DRAFT);
        adminService.persist(contractApprovalRequest);
        contract.putPeriodApprovalRequest(period, contractApprovalRequest);
        calculationService.update(contract);

    }

}
