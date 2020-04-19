package ru.ifmo.rain.bobrov.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {

    final private Comparator<Student> STUDENT_COMP = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::compareTo);

    private Predicate<Student> equalsBy(Function<Student, String> fun, String equalTo) {
        return (Student s) -> (fun.apply(s).equals(equalTo));
    }

    private Stream<String> mapStream(Function<Student, String> fun, List<Student> collection) {
        return collection.stream().map(fun);
    }

    private List<Student> sortStream(Comparator<Student> comp, Collection<Student> collection) {
        return collection.stream().sorted(comp).collect(Collectors.toList());
    }

    private Stream<Student> filterStream(Predicate<Student> fun, Collection<Student> collection) {
        return collection.stream().filter(fun).sorted(STUDENT_COMP);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapStream(Student::getFirstName, students)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapStream(Student::getLastName, students).collect(Collectors.toList());
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapStream(Student::getGroup, students).collect(Collectors.toList());
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapStream((Student s) -> (s.getFirstName() + " " + s.getLastName()), students)
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapStream(Student::getFirstName, students)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students
                .stream()
                .min(Student::compareTo)
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStream(Student::compareTo, students);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStream(STUDENT_COMP, students);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterStream(equalsBy(Student::getFirstName, name), students)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterStream(equalsBy(Student::getLastName, name), students)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return filterStream(equalsBy(Student::getGroup, group), students)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return filterStream(equalsBy(Student::getGroup, group), students)
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }
}
