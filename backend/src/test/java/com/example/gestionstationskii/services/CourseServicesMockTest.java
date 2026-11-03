package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServicesMockTest {

    @Mock
    private ICourseRepository courseRepository;
    @Mock
    private IRegistrationRepository registrationRepository;
    @Mock
    private PlanningSlotRepository slotRepository;
    @Mock
    private IInstructorRepository instructorRepository;

    @InjectMocks
    private CourseServicesImpl courseServices;

    @Test
    void retrieveAllCoursesReturnsRepositoryData() {
        when(courseRepository.findAll())
                .thenReturn(Arrays.asList(buildCourse(), buildCourse()));

        List<Course> result = courseServices.retrieveAllCourses();

        assertEquals(2, result.size());
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    void addCourseSavesValidCourse() {
        Course course = buildCourse();
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course saved = courseServices.addCourse(course);

        assertNotNull(saved);
        assertEquals(course.getLevel(), saved.getLevel());
        assertNotNull(saved.getRegistrations());
        verify(courseRepository, times(1)).save(course);
    }

    @Test
    void retrieveCourseReturnsExistingCourse() {
        Course course = buildCourse();
        course.setNumCourse(1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Course result = courseServices.retrieveCourse(1L);

        assertEquals(1L, result.getNumCourse());
        verify(courseRepository).findById(1L);
    }

    @Test
    void retrieveCourseThrowsWhenMissing() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> courseServices.retrieveCourse(99L));
    }

    @Test
    void updateCourseChangesPrice() {
        Course current = buildCourse();
        current.setNumCourse(1L);
        Course update = buildCourse();
        update.setNumCourse(1L);
        update.setPrice(200F);
        when(courseRepository.lockById(1L)).thenReturn(Optional.of(current));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = courseServices.updateCourse(update);

        assertEquals(200F, result.getPrice());
        verify(courseRepository).save(current);
    }

    @Test
    void removeCourseDeletesWhenNoLinks() {
        Course course = buildCourse();
        course.setNumCourse(1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(instructorRepository.findByCourses_NumCourse(1L))
                .thenReturn(Collections.<Instructor>emptyList());

        courseServices.removeCourse(1L);

        verify(courseRepository, times(1)).delete(course);
    }

    @Test
    void removeCourseRefusedWhenRegistrationsExist() {
        Course course = buildCourse();
        course.setNumCourse(1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(registrationRepository.existsByCourse_NumCourse(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> courseServices.removeCourse(1L));
        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    void addCourseRejectsPreassignedIdentifier() {
        Course course = buildCourse();
        course.setNumCourse(1L);

        assertThrows(BusinessException.class, () -> courseServices.addCourse(course));
        verifyNoInteractions(courseRepository, registrationRepository, slotRepository, instructorRepository);
    }

    @Test
    void addCourseRejectsInvalidPrice() {
        Course invalidPrice = buildCourse();
        invalidPrice.setPrice(Float.NaN);
        assertThrows(BusinessException.class, () -> courseServices.addCourse(invalidPrice));

        verifyNoInteractions(courseRepository, registrationRepository, slotRepository, instructorRepository);
    }

    @Test
    void addCourseValidatesLevelTypeAndSupportIndependently() {
        Course invalidLevel = buildCourse();
        invalidLevel.setLevel(0);
        assertThrows(BusinessException.class, () -> courseServices.addCourse(invalidLevel));

        Course missingType = buildCourse();
        missingType.setTypeCourse(null);
        assertThrows(BusinessException.class, () -> courseServices.addCourse(missingType));

        Course missingSupport = buildCourse();
        missingSupport.setSupport(null);
        assertThrows(BusinessException.class, () -> courseServices.addCourse(missingSupport));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void updateCourseRejectsMissingCourseAndInvalidIdentifier() {
        Course update = buildCourse();
        update.setNumCourse(0L);
        assertThrows(BusinessException.class, () -> courseServices.updateCourse(update));

        update.setNumCourse(1L);
        when(courseRepository.lockById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> courseServices.updateCourse(update));
        verify(courseRepository).lockById(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updateCourseRejectsChangingImmutableFieldsWhenRegisteredOrScheduled() {
        Course current = buildCourse();
        current.setNumCourse(1L);
        Course update = buildCourse();
        update.setNumCourse(1L);
        update.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        when(courseRepository.lockById(1L)).thenReturn(Optional.of(current));
        when(registrationRepository.existsByCourse_NumCourse(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> courseServices.updateCourse(update));
        verify(slotRepository, never()).existsByCourse_NumCourse(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updateCourseAllowsPriceChangeWhenLinkedFieldsRemainSame() {
        Course current = buildCourse();
        current.setNumCourse(1L);
        Course update = buildCourse();
        update.setNumCourse(1L);
        update.setPrice(150F);
        when(courseRepository.lockById(1L)).thenReturn(Optional.of(current));
        when(registrationRepository.existsByCourse_NumCourse(1L)).thenReturn(true);
        when(courseRepository.save(current)).thenReturn(current);

        assertEquals(150F, courseServices.updateCourse(update).getPrice());
        verify(courseRepository).save(current);
    }

    @Test
    void updateCourseRejectsChangingSupportOrLevelWhenScheduled() {
        for (int changedField = 0; changedField < 2; changedField++) {
            Course current = buildCourse();
            current.setNumCourse(1L);
            Course update = buildCourse();
            update.setNumCourse(1L);
            if (changedField == 0) update.setSupport(Support.SNOWBOARD);
            else update.setLevel(2);
            when(courseRepository.lockById(1L)).thenReturn(Optional.of(current));
            when(registrationRepository.existsByCourse_NumCourse(1L)).thenReturn(false);
            when(slotRepository.existsByCourse_NumCourse(1L)).thenReturn(true);

            assertThrows(BusinessException.class, () -> courseServices.updateCourse(update));
        }
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void removeCourseRefusedWhenSlotOrInstructorLinksExist() {
        Course course = buildCourse();
        course.setNumCourse(1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(slotRepository.existsByCourse_NumCourse(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> courseServices.removeCourse(1L));
        verify(instructorRepository, never()).findByCourses_NumCourse(1L);

        when(slotRepository.existsByCourse_NumCourse(1L)).thenReturn(false);
        when(instructorRepository.findByCourses_NumCourse(1L))
                .thenReturn(Collections.singletonList(new Instructor()));
        assertThrows(BusinessException.class, () -> courseServices.removeCourse(1L));
        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    void searchAppliesOptionalFiltersAndRejectsInvalidLevel() {
        Course matching = buildCourse();
        Course other = buildCourse();
        other.setSupport(Support.SNOWBOARD);
        Course differentTypeAndLevel = buildCourse();
        differentTypeAndLevel.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        differentTypeAndLevel.setLevel(2);
        when(courseRepository.findAll()).thenReturn(Arrays.asList(matching, other, differentTypeAndLevel));

        assertEquals(Arrays.asList(matching, other, differentTypeAndLevel), courseServices.search(null, null, null));
        assertEquals(Collections.singletonList(matching),
                courseServices.search(TypeCourse.INDIVIDUAL, Support.SKI, 1));
        assertEquals(Collections.singletonList(other), courseServices.search(null, Support.SNOWBOARD, null));
        assertEquals(Collections.singletonList(differentTypeAndLevel),
                courseServices.search(TypeCourse.COLLECTIVE_ADULT, null, null));
        assertEquals(Collections.singletonList(differentTypeAndLevel), courseServices.search(null, null, 2));
        assertThrows(BusinessException.class, () -> courseServices.search(null, null, 0));
    }

    @Test
    void participantsCountsDistinctNonCancelledSkiers() {
        Course course = buildCourse();
        course.setNumCourse(1L);
        Registration active = new Registration();
        Skier firstSkier = new Skier();
        firstSkier.setNumSkier(8L);
        active.setSkier(firstSkier);
        Registration duplicate = new Registration();
        duplicate.setSkier(firstSkier);
        Registration cancelled = new Registration();
        cancelled.setCancelled(true);
        Skier secondSkier = new Skier();
        secondSkier.setNumSkier(9L);
        cancelled.setSkier(secondSkier);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(registrationRepository.findByCourse_NumCourse(1L))
                .thenReturn(Arrays.asList(active, duplicate, cancelled));

        assertEquals(1L, courseServices.participants(1L));
    }

    @Test
    void exposesCourseRegistrationsAndInstructors() {
        Course course = buildCourse();
        course.setNumCourse(1L);
        Registration registration = new Registration();
        Instructor instructor = new Instructor();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(registrationRepository.findByCourse_NumCourse(1L)).thenReturn(Collections.singletonList(registration));
        when(instructorRepository.findByCourses_NumCourse(1L)).thenReturn(Collections.singletonList(instructor));

        assertEquals(Collections.singletonList(registration), courseServices.registrations(1L));
        assertEquals(Collections.singletonList(instructor), courseServices.instructors(1L));
    }

    private Course buildCourse() {
        Course course = new Course();
        course.setLevel(1);
        course.setTypeCourse(TypeCourse.INDIVIDUAL);
        course.setSupport(Support.SKI);
        course.setPrice(100F);
        return course;
    }
}
