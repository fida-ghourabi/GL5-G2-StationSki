package com.example.gestionstationskii.controllers;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.services.EquipmentRentalServicesImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/equipment") @RequiredArgsConstructor
public class EquipmentRestController {
    private final EquipmentRentalServicesImpl service;
    @Operation(summary = "Créer un exemplaire physique de matériel") @PostMapping
    public Equipment create(@RequestBody Requests.EquipmentInput input) { return service.createEquipment(input); }
    @Operation(summary = "Lister le parc de matériel") @GetMapping
    public List<Equipment> all() { return service.allEquipment(); }
    @Operation(summary = "Consulter un exemplaire") @GetMapping("/{id}")
    public Equipment get(@PathVariable Long id) { return service.getEquipment(id); }
    @Operation(summary = "Modifier un exemplaire sans réservation en cours") @PutMapping("/{id}")
    public Equipment update(@PathVariable Long id, @RequestBody Requests.EquipmentInput input) { return service.updateEquipment(id, input); }
    @Operation(summary = "Supprimer un exemplaire sans historique") @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.deleteEquipment(id); }
    @Operation(summary = "Matériel disponible sur [from,to[, filtré par type") @GetMapping("/available")
    public List<Equipment> available(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) EquipmentType type) { return service.available(from, to, type); }
}
