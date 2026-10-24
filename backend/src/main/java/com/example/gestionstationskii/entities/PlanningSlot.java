package com.example.gestionstationskii.entities;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.io.Serializable;

@Entity @Getter @Setter
public class PlanningSlot implements Serializable {
    private static final long serialVersionUID = 1L;
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
