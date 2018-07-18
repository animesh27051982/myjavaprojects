/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author shubhamv
 */
@Stateless
public class DataUploadService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    public void persist(Object object) {
        em.persist(object);
    }

    public void processUploadedCalculationData(String msAccDB) {
        Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, "Processing POCI Data: " + msAccDB);

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        // Step 1: Loading or registering Oracle JDBC driver class
        try {

            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        } catch (ClassNotFoundException cnfex) {

            System.out.println("Problem in loading or "
                    + "registering MS Access JDBC driver");
            cnfex.printStackTrace();
        }

        // Step 2: Opening database connection
        try {

            String dbURL = "jdbc:ucanaccess://" + msAccDB;

            // Step 2.A: Create and get connection using DriverManager class
            connection = DriverManager.getConnection(dbURL);

            // Step 2.B: Creating JDBC Statement
            statement = connection.createStatement();

            // Step 2.C: Executing SQL &amp; retrieve data into ResultSet
            resultSet = statement.executeQuery("SELECT * FROM tbl_POCI");

            Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, "Period\tPOCC File Name\tC Page Number\tReporting Unit Number");
            Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, "==\t================\t===\t=======");

            // processing returned data and printing into console
            while (resultSet.next()) {
                Logger.getLogger(DataUploadService.class.getName()).log(Level.INFO, resultSet.getString(1) + "\t"
                        + resultSet.getString(2) + "\t"
                        + resultSet.getInt(3) + "\t"
                        + resultSet.getString(4));
            }
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
        } finally {

            // Step 3: Closing database connection
            try {
                if (null != connection) {

                    // cleanup resources, once after processing
                    resultSet.close();
                    statement.close();

                    // and then finally close connection
                    connection.close();
                }
            } catch (SQLException sqlex) {
                sqlex.printStackTrace();
            }
        }
    }

}
