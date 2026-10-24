package com.example.gestionstationskii.repositories;

import com.example.gestionstationskii.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ISkierRepository extends JpaRepository<Skier, Long> {
   @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
   @org.springframework.data.jpa.repository.Query("select s from Skier s where s.numSkier = :id")
   java.util.Optional<Skier> lockById(@org.springframework.data.repository.query.Param("id") Long id);
   @org.springframework.data.jpa.repository.Query("select s from Skier s where (:name = '' or lower(concat(s.firstName, ' ', s.lastName)) like lower(concat('%', :name, '%'))) and (:city = '' or lower(s.city) like lower(concat('%', :city, '%')))")
   org.springframework.data.domain.Page<Skier> search(@org.springframework.data.repository.query.Param("name") String name,
       @org.springframework.data.repository.query.Param("city") String city, org.springframework.data.domain.Pageable pageable);
   List<Skier> findBySubscriptionTypeSub(TypeSubscription typeSubscription);
   Skier findBySubscription(Subscription subscription);


}
