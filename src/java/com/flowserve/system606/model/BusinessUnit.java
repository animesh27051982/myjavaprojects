/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.util.logging.Logger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "BUSINESS_UNITS")

public class BusinessUnit implements Comparable<BusinessUnit>, Serializable {

    private static final long serialVersionUID = -5428359272300395184L;
    private static final Logger LOG = Logger.getLogger(BusinessUnit.class.getName());

    @Id
    @Column(name = "BUSINESS_UNIT_ID")
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "DESCRIPTION")
    private String description;

    @JoinColumn(name = "PARENT_ID")
    private BusinessUnit parent;

    public BusinessUnit() {
    }

    @Override
    public int compareTo(BusinessUnit obj) {
        return this.name.compareTo(obj.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BusinessUnit) {
            return this.name.equals(((BusinessUnit) obj).getName());
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BusinessUnit getParent() {
        return parent;
    }

    public void setParent(BusinessUnit parent) {
        this.parent = parent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
