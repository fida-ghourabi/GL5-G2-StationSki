package com.example.gestionstationskii.entities;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Getter @Setter
public class Rental {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Skier skier;
    @ManyToOne(optional = false)
    private Equipment equipment;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualReturnDate;
    @Column(precision = 12, scale = 2)
    private BigDecimal dailyRate;
    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    private RentalStatus status;
}
