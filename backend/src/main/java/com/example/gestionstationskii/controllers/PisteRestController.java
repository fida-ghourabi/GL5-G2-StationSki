package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.entities.Piste;
import com.example.gestionstationskii.dto.Requests;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.gestionstationskii.services.*;
import com.example.gestionstationskii.entities.*;


import java.util.List;

@Tag(name = "\uD83C\uDFBF Piste Management")
@RestController
@RequestMapping("/piste")
@RequiredArgsConstructor
public class PisteRestController {

    private final IPisteServices pisteServices;

    @Operation(summary = "Modifier une piste")
    @PutMapping("/update")
    public Piste update(@RequestBody Requests.PisteInput input) { return pisteServices.updatePiste(toPiste(input)); }

    @Operation(summary = "Filtrer les pistes par couleur / difficulté")
    @GetMapping("/search")
    public List<Piste> search(@RequestParam(required = false) Color color) { return pisteServices.search(color); }

    @Operation(summary = "Consulter les skieurs affectés à une piste")
    @GetMapping("/{id}/skiers")
    public java.util.Set<Skier> skiers(@PathVariable Long id) { return pisteServices.skiers(id); }

    @Operation(description = "Add Piste")
    @PostMapping("/add")
    public Piste addPiste(@RequestBody Requests.PisteInput input){
        return pisteServices.addPiste(toPiste(input));
    }
    @Operation(description = "Retrieve all Pistes")
    @GetMapping("/all")
    public List<Piste> getAllPistes(){
        return pisteServices.retrieveAllPistes();
    }

    @Operation(description = "Retrieve Piste by Id")
    @GetMapping("/get/{id-piste}")
    public Piste getById(@PathVariable("id-piste") Long numPiste){
        return pisteServices.retrievePiste(numPiste);
    }

    @Operation(description = "Delete Piste by Id")
    @DeleteMapping("/delete/{id-piste}")
    public void deleteById(@PathVariable("id-piste") Long numPiste){
        pisteServices.removePiste(numPiste);
    }

    private static Piste toPiste(Requests.PisteInput input) {
        Piste piste = new Piste();
        piste.setNumPiste(input.getNumPiste());
        piste.setNamePiste(input.getNamePiste());
        piste.setColor(input.getColor());
        piste.setLength(input.getLength());
        piste.setSlope(input.getSlope());
        return piste;
    }


}
