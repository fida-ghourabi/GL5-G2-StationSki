package com.example.gestionstationskii.entities;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;
@Entity @Getter @Setter
public class Equipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String inventoryCode;
    @Enumerated(EnumType.STRING)
    private EquipmentType type;
    private String brand;
    private String size;
    @Column(precision = 12, scale = 2)
    private BigDecimal dailyRate;
    @Enumerated(EnumType.STRING)
    private EquipmentStatus status;
}
