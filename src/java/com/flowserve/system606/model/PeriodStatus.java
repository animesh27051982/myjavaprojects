/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

/**
 *
 * @author kgraves
 */
public enum PeriodStatus {
    OPEN("O"), CLOSED("C");

    private String shortName;

    private PeriodStatus(String shortName) {
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }

    public static PeriodStatus fromShortName(String shortName) {
        switch (shortName) {
            case "O":
                return PeriodStatus.OPEN;

            case "C":
                return PeriodStatus.CLOSED;

            default:
                throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
        }
    }
}
