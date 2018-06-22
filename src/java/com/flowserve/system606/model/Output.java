package com.flowserve.system606.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import static javax.persistence.TemporalType.TIMESTAMP;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "VALUE_TYPE")
@Table(name = "OUTPUTS")
public abstract class Output<T> extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FLS_SEQ")
    @SequenceGenerator(name = "FLS_SEQ", sequenceName = "FLS_SEQ", allocationSize = 1)
    @Column(name = "OUTPUT_ID")
    private Long id;

    @OneToOne
    @JoinColumn(name = "OUTPUT_TYPE_ID")
    private InputType outputType;
    @Column(name = "CURRENCY_CODE")
    private String currencyCode;  // Consider JavaMoney, etc.,  java.util.Currency.. or our own Currency class.
    @OneToOne
    @JoinColumn(name = "CREATED_BY_ID")
    private User createdBy;
    @Temporal(TIMESTAMP)
    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;
    @Column(name = "IS_VALID")
    private boolean valid;
    @Column(name = "MESSAGE", length = 2048)
    private String message;

    public Output() {
    }

    public abstract T getValue();

    public abstract void setValue(T value);

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public InputType getOutputType() {
        return outputType;
    }

    public void setOutputType(InputType outputType) {
        this.outputType = outputType;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
