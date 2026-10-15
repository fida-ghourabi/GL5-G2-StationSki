package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class EquipmentRentalServicesImpl {
    private final EquipmentRepository equipment;
    private final RentalRepository rentals;
    private final ISkierRepository skiers;
    @Value("${station.rental.include-return-day:false}")
    private boolean includeReturnDay;

    public Equipment getEquipment(Long id) {
        Checks.id(id); return equipment.findById(id).orElseThrow(() -> BusinessException.missing("Matériel"));
    }
    public List<Equipment> allEquipment() { return equipment.findAll(); }
    public Equipment createEquipment(Requests.EquipmentInput input) { return saveEquipment(new Equipment(), input); }
    public Equipment updateEquipment(Long id, Requests.EquipmentInput input) {
        Checks.id(id);
        Equipment current = equipment.lockById(id).orElseThrow(() -> BusinessException.missing("Matériel"));
        if (rentals.findByEquipment_Id(id).stream().anyMatch(r -> r.getStatus() == RentalStatus.ACTIVE || r.getStatus() == RentalStatus.RESERVED))
            throw BusinessException.conflict("Matériel réservé ou loué : modification refusée");
        return saveEquipment(current, input);
    }
    private Equipment saveEquipment(Equipment e, Requests.EquipmentInput input) {
        Checks.text(input.getInventoryCode(), "Code d'inventaire"); Checks.text(input.getBrand(), "Marque"); Checks.text(input.getSize(), "Taille");
        Checks.money(input.getDailyRate());
        Checks.require(input.getType() != null && input.getStatus() != null && input.getStatus() != EquipmentStatus.RENTED,
                "Type et état requis ; RENTED est géré par les locations");
        if (!input.getInventoryCode().equals(e.getInventoryCode()) && equipment.existsByInventoryCode(input.getInventoryCode()))
            throw BusinessException.conflict("Code d'inventaire déjà utilisé");
        e.setInventoryCode(input.getInventoryCode()); e.setType(input.getType()); e.setBrand(input.getBrand());
        e.setSize(input.getSize()); e.setDailyRate(input.getDailyRate()); e.setStatus(input.getStatus());
        return equipment.save(e);
    }
    public void deleteEquipment(Long id) {
        Equipment e = getEquipment(id);
        if (rentals.existsByEquipment_Id(id)) throw BusinessException.conflict("Matériel avec historique : utiliser OUT_OF_SERVICE");
        equipment.delete(e);
    }
    private boolean overlaps(Rental r, LocalDate from, LocalDate to) {
        if (r.getStatus() == RentalStatus.CANCELLED || r.getStatus() == RentalStatus.RETURNED) return false;
        // An active item stays unavailable after its planned end until physically returned.
        LocalDate end = r.getStatus() == RentalStatus.ACTIVE ? LocalDate.MAX : r.getExpectedEndDate();
        return r.getStartDate().isBefore(to) && from.isBefore(end);
    }
    private boolean usable(Equipment e) { return e.getStatus() != EquipmentStatus.MAINTENANCE && e.getStatus() != EquipmentStatus.OUT_OF_SERVICE; }
    public List<Equipment> available(LocalDate from, LocalDate to, EquipmentType type) {
        Checks.period(from, to);
        return equipment.findAll().stream().filter(e -> usable(e) && (type == null || e.getType() == type)
                && rentals.findByEquipment_Id(e.getId()).stream().noneMatch(r -> overlaps(r, from, to))).collect(Collectors.toList());
    }
    public Rental reserve(Requests.RentalInput input) {
        Checks.id(input.getSkierId()); Checks.id(input.getEquipmentId()); Checks.period(input.getStartDate(), input.getExpectedEndDate());
        Checks.require(!input.getStartDate().isBefore(LocalDate.now()), "Réservation dans le passé interdite");
        Skier skier = skiers.lockById(input.getSkierId()).orElseThrow(() -> BusinessException.missing("Skieur"));
        Equipment e = equipment.lockById(input.getEquipmentId()).orElseThrow(() -> BusinessException.missing("Matériel"));
        if (!usable(e)) throw BusinessException.conflict("Matériel en maintenance ou hors service");
        if (rentals.findByEquipment_Id(e.getId()).stream().anyMatch(r -> overlaps(r, input.getStartDate(), input.getExpectedEndDate())))
            throw BusinessException.conflict("Matériel déjà réservé ou loué sur la période");
        Rental r = new Rental(); r.setSkier(skier); r.setEquipment(e); r.setStartDate(input.getStartDate()); r.setExpectedEndDate(input.getExpectedEndDate());
        r.setDailyRate(e.getDailyRate()); r.setTotalAmount(price(r.getDailyRate(), r.getStartDate(), r.getExpectedEndDate())); r.setStatus(RentalStatus.RESERVED);
        return rentals.save(r);
    }
    public BigDecimal price(BigDecimal rate, LocalDate from, LocalDate to) {
        Checks.money(rate);
        Checks.require(from != null && to != null && !to.isBefore(from), "Période de facturation invalide");
        long days = Math.max(1, ChronoUnit.DAYS.between(from, to) + (includeReturnDay ? 1 : 0));
        BigDecimal result = rate.multiply(BigDecimal.valueOf(days)); Checks.money(result); return result;
    }
    public Rental getRental(Long id) { Checks.id(id); return rentals.findById(id).orElseThrow(() -> BusinessException.missing("Location")); }
    private Rental lockedRental(Long id) {
        Rental r = getRental(id);
        Equipment e = equipment.lockById(r.getEquipment().getId()).orElseThrow(() -> BusinessException.missing("Matériel"));
        entityManager.refresh(e);
        // Refresh after waiting for the equipment lock, to avoid stale transition decisions.
        entityManager.refresh(r);
        return r;
    }
    private final javax.persistence.EntityManager entityManager;
    public Rental activate(Long id) {
        Rental r = lockedRental(id);
        if (r.getStatus() != RentalStatus.RESERVED) throw BusinessException.conflict("Seule une réservation peut être activée");
        if (LocalDate.now().isBefore(r.getStartDate()) || !LocalDate.now().isBefore(r.getExpectedEndDate()))
            throw BusinessException.conflict("Activation autorisée uniquement pendant la période réservée");
        if (!usable(r.getEquipment()) || rentals.findByEquipment_Id(r.getEquipment().getId()).stream()
                .anyMatch(other -> !other.getId().equals(id) && other.getStatus() == RentalStatus.ACTIVE))
            throw BusinessException.conflict("Exemplaire indisponible ou déjà en location active");
        r.setStatus(RentalStatus.ACTIVE); r.getEquipment().setStatus(EquipmentStatus.RENTED); return r;
    }
    public Rental returnRental(Long id, Requests.RentalReturn input) {
        Rental r = lockedRental(id);
        if (r.getStatus() != RentalStatus.ACTIVE) throw BusinessException.conflict("Seule une location active peut être retournée");
        Checks.require(input.getActualReturnDate() != null && !input.getActualReturnDate().isBefore(r.getStartDate())
                && !input.getActualReturnDate().isAfter(LocalDate.now()), "Retour à partir du début et au plus aujourd'hui requis");
        r.setActualReturnDate(input.getActualReturnDate()); r.setTotalAmount(price(r.getDailyRate(), r.getStartDate(), input.getActualReturnDate()));
        r.setStatus(RentalStatus.RETURNED); r.getEquipment().setStatus(EquipmentStatus.AVAILABLE); return r;
    }
    public Rental cancel(Long id) {
        Rental r = lockedRental(id);
        if (r.getStatus() != RentalStatus.RESERVED) throw BusinessException.conflict("Seule une réservation peut être annulée");
        r.setStatus(RentalStatus.CANCELLED); return r;
    }
    public List<Rental> bySkier(Long id) {
        Checks.id(id); skiers.findById(id).orElseThrow(() -> BusinessException.missing("Skieur")); return rentals.findBySkier_NumSkier(id);
    }
    public List<Rental> search(LocalDate from, LocalDate to) {
        Checks.period(from, to);
        return rentals.findAll().stream().filter(r -> r.getStartDate().isBefore(to)
                && from.isBefore(r.getActualReturnDate() == null ? r.getExpectedEndDate() : r.getActualReturnDate())).collect(Collectors.toList());
    }
}
