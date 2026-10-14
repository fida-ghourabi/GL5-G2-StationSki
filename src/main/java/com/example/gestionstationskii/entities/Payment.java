package com.example.gestionstationskii.entities;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Getter @Setter
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Skier skier;
    @ManyToOne(optional = false)
    private Subscription subscription;
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private LocalDateTime paidAt;
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    @Column(unique = true)
    private String transactionReference;
    @Column(nullable = false, unique = true)
    private String receiptNumber;
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
}
