/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.view;

import com.flowserve.system606.model.InputType;
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
public class InputTypeList implements Serializable {

    private static final long serialVersionUID = -1438027991420003830L;
    List<InputType> inputTypes = new ArrayList<InputType>();
    @Inject
    InputService inputService;

    public List<InputType> getInputTypes() throws Exception {
        inputTypes = inputService.findInputType();
        Collections.sort(inputTypes);
        return inputTypes;
    }

    public void setInputTypes(List<InputType> inputTypes) {
        this.inputTypes = inputTypes;
    }

}
