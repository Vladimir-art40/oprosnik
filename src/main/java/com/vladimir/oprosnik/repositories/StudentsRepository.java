package com.vladimir.oprosnik.repositories;

import com.vladimir.oprosnik.models.Student;
import org.springframework.data.repository.CrudRepository;

public interface StudentsRepository extends CrudRepository<Student, Integer> {
    Student getByUsername(String username);
    Iterable<Student> findAllByCls(String cls);
}