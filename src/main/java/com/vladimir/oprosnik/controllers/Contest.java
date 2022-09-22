package com.vladimir.oprosnik.controllers;

import com.vladimir.oprosnik.Questions;
import com.vladimir.oprosnik.models.Student;
import com.vladimir.oprosnik.repositories.StudentsRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Scanner;

@Controller
@RequestMapping("/")
public class Contest {
    final StudentsRepository studentsRepository;

    public Contest(StudentsRepository studentsRepository) {
        this.studentsRepository = studentsRepository;
    }

    private int getClsNum(String cls){
        int i = 1;
        while (i < cls.length() && Character.isDigit(cls.toCharArray()[i]))
            i++;
        return Integer.parseInt(cls.substring(0, i));
    }

    @GetMapping("")
    private String getMain(Principal principal, Model model) {
        String username = principal.getName();
        if(username.equals("admin")){
            return "redirect:/admin";
        }

        Student student = studentsRepository.getByUsername(username);

        if(student.getAnswers() != null){
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        model.addAttribute("student", student);

        String questions_raw;
        if(getClsNum(student.getCls()) >= 10){
            questions_raw = Questions.q10_11;
        }else{
            questions_raw = Questions.q7_9;
        }
        String[] qu = questions_raw.split("\n");

        ArrayList<String> questions = new ArrayList<>();
        ArrayList<Integer> qnum = new ArrayList<>();
        int i = 0;
        for (String y:
             qu) {
            questions.add(y.strip());
            qnum.add(i++);
        }
        model.addAttribute("qnum", qnum);
        model.addAttribute("questions", questions);
        return "opros";
    }

    @PostMapping("save")
    private @ResponseBody String save(HttpEntity<String> httpEntity, Principal principal){
        String json = httpEntity.getBody();
        assert json != null;
        String[] answers = json.split("&");
        StringBuilder ans = new StringBuilder();
        for (int i = 1; i < answers.length; i++) {
            ans.append(4 - Integer.parseInt(answers[i].split("=")[1]));
        }
        String username = principal.getName();
        Student student = studentsRepository.getByUsername(username);
        student.setAnswers(ans.toString());
        studentsRepository.save(student);
        return "Спасибо за прохождения опроса, тут должна быть страница, но её нет.";
    }
}
