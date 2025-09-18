package com.justjava.devFlow.tasks;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "form")
public class Form {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "form_name", nullable = false)
    private String formName;
    @Column(name = "form_code", nullable = false)
    private String formCode;

}