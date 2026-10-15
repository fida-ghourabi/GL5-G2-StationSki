package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;

import java.util.List;

public interface ISkierServices {
    Skier updateSkier(Skier skier);
    org.springframework.data.domain.Page<Skier> search(String name, String city, int page, int size);

	List<Skier> retrieveAllSkiers();

	Skier  addSkier(Skier  skier);

	Skier assignSkierToSubscription(Long numSkier, Long numSubscription);

	Skier addSkierAndAssignToCourse(Skier skier, Long numCourse);

	void removeSkier (Long numSkier);

	Skier retrieveSkier (Long numSkier);


	Skier assignSkierToPiste(Long numSkieur, Long numPiste);

	List<Skier> retrieveSkiersBySubscriptionType(TypeSubscription typeSubscription);

}
