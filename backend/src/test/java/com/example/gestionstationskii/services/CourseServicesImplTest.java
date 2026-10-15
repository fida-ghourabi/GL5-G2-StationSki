package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.entities.Registration;
import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.entities.Support;
import com.example.gestionstationskii.entities.TypeCourse;
import com.example.gestionstationskii.repositories.ICourseRepository;
import com.example.gestionstationskii.repositories.IInstructorRepository;
import com.example.gestionstationskii.repositories.IRegistrationRepository;
import com.example.gestionstationskii.repositories.PlanningSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServicesImplTest {

    @Mock private ICourseRepository courseRepository;
    @Mock private IRegistrationRepository registrationRepository;
    @Mock private PlanningSlotRepository slotRepository;
    @Mock private IInstructorRepository instructorRepository;

    @InjectMocks private CourseServicesImpl service;

    // ------------------------------------------------------------------ fixtures

    private static Course course(Long id, int level, TypeCourse type, Support support) {
        Course c = new Course();
        c.setNumCourse(id);
        c.setLevel(level);
        c.setTypeCourse(type);
        c.setSupport(support);
        c.setPrice(100f);
        c.setTimeSlot(1);
        return c;
    }

    /** Valid course without id (creation payload). */
    private static Course newCourse() {
        return course(null, 1, TypeCourse.COLLECTIVE_ADULT, Support.SKI);
    }

    /** Valid persisted course with id 1. */
    private static Course existing() {
        return course(1L, 1, TypeCourse.COLLECTIVE_ADULT, Support.SKI);
    }

    private static Registration registration(long skierId, boolean cancelled) {
        Skier skier = new Skier();
        skier.setNumSkier(skierId);
        Registration r = new Registration();
        r.setSkier(skier);
        r.setCancelled(cancelled);
        return r;
    }

    private static void assertBusiness(HttpStatus expected, Executable action) {
        BusinessException ex = assertThrows(BusinessException.class, action);
        assertEquals(expected, ex.getStatus());
    }

    private static Arguments change(String label, Consumer<Course> mutation) {
        return Arguments.of(label, mutation);
    }

    /** Every way a course can be invalid (shared by add and update). */
    static Stream<Arguments> invalidCourses() {
        return Stream.of(
                change("level = 0", c -> c.setLevel(0)),
                change("level < 0", c -> c.setLevel(-3)),
                change("typeCourse null", c -> c.setTypeCourse(null)),
                change("support null", c -> c.setSupport(null)),
                change("price null", c -> c.setPrice(null)),
                change("price < 0", c -> c.setPrice(-1f)),
                change("price NaN", c -> c.setPrice(Float.NaN)),
                change("price infinite", c -> c.setPrice(Float.POSITIVE_INFINITY)),
                change("timeSlot < 0", c -> c.setTimeSlot(-1)));
    }

    /** Changes to type / support / level, which are frozen once a course is used. */
    static Stream<Arguments> immutableFieldChanges() {
        return Stream.of(
                change("typeCourse", c -> c.setTypeCourse(TypeCourse.INDIVIDUAL)),
                change("support", c -> c.setSupport(Support.SNOWBOARD)),
                change("level", c -> c.setLevel(3)));
    }

    // ------------------------------------------------------------------ retrieveAllCourses

    @Test
    void retrieveAllCourses_returnsRepositoryContent() {
        List<Course> courses = Arrays.asList(existing(), course(2L, 2, TypeCourse.INDIVIDUAL, Support.SNOWBOARD));
        when(courseRepository.findAll()).thenReturn(courses);

        assertSame(courses, service.retrieveAllCourses());
    }

    // ------------------------------------------------------------------ addCourse

    @Test
    void addCourse_savesAndResetsRegistrations() {
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        Course input = newCourse();
        input.setRegistrations(new HashSet<>(Collections.singleton(new Registration())));

        Course saved = service.addCourse(input);

        assertNotNull(saved.getRegistrations());
        assertTrue(saved.getRegistrations().isEmpty());
        verify(courseRepository).save(input);
    }

    @Test
    void addCourse_rejectsPreassignedId() {
        Course input = newCourse();
        input.setNumCourse(7L);

        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.addCourse(input));
        verifyNoInteractions(courseRepository);
    }

    @ParameterizedTest(name = "addCourse rejects: {0}")
    @MethodSource("invalidCourses")
    void addCourse_rejectsInvalidData(String label, Consumer<Course> corruption) {
        Course input = newCourse();
        corruption.accept(input);

        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.addCourse(input));
        verifyNoInteractions(courseRepository);
    }

    // ------------------------------------------------------------------ updateCourse

    @ParameterizedTest(name = "updateCourse rejects: {0}")
    @MethodSource("invalidCourses")
    void updateCourse_rejectsInvalidData(String label, Consumer<Course> corruption) {
        Course input = existing();
        corruption.accept(input);

        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.updateCourse(input));
        verifyNoInteractions(courseRepository);
    }

    @ParameterizedTest(name = "updateCourse rejects id = {0}")
    @NullSource
    @ValueSource(longs = {0, -1})
    void updateCourse_rejectsInvalidId(Long id) {
        Course input = newCourse();
        input.setNumCourse(id);

        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.updateCourse(input));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void updateCourse_missingCourse_isNotFound() {
        when(courseRepository.lockById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.updateCourse(existing()));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updateCourse_notLinked_copiesEveryEditableField() {
        Course current = existing();
        when(courseRepository.lockById(1L)).thenReturn(Optional.of(current));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        Course update = course(1L, 3, TypeCourse.INDIVIDUAL, Support.SNOWBOARD);
        update.setPrice(250f);
        update.setTimeSlot(4);

        Course saved = service.updateCourse(update);

        assertSame(current, saved);
        assertAll(
                () -> assertEquals(3, saved.getLevel()),
                () -> assertEquals(TypeCourse.INDIVIDUAL, saved.getTypeCourse()),
                () -> assertEquals(Support.SNOWBOARD, saved.getSupport()),
                () -> assertEquals(Float.valueOf(250f), saved.getPrice()),
                () -> assertEquals(4, saved.getTimeSlot()));
    }

    @ParameterizedTest(name = "frozen when registrations exist: {0}")
    @MethodSource("immutableFieldChanges")
    void updateCourse_withRegistrations_refusesImmutableChange(String label, Consumer<Course> mutation) {
        when(courseRepository.lockById(1L)).thenReturn(Optional.of(existing()));
        when(registrationRepository.existsByCourse_NumCourse(1L)).thenReturn(true);
        Course update = existing();
        mutation.accept(update);

        assertBusiness(HttpStatus.CONFLICT, () -> service.updateCourse(update));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @ParameterizedTest(name = "frozen when slots exist: {0}")
    @MethodSource("immutableFieldChanges")
    void updateCourse_withSlots_refusesImmutableChange(String label, Consumer<Course> mutation) {
        when(courseRepository.lockById(1L)).thenReturn(Optional.of(existing()));
        when(slotRepository.existsByCourse_NumCourse(1L)).thenReturn(true);
        Course update = existing();
        mutation.accept(update);

        assertBusiness(HttpStatus.CONFLICT, () -> service.updateCourse(update));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updateCourse_linked_stillAllowsPriceAndTimeSlotChange() {
        Course current = existing();
        when(courseRepository.lockById(1L)).thenReturn(Optional.of(current));
        when(registrationRepository.existsByCourse_NumCourse(1L)).thenReturn(true);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        Course update = existing();
        update.setPrice(999f);
        update.setTimeSlot(8);

        Course saved = service.updateCourse(update);

        assertEquals(Float.valueOf(999f), saved.getPrice());
        assertEquals(8, saved.getTimeSlot());
    }

    // ------------------------------------------------------------------ retrieveCourse

    @Test
    void retrieveCourse_found() {
        Course c = existing();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(c));

        assertSame(c, service.retrieveCourse(1L));
    }

    @Test
    void retrieveCourse_missing_isNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.retrieveCourse(1L));
    }

    @ParameterizedTest(name = "retrieveCourse rejects id = {0}")
    @NullSource
    @ValueSource(longs = {0, -1})
    void retrieveCourse_rejectsInvalidId(Long id) {
        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.retrieveCourse(id));
        verifyNoInteractions(courseRepository);
    }

    // ------------------------------------------------------------------ removeCourse

    @Test
    void removeCourse_unused_deletesIt() {
        Course c = existing();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(c));

        service.removeCourse(1L);

        verify(courseRepository).delete(c);
    }

    @ParameterizedTest(name = "removeCourse refused: registrations={0}, slots={1}, instructor={2}")
    @CsvSource({"true,false,false", "false,true,false", "false,false,true"})
    void removeCourse_inUse_isRefused(boolean registrations, boolean slots, boolean instructor) {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing()));
        if (registrations) when(registrationRepository.existsByCourse_NumCourse(1L)).thenReturn(true);
        if (slots) when(slotRepository.existsByCourse_NumCourse(1L)).thenReturn(true);
        if (instructor) when(instructorRepository.findByCourses_NumCourse(1L)).thenReturn(Collections.singletonList(new Instructor()));

        assertBusiness(HttpStatus.CONFLICT, () -> service.removeCourse(1L));
        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    void removeCourse_missing_isNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.removeCourse(1L));
        verify(courseRepository, never()).delete(any(Course.class));
    }

    // ------------------------------------------------------------------ search

    private static List<Course> catalog() {
        return Arrays.asList(
                course(1L, 1, TypeCourse.COLLECTIVE_CHILDREN, Support.SKI),
                course(2L, 2, TypeCourse.COLLECTIVE_ADULT, Support.SKI),
                course(3L, 1, TypeCourse.INDIVIDUAL, Support.SNOWBOARD),
                course(4L, 2, TypeCourse.COLLECTIVE_ADULT, Support.SNOWBOARD));
    }

    static Stream<Arguments> searchCases() {
        return Stream.of(
                Arguments.of(null, null, null, Arrays.asList(1L, 2L, 3L, 4L)),
                Arguments.of(TypeCourse.COLLECTIVE_ADULT, null, null, Arrays.asList(2L, 4L)),
                Arguments.of(null, Support.SKI, null, Arrays.asList(1L, 2L)),
                Arguments.of(null, null, 1, Arrays.asList(1L, 3L)),
                Arguments.of(TypeCourse.COLLECTIVE_ADULT, Support.SNOWBOARD, null, Collections.singletonList(4L)),
                Arguments.of(TypeCourse.COLLECTIVE_ADULT, Support.SKI, 2, Collections.singletonList(2L)),
                Arguments.of(TypeCourse.INDIVIDUAL, Support.SKI, null, Collections.<Long>emptyList()));
    }

    @ParameterizedTest(name = "search(type={0}, support={1}, level={2}) -> {3}")
    @MethodSource("searchCases")
    void search_filtersOnEveryCriterion(TypeCourse type, Support support, Integer level, List<Long> expectedIds) {
        when(courseRepository.findAll()).thenReturn(catalog());

        List<Long> ids = service.search(type, support, level).stream()
                .map(Course::getNumCourse).collect(Collectors.toList());

        assertEquals(expectedIds, ids);
    }

    @ParameterizedTest(name = "search rejects level = {0}")
    @ValueSource(ints = {0, -1})
    void search_rejectsNonPositiveLevel(int level) {
        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.search(null, null, level));
        verifyNoInteractions(courseRepository);
    }

    // ------------------------------------------------------------------ registrations

    @Test
    void registrations_returnsRegistrationsOfTheCourse() {
        List<Registration> registrations = Collections.singletonList(registration(1, false));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing()));
        when(registrationRepository.findByCourse_NumCourse(1L)).thenReturn(registrations);

        assertSame(registrations, service.registrations(1L));
    }

    @Test
    void registrations_missingCourse_isNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.registrations(1L));
        verifyNoInteractions(registrationRepository);
    }

    // ------------------------------------------------------------------ participants

    static Stream<Arguments> participantCases() {
        return Stream.of(
                Arguments.of("no registration", Collections.<Registration>emptyList(), 0L),
                Arguments.of("same skier registered twice counts once",
                        Arrays.asList(registration(1, false), registration(1, false)), 1L),
                Arguments.of("cancelled registrations are ignored",
                        Arrays.asList(registration(1, true), registration(2, true)), 0L),
                Arguments.of("mix of duplicates, cancelled and distinct",
                        Arrays.asList(registration(1, false), registration(1, false),
                                registration(2, true), registration(3, false)), 2L));
    }

    @ParameterizedTest(name = "participants: {0}")
    @MethodSource("participantCases")
    void participants_countsDistinctActiveSkiers(String label, List<Registration> registrations, long expected) {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing()));
        when(registrationRepository.findByCourse_NumCourse(1L)).thenReturn(registrations);

        assertEquals(expected, service.participants(1L));
    }

    // ------------------------------------------------------------------ instructors

    @Test
    void instructors_returnsInstructorsOfTheCourse() {
        List<Instructor> instructors = Collections.singletonList(new Instructor());
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing()));
        when(instructorRepository.findByCourses_NumCourse(1L)).thenReturn(instructors);

        assertSame(instructors, service.instructors(1L));
    }

    @Test
    void instructors_missingCourse_isNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.instructors(1L));
        verifyNoInteractions(instructorRepository);
    }
}