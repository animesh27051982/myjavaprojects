/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.Holiday;
import com.flowserve.system606.service.AdminService;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;

/**
 *
 * @author shubhamv
 */
@Named
@ViewScoped
public class Calendar implements Serializable {

    private ScheduleModel eventModel;
    @Inject
    private AdminService adminService;

    public Calendar() {
    }

    @PostConstruct
    public void init() {
        try {
            List<Holiday> holidays = adminService.findHolidayList();
            eventModel = new DefaultScheduleModel();
            for (Holiday holiday : holidays) {

                Date date = Date.from(holiday.getHolidayDate().atStartOfDay(ZoneOffset.UTC).toInstant());
                eventModel.addEvent(new DefaultScheduleEvent(holiday.getName(), date, date, true));
            }
        } catch (Exception ex) {
            Logger.getLogger(Calendar.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public ScheduleModel getEventModel() {
        return eventModel;
    }

}
