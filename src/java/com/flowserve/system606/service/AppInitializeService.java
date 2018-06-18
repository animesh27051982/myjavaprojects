/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.ExchangeRate;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.PerformanceObligation;
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
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author shubhamv
 */
@Singleton
@Startup
public class AppInitializeService {

    private static Logger logger = Logger.getLogger("com.flowserve.system606");

    List<User> admin = null;
    User ad;

    @EJB
    private AdminService adminService;
    @EJB
    private PerformanceObligationService pobService;

    @EJB
    private CurrencyService currService;

    @PostConstruct
    public void init() {
        logger.info("Initializing App Objects");
        try {
            initUsers();

            initCurrencyConverter();
            initInputTypes();
            initCountries();
            initPOBs();
        } catch (Exception ex) {
            Logger.getLogger(AppInitializeService.class.getName()).log(Level.SEVERE, null, ex);
        }

        logger.info("Initializing App Objects Done");
    }

    private void initUsers() throws Exception {
        admin = adminService.findUserByFlsId("admin");
        if (admin.isEmpty()) {
            logger.info("Creating admin user");
            ad = new User("admin", "Administrator", "admin@gmail.com");
            adminService.updater(ad);
        }

        if (adminService.findUserByFlsId("pkaranam").isEmpty()) {
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

                adminService.updater(user);
            }

            reader.close();

            logger.info("Finished initializing users.");
        }
    }

    private void initCurrencyConverter() throws Exception {
        List<ExchangeRate> er = currService.findRatesByNextDate();
        System.out.println("com" + er);
        if (er.isEmpty()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/currency_rate_file/currency.txt"), "UTF-8"));
            currService.deleteExchangeRate();
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                String[] from = line.split("\\t");
                if (!from[2].equalsIgnoreCase("")) {
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/currency_rate_file/currency.txt"), "UTF-8"));

                    String innerLine = null;
                    while ((innerLine = reader2.readLine()) != null) {
                        if (innerLine.trim().length() == 0) {
                            continue;
                        }

                        String[] to = innerLine.split("\\t");
                        if (!to[2].equalsIgnoreCase("")) {
                            BigDecimal usdRate = new BigDecimal(1.0);
                            BigDecimal sourceRate = new BigDecimal(from[4]);
                            BigDecimal targetRate = new BigDecimal(to[4]);

                            String type = from[2];
                            Currency fromCurrency = Currency.getInstance(from[3]);
                            Currency toCurrency = Currency.getInstance(to[3]);
                            LocalDate effectiveDate = LocalDate.now().plusDays(30);
                            //Currency Conversion Formula
                            BigDecimal rate = usdRate.divide(sourceRate, 100, BigDecimal.ROUND_HALF_UP).multiply(targetRate);

                            ExchangeRate exchangeRate = new ExchangeRate(type, fromCurrency, toCurrency, effectiveDate, rate);
                            currService.updater(exchangeRate);
                            //logger.info("From Country: " + effectiveDate + "  To Country: " + toCurrency + "   Rate" + rate);
                        }
                    }
                    reader2.close();
                }
            }

            reader.close();

            logger.info("Finished initializing users.");
        }
    }

    private void initInputTypes() throws Exception {

        admin = adminService.findUserByFlsId("admin");

        if (adminService.findInputTypeByName("Estimated Cost at Completion").isEmpty()) {
            logger.info("Initializing InputTypes");
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/init_input_types.txt"), "UTF-8"));

            int count = 0;
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                count = 0;
                String[] values = line.split("\\|");

                InputType inputType = new InputType();
                inputType.setOwnerEntityType(values[count++]);
                inputType.setInputClass(values[count++]);
                inputType.setName(values[count++]);
                inputType.setDescription(values[count++]);
                inputType.setExcelSheet(values[count++]);
                inputType.setExcelCol(values[count++]);
                inputType.setGroupName(values[count++]);
                inputType.setGroupPosition(Integer.parseInt(values[count++]));
                inputType.setEffectiveFrom(LocalDate.now());
                //inputType.setEffectiveTo(LocalDate.now());
                inputType.setActive(true);

                logger.info("Creating InputType: " + inputType.getName());

                adminService.persist(inputType);

            }

            reader.close();

            logger.info("Finished initializing InputTypes.");
        }
    }

    private void initPOBs() throws Exception {

        User administrator = admin.get(0);

        if (pobService.findById(10660L) == null) {
            logger.info("Initializing POBs");
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/pob_data.txt"), "UTF-8"));

            int count = 0;
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }

                count = 0;
                String[] values = line.split("\\t");

                PerformanceObligation pob = new PerformanceObligation();

                pob.setName(values[count++]);
                pob.setId(new Long(values[count++]));
                pob.setActive(true);

                pobService.persist(pob, administrator);
            }

            reader.close();

            logger.info("Finished initializing POBs.");
        }
    }

    private void initCountries() throws Exception {
        if (adminService.findCountryById("USA") == null) {
            logger.info("Initializing Countries");

            String[] countryCodes = Locale.getISOCountries();
            for (String countryCode : countryCodes) {

                Locale locale = new Locale("", countryCode);
                Country country = new Country(locale.getISO3Country(), locale.getCountry(), locale.getDisplayCountry());
                adminService.persist(country);
            }

            logger.info("Finished initializing InputTypes.");
        }
    }
}
