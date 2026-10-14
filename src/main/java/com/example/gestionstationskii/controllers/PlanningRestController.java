package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.services.PlanningServicesImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;

@RestController @RequestMapping("/planning") @RequiredArgsConstructor
public class PlanningRestController {
    private final PlanningServicesImpl service;
    @Operation(summary = "Ajouter une disponibilité récurrente (dates de validité incluses)")
    @PostMapping("/availability/{instructorId}")
    public InstructorAvailability add(@PathVariable Long instructorId, @RequestBody Requests.Availability input) { return service.addAvailability(instructorId, input); }
    @Operation(summary = "Disponibilités d'un moniteur")
    @GetMapping("/instructors/{id}/availability")
    public List<InstructorAvailability> availability(@PathVariable Long id) { return service.availability(id); }
    @Operation(summary = "Créer un créneau dans les disponibilités du moniteur")
    @PostMapping("/slots")
    public PlanningSlot create(@RequestBody Requests.Slot input) { return service.create(input); }
    @Operation(summary = "Consulter un créneau")
    @GetMapping("/slots/{id}")
    public PlanningSlot get(@PathVariable Long id) { return service.get(id); }
    @Operation(summary = "Modifier un créneau futur sans inscriptions")
    @PutMapping("/slots/{id}")
    public PlanningSlot update(@PathVariable Long id, @RequestBody Requests.Slot input) { return service.update(id, input); }
    @Operation(summary = "Annuler un créneau et ses inscriptions")
    @DeleteMapping("/slots/{id}")
    public void cancel(@PathVariable Long id) { service.cancel(id); }
    @Operation(summary = "Créneaux chevauchant la période [from,to[, filtres optionnels")
    @GetMapping("/slots")
    public List<PlanningSlot> search(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long courseId, @RequestParam(required = false) Long instructorId) {
        return service.search(from, to, courseId, instructorId, false);
    }
    @Operation(summary = "Créneaux futurs ouverts avec places disponibles sur [from,to[")
    @GetMapping("/slots/available")
    public List<PlanningSlot> available(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long courseId) { return service.search(from, to, courseId, null, true); }
}
