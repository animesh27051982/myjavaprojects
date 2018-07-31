/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

//@Entity
@Table(name = "SL_ACCOUNTS")
public class SubledgerAccount implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SL_ACCT_SEQ")
    @SequenceGenerator(name = "SL_ACCT_SEQ", sequenceName = "SL_ACCT_SEQ", allocationSize = 1)
    @Column(name = "SL_ACCT_ID")
    private Long id;
    @Column(name = "ACCT_NAME")
    private String name;
    @Column(name = "ACCT_DESC")
    private String description;
    @Column(name = "ACCT_CODE")
    private String code;
    @Column(name = "ACCT_TYPE")
    private String accountType;
    @OneToOne
    @JoinColumn(name = "COMPANY_ID")
    private Company company;
}
