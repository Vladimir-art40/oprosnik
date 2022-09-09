package com.vladimir.oprosnik.controllers;

import com.vladimir.oprosnik.models.Student;
import com.vladimir.oprosnik.repositories.StudentsRepository;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Controller
@RequestMapping("/admin")
public class Admin {
    final StudentsRepository studentsRepository;

    public Admin(StudentsRepository studentsRepository) {
        this.studentsRepository = studentsRepository;
    }
    private static int selected = 1;

    private ArrayList<Student> getAllSorted(){
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

        return students;
    }

    @GetMapping("")
    private String main(@RequestParam(defaultValue = "0") int sel, Principal principal, Model model){
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if(sel != 0)
            selected = sel;

        ArrayList<String> cond = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if(selected == i + 1)
                cond.add("nav-link text-white active");
            else
                cond.add("nav-link text-white");
        }
        model.addAttribute("cond", cond);

        if(selected == 1 || selected == 2){
            ArrayList<Student> students = getAllSorted();
            ArrayList<String> clss = new ArrayList<>();
            for (Student student:
                 students) {
                if(!clss.contains(student.getCls())){
                    clss.add(student.getCls());
                }
            }
            Map<String, ArrayList<Student>> studentsByCls = new HashMap<>();
            for (String cls:
                 clss) {
                ArrayList<Student> f = new ArrayList<>();
                for (Student student:
                        students) {
                    if(student.getCls().equals(cls)){
                        f.add(student);
                    }
                }
                studentsByCls.put(cls, f);
            }
            model.addAttribute("clss", clss);
            model.addAttribute("students", studentsByCls);
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

    @GetMapping("getAll")
    private @ResponseBody String getAll(@RequestParam(defaultValue = "all") String cls, Principal principal){
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        StringBuilder ans = new StringBuilder();
        if(!cls.equals("all")){
            for (Student s:
                    studentsRepository.findAllByCls(cls)) {
                ans.append(s.getCls()).append(",").append(s.getName()).append(",").append(s.getUsername()).append(",").append(s.getPassword()).append(",").append(s.getAnswers()).append("\n");
            }
            return ans.toString();
        }

        ArrayList<Student> students = getAllSorted();

        for (Student s:
             students) {
            ans.append(s.getCls()).append(",").append(s.getName()).append(",").append(s.getUsername()).append(",").append(s.getPassword()).append(",").append(s.getAnswers()).append("\n");
        }
        return ans.toString();
    }

    @PostMapping("addStudent")
    private @ResponseBody String addStudent(@RequestParam String cls, @RequestParam String name, Principal principal){
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        int r = ThreadLocalRandom.current().nextInt(0, 1001);
        while (studentsRepository.countStudentByUsername("student_" + cls + "_" + r) != 0)
            r = ThreadLocalRandom.current().nextInt(0, 1001);

        Student student = new Student();
        student.setCls(cls);
        student.setName(name);
        student.setUsername("student_" + cls + "_" + r);
        student.setPassword(genPassword());
        studentsRepository.save(student);

        return "OK";
    }

    @PostMapping("addStudents")
    private @ResponseBody String addStudents(@RequestParam("file") MultipartFile file, Principal principal) throws IOException {
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        ArrayList<Student> to_add = new ArrayList<>();

        XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream());
        for (int sheet_number = 0; sheet_number < wb.getNumberOfSheets(); sheet_number++) {
            XSSFSheet sheet = wb.getSheetAt(sheet_number);
            String cls = sheet.getRow(0).getCell(0).getStringCellValue().strip().split(" ")[0];
            if(studentsRepository.countStudentByCls(cls) > 0){
                return "Failed, class " + cls + " already exists.";
            }
            int poz = 2;
            int r = ThreadLocalRandom.current().nextInt(0, 1001);
            while (sheet.getRow(poz) != null && sheet.getRow(poz).getCell(0) != null && sheet.getRow(poz).getCell(0).getCellType() == CellType.NUMERIC){
                String name = sheet.getRow(poz).getCell(1).getStringCellValue();
                Student student = new Student();
                student.setCls(cls);
                student.setName(name);
                student.setUsername("student_" + cls + "_" + (r + poz));
                student.setPassword(genPassword());
                to_add.add(student);
                poz++;
            }
        }

        studentsRepository.saveAll(to_add);
        return "OK";
    }

    @GetMapping("listStudents")
    private String getStudents(@RequestParam String cls, @RequestParam boolean otv, Principal principal, Model model){
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        ArrayList<Student> students = new ArrayList<>();
        for (Student s:
                studentsRepository.findAllByCls(cls)) {
            students.add(s);
        }
        model.addAttribute("cls", cls);
        model.addAttribute("students", students);
        model.addAttribute("otv", otv);

        return "listStudents";
    }

    @GetMapping("drop")
    private @ResponseBody String drop(Principal principal){
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        studentsRepository.deleteAll();
        return "OK";
    }

    @GetMapping("download")
    private @ResponseBody ResponseEntity<Resource> download(Principal principal) throws FileNotFoundException, MalformedURLException {
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        PrintWriter p = new PrintWriter("tmp.csv");
        ArrayList<Student> students = getAllSorted();
        for (Student s:
                students) {
            p.println(s.getCls() + "," + s.getName() + "," + s.getAnswers());
        }
        p.flush();
        p.close();

        Resource resource = new UrlResource(new File("tmp.csv").toPath().toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("eraseAnswer")
    private @ResponseBody String eraseAnswer(@RequestParam String username, Principal principal){
        if(!principal.getName().equals("admin")){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Student student = studentsRepository.getByUsername(username);
        student.setAnswers(null);
        studentsRepository.save(student);

        return "OK";
    }
}
