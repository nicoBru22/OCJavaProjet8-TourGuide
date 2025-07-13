package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

/**
 * Service gérant le calcul et l'attribution des récompenses liées
 * aux attractions visitées par un utilisateur.
 * <p>
 * Ce service utilise {@link GpsUtil} pour récupérer les attractions et
 * calculer les distances, et {@link RewardCentral} pour obtenir
 * les points de récompense.
 * </p>
 * <p>
 * Il propose des méthodes asynchrones pour optimiser les performances
 * via un {@link ExecutorService}.
 * </p>
 */
@Service
public class RewardsService {
    
    /**
     * Conversion entre miles nautiques et miles terrestres.
     */
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

    /**
     * Distance par défaut en miles pour considérer la proximité d'une attraction.
     */
    private int defaultProximityBuffer = 10;

    /**
     * Distance de proximité actuelle utilisée.
     */
    private int proximityBuffer = defaultProximityBuffer;

    /**
     * Distance maximale en miles pour considérer une attraction comme proche.
     */
    private int attractionProximityRange = 200;

    /**
     * Service GPS pour récupérer les attractions et localisations.
     */
    private final GpsUtil gpsUtil;

    /**
     * Service pour obtenir les points de récompense.
     */
    private final RewardCentral rewardsCentral;

    /**
     * Pool de threads pour l'exécution asynchrone des tâches.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(100);

    /**
     * Constructeur initialisant les dépendances du service.
     * 
     * @param gpsUtil service GPS utilisé pour les données d'attractions
     * @param rewardCentral service fournissant les points de récompense
     */
    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
    }

    /**
     * Définit la distance de proximité (en miles) utilisée pour déterminer
     * si une attraction est proche d'une localisation.
     * 
     * @param proximityBuffer nouvelle distance de proximité
     */
    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    /**
     * Remet la distance de proximité à sa valeur par défaut.
     */
    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    /**
     * Vérifie si une attraction est dans la portée de proximité définie
     * autour d'une localisation donnée.
     * 
     * @param attraction attraction à vérifier
     * @param location localisation de référence
     * @return true si dans la portée, false sinon
     */
    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) <= attractionProximityRange;
    }

    /**
     * Vérifie si une attraction est proche d'une localisation visitée.
     * Méthode interne utilisée pour la recherche d'attractions proches.
     * 
     * @param visitedLocation localisation visitée par l'utilisateur
     * @param attraction attraction à vérifier
     * @return true si proche, false sinon
     */
    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
    }

    /**
     * Récupère les points de récompense pour une attraction donnée et un utilisateur.
     * 
     * @param attraction attraction concernée
     * @param user utilisateur concerné
     * @return nombre de points de récompense
     */
    public int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    /**
     * Recherche de manière parallèle les attractions proches des lieux visités par l'utilisateur.
     * 
     * Cette méthode compare toutes les attractions connues avec les localisations visitées par l'utilisateur,
     * et retourne celles qui sont géographiquement proches selon la méthode {@code nearAttraction}.
     *
     * @param user l'utilisateur dont on veut identifier les attractions à proximité
     * @return un ensemble d'attractions proches (potentiellement à récompenser)
     */
    public Set<Attraction> filterNearbyAttractions(User user) {
        List<VisitedLocation> userLocations = new ArrayList<>(user.getVisitedLocations());
        List<Attraction> attractions = new ArrayList<>(gpsUtil.getAttractions());

        Set<Attraction> setAttraction = userLocations.parallelStream()
                .flatMap(visitedLocation -> attractions.parallelStream()
                        .filter(attraction -> nearAttraction(visitedLocation, attraction)))
                .collect(Collectors.toSet());

        return setAttraction;
    }
    
    /**
     * Récupère les noms des attractions pour lesquelles l'utilisateur a déjà reçu une récompense.
     *
     * @param user l'utilisateur concerné
     * @return un ensemble de noms d'attractions déjà récompensées
     */
    private Set<String> rewardedAttraction(User user) {
        Set<String> rewardedAttractions = user.getUserRewards().stream()
                .map(r -> r.attraction.attractionName)
                .collect(Collectors.toSet());
		return rewardedAttractions;
    }
    
    /**
     * Filtre les attractions proches en excluant celles déjà récompensées pour l'utilisateur.
     *
     * @param nearbyAttractions les attractions proches de l'utilisateur
     * @param rewardedAttractions les noms des attractions pour lesquelles l'utilisateur a déjà reçu une récompense
     * @return un ensemble d'attractions proches non encore récompensées
     */
    private Set<Attraction> filterUnrewardedAttractions(Set<Attraction> nearbyAttractions, Set<String> rewardedAttractions) {
    	Set<Attraction> unrewardedAttraction = nearbyAttractions.parallelStream()
                .filter(attraction -> !rewardedAttractions.contains(attraction.attractionName))
                .collect(Collectors.toSet());
        return unrewardedAttraction;
    }

    /**
     * Ajoute des récompenses à l'utilisateur pour les attractions proches non encore récompensées.
     *
     * Cette méthode vérifie pour chaque attraction proche si l'utilisateur s'en est approché,
     * puis calcule les points de récompense et les ajoute de façon thread-safe à l'utilisateur.
     *
     * @param user l'utilisateur auquel attribuer des récompenses
     * @param nearbyAttractions ensemble des attractions proches à examiner
     */
    public void addRewards(User user, Set<Attraction> nearbyAttractions) {
        List<VisitedLocation> visitedLocations = new ArrayList<>(user.getVisitedLocations());
        Set<String> rewardedAttractions = rewardedAttraction(user);        

        Set<Attraction> filteredAttractions = filterUnrewardedAttractions(nearbyAttractions, rewardedAttractions);

        filteredAttractions.forEach(attraction -> {
            visitedLocations.stream()
                .filter(location -> nearAttraction(location, attraction))
                .forEach(location -> {
                    int points = getRewardPoints(attraction, user);
                    synchronized (user.getUserRewards()) {
                        user.addUserReward(new UserReward(location, attraction, points));
                    }
                });
        });
    }


    /**
     * Lance de manière asynchrone le calcul des récompenses pour un utilisateur.
     *
     * Cette méthode utilise un {@link CompletableFuture} pour rechercher les attractions proches
     * en parallèle, puis appelle {@code addRewards} pour traiter les récompenses, 
     * le tout en s'appuyant sur un {@code ExecutorService}.
     *
     * @param user l'utilisateur dont les récompenses doivent être calculées
     * @return un {@code CompletableFuture<Void>} indiquant la fin du traitement asynchrone
     */
    public CompletableFuture<Void> calculateRewardsAsync(User user) {
        return CompletableFuture
            .supplyAsync(() -> filterNearbyAttractions(user), executorService)
            .thenAcceptAsync(nearbyAttractions -> addRewards(user, nearbyAttractions), executorService);
    }



    /**
     * Calcule la distance en miles terrestres entre deux localisations GPS.
     * 
     * @param loc1 première localisation
     * @param loc2 deuxième localisation
     * @return distance en miles terrestres
     */
    public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
    }
}
