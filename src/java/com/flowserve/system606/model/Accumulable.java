/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.util.List;

/**
 *
 * @author kgraves
 */
public interface Accumulable {

    public List<Accumulable> getChildAccumulables();
}
