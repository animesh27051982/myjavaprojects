/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.User;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.List;
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

    @PostConstruct
    public void init() {
        logger.info("Initializing App Objects");
        try {
            initUsers();
            initInputTypes();
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("init_users.txt"), "UTF-8"));

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
}
