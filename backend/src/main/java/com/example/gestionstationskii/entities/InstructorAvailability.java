package com.example.gestionstationskii.entities;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.*;

@Entity @Getter @Setter
public class InstructorAvailability {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Instructor instructor;
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate validFrom;
    private LocalDate validUntil;
}
