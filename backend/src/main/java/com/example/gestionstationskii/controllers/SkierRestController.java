package com.example.gestionstationskii.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.services.*;
import com.example.gestionstationskii.entities.*;

import java.util.List;

@Tag(name = "\uD83C\uDFC2 Skier Management")
@RestController
@RequestMapping("/skier")
@RequiredArgsConstructor
public class SkierRestController {

    private final ISkierServices skierServices;

    @Operation(summary = "Modifier les informations personnelles d'un skieur")
    @PutMapping("/update")
    public Skier update(@RequestBody Requests.SkierInput input) { return skierServices.updateSkier(toSkier(input)); }

    @Operation(summary = "Rechercher par nom et ville ; pagination indexée à zéro")
    @GetMapping("/search")
    public org.springframework.data.domain.Page<Skier> search(@RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String city, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return skierServices.search(name, city, page, size); }

    @Operation(description = "Add Skier")
    @PostMapping("/add")
    public Skier addSkier(@RequestBody Requests.SkierInput input){
        return skierServices.addSkier(toSkier(input));
    }

    @Operation(description = "Add Skier And Assign To Course")
    @PostMapping("/addAndAssign/{numCourse}")
    public Skier addSkierAndAssignToCourse(@RequestBody Requests.SkierInput input,
                                           @PathVariable("numCourse") Long numCourse){
        return skierServices.addSkierAndAssignToCourse(toSkier(input),numCourse);
    }
    @Operation(description = "Assign Skier To Subscription")
    @PutMapping("/assignToSub/{numSkier}/{numSub}")
    public Skier assignToSubscription(@PathVariable("numSkier")Long numSkier,
                               @PathVariable("numSub") Long numSub){
        return skierServices.assignSkierToSubscription(numSkier, numSub);
    }

    @Operation(description = "Assign Skier To Piste")
    @PutMapping("/assignToPiste/{numSkier}/{numPiste}")
    public Skier assignToPiste(@PathVariable("numSkier")Long numSkier,
                               @PathVariable("numPiste") Long numPiste){
        return skierServices.assignSkierToPiste(numSkier,numPiste);
    }
    @Operation(description = "retrieve Skiers By Subscription Type")
    @GetMapping("/getSkiersBySubscription")
    public List<Skier> retrieveSkiersBySubscriptionType(@RequestParam TypeSubscription typeSubscription) {
        return skierServices.retrieveSkiersBySubscriptionType(typeSubscription);
    }
    @Operation(description = "Retrieve Skier by Id")
    @GetMapping("/get/{id-skier}")
    public Skier getById(@PathVariable("id-skier") Long numSkier){
        return skierServices.retrieveSkier(numSkier);
    }

    @Operation(description = "Delete Skier by Id")
    @DeleteMapping("/delete/{id-skier}")
    public void deleteById(@PathVariable("id-skier") Long numSkier){
        skierServices.removeSkier(numSkier);
    }

    @Operation(description = "Retrieve all Skiers")
    @GetMapping("/all")
    public List<Skier> getAllSkiers(){
        return skierServices.retrieveAllSkiers();
    }

    private static Skier toSkier(Requests.SkierInput input) {
        Skier skier = new Skier();
        skier.setNumSkier(input.getNumSkier());
        skier.setFirstName(input.getFirstName());
        skier.setLastName(input.getLastName());
        skier.setDateOfBirth(input.getDateOfBirth());
        skier.setCity(input.getCity());
        if (input.getSubscription() != null) {
            Subscription subscription = new Subscription();
            subscription.setNumSub(input.getSubscription().getNumSub());
            subscription.setStartDate(input.getSubscription().getStartDate());
            subscription.setPrice(input.getSubscription().getPrice());
            subscription.setTypeSub(input.getSubscription().getTypeSub());
            skier.setSubscription(subscription);
        }
        return skier;
    }

}
