package com.openclassrooms.tourguide.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * Nombre de cœurs processeur pour dimensionner le pool de threads.
     */
    int cores = Runtime.getRuntime().availableProcessors();

    /**
     * Pool de threads pour l'exécution asynchrone des tâches.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(cores * 2);

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
     * Recherche asynchrone des attractions proches des localisations visitées par un utilisateur.
     * 
     * @param user utilisateur dont on veut trouver les attractions proches
     * @return un CompletableFuture contenant l'ensemble des attractions proches
     */
    public CompletableFuture<Set<Attraction>> findNearbyAttractionsAsync(User user) {
        return CompletableFuture.supplyAsync(() -> {
            List<VisitedLocation> userLocations = user.getVisitedLocations();
            List<Attraction> attractions = gpsUtil.getAttractions();
            Set<Attraction> nearbyAttractions = new HashSet<>();

            for (VisitedLocation visitedLocation : userLocations) {
                for (Attraction attraction : attractions) {
                    if (nearAttraction(visitedLocation, attraction)) {
                        nearbyAttractions.add(attraction);
                    }
                }
            }
            return nearbyAttractions;
        }, executorService);
    }

    /**
     * Ajoute des récompenses à l'utilisateur pour les attractions proches qui n'ont
     * pas encore été récompensées.
     * 
     * @param user utilisateur auquel ajouter les récompenses
     * @param nearbyAttractions ensemble des attractions proches
     */
    public void addRewards(User user, Set<Attraction> nearbyAttractions) {
        synchronized (user.getUserRewards()) {
            for (Attraction attraction : nearbyAttractions) {
                boolean alreadyRewarded = user.getUserRewards().stream()
                        .anyMatch(r -> r.attraction.attractionName.equals(attraction.attractionName));
                if (!alreadyRewarded) {
                    VisitedLocation location = user.getVisitedLocations().get(0);
                    user.addUserReward(new UserReward(location, attraction, getRewardPoints(attraction, user)));
                }
            }
        }
    }

    /**
     * Calcule asynchronement les récompenses pour un utilisateur en trouvant
     * les attractions proches et en les ajoutant à ses récompenses.
     * 
     * @param user utilisateur pour lequel calculer les récompenses
     * @return un CompletableFuture indiquant la fin du calcul
     */
    public CompletableFuture<Void> calculateRewardsAsync(User user) {
        CompletableFuture<Set<Attraction>> nearbyAttractionsFuture = findNearbyAttractionsAsync(user);

        return nearbyAttractionsFuture.thenAcceptAsync(
                nearbyAttractions -> addRewards(user, nearbyAttractions),
                executorService);
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
