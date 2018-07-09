/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.ExchangeRate;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author shubhamv
 */
@Named
@Stateless
public class AdminService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;
    @Inject
    private FinancialPeriodService financialPeriodService;

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    public List<User> searchUsers(String searchString) throws Exception {  // Need an application exception type defined.
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE UPPER(u.name) LIKE :NAME OR UPPER(u.flsId) LIKE :NAME ORDER BY UPPER(u.name)", User.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
        return (List<User>) query.getResultList();
    }

    public List<BusinessUnit> findBusinessUnits() throws Exception {  // Need an application exception type defined.

        TypedQuery<BusinessUnit> query = em.createQuery("SELECT b FROM BusinessUnit b", BusinessUnit.class);
        return (List<BusinessUnit>) query.getResultList();
    }

    public List<BusinessUnit> getBusinessUnit(String searchString) throws Exception {  // Need an application exception type defined.
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }
        System.out.println("Search" + searchString.toUpperCase());
        TypedQuery<BusinessUnit> query = em.createQuery("SELECT u FROM BusinessUnit u WHERE UPPER(u.name) LIKE :NAME ORDER BY UPPER(u.name)", BusinessUnit.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
        return (List<BusinessUnit>) query.getResultList();
    }

    public BusinessUnit findBusinessUnitById(Long id) {

        return em.find(BusinessUnit.class, id);
    }

    public void updateUser(User u) throws Exception {
        em.merge(u);
    }

    public void updateBusinessUnit(BusinessUnit u) throws Exception {
        em.merge(u);
    }

    public void persist(BusinessUnit bu) throws Exception {
        em.persist(bu);
    }

    public void persist(Object object) {
        em.persist(object);
    }

    public List<User> findUserByFlsId(String adname) {
        Query query = em.createQuery("SELECT u FROM User u WHERE UPPER(u.flsId) = :FLS_ID ORDER BY UPPER(u.id)");
        query.setParameter("FLS_ID", adname.toUpperCase());
        return (List<User>) query.getResultList();

    }

    public User findUserByFlsIdType(String adname) {
        Query query = em.createQuery("SELECT u FROM User u WHERE UPPER(u.flsId) = :FLS_ID ORDER BY UPPER(u.id)");
        query.setParameter("FLS_ID", adname.toUpperCase());

        List<User> user = query.getResultList();
        if (user.size() > 0) {
            return user.get(0);
        }
        return null;

    }

    public User findUserById(Long id) {

        return em.find(User.class, id);
    }

    public ReportingUnit findReportingUnitById(String id) {
        return em.find(ReportingUnit.class, id);
    }

    public ReportingUnit findReportingUnitById(Long id) {
        return em.find(ReportingUnit.class, id);
    }

    public Country findCountryById(String id) {
        return em.find(Country.class, id);
    }

    public Country findCompanyById(String id) {
        return em.find(Country.class, id);
    }

    public List<ReportingUnit> findAllReportingUnits() {
        Query query = em.createQuery("SELECT ru FROM ReportingUnit ru ORDER BY ru.code");
        return (List<ReportingUnit>) query.getResultList();
    }

    public ReportingUnit findReportingUnitByCode(String code) {
        Query query = em.createQuery("SELECT reportingUnit FROM ReportingUnit reportingUnit WHERE reportingUnit.code = :CODE");
        query.setParameter("CODE", code);
        List<ReportingUnit> reportingUnits = query.getResultList();
        if (reportingUnits.size() > 0) {
            return reportingUnits.get(0);
        }
        return null;
    }

    public ReportingUnit findPreparersByReportingUnitCode(String code) {
        Query query = em.createQuery("SELECT reportingUnit FROM ReportingUnit reportingUnit WHERE reportingUnit.code = :CODE");
        query.setParameter("CODE", code);
        List<ReportingUnit> reportingUnits = query.getResultList();
        List<User> user = reportingUnits.get(0).getPreparers();
        if (user.size() > 0) {
            return reportingUnits.get(0);
        }
        return null;
    }

    public Country findCountryByCode(String code) {
        Query query = em.createQuery("SELECT country FROM Country country WHERE country.code = :CODE");
        query.setParameter("CODE", code);
        List<Country> countries = query.getResultList();
        if (countries.size() > 0) {
            return countries.get(0);
        }
        return null;
    }

    public void persist(MetricType inputType) throws Exception {
        em.persist(inputType);
    }

    public void persist(Country country) throws Exception {
        em.persist(country);
    }

    public void persist(ReportingUnit ru) throws Exception {
        em.persist(ru);
    }

    public void update(List<ReportingUnit> rus) throws Exception {
        for (ReportingUnit ru : rus) {
            update(ru);
        }
    }

    public ReportingUnit update(ReportingUnit ru) throws Exception {
        return em.merge(ru);
    }

    public Company update(Company c) throws Exception {
        return em.merge(c);
    }

    public void updater(User u) throws Exception {
        em.persist(u);
    }

    public List<ReportingUnit> searchReportingUnits(String searchString) throws Exception {  // Need an application exception type defined.
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }
        TypedQuery<ReportingUnit> query = em.createQuery("SELECT ru  FROM ReportingUnit ru WHERE UPPER(ru.name) LIKE :NAME OR UPPER(ru.code) LIKE :NAME ORDER BY UPPER(ru.name)", ReportingUnit.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");

        return (List<ReportingUnit>) query.getResultList();
    }

    public List<BusinessUnit> searchSites(String searchString) throws Exception {
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }

        TypedQuery<BusinessUnit> query = em.createQuery(
                "SELECT s FROM BusinessUnit s WHERE UPPER(s.name) LIKE :NAME order by UPPER(s.name)", BusinessUnit.class);
        // query.setParameter("DOM", domain);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
        // System.out.println("searchSites:" + query);
        logger.info("searchSites:" + query.toString());
        return (List<BusinessUnit>) query.getResultList();
    }

    public List<Country> AllCountry() throws Exception {
        TypedQuery<Country> query = em.createQuery("SELECT c  FROM Country c ORDER BY UPPER(c.name)", Country.class);

        return (List<Country>) query.getResultList();
    }

    public List<User> findByStartsWithLastName(String searchname) {
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE UPPER(u.name) LIKE :NAME OR UPPER(u.flsId) LIKE :NAME ORDER BY UPPER(u.name)", User.class);
        query.setParameter("NAME", "%" + searchname.toUpperCase() + "%");
        return (List<User>) query.getResultList();
    }

    public void initUsers() throws Exception {
        List<User> admin = findUserByFlsId("bga_admin");
        User ad;
        if (admin.isEmpty()) {
            logger.info("Creating admin user");
            ad = new User("bga_admin", "M, Padmini", "M, Padmini", "bga_admin@flowserve.com");
            updater(ad);
        }

        if (findUserByFlsId("aloeffler").isEmpty()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/fls_user_init.txt"), "UTF-8"));

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                String[] values = line.split("\\t");
                if (values.length == 7 && !values[6].equalsIgnoreCase("ORG_LEVEL")) {
                    String flsId = values[0];
                    String displayName = values[1];
                    String commonNameLDAP = values[2];
                    String emailAddress = values[3];
                    String officeName = values[4];
                    String title = values[5];
                    int orgLevel = Integer.parseInt(values[6]);
                    User user = new User(flsId, displayName, commonNameLDAP, emailAddress, officeName, title, orgLevel);

                    updater(user);
                }
            }

            reader.close();
            //this.initSupervisor();
            logger.info("Finished initializing users.");
        }
    }

    public void initSupervisor() throws Exception {
        System.out.println("call");
        BufferedReader breader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/fls_supervisor_init.txt"), "UTF-8"));

        String line = null;
        while ((line = breader.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }

            String[] values = line.split("\\t");
            User us = findUserByFlsIdType(values[0]);
            if (us != null) {
                us.setSupervisor(findUserByFlsIdType(values[1]));
                updateUser(us);
            }

        }
        breader.close();
    }

    public void initReportingUnits() throws Exception {

        if (findReportingUnitByCode("0100") == null) {
            logger.info("Initializing Reporting Units");
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/reporting_units.txt"), "UTF-8"));

            int count = 0;
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                count = 0;
                String[] values = line.split("\\t");
                if (!values[1].equalsIgnoreCase("NAME")) {
                    ReportingUnit ru = new ReportingUnit();
                    ru.setCode(values[count++]);
                    ru.setName(ru.getCode() + " " + values[count++]);
                    if (values.length > 2) {
                        ru.setLocalCurrency(Currency.getInstance(new Locale("en", values[count])));
                        ru.setCountry(findCountryByCode(values[count++]));
                    }
                    ru.setActive(true);
                    persist(ru);
                }

            }

            reader.close();

            logger.info("Finished initializing Reporting Units.");
        }
    }

    public void initAssignPreparersForReportingUnit() throws Exception {

        if (findPreparersByReportingUnitCode("8000") == null) {
            logger.info("Initializing Reporting Units Preparers");
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/preparers_list.txt"), "UTF-8"));

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                String[] values = line.split("\\t");
                if (values.length > 6 && values[5].equalsIgnoreCase("Preparer")) {

                    String[] code = values[6].split(",");
                    User user = findUserByFlsIdType(values[0]);
                    int len = code.length;
                    for (int i = 0; i < len; i++) {
                        ReportingUnit ru = findReportingUnitByCode(code[i]);
                        if (ru != null && user != null) {
                            ru.getPreparers().add(user);
                            update(ru);
                        }

                    }

                }

            }

            reader.close();

            logger.info("Finished initializing Reporting Units.");
        }
    }

    public void initCompanies() throws Exception {
        if (findCompanyById("FLS") == null) {
            Company fls = new Company();
            fls.setId("FLS");
            fls.setName("Flowserve");
            fls.setDescription("Flowserve");
            fls.setInputFreezeWorkday(15);
            fls.setCurrentPeriod(financialPeriodService.findById("MAY-18"));

            update(fls);
        }
    }

    public void initCountries() throws Exception {
        if (findCountryById("USA") == null) {
            logger.info("Initializing Countries");

            String[] countryCodes = Locale.getISOCountries();
            for (String countryCode : countryCodes) {

                Locale locale = new Locale("", countryCode);
                Country country = new Country(locale.getISO3Country(), locale.getCountry(), locale.getDisplayCountry());
                persist(country);
            }

            logger.info("Finished initializing Countries.");
        }
    }

    public List<ExchangeRate> searchExchangeRates(String searchString) throws Exception {  // Need an application exception type defined.
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }
        TypedQuery<ExchangeRate> query = em.createQuery("SELECT er FROM ExchangeRate er WHERE UPPER(er.fromCurrency) LIKE :Currency OR UPPER(er.toCurrency) LIKE :Currency", ExchangeRate.class);
        query.setParameter("Currency", "%" + searchString.toUpperCase() + "%");
        return (List<ExchangeRate>) query.getResultList();
    }

    public List<ReportingUnit> getPreparableReportingUnits() {   // TODO - Move this to UserService.
        // TODO - figure out logged in user.
        List<ReportingUnit> rus = new ArrayList<ReportingUnit>();
        rus.add(findReportingUnitByCode("1015"));
        rus.add(findReportingUnitByCode("1100"));

        return rus;
    }

    public List<FinancialPeriod> findFinancialPeriods() {
        TypedQuery<FinancialPeriod> query = em.createQuery("SELECT b FROM FinancialPeriod b", FinancialPeriod.class);
        return (List<FinancialPeriod>) query.getResultList();
    }

    public void updateFinancialPeriod(FinancialPeriod financialPeriod) {
        em.merge(financialPeriod);
    }

}
