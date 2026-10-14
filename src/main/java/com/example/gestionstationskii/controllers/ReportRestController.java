package com.example.gestionstationskii.controllers;
import com.example.gestionstationskii.dto.ReportResponses.*;
import com.example.gestionstationskii.services.ReportServicesImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;
import java.util.List;
@RestController @RequestMapping("/reports") @RequiredArgsConstructor
public class ReportRestController {
    private final ReportServicesImpl service;
    @Operation(summary = "Encaissements, remboursements et net sur [from,to[, ventilés par mois") @GetMapping("/revenue")
    public Revenue revenue(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) { return service.revenue(from, to); }
    @Operation(summary = "Abonnements actifs, expirés, futurs et à échéance sous 30 jours") @GetMapping("/subscriptions")
    public Subscriptions subscriptions(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) { return service.subscriptions(asOf); }
    @Operation(summary = "Remplissage des cours collectifs dont les créneaux commencent sur [from,to[") @GetMapping("/courses/occupancy")
    public List<CourseOccupancy> occupancy(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) { return service.occupancy(from, to); }
    @Operation(summary = "Inscriptions non annulées par cours et semaine ISO sur [from,to[") @GetMapping("/registrations")
    public List<WeeklyRegistrations> registrations(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) { return service.registrations(from, to); }
}
