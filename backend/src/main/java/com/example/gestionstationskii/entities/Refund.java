package com.example.gestionstationskii.entities;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Getter @Setter
public class Refund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @ManyToOne(optional = false)
    private Payment payment;
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;
    private LocalDateTime refundDate;
    private String reason;
}
