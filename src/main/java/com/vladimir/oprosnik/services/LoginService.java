package com.vladimir.oprosnik.services;

import com.vladimir.oprosnik.models.Student;
import com.vladimir.oprosnik.repositories.StudentsRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class LoginService implements UserDetailsService {
    final StudentsRepository studentsRepository;
    public LoginService(StudentsRepository studentsRepository) {
        this.studentsRepository = studentsRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if(Objects.equals(username, "admin")){
            return User.builder()
                    .username("admin")
                    .password("Vladimir_132")
                    .roles("ADMIN")
                    .build();
        }

        Student user = studentsRepository.getByUsername(username);

        if (user == null)
            throw new UsernameNotFoundException("User not found");


        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles("STUDENT")
                .build();
    }
}