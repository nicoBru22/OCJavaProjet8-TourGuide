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
	private final ExecutorService executorService = Executors.newFixedThreadPool(20);
	
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
	
	//méthode modifiée pour le test nearAllAttractions
	public CompletableFuture<Void> calculateRewardsAsync(User user) {
	    // Étape 1 : lancer la recherche des attractions proches dans un thread du pool
	    CompletableFuture<Set<Attraction>> nearbyAttractionsFuture = CompletableFuture.supplyAsync(() -> {
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

	    // Étape 2 : quand la recherche est terminée, ajouter les récompenses (avec synchronisation)
	    CompletableFuture<Void> rewardsAddedFuture = nearbyAttractionsFuture.thenAcceptAsync(nearbyAttractions -> {
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
	    }, executorService);

	    // Étape 3 : retourner la CompletableFuture finale
	    return rewardsAddedFuture;
	}




	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
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
