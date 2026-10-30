package com.example.gestionstationskii.entities;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity @Getter @Setter
public class PlanningSlot implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Course course;
    @ManyToOne(optional = false)
    private Instructor instructor;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int maxCapacity;
    @Enumerated(EnumType.STRING)
    private SlotStatus status;
}
