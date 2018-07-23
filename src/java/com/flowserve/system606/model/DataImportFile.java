/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 *
 * @author shubhamv
 */
@Entity
@Table(name = "DATA_IMPORT_FILE")
public class DataImportFile implements Comparable<DataImportFile>, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FLS_SEQ")
    @SequenceGenerator(name = "FLS_SEQ", sequenceName = "FLS_SEQ", allocationSize = 1)
    @Column(name = "DATA_IMPORT_ID")
    private String id;
    @Column(name = "FILE_NAME")
    private String filename;
    @OneToOne
    @JoinColumn(name = "COMPANY_ID")
    private Company company;
    @Column(name = "UPLOAD_DATE")
    private LocalDate uploadDate;
    @Column(name = "UPLOADED_BY")
    private String uploadedBy;
    @Column(name = "STATUS")
    private String status;

    @ElementCollection
    @CollectionTable(name = "IMPORT_MESSAGE")
    private List<String> DataImportMessage = new ArrayList<String>();

    public DataImportFile() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public LocalDate getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDate uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getDataImportMessage() {
        return DataImportMessage;
    }

    public void setDataImportMessage(List<String> DataImportMessage) {
        this.DataImportMessage = DataImportMessage;
    }

    @Override
    public int compareTo(DataImportFile o) {
        return this.filename.compareTo(o.getFilename());
    }

}
