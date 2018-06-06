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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "SYSTEM_USERS")
public class User implements Comparable<User>, Serializable {

    private static final long serialVersionUID = -4257640938927294079L;
    private static final Logger LOG = Logger.getLogger(User.class.getName());

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FLS_SEQ")
    @SequenceGenerator(name = "FLS_SEQ", sequenceName = "FLS_SEQ", allocationSize = 1)
    @Column(name = "USER_ID")
    private Long id;
    @Column(name = "FLS_ID", nullable = false, length = 30)
    private String flsId;
    @Column(nullable = false, length = 512)
    private String name;
    @Column(name = "EMAIL_ADDRESS", length = 512)
    private String emailAddress;

    @Override
    public int compareTo(User obj) {
        return this.name.compareTo(obj.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            return this.id.equals(((User) obj).getId());
        }
        return false;
    }

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFlsId() {
        return flsId;
    }

    public void setFlsId(String flsId) {
        this.flsId = flsId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
