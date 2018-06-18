package com.flowserve.system606.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "COUNTRIES")
public class Country extends BaseEntity<String> implements Comparable<Country> {

    @Id
    @Column(name = "COUNTRY_ID")
    private String id;
    @Column(name = "CODE")
    private String code;
    @Column(name = "NAME")
    private String name;

    public Country() {
    }

    public Country(String id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(Country o) {
        return this.name.compareTo(o.getName());
    }

    @Override
    public String toString() {
        return "Country{"
                + "id='" + id + '\''
                + ", code='" + code + '\''
                + ", name='" + name + '\''
                + '}';
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
