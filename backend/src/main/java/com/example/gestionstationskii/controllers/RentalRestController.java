package com.example.gestionstationskii.controllers;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Rental;
import com.example.gestionstationskii.services.EquipmentRentalServicesImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/rentals") @RequiredArgsConstructor
public class RentalRestController {
    private final EquipmentRentalServicesImpl service;
    @Operation(summary = "Réserver un exemplaire pour un skieur") @PostMapping
    public Rental create(@RequestBody Requests.RentalInput input) { return service.reserve(input); }
    @Operation(summary = "Consulter une location") @GetMapping("/{id}")
    public Rental get(@PathVariable Long id) { return service.getRental(id); }
    @Operation(summary = "Historique des locations d'un skieur") @GetMapping("/skier/{skierId}")
    public List<Rental> bySkier(@PathVariable Long skierId) { return service.bySkier(skierId); }
    @Operation(summary = "Historique des locations chevauchant [from,to[") @GetMapping
    public List<Rental> search(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return service.search(from, to); }
    @Operation(summary = "Remettre le matériel au skieur") @PutMapping("/{id}/activate")
    public Rental activate(@PathVariable Long id) { return service.activate(id); }
    @Operation(summary = "Enregistrer le retour et recalculer le montant réel") @PutMapping("/{id}/return")
    public Rental returned(@PathVariable Long id, @RequestBody Requests.RentalReturn input) { return service.returnRental(id, input); }
    @Operation(summary = "Annuler une réservation") @PutMapping("/{id}/cancel")
    public Rental cancel(@PathVariable Long id) { return service.cancel(id); }
}
