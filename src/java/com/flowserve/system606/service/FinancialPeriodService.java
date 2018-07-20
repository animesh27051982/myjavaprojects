package com.flowserve.system606.service;

import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.model.PeriodStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author kgraves
 */
@Stateless
public class FinancialPeriodService {

    @Inject
    private  AdminService adminService;
    private static final Logger logger = Logger.getLogger(FinancialPeriodService.class.getName());

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    private DateTimeFormatter periodNameFormatter = DateTimeFormatter.ofPattern("MMM-yy");

    @PostConstruct
    public void init() {
    }

//    public String getPeriodIdByDate(LocalDate date) {
//        return periodNameFormatter.format(date);
//    }
    public FinancialPeriod findById(String id) {
        return em.find(FinancialPeriod.class, id);
    }

    public void persist(FinancialPeriod fp) throws Exception {
        em.persist(fp);
    }

    public void initFinancialPeriods() throws Exception {
        logger.info("Initializing FinancialPeriods");
        if (findById("APR-18") == null) {
            FinancialPeriod period = new FinancialPeriod("APR-18", "APR-18", LocalDate.of(2018, Month.APRIL, 1), LocalDate.of(2018, Month.APRIL, 30), 2018, 4, PeriodStatus.OPENED);
            persist(period);
        }
        if (findById("MAY-18") == null) {
            logger.info("Initializing FinancialPeriods");
            FinancialPeriod period = new FinancialPeriod("MAY-18", "MAY-18", LocalDate.of(2018, Month.MAY, 1), LocalDate.of(2018, Month.MAY, 31), 2018, 5, PeriodStatus.OPENED);
            persist(period);

        }
        logger.info("Finished initializing FinancialPeriods.");
    }

    public FinancialPeriod getCurrentFinancialPeriod() {
        return findById("MAY-18");
    }

    public FinancialPeriod getPriorFinancialPeriod() {
        return findById("APR-18");
    }
    
 
private static  List<Integer> months = null;
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


	public static LocalDate getLastPeriod(int versionYear, int versionMonth) throws Exception {

		LocalDate datetime = LocalDate.of(versionYear, versionMonth, 1);
		if (versionMonth != (Month.JANUARY.getValue())) {
			datetime=datetime.minusMonths(1);
		}

		return datetime;
	}
        
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
                date=date.minusMonths(13);
		List periods = new ArrayList();

		for (int i = 0; i <= 12; i++) {
			date=date.plusMonths(1);
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
               public  List<Holiday> getFutureHolidays() throws Exception {
		// Relevant is defined as those holidays existing in the current month or beyond
		LocalDate thisMonth=LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 1);
		List<Holiday> futureHolidays = new ArrayList<Holiday>();
		for (Holiday holiday : adminService.findHolidayList()) {
			if (holiday.getHolidayDate().isAfter(thisMonth)) {
				futureHolidays.add(holiday);
			}
		}
		return futureHolidays;
	}
                
                
                public static  int getForecastEndWorkday(String divisionId )
                {
                return 5;
                }
                
                public  boolean isForecastingDue(String divisionId) throws Exception {
		LocalDate today=LocalDate.now();

		return isXWorkday(today, getForecastEndWorkday(divisionId), adminService.findHolidayList());
                
	}
                
                public static boolean isXWorkday(LocalDate date, int workday, List<Holiday> holidays) throws Exception {

		LocalDate date1=LocalDate.of(date.getYear(),date.getMonthValue() ,1 );
		int workdayCount = isWorkday(date1, holidays) ? 1 : 0;
		

		while (workdayCount < workday) {
			date1=date1.plusDays(1);
			if (isWorkday(date1, holidays)) {
				workdayCount++;
			}
		}

		if (date1.compareTo(date) == 0) {
			return true;
		}

		return false;
	}
                
                
                public  int getWorkday(LocalDate date) throws Exception {

		List<Holiday> holidays = adminService.findHolidayList();
		
		LocalDate iteratorWorkday=LocalDate.of(date.getYear(), date.getMonthValue(), 1);
		LocalDate targetWorkday = date;
		int workdayCount = isWorkday(iteratorWorkday, holidays) ? 1 : 0;

		while (iteratorWorkday.compareTo(targetWorkday)<0) {
			iteratorWorkday=iteratorWorkday.plusDays(1);
			if (isWorkday(iteratorWorkday, holidays)) {
				workdayCount++;
			}
		}

		return workdayCount;
	}
                
                public static boolean isWorkday(LocalDate date, List<Holiday> holidays) {

		if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY && !holidays.contains(date)) {
			return true;
		} else {
			return false;
		}
	}
                
//                private static SortedSet<KeyDate> getAllHolidays() throws Exception {
//		String userQuery = "select holiday_date, holiday_desc from xxfc_holiday order by holiday_date";
//
//		List<KeyDate> holidays = JdbcTemplateManager.getJdbcTemplate().query(userQuery, new Object[] {}, new int[] {}, new RowMapper<KeyDate>() {
//
//			public KeyDate mapRow(ResultSet rs, int rowNum) throws SQLException {
//				return new KeyDate(new DateTime(rs.getDate("holiday_date")), rs.getString("holiday_desc"));
//			}
//		});
//
//		SortedSet<KeyDate> sortedHolidays = new TreeSet<KeyDate>(DateTimeComparator.getDateOnlyInstance());
//		sortedHolidays.addAll(holidays);
//		return sortedHolidays;
//	}
                
                
//                public static void updateCurrentClosePeriod(Division division) throws Exception {
//		/**
//		 * KG - Oct2010 - Lost XA capabilities due to port from WebSphere to Tomcat.
//		 */
//		// JtaTransactionManager txManager = new JtaTransactionManager((TransactionManager)(new WebSphereTransactionManagerFactoryBean().getObject()));
//		// DefaultTransactionDefinition def = new DefaultTransactionDefinition();
//		// def.setName("currentPeriodUpdate");
//		// def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
//		// TransactionStatus status = txManager.getTransaction(def);
//
//		logger.debug("Updating current period for " + division.getName());
//
//		try {
//			MutableDateTime closeMonth = null;
//			int currentWorkday = FlowcastCalendar.getWorkday(new DateTime());
//			// Current close is always one month behind the system date. unless we are Pumps, then on day 10 we flip to new month.
//			if (division.getName().matches("Pumps|EPO|IPO|AMSS") && currentWorkday >= 10) {
//				closeMonth = new MutableDateTime();
//			} else {
//				closeMonth = new MutableDateTime();
//				closeMonth.addMonths(-1);
//			}
//
//			JdbcTemplateManager.getJdbcTemplate().update(
//					"update xxfc_general_status set version_year = ?, version_month = ?, current_workday = ? WHERE group_id = '" + division.getId() + "'",
//					new Object[] { new Integer(closeMonth.getYear()), new Integer(closeMonth.getMonthOfYear()), new Integer(currentWorkday) });
//			// Update the Default row along with any group that gets updated as the entire company is on the same schedule
//			// TODO - This can probably be safely removed at this point. Pretty sure all depedencies on the default division have been removed (a legacy
//			// concept.)
//			JdbcTemplateManager.getJdbcTemplate().update(
//					"update xxfc_general_status set version_year = ?, version_month = ?, current_workday = ? WHERE group_id = '0'",
//					new Object[] { new Integer(closeMonth.getYear()), new Integer(closeMonth.getMonthOfYear()), new Integer(currentWorkday) });
//		} catch (Exception ex) {
//			// txManager.rollback(status);
//			throw ex;
//		}
//		// txManager.commit(status);
//	}
                
          public static int getCurrentClosePeriodYear()
          {
          return 2018;
          }      
         public static int getCurrentClosePeriodMonth()
         {
         return 7;
         }
                
       public static int getCurrentClosePeriodMonthPlusOneMonth() throws Exception {
        LocalDate date=LocalDate.of(getCurrentClosePeriodYear(), getCurrentClosePeriodMonth(), 1);
        date=date.plusMonths(1);

        return date.getMonthValue();
    }
       
       public static int getCurrentClosePeriodYearPlusOneMonth() throws Exception {
		 LocalDate date=LocalDate.of(getCurrentClosePeriodYear(), getCurrentClosePeriodMonth(), 1);
                 date=date.plusMonths(1);
		return date.getYear();
	}
       
      public static int getCurrentClosePeriodMonthMinusOneMonth() throws Exception {
		 LocalDate date=LocalDate.of(getCurrentClosePeriodYear(), getCurrentClosePeriodMonth(), 1);
                 date=date.minusMonths(1);
		return date.getMonthValue();
	} 
      
      public static int getCurrentClosePeriodYearMinusOneMonth() throws Exception {
		LocalDate date=LocalDate.of(getCurrentClosePeriodYear(), getCurrentClosePeriodMonth(), 1);
                 date=date.minusMonths(1);

		return date.getYear();
	}
      
      public static int getPresentYear() throws Exception {
		return  LocalDate.now().getYear();
	}
      
      public static int getPresentMonth() throws Exception {
		return LocalDate.now().getMonthValue();
	}
      public static int getCurrentWorkday() throws Exception {
	//	return JdbcTemplateManager.getJdbcTemplate().queryForInt("select current_workday from xxfc_general_status where group_id = '0'");
	
      return 0;
              
      }

}
