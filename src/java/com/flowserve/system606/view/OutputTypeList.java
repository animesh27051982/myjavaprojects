/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.OutputType;
import com.flowserve.system606.service.InputService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author shubhamv
 */
@Named
@ViewScoped
public class OutputTypeList implements Serializable {

    List<OutputType> outputTypes = new ArrayList<OutputType>();
    @Inject
    InputService inputService;

    public List<OutputType> getOutputTypes() throws Exception {
        outputTypes = inputService.findOutputType();
        Collections.sort(outputTypes);
        return outputTypes;
    }

    public void setOutputTypes(List<OutputType> outputTypes) {
        this.outputTypes = outputTypes;
    }

}
