package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.entities.PlanningSlot;
import com.example.gestionstationskii.entities.SlotStatus;
import com.example.gestionstationskii.repositories.ICourseRepository;
import com.example.gestionstationskii.repositories.IInstructorRepository;
import com.example.gestionstationskii.repositories.InstructorAvailabilityRepository;
import com.example.gestionstationskii.repositories.PlanningSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorServicesImplTest {

    @Mock private IInstructorRepository instructorRepository;
    @Mock private ICourseRepository courseRepository;
    @Mock private PlanningSlotRepository slotRepository;
    @Mock private InstructorAvailabilityRepository availabilityRepository;

    @InjectMocks private InstructorServicesImpl service;

    // ------------------------------------------------------------------ fixtures

    private static Instructor instructor(Long id, String firstName, String lastName) {
        Instructor i = new Instructor();
        i.setNumInstructor(id);
        i.setFirstName(firstName);
        i.setLastName(lastName);
        i.setDateOfHire(LocalDate.now()); // today is the boundary of the accepted range
        i.setCourses(new HashSet<>());
        return i;
    }

    private static Instructor instructor(Long id) {
        return instructor(id, "John", "Smith");
    }

    private static Course course(Long id) {
        Course c = new Course();
        c.setNumCourse(id);
        return c;
    }

    private static PlanningSlot slot(Course course, Instructor instructor, SlotStatus status, LocalDateTime end) {
        PlanningSlot s = new PlanningSlot();
        s.setCourse(course);
        s.setInstructor(instructor);
        s.setStatus(status);
        s.setStartTime(end.minusHours(1));
        s.setEndTime(end);
        return s;
    }

    private static String longText() {
        return new String(new char[256]).replace('\0', 'a');
    }

    private static void assertBusiness(HttpStatus expected, Executable action) {
        BusinessException ex = assertThrows(BusinessException.class, action);
        assertEquals(expected, ex.getStatus());
    }

    private static Arguments change(String label, Consumer<Instructor> mutation) {
        return Arguments.of(label, mutation);
    }

    /** Every way an instructor can be invalid (shared by add and update). */
    static Stream<Arguments> invalidInstructors() {
        return Stream.of(
                change("firstName null", i -> i.setFirstName(null)),
                change("firstName blank", i -> i.setFirstName("   ")),
                change("firstName > 255 chars", i -> i.setFirstName(longText())),
                change("lastName null", i -> i.setLastName(null)),
                change("lastName blank", i -> i.setLastName("")),
                change("lastName > 255 chars", i -> i.setLastName(longText())),
                change("dateOfHire null", i -> i.setDateOfHire(null)),
                change("dateOfHire in the future", i -> i.setDateOfHire(LocalDate.now().plusDays(1))));
    }

    // ------------------------------------------------------------------ addInstructor

    @ParameterizedTest(name = "addInstructor saves with an empty course set (input: {0})")
    @NullAndEmptySource
    void addInstructor_savesWithEmptyCourseSet(Set<Course> courses) {
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> inv.getArgument(0));
        Instructor input = instructor(null);
        input.setCourses(courses);

        Instructor saved = service.addInstructor(input);

        assertNotNull(saved.getCourses());
        assertTrue(saved.getCourses().isEmpty());
        verify(instructorRepository).save(input);
    }

    @Test
    void addInstructor_rejectsPreassignedId() {
        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.addInstructor(instructor(3L)));
        verifyNoInteractions(instructorRepository);
    }

    @Test
    void addInstructor_rejectsPreassignedCourses() {
        Instructor input = instructor(null);
        input.getCourses().add(course(1L));

        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.addInstructor(input));
        verifyNoInteractions(instructorRepository);
    }

    @ParameterizedTest(name = "addInstructor rejects: {0}")
    @MethodSource("invalidInstructors")
    void addInstructor_rejectsInvalidData(String label, Consumer<Instructor> corruption) {
        Instructor input = instructor(null);
        corruption.accept(input);

        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.addInstructor(input));
        verifyNoInteractions(instructorRepository);
    }

    // ------------------------------------------------------------------ retrieveAllInstructors

    @Test
    void retrieveAllInstructors_returnsRepositoryContent() {
        List<Instructor> all = Arrays.asList(instructor(1L), instructor(2L));
        when(instructorRepository.findAll()).thenReturn(all);

        assertSame(all, service.retrieveAllInstructors());
    }

    // ------------------------------------------------------------------ updateInstructor

    @ParameterizedTest(name = "updateInstructor rejects: {0}")
    @MethodSource("invalidInstructors")
    void updateInstructor_rejectsInvalidData(String label, Consumer<Instructor> corruption) {
        Instructor input = instructor(1L);
        corruption.accept(input);

        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.updateInstructor(input));
        verifyNoInteractions(instructorRepository);
    }

    @ParameterizedTest(name = "updateInstructor rejects id = {0}")
    @NullSource
    @ValueSource(longs = {0, -1})
    void updateInstructor_rejectsInvalidId(Long id) {
        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.updateInstructor(instructor(id)));
        verifyNoInteractions(instructorRepository);
    }

    @Test
    void updateInstructor_missing_isNotFound() {
        when(instructorRepository.findById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.updateInstructor(instructor(1L)));
        verify(instructorRepository, never()).save(any(Instructor.class));
    }

    @Test
    void updateInstructor_copiesIdentityFields_andKeepsCourses() {
        Instructor current = instructor(1L, "Old", "Name");
        Course kept = course(10L);
        current.getCourses().add(kept);
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(current));
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> inv.getArgument(0));
        Instructor update = instructor(1L, "New", "Person");
        update.setDateOfHire(LocalDate.now().minusYears(2));

        Instructor saved = service.updateInstructor(update);

        assertSame(current, saved);
        assertAll(
                () -> assertEquals("New", saved.getFirstName()),
                () -> assertEquals("Person", saved.getLastName()),
                () -> assertEquals(LocalDate.now().minusYears(2), saved.getDateOfHire()),
                () -> assertTrue(saved.getCourses().contains(kept)));
    }

    // ------------------------------------------------------------------ retrieveInstructor

    @Test
    void retrieveInstructor_found() {
        Instructor i = instructor(1L);
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(i));

        assertSame(i, service.retrieveInstructor(1L));
    }

    @Test
    void retrieveInstructor_missing_isNotFound() {
        when(instructorRepository.findById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.retrieveInstructor(1L));
    }

    @ParameterizedTest(name = "retrieveInstructor rejects id = {0}")
    @NullSource
    @ValueSource(longs = {0, -1})
    void retrieveInstructor_rejectsInvalidId(Long id) {
        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.retrieveInstructor(id));
        verifyNoInteractions(instructorRepository);
    }

    // ------------------------------------------------------------------ addInstructorAndAssignToCourse

    @Test
    void addInstructorAndAssignToCourse_savesThenAssigns() {
        Course c = course(10L);
        Instructor input = instructor(null);
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> {
            Instructor saved = inv.getArgument(0);
            if (saved.getNumInstructor() == null) saved.setNumInstructor(5L);
            return saved;
        });
        when(courseRepository.lockById(10L)).thenReturn(Optional.of(c));
        when(instructorRepository.findById(5L)).thenReturn(Optional.of(input));

        Instructor result = service.addInstructorAndAssignToCourse(input, 10L);

        assertEquals(Long.valueOf(5L), result.getNumInstructor());
        assertTrue(result.getCourses().contains(c));
        verify(instructorRepository, times(2)).save(input);
    }

    @Test
    void addInstructorAndAssignToCourse_missingCourse_isNotFound() {
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> {
            Instructor saved = inv.getArgument(0);
            saved.setNumInstructor(5L);
            return saved;
        });
        when(courseRepository.lockById(10L)).thenReturn(Optional.empty());

        // In production @Transactional rolls the first save back when this exception escapes.
        assertBusiness(HttpStatus.NOT_FOUND, () -> service.addInstructorAndAssignToCourse(instructor(null), 10L));
    }

    // ------------------------------------------------------------------ assignCourse

    @ParameterizedTest(name = "assignCourse rejects id={0}, courseId={1}")
    @CsvSource(value = {"null,1", "1,null", "0,1", "1,0", "-1,1", "1,-1"}, nullValues = "null")
    void assignCourse_rejectsInvalidIds(Long id, Long courseId) {
        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.assignCourse(id, courseId));
        verifyNoInteractions(courseRepository, instructorRepository);
    }

    @Test
    void assignCourse_missingCourse_isNotFound() {
        when(courseRepository.lockById(10L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.assignCourse(1L, 10L));
        verifyNoInteractions(instructorRepository);
    }

    @Test
    void assignCourse_missingInstructor_isNotFound() {
        when(courseRepository.lockById(10L)).thenReturn(Optional.of(course(10L)));
        when(instructorRepository.findById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.assignCourse(1L, 10L));
        verify(instructorRepository, never()).save(any(Instructor.class));
    }

    @Test
    void assignCourse_alreadyTakenByAnInstructor_isConflict() {
        when(courseRepository.lockById(10L)).thenReturn(Optional.of(course(10L)));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor(1L)));
        when(instructorRepository.findByCourses_NumCourse(10L)).thenReturn(Collections.singletonList(instructor(2L)));

        assertBusiness(HttpStatus.CONFLICT, () -> service.assignCourse(1L, 10L));
        verify(instructorRepository, never()).save(any(Instructor.class));
    }

    @Test
    void assignCourse_success_addsTheCourse() {
        Course c = course(10L);
        Instructor i = instructor(1L);
        when(courseRepository.lockById(10L)).thenReturn(Optional.of(c));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(i));
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> inv.getArgument(0));

        Instructor result = service.assignCourse(1L, 10L);

        assertTrue(result.getCourses().contains(c));
    }

    // ------------------------------------------------------------------ unassignCourse

    @ParameterizedTest(name = "unassignCourse rejects courseId = {0}")
    @NullSource
    @ValueSource(longs = {0, -1})
    void unassignCourse_rejectsInvalidCourseId(Long courseId) {
        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.unassignCourse(1L, courseId));
        verifyNoInteractions(courseRepository, instructorRepository, slotRepository);
    }

    @ParameterizedTest(name = "unassignCourse rejects instructor id = {0}")
    @NullSource
    @ValueSource(longs = {0, -1})
    void unassignCourse_rejectsInvalidInstructorId(Long id) {
        when(courseRepository.lockById(10L)).thenReturn(Optional.of(course(10L)));

        assertBusiness(HttpStatus.BAD_REQUEST, () -> service.unassignCourse(id, 10L));
        verifyNoInteractions(slotRepository);
    }

    @Test
    void unassignCourse_missingCourse_isNotFound() {
        when(courseRepository.lockById(10L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.unassignCourse(1L, 10L));
        verifyNoInteractions(instructorRepository, slotRepository);
    }

    @Test
    void unassignCourse_missingInstructor_isNotFound() {
        when(courseRepository.lockById(10L)).thenReturn(Optional.of(course(10L)));
        when(instructorRepository.findById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.unassignCourse(1L, 10L));
        verifyNoInteractions(slotRepository);
    }

    @ParameterizedTest(name = "unassignCourse refused by a future {0} slot")
    @EnumSource(value = SlotStatus.class, names = {"OPEN", "FULL"})
    void unassignCourse_withFutureActiveSlot_isConflict(SlotStatus status) {
        Course c = course(10L);
        Instructor i = instructor(1L);
        i.getCourses().add(c);
        when(courseRepository.lockById(10L)).thenReturn(Optional.of(c));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(i));
        when(slotRepository.findAll()).thenReturn(
                Collections.singletonList(slot(c, i, status, LocalDateTime.now().plusDays(1))));

        assertBusiness(HttpStatus.CONFLICT, () -> service.unassignCourse(1L, 10L));
        assertTrue(i.getCourses().contains(c));
        verify(instructorRepository, never()).save(any(Instructor.class));
    }

    static Stream<Arguments> slotsThatDoNotBlockUnassignment() {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        return Stream.of(
                Arguments.of("cancelled slot",
                        (BiFunction<Course, Instructor, PlanningSlot>) (c, i) -> slot(c, i, SlotStatus.CANCELLED, future)),
                Arguments.of("already finished slot",
                        (BiFunction<Course, Instructor, PlanningSlot>) (c, i) -> slot(c, i, SlotStatus.OPEN, LocalDateTime.now().minusHours(2))),
                Arguments.of("slot of another course",
                        (BiFunction<Course, Instructor, PlanningSlot>) (c, i) -> slot(course(99L), i, SlotStatus.OPEN, future)),
                Arguments.of("slot of another instructor",
                        (BiFunction<Course, Instructor, PlanningSlot>) (c, i) -> slot(c, instructor(99L), SlotStatus.OPEN, future)));
    }

    @ParameterizedTest(name = "unassignCourse allowed despite: {0}")
    @MethodSource("slotsThatDoNotBlockUnassignment")
    void unassignCourse_ignoresIrrelevantSlots(String label, BiFunction<Course, Instructor, PlanningSlot> slotFactory) {
        Course c = course(10L);
        Instructor i = instructor(1L);
        i.getCourses().add(c);
        when(courseRepository.lockById(10L)).thenReturn(Optional.of(c));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(i));
        when(slotRepository.findAll()).thenReturn(Collections.singletonList(slotFactory.apply(c, i)));
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> inv.getArgument(0));

        Instructor result = service.unassignCourse(1L, 10L);

        assertFalse(result.getCourses().contains(c));
    }

    @Test
    void unassignCourse_notAssigned_isConflict() {
        when(courseRepository.lockById(10L)).thenReturn(Optional.of(course(10L)));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor(1L)));

        assertBusiness(HttpStatus.CONFLICT, () -> service.unassignCourse(1L, 10L));
        verify(instructorRepository, never()).save(any(Instructor.class));
    }

    // ------------------------------------------------------------------ removeInstructor

    @Test
    void removeInstructor_unused_deletesIt() {
        Instructor i = instructor(1L);
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(i));

        service.removeInstructor(1L);

        verify(instructorRepository).delete(i);
    }

    @ParameterizedTest(name = "removeInstructor refused: courses={0}, slots={1}, availabilities={2}")
    @CsvSource({"true,false,false", "false,true,false", "false,false,true"})
    void removeInstructor_inUse_isRefused(boolean hasCourses, boolean hasSlots, boolean hasAvailabilities) {
        Instructor i = instructor(1L);
        if (hasCourses) i.getCourses().add(course(10L));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(i));
        if (hasSlots) when(slotRepository.existsByInstructor_NumInstructor(1L)).thenReturn(true);
        if (hasAvailabilities) when(availabilityRepository.existsByInstructor_NumInstructor(1L)).thenReturn(true);

        assertBusiness(HttpStatus.CONFLICT, () -> service.removeInstructor(1L));
        verify(instructorRepository, never()).delete(any(Instructor.class));
    }

    @Test
    void removeInstructor_missing_isNotFound() {
        when(instructorRepository.findById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.removeInstructor(1L));
        verify(instructorRepository, never()).delete(any(Instructor.class));
    }

    // ------------------------------------------------------------------ courses

    @Test
    void courses_returnsTheInstructorCourses() {
        Instructor i = instructor(1L);
        i.getCourses().add(course(10L));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(i));

        assertSame(i.getCourses(), service.courses(1L));
    }

    @Test
    void courses_missingInstructor_isNotFound() {
        when(instructorRepository.findById(1L)).thenReturn(Optional.empty());

        assertBusiness(HttpStatus.NOT_FOUND, () -> service.courses(1L));
    }

    // ------------------------------------------------------------------ search

    private static List<Instructor> team() {
        return Arrays.asList(
                instructor(1L, "John", "Smith"),
                instructor(2L, "Jane", "Doe"),
                instructor(3L, "Alice", "Smith"));
    }

    static Stream<Arguments> searchCases() {
        return Stream.of(
                Arguments.of("john", Collections.singletonList(1L)),
                Arguments.of("SMITH", Arrays.asList(1L, 3L)),
                Arguments.of("n sm", Collections.singletonList(1L)),   // spans "first last"
                Arguments.of("", Arrays.asList(1L, 2L, 3L)),
                Arguments.of("zzz", Collections.<Long>emptyList()));
    }

    @ParameterizedTest(name = "search(\"{0}\") -> {1}")
    @MethodSource("searchCases")
    void search_isCaseInsensitive_andMatchesFullName(String query, List<Long> expectedIds) {
        when(instructorRepository.findAll()).thenReturn(team());

        List<Long> ids = service.search(query).stream()
                .map(Instructor::getNumInstructor).collect(Collectors.toList());

        assertEquals(expectedIds, ids);
    }
}