package com.vladimir.oprosnik.controllers;

import com.vladimir.oprosnik.models.Student;
import com.vladimir.oprosnik.repositories.StudentsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

@Controller
@RequestMapping("/admin")
public class Admin {
    final StudentsRepository studentsRepository;

    public Admin(StudentsRepository studentsRepository) {
        this.studentsRepository = studentsRepository;
    }

    @GetMapping("")
    private String get(Principal principal){
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        return "admin";
    }

    private int getClsNum(String cls){
        int i = 1;
        while (i < cls.length() && Character.isDigit(cls.toCharArray()[i]))
            i++;
        return Integer.parseInt(cls.substring(0, i));
    }

    private String genPassword() {
        char[] r = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            s.append(r[ThreadLocalRandom.current().nextInt(0, r.length)]);
        }
        return s.toString();
    }

    @GetMapping("addUser")
    private @ResponseBody String addUser(@RequestParam int count, @RequestParam String cls, Principal principal){
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        int r = ThreadLocalRandom.current().nextInt(0, 1001);
        for (int i = 0; i < count; i++) {
            Student student = new Student();
            student.setCls(cls);
            student.setName("Имя то где брать");
            student.setUsername("student_" + cls + "_" + (r + i));
            student.setPassword(genPassword());
            studentsRepository.save(student);
        }
        return "OK";
    }

    @GetMapping("getAll")
    private @ResponseBody String getAll(@RequestParam(defaultValue = "all") String cls, Principal principal){
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }

        StringBuilder ans = new StringBuilder();
        if(!cls.equals("all")){
            for (Student s:
                    studentsRepository.findAllByCls(cls)) {
                ans.append(s.getCls()).append(",").append(s.getName()).append(",").append(s.getUsername()).append(",").append(s.getPassword()).append(",").append(s.getAnswers()).append("\n");
            }
            return ans.toString();
        }

        ArrayList<Student> students = new ArrayList<>();
        for (Student student:
                studentsRepository.findAll()) {
            students.add(student);
        }

        students.sort((lhs, rhs) -> {
            int lcls = getClsNum(lhs.getCls());
            int rcls = getClsNum(rhs.getCls());
            if(lcls != rcls)
                return Integer.compare(lcls, rcls);
            if(lhs.getCls().compareTo(rhs.getCls()) != 0)
                return lhs.getCls().compareTo(rhs.getCls());
            return lhs.getName().compareTo(rhs.getName());
        });

        for (Student s:
             students) {
            ans.append(s.getCls()).append(",").append(s.getName()).append(",").append(s.getUsername()).append(",").append(s.getPassword()).append(",").append(s.getAnswers()).append("\n");
        }
        return ans.toString();
    }
}
