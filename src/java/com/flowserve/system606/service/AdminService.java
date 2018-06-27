/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.User;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.ejb.Stateless;
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

    public void persist(Object object) {
        em.persist(object);
    }

    public List<User> findUserByFlsId(String adname) {
        Query query = em.createQuery("SELECT u FROM User u WHERE UPPER(u.flsId) = :FLS_ID ORDER BY UPPER(u.id)");
        query.setParameter("FLS_ID", adname.toUpperCase());
        return (List<User>) query.getResultList();

    }

    public User findUserById(Long id) {

        return em.find(User.class, id);
    }

    public ReportingUnit findReportingUnitById(String id) {
        return em.find(ReportingUnit.class, id);
    }

    public Country findCountryById(String id) {
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

    public Country findCountryByCode(String code) {
        Query query = em.createQuery("SELECT country FROM Country country WHERE country.code = :CODE");
        query.setParameter("CODE", code);
        List<Country> countries = query.getResultList();
        if (countries.size() > 0) {
            return countries.get(0);
        }
        return null;
    }

    public void persist(InputType inputType) throws Exception {
        em.persist(inputType);
    }

    public void persist(Country country) throws Exception {
        em.persist(country);
    }

    public void persist(ReportingUnit ru) throws Exception {
        em.persist(ru);
    }

    public ReportingUnit update(ReportingUnit ru) throws Exception {

        return em.merge(ru);

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
        List<User> admin = findUserByFlsId("admin");
        User ad;

        if (admin.isEmpty()) {
            logger.info("Creating admin user");
            ad = new User("admin", "Administrator", "admin@gmail.com");
            updater(ad);
        }

        if (findUserByFlsId("pkaranam").isEmpty()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/init_users/init_users.txt"), "UTF-8"));

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                String[] values = line.split("\\t");

                String name = values[0];
                String flsId = values[3];
                String email = values[4];

                User user = new User(flsId, name, email);

                logger.info("Creating user: " + name);

                updater(user);
            }

            reader.close();

            logger.info("Finished initializing users.");
        }
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

                ReportingUnit ru = new ReportingUnit();

                ru.setCode(values[count++]);
                ru.setName(values[count++]);
                if (values.length > 2) {
                    ru.setCountry(findCountryByCode(values[count++]));
                }
                ru.setActive(true);

                persist(ru);
            }

            reader.close();

            logger.info("Finished initializing Reporting Units.");
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

}
