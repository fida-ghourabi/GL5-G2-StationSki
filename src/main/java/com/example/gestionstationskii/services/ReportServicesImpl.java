package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.ReportResponses.*;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.IsoFields;
import java.math.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class ReportServicesImpl {
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final ISubscriptionRepository subscriptions;
    private final ICourseRepository courses;
    private final PlanningSlotRepository slots;
    private final IRegistrationRepository registrations;

    private void period(LocalDateTime from, LocalDateTime to) { Checks.require(from != null && to != null && from.isBefore(to), "Période [from,to[ invalide"); }
    private boolean inside(LocalDateTime date, LocalDateTime from, LocalDateTime to) { return date != null && !date.isBefore(from) && date.isBefore(to); }
    public Revenue revenue(LocalDateTime from, LocalDateTime to) {
        period(from, to);
        Map<YearMonth, BigDecimal[]> months = new TreeMap<>();
        BigDecimal collected = BigDecimal.ZERO, refunded = BigDecimal.ZERO;
        for (Payment p : payments.findAll()) {
            if (inside(p.getPaidAt(), from, to) && (p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.PARTIALLY_REFUNDED || p.getStatus() == PaymentStatus.REFUNDED)) {
                collected = collected.add(p.getAmount());
                BigDecimal[] totals = months.computeIfAbsent(YearMonth.from(p.getPaidAt()), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                totals[0] = totals[0].add(p.getAmount());
            }
        }
        for (Refund r : refunds.findAll()) {
            if (inside(r.getRefundDate(), from, to)) {
                refunded = refunded.add(r.getAmount());
                BigDecimal[] totals = months.computeIfAbsent(YearMonth.from(r.getRefundDate()), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                totals[1] = totals[1].add(r.getAmount());
            }
        }
        List<MonthlyRevenue> monthly = months.entrySet().stream().map(e -> new MonthlyRevenue(e.getKey().toString(), e.getValue()[0], e.getValue()[1], e.getValue()[0].subtract(e.getValue()[1]))).collect(Collectors.toList());
        return new Revenue(from, to, collected, refunded, collected.subtract(refunded), monthly);
    }
    public Subscriptions subscriptions(LocalDate asOf) {
        Checks.require(asOf != null, "Date de référence requise");
        long active = 0, expired = 0, upcoming = 0, expiring = 0;
        for (Subscription s : subscriptions.findAll()) {
            if (s.getStartDate() == null || s.getEndDate() == null) throw BusinessException.conflict("Abonnement historique sans dates : corriger avant le rapport");
            if (asOf.isBefore(s.getStartDate())) upcoming++;
            else if (!asOf.isBefore(s.getEndDate())) expired++;
            else { active++; if (!s.getEndDate().isAfter(asOf.plusDays(30))) expiring++; }
        }
        return new Subscriptions(asOf, active, expired, upcoming, expiring);
    }
    public List<CourseOccupancy> occupancy(LocalDateTime from, LocalDateTime to) {
        period(from, to);
        List<CourseOccupancy> result = new ArrayList<>();
        List<PlanningSlot> allSlots = slots.findAll();
        List<Registration> allRegistrations = registrations.findAll();
        for (Course c : courses.findAll()) {
            if (c.getTypeCourse() == TypeCourse.INDIVIDUAL) continue;
            Set<Long> ids = allSlots.stream().filter(s -> s.getCourse().getNumCourse().equals(c.getNumCourse()) && s.getStatus() != SlotStatus.CANCELLED && inside(s.getStartTime(), from, to))
                    .map(PlanningSlot::getId).collect(Collectors.toSet());
            long capacity = allSlots.stream().filter(s -> ids.contains(s.getId())).mapToLong(PlanningSlot::getMaxCapacity).sum();
            long count = allRegistrations.stream().filter(r -> !r.isCancelled() && r.getPlanningSlot() != null && ids.contains(r.getPlanningSlot().getId())).count();
            BigDecimal rate = capacity == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
            result.add(new CourseOccupancy(c.getNumCourse(), ids.size(), capacity, count, rate));
        }
        return result;
    }
    public List<WeeklyRegistrations> registrations(LocalDateTime from, LocalDateTime to) {
        period(from, to);
        Map<Long, Map<LocalDate, Long>> counts = new TreeMap<>();
        for (Registration r : registrations.findAll()) {
            if (r.isCancelled() || r.getPlanningSlot() == null || r.getPlanningSlot().getStatus() == SlotStatus.CANCELLED || !inside(r.getPlanningSlot().getStartTime(), from, to)) continue;
            LocalDate monday = r.getPlanningSlot().getStartTime().toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            counts.computeIfAbsent(r.getCourse().getNumCourse(), k -> new TreeMap<>()).merge(monday, 1L, Long::sum);
        }
        List<WeeklyRegistrations> result = new ArrayList<>();
        counts.forEach((course, weeks) -> weeks.forEach((date, count) -> result.add(new WeeklyRegistrations(course,
                date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), count))));
        return result;
    }
}
