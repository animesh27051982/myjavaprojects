/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Output;
import com.flowserve.system606.model.OutputType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author kgraves
 */
@Stateless
public class OutputService {

    private static final Logger logger = Logger.getLogger(OutputService.class.getName());

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    @EJB
    private AdminService adminService;

    public List<OutputType> findOutputTypes() {
        Query query = em.createQuery("SELECT ot FROM OutputType ot");
        return (List<OutputType>) query.getResultList();
    }

    private List<OutputType> findActiveOutputTypes() {
        Query query = em.createQuery("SELECT ot FROM OutputType ot WHERE ot.active = TRUE");
        return (List<OutputType>) query.getResultList();
    }

    public List<OutputType> findActiveOutputTypesPob() {
        return findActiveOutputTypesByOwnerEntityType("POB");
    }

    private List<OutputType> findActiveOutputTypesByOwnerEntityType(String ownerEntityType) {
        Query query = em.createQuery("SELECT ot FROM OutputType ot WHERE ot.ownerEntityType = :OET AND ot.active = TRUE");
        query.setParameter("OET", ownerEntityType);
        return (List<OutputType>) query.getResultList();
    }

    public void persist(Output output) throws Exception {
        em.persist(output);
    }

    public OutputType findOutputTypeById(String id) {
        return em.find(OutputType.class, id);
    }

    public OutputType findOutputTypeByName(String outputName) {
        Query query = em.createQuery("SELECT ot FROM OutputType ot WHERE ot.name = :ON");
        query.setParameter("ON", outputName);
        return (OutputType) query.getSingleResult();  // we want an exception if not one and only one.
    }

    public void initOutputTypes() throws Exception {

        logger.info("Initializing OutputTypes");
        BufferedReader reader = new BufferedReader(new InputStreamReader(AppInitializeService.class.getResourceAsStream("/resources/app_data_init_files/init_output_types.txt"), "UTF-8"));

        int count = 0;
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }

            count = 0;

            String[] values = line.split("\\|");

            OutputType outputType = new OutputType();
            outputType.setId(values[count++]);
            try {
                if (findOutputTypeById(outputType.getId()) != null) {
                    continue;
                }
            } catch (Exception e) {
                Logger.getLogger(OutputService.class.getName()).log(Level.FINE, "Adding OutputType: " + line);
            }

            outputType.setOwnerEntityType(values[count++]);
            outputType.setOutputClass(values[count++]);
            outputType.setName(values[count++]);
            outputType.setDescription(values[count++]);
            outputType.setExcelSheet(values[count++]);
            outputType.setExcelCol(values[count++]);
            outputType.setGroupName(values[count++]);
            outputType.setGroupPosition(Integer.parseInt(values[count++]));
            outputType.setEffectiveFrom(LocalDate.now());
            outputType.setActive(true);
            logger.info("Creating OutputType: " + outputType.getName());

            adminService.persist(outputType);
        }

        reader.close();

        logger.info("Finished initializing OutputTypes.");
    }

//    public void initOutputTypeMap() {
//        List<OutputType> outputTypes = findOutputTypes();
//        outputTypes.forEach(outputType -> OutputTypeKey.putOutputType(outputType.getName(), outputType));
//    }
}
