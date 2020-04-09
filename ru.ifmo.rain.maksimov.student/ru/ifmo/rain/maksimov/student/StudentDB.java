package ru.ifmo.rain.maksimov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {

    private String getStudentFullName(Student student) {
        return student.getFirstName() + ' ' + student.getLastName();
    }

    private Stream<Map.Entry<String, List<Student>>> getAnyGroupsStream(Collection<Student> students, Supplier<Map<String, List<Student>>> mapSupplier) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup, mapSupplier, Collectors.toList()))
                .entrySet()
                .stream();
    }

    private Stream<Map.Entry<String, List<Student>>> getGroupsStream(Collection<Student> students) {
        return getAnyGroupsStream(students, HashMap::new);
    }

    private Stream<Map.Entry<String, List<Student>>> getSortedGroupsStream(Collection<Student> students) {
        return getAnyGroupsStream(students, TreeMap::new);
    }

    private List<Group> getSortedGroups(Stream<Map.Entry<String, List<Student>>> groups, UnaryOperator<List<Student>> sorter) {
        return groups.map(entry -> new Group(entry.getKey(), sorter.apply(entry.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getSortedGroups(getSortedGroupsStream(students), this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getSortedGroups(getSortedGroupsStream(students), this::sortStudentsById);
    }

    private String getLargestGroupBy(Stream<Map.Entry<String, List<Student>>> groups, ToIntFunction<List<Student>> calc) {
        return groups
                .max(Comparator
                        .comparingInt((Map.Entry<String, List<Student>> group) -> calc.applyAsInt(group.getValue()))
                        .thenComparing(Map.Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Map.Entry::getKey)
                .orElse("");
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupBy(getGroupsStream(students), List::size);
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupBy(getGroupsStream(students), studentsList -> getDistinctFirstNames(studentsList).size());
    }

    private <C extends Collection<String>> C mapStudentsCollection(List<Student> students, Function<Student, String> mapper, Supplier<C> collection) {
        return students.stream().map(mapper).collect(Collectors.toCollection(collection));
    }

    private List<String> mapStudentsList(List<Student> students, Function<Student, String> mapper) {
        return mapStudentsCollection(students, mapper, ArrayList::new);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapStudentsList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapStudentsList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapStudentsList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapStudentsList(students, this::getStudentFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapStudentsCollection(students, Student::getFirstName, TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    private List<Student> sortStreamStudents(Stream<Student> stream, Comparator<Student> comparator) {
        return stream.sorted(comparator).collect(Collectors.toList());
    }

    private List<Student> sortStudents(Collection<Student> students, Comparator<Student> comparator) {
        return sortStreamStudents(students.stream(), comparator);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudents(students, Student::compareTo);
    }

    private static final Comparator<Student> nameComparator = Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId);

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudents(students, nameComparator);
    }

    private Predicate<Student> getPredicate(String query, Function<Student, String> getter) {
        return student -> query.equals(getter.apply(student));
    }

    private Stream<Student> filterStudentsStream(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().filter(predicate);
    }

    private List<Student> filterAndSortByName(Collection<Student> students, Predicate<Student> predicate) {
        return sortStreamStudents(filterStudentsStream(students, predicate), nameComparator);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterAndSortByName(students, getPredicate(name, Student::getFirstName));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterAndSortByName(students, getPredicate(name, Student::getLastName));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return filterAndSortByName(students, getPredicate(group, Student::getGroup));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return filterStudentsStream(students, getPredicate(group, Student::getGroup))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    /**
     * Returns a map from full name to count of groups that contains a student with this full name
     */
    private Map<String, Integer> nameMap(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(this::getStudentFullName,
                        Collectors.mapping(Student::getGroup,
                                Collectors.collectingAndThen(Collectors.toSet(), Set::size))));
    }

    private String getPriorityStudent(Map<String, Integer> priority) {
        return priority.entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry::getKey))
                .orElse(Map.entry("", 0))
                .getKey();
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getPriorityStudent(nameMap(students));
    }

    private List<String> getStudentsFieldAsList(Collection<Student> students, Function<Student, String> toGet) {
        return students.stream()
                .map(toGet)
                .collect(Collectors.toList());
    }

    private List<String> getByIndices(List<String> students, int[] indices) {
        return Arrays.stream(indices)
                .mapToObj(students::get)
                .collect(Collectors.toList());
    }

    private List<String> getSomething(Collection<Student> students, int[] indices, Function<Student, String> toGet) {
        return getByIndices(getStudentsFieldAsList(students, toGet), indices);
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getSomething(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getSomething(students, indices, Student::getLastName);
    }

    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return getSomething(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getSomething(students, indices, this::getStudentFullName);
    }
}
