package com.vladimir.oprosnik.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Getter
@Setter
public class Student{
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private int id;

    private String cls;
    private String name;

    private String username;
    private String password;

    private String answers;
}
