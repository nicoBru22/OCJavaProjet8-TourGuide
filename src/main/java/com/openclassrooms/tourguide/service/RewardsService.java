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

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private final ExecutorService executorService = Executors.newFixedThreadPool(100);
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}
	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public CompletableFuture<Set<Attraction>> findNearbyAttractionsAsync(User user) {
	    // Étape 1 : Exécuter la recherche des attractions proches en tâche de fond via executorService
	    CompletableFuture<Set<Attraction>> futureNearbyAttractions = CompletableFuture.supplyAsync(() -> {
	        
	        // Récupérer les lieux visités par l'utilisateur
	        List<VisitedLocation> userLocations = user.getVisitedLocations();
	        
	        // Récupérer la liste de toutes les attractions disponibles
	        List<Attraction> attractions = gpsUtil.getAttractions();

	        // Créer un ensemble pour stocker les attractions proches
	        Set<Attraction> nearbyAttractions = new HashSet<>();

	        // Parcourir chaque lieu visité et chaque attraction pour vérifier s'ils sont proches
	        for (VisitedLocation visitedLocation : userLocations) {
	            for (Attraction attraction : attractions) {
	                if (nearAttraction(visitedLocation, attraction)) {
	                    nearbyAttractions.add(attraction);
	                }
	            }
	        }

	        // Retourner les attractions proches trouvées
	        return nearbyAttractions;

	    }, executorService); // Utiliser un thread du pool pour exécuter cette tâche

	    // Étape 2 : Retourner le CompletableFuture résultant
	    return futureNearbyAttractions;
	}


	// Étape 2 : ajouter les récompenses (peut être synchrone ou async)
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

	public CompletableFuture<Void> calculateRewardsAsync(User user) {
	    // Étape 1 : Trouver les attractions proches de l'utilisateur (opération asynchrone)
	    CompletableFuture<Set<Attraction>> nearbyAttractionsFuture = findNearbyAttractionsAsync(user);

	    // Étape 2 : Une fois qu'on a les attractions proches, on ajoute les récompenses (aussi de façon asynchrone)
	    CompletableFuture<Void> addRewardsFuture = nearbyAttractionsFuture.thenAcceptAsync(
	        nearbyAttractions -> addRewards(user, nearbyAttractions),
	        executorService
	    );

	    // Étape 3 : Retourner la future qui complétera l'opération une fois les récompenses ajoutées
	    return addRewardsFuture;
	}


	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
