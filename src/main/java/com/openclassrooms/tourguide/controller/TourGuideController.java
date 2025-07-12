package com.openclassrooms.tourguide.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;

/**
 * Contrôleur REST pour gérer les requêtes liées aux fonctionnalités principales
 * de l'application TourGuide, telles que la récupération des emplacements des utilisateurs,
 * des attractions à proximité, des récompenses, et des offres de voyage.
 * <p>
 * Ce contrôleur expose plusieurs endpoints accessibles via HTTP GET.
 * </p>
 */
@RestController
public class TourGuideController {

	private Logger logger = LogManager.getLogger();
	
	@Autowired
	TourGuideService tourGuideService;
	
    /**
     * Endpoint racine de l'application.
     * 
     * @return Un message de bienvenue.
     */
    @GetMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    /**
     * Récupère la localisation actuelle d'un utilisateur donné.
     * 
     * @param userName Le nom de l'utilisateur dont on souhaite obtenir la localisation.
     * @return La dernière localisation visitée de l'utilisateur.
     * @throws InterruptedException Si le thread est interrompu pendant la récupération.
     * @throws ExecutionException Si une erreur survient durant l'exécution asynchrone.
     */
    @GetMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) throws InterruptedException, ExecutionException {
    	User actualUser = tourGuideService.getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(actualUser);
    	return visitedLocation;
    }

    /**
     * Récupère la liste des attractions proches de la localisation actuelle de l'utilisateur.
     * 
     * @param userName Le nom de l'utilisateur dont on souhaite obtenir les attractions proches.
     * @return Une liste d'objets Attraction situés à proximité.
     * @throws InterruptedException Si le thread est interrompu pendant la récupération.
     * @throws ExecutionException Si une erreur survient durant l'exécution asynchrone.
     */
    @GetMapping("/getNearbyAttractions") 
    public List<Attraction> getNearbyAttractions(@RequestParam String userName) throws InterruptedException, ExecutionException {
    	User actualUser = tourGuideService.getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(actualUser);
    	List<Attraction> listAllNearAttraction = tourGuideService.getNearByAttractions(visitedLocation);
    	logger.info("Récupération des attractions selon distance réussie.");
    	return listAllNearAttraction;
    }
    
    /**
     * Récupère les cinq attractions les plus proches de l'utilisateur avec des détails supplémentaires.
     * 
     * @param userName Le nom de l'utilisateur dont on souhaite obtenir les cinq attractions proches.
     * @return Une liste de maps contenant des informations détaillées sur les attractions.
     * @throws InterruptedException Si le thread est interrompu pendant la récupération.
     * @throws ExecutionException Si une erreur survient durant l'exécution asynchrone.
     */
    @GetMapping("/getFiveNearbyAttractions") 
    public List<Map<String, Object>> getFiveNearbyAttractions(@RequestParam String userName) throws InterruptedException, ExecutionException {
    	User actualUser = tourGuideService.getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(actualUser);
    	List<Map<String, Object>> listFiveNearAttraction = tourGuideService.getFiveNearAttractions(visitedLocation, actualUser);
    	return listFiveNearAttraction;
    }
    
    /**
     * Récupère la liste des récompenses de l'utilisateur.
     * 
     * @param userName Le nom de l'utilisateur dont on souhaite obtenir les récompenses.
     * @return Une liste d'objets UserReward correspondant aux récompenses de l'utilisateur.
     */
    @GetMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	User actualUser = tourGuideService.getUser(userName);
    	List<UserReward> listUserReward = tourGuideService.getUserRewards(actualUser);
    	return listUserReward;
    }
       
    /**
     * Récupère la liste des offres de voyage (trip deals) pour l'utilisateur.
     * 
     * @param userName Le nom de l'utilisateur dont on souhaite obtenir les offres de voyage.
     * @return Une liste d'objets Provider représentant les offres de voyage.
     */
    @GetMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	User actualUser = tourGuideService.getUser(userName);
    	List<Provider> listProvider = tourGuideService.getTripDeals(actualUser);
    	return listProvider;
    }

}