/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.BillingEvent;
import com.flowserve.system606.model.BusinessUnit;
import com.flowserve.system606.model.Company;
import com.flowserve.system606.model.Contract;
import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.DataImportFile;
import com.flowserve.system606.model.ExchangeRate;
import com.flowserve.system606.model.FinancialPeriod;
import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.model.Measurable;
import com.flowserve.system606.model.MetricType;
import com.flowserve.system606.model.ReportingUnit;
import com.flowserve.system606.model.SubledgerAccount;
import com.flowserve.system606.model.User;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
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
    @Inject
    ContractService contractService;

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
        Logger.getLogger(AdminService.class.getName()).log(Level.FINE, "Search" + searchString.toUpperCase());
        TypedQuery<BusinessUnit> query = em.createQuery("SELECT u FROM BusinessUnit u WHERE UPPER(u.name) LIKE :NAME ORDER BY UPPER(u.name)", BusinessUnit.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
        return (List<BusinessUnit>) query.getResultList();
    }

    public BusinessUnit findBusinessUnitById(String id) {

        return em.find(BusinessUnit.class, id);
    }

    public List<DataImportFile> findDataImportFileByType(String type) throws Exception {  // Need an application exception type defined.

        TypedQuery<DataImportFile> query = em.createQuery("SELECT b FROM DataImportFile b  WHERE UPPER(b.type) = :TYPE ORDER BY b.uploadDate DESC", DataImportFile.class);
        query.setParameter("TYPE", type.toUpperCase());
        return (List<DataImportFile>) query.getResultList();
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

    public void persist(BillingEvent be) throws Exception {
        em.persist(be);
    }

    public BillingEvent update(BillingEvent be) throws Exception {
        return em.merge(be);
    }

    public void persist(Object object) {
        em.persist(object);
    }

    public List<Holiday> findHolidayList() throws Exception {  // Need an application exception type defined.

        TypedQuery<Holiday> query = em.createQuery("SELECT b FROM Holiday b", Holiday.class);
        return (List<Holiday>) query.getResultList();
    }

    public void updateHoliday(Holiday h) {
        em.merge(h);
    }

    public void deleteHoliday(Holiday h) throws Exception {
        if (!em.contains(h)) {
            h = em.merge(h);
        }
        em.remove(h);
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

    public Company findCompanyById(String id) {
        return em.find(Company.class, id);
    }

    public MetricType findMetricTypeByCode(String code)
    {
    Query query = em.createQuery("SELECT m FROM MetricType m WHERE UPPER(m.code)= :code");
    query.setParameter("code", code.toUpperCase());
     List<MetricType> list=query.getResultList();
      if (list.size() > 0) {
            return list.get(0);
        }
      return null;
    }
    
    public SubledgerAccount findSubledgerAccountByName(String name)
    {
    Query query = em.createQuery("SELECT s FROM SubledgerAccount s WHERE (s.name)=:name ");
     query.setParameter("name", name);
     List<SubledgerAccount> list=query.getResultList();
     if (list.size() > 0) {
            return list.get(0);
        }
     return null;
    }
    
    public List<BillingEvent> findBillingEvents() {
        Query query = em.createQuery("SELECT b FROM BillingEvent b");
        return (List<BillingEvent>) query.getResultList();
    }

    public List<BillingEvent> findBillingEventsByContract(Contract contract) {
        Query query = em.createQuery("SELECT b FROM BillingEvent b WHERE b.contract = :CNT");
        query.setParameter("CNT", contract);
        return (List<BillingEvent>) query.getResultList();
    }

    public List<ReportingUnit> findAllReportingUnits() {
        Query query = em.createQuery("SELECT ru FROM ReportingUnit ru ORDER BY ru.code");
        return (List<ReportingUnit>) query.getResultList();
    }

    public List<Long> findAllReportingUnitIds() {
        Query query = em.createQuery("SELECT ru.id FROM ReportingUnit ru");
        return (List<Long>) query.getResultList();
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

    public ReportingUnit findBUByReportingUnitCode(String code) {
        Query query = em.createQuery("SELECT reportingUnit FROM ReportingUnit reportingUnit WHERE reportingUnit.code = :CODE");
        query.setParameter("CODE", code);
        List<ReportingUnit> reportingUnits = query.getResultList();
        BusinessUnit bu = reportingUnits.get(0).getBusinessUnit();
        if (bu != null) {
            return reportingUnits.get(0);
        }
        return null;
    }

    public ReportingUnit findParentInReportingUnitCode(String code) {
        Query query = em.createQuery("SELECT reportingUnit FROM ReportingUnit reportingUnit WHERE reportingUnit.code = :CODE");
        query.setParameter("CODE", code);
        List<ReportingUnit> reportingUnits = query.getResultList();
        ReportingUnit bu = reportingUnits.get(0).getParent();
        if (bu != null) {
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

    public Country findCountryByName(String name) {
        Query query = em.createQuery("SELECT country FROM Country country WHERE country.name = :NAME OR country.code = :NAME");
        query.setParameter("NAME", name);
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

    public void persist(DataImportFile importFile) throws Exception {
        em.persist(importFile);
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

    public Measurable update(Measurable measurable) throws Exception {
        return em.merge(measurable);
    }

    public Company update(Company c) throws Exception {
        return em.merge(c);
    }

    public void updater(User u) throws Exception {
        em.persist(u);
    }

    public void update(Country country) throws Exception {
        em.merge(country);
    }

    public List<ReportingUnit> searchReportingUnits(String searchString) throws Exception {  // Need an application exception type defined.
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }
        TypedQuery<ReportingUnit> query = em.createQuery("SELECT ru  FROM ReportingUnit ru WHERE UPPER(ru.name) LIKE :NAME OR UPPER(ru.code) LIKE :NAME ORDER BY UPPER(ru.name)", ReportingUnit.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");

        return (List<ReportingUnit>) query.getResultList();
    }

    public List<ReportingUnit> parentReportingUnits(String searchString, ReportingUnit ru) throws Exception {  // Need an application exception type defined.
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }
        TypedQuery<ReportingUnit> query = em.createQuery("SELECT ru  FROM ReportingUnit ru WHERE ru.id != :ID AND (UPPER(ru.name) LIKE :NAME OR UPPER(ru.code) LIKE :NAME) ORDER BY UPPER(ru.name)", ReportingUnit.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
        query.setParameter("ID", ru.getId());

        return (List<ReportingUnit>) query.getResultList();
    }

    public List<BusinessUnit> searchSites(String searchString) throws Exception {
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }

        TypedQuery<BusinessUnit> query = em.createQuery("SELECT s FROM BusinessUnit s WHERE UPPER(s.name) LIKE :NAME order by UPPER(s.name)", BusinessUnit.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
        return (List<BusinessUnit>) query.getResultList();
    }

    public List<BusinessUnit> searchParentBu(String searchString, BusinessUnit bu) throws Exception {
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }

        TypedQuery<BusinessUnit> query = em.createQuery("SELECT s FROM BusinessUnit s WHERE s.id != :ID AND UPPER(s.name) LIKE :NAME order by UPPER(s.name)", BusinessUnit.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
        query.setParameter("ID", bu.getId());
        return (List<BusinessUnit>) query.getResultList();
    }

    public List<Company> searchCompany(String searchString) throws Exception {
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }

        TypedQuery<Company> query = em.createQuery("SELECT s FROM Company s WHERE UPPER(s.name) LIKE :NAME OR UPPER(s.id) LIKE :NAME order by UPPER(s.name)", Company.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
        Logger.getLogger(AdminService.class.getName()).log(Level.FINE, "searchSites:" + query.toString());
        return (List<Company>) query.getResultList();
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
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Creating admin user");
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
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Finished initializing users.");
        }
    }

    public void initSupervisor() throws Exception {
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

        if (findReportingUnitByCode("1105") == null) {
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Initializing Reporting Units");
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/reporting_units.txt"), "UTF-8"));

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                String[] values = line.split("\\t");
                ReportingUnit ru = new ReportingUnit();
                ru.setCode(values[1].trim());
                ru.setName(ru.getCode() + " " + values[0]);
                if (values.length > 2) {
                    Country cn = findCountryByName(values[2]);
                    if (cn != null) {
                        ru.setCountry(cn);
                        ru.setLocalCurrency(Currency.getInstance(new Locale("en", cn.getCode())));

                    }
                }
                ru.setActive(true);
                persist(ru);
                //initializing RUs in Countries
                if (values.length > 2) {
                    Country cnt = findCountryByName(values[2]);
                    if (cnt != null) {
                        ReportingUnit addRU = findReportingUnitByCode(values[1].trim());
                        cnt.getReportingUnit().add(addRU);
                        update(cnt);
                    }
                }
            }
            reader.close();
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Finished initializing Reporting Units.");
        }
    }

    public void initBusinessUnit() throws Exception {
        if (findBusinessUnitById("AMSS") == null) {
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Initializing Business Units");
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/business_units.txt"), "UTF-8"));

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                String[] values = line.split("\\t");
                BusinessUnit bu = new BusinessUnit();
                bu.setId(values[0]);
                bu.setName(values[0]);
                bu.setType("Platform");
                persist(bu);
            }
            reader.close();
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Finished initializing Business Units.");
        }
    }

    public void initBUinRU() throws Exception {
        if (findBUByReportingUnitCode("8000") == null) {
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "initBUinRU");
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/business_unit_reporting.txt"), "UTF-8"));

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                String[] values = line.split("\\t");
                if (values.length == 3) {
                    ReportingUnit ru = findReportingUnitByCode(values[0]);
                    BusinessUnit bu = findBusinessUnitById(values[2]);
                    ru.setBusinessUnit(bu);
                    bu.getReportingUnit().add(ru);
                    update(ru);
                    updateBusinessUnit(bu);
                }
            }
            reader.close();
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Finished initBUinRU.");
        }
    }

    public void initCoEtoParentRU() throws Exception {
        if (findParentInReportingUnitCode("8000") == null) {
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "initCoEtoParentRU");
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/reporting_units.txt"), "UTF-8"));

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                String[] values = line.split("\\t");
                if (values.length > 3) {
                    ReportingUnit ru = findReportingUnitByCode(values[3]);
                    if (ru == null) {
                        ReportingUnit addRU = new ReportingUnit();
                        String code = values[3];
                        addRU.setCode(code);
                        String[] sp = code.split("\\s+");
                        String CoENumber = sp[sp.length - 1];
                        addRU.setName("Center of Excellence " + CoENumber);
                        addRU.setActive(true);
                        persist(addRU);
                        ru = findReportingUnitByCode(code);
                    }
                    ReportingUnit preRU = findReportingUnitByCode(values[1]);
                    preRU.setParent(ru);
                    ru.getChildReportingUnits().add(preRU);
                    update(preRU);
                    update(ru);
                }
            }
            reader.close();
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "initCoEtoParentRU");
        }
    }

    public void initSubledgerAccount()
    {
    MetricType mt=findMetricTypeByCode("BOOKING_DATE");
    MetricType mt2=findMetricTypeByCode("ESTIMATED_COST_AT_COMPLETION_LC");
    Company com=findCompanyById("FLS");
     if (findSubledgerAccountByName("DummyDR") == null){
    SubledgerAccount subledger=new SubledgerAccount();
    Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Initializing Subledger Accounts");
    subledger.setCompany(com);
    subledger.setName("DummyDR");
    subledger.setAccountType("Payable");
    subledger.setDescription("DummyDR");
    subledger.setCode("DummyDR");
    subledger.setCreditAccount(mt);
    subledger.setDebitAccount(mt);
    persist(subledger);
     }
     
     if (findSubledgerAccountByName("DummyCR") == null){
    SubledgerAccount ledger=new SubledgerAccount();
    Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Initializing Subledger Accounts");
    ledger.setCompany(com);
    ledger.setName("DummyCR");
    ledger.setAccountType("Receivable");
    ledger.setDescription("DummyCR");
    ledger.setCode("DummyCR");
    ledger.setCreditAccount(mt2);
    ledger.setDebitAccount(mt2);
    persist(ledger);
     }
    }
    public void initPreparersReviewerForRU() throws Exception {

        if (findPreparersByReportingUnitCode("8000") == null) {
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Initializing Reporting Units Preparers");
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

                } else if (values.length > 6 && values[5].equalsIgnoreCase("Reviewer")) {
                    String[] code = values[6].split(",");
                    User user = findUserByFlsIdType(values[0]);
                    int len = code.length;
                    for (int i = 0; i < len; i++) {
                        ReportingUnit ru = findReportingUnitByCode(code[i]);
                        if (ru != null && user != null) {
                            ru.getApprovers().add(user);
                            update(ru);
                        }

                    }
                }

            }

            reader.close();

            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Finished initializing Reporting Units.");
        }
    }

    public void initCompaniesInRUs() throws Exception {

        if (findReportingUnitByCode("8000").getCompany() == null) {
            Company cm = findCompanyById("FLS");
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "initializing Assign Company to RU");
            List<ReportingUnit> reportingUnits = findAllReportingUnits();
            for (ReportingUnit ru : reportingUnits) {
                cm.getReportingUnit().add(ru);
                ru.setCompany(cm);
                update(ru);
                update(cm);
            }
        }

    }

    public void initCompanies() throws Exception {
        if (findCompanyById("FLS") == null) {
            Company fls = new Company();
            fls.setId("FLS");
            fls.setName("Flowserve");
            fls.setDescription("Flowserve");
            fls.setInputFreezeWorkday(15);
            fls.setReportingCurrency(Currency.getInstance(new Locale("en", "US")));
            fls.setPociDueWorkday(10);

            update(fls);
        }
    }

    public void initBilings() throws Exception {
        Contract ct = contractService.findContractById(new Long(3822));
        if (findBillingEvents().isEmpty()) {
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "initBilings");
            BillingEvent be = new BillingEvent();
            be.setAmountContractCurrency(new BigDecimal(50));
            be.setAmountLocalCurrency(new BigDecimal(50));
            be.setBillingDate(LocalDate.now());
            be.setContract(ct);
            be.setDeliveryDate(LocalDate.now());
            be.setInvoiceNumber("1234");
            persist(be);
        }
    }

    public void initCountries() throws Exception {
        if (findCountryById("USA") == null) {
            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Initializing Countries");

            String[] countryCodes = Locale.getISOCountries();
            for (String countryCode : countryCodes) {

                Locale locale = new Locale("", countryCode);
                Country country = new Country(locale.getISO3Country(), locale.getCountry(), locale.getDisplayCountry());
                persist(country);
            }

            Logger.getLogger(AdminService.class.getName()).log(Level.INFO, "Finished initializing Countries.");
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

    public ExchangeRate findExchangeRatesByFinancialPeriod(FinancialPeriod fp) {
        Query query = em.createQuery("SELECT er FROM ExchangeRate er WHERE er.financialPeriod = :fPeriod");
        query.setParameter("fPeriod", fp);

        List<ExchangeRate> er = query.getResultList();
        if (er.size() > 0) {
            return er.get(0);
        }
        return null;

    }

//    public List<ReportingUnit> getPreparableReportingUnits() {   // TODO - Move this to UserService.
//        // TODO - figure out logged in user.
//        List<ReportingUnit> rus = new ArrayList<ReportingUnit>();
//        rus.add(findReportingUnitByCode("1225"));
//        //rus.add(findReportingUnitByCode("1100"));
//        rus.add(findReportingUnitByCode("8025"));
//
//        return rus;
//    }
    public List<Company> findAllCompany() throws Exception {  // Need an application exception type defined.

        TypedQuery<Company> query = em.createQuery("SELECT c FROM Company c", Company.class);
        return (List<Company>) query.getResultList();
    }

    public List<Contract> searchContract(String searchString) throws Exception {  // Need an application exception type defined.
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }
        TypedQuery<Contract> query = em.createQuery("SELECT c FROM Contract c WHERE UPPER(c.name) LIKE :NAME ORDER BY UPPER(c.name)", Contract.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
        return (List<Contract>) query.getResultList();
    }
}
