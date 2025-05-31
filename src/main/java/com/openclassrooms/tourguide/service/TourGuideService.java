package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.helper.InternalTestUserInitializer;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	private final ExecutorService executorService = Executors.newFixedThreadPool(100);
	private static final String tripPricerApiKey = "test-server-api-key";

	
    // Nouveau champ pour stocker les utilisateurs internes
    private final Map<String, User> internalUserMap = new HashMap<>();

    // Instance de la classe d'initialisation
    private final InternalTestUserInitializer internalTestUserInitializer = new InternalTestUserInitializer();

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			internalUserMap.putAll(internalTestUserInitializer.initializeUsers(InternalTestHelper.getInternalUserNumber()));
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) throws InterruptedException, ExecutionException {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocation(user).get(); //get car c'est un completableFutur
		return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		
		return providers;
	}

	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
	    // 1. Récupérer la localisation de l'utilisateur de manière asynchrone
	    CompletableFuture<VisitedLocation> futureVisitedLocation = CompletableFuture.supplyAsync(() -> {
	        return gpsUtil.getUserLocation(user.getUserId());
	    }, executorService);
	    
	    // 2. Dès qu'on a la localisation, on l'ajoute à l'historique utilisateur (opération rapide, synchrone)
	    CompletableFuture<VisitedLocation> visitedLocationToAdd = futureVisitedLocation.thenApply(visitedLocation -> {
	        user.addToVisitedLocations(visitedLocation);
	        return visitedLocation;
	    });
	    
	    // 3. Ensuite, on lance le calcul des récompenses de façon asynchrone et on attend sa fin avant de retourner le résultat
	    CompletableFuture<VisitedLocation> result = visitedLocationToAdd.thenCompose(visitedLocation ->
	        rewardsService.calculateRewardsAsync(user)  // méthode déjà asynchrone qui renvoie CompletableFuture<Void>
	        .thenApply(v -> visitedLocation)         // on retourne visitedLocation après calcul des récompenses
		);
	    
	    return result;
	}


	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
	    return gpsUtil.getAttractions().stream()
	            .sorted(Comparator.comparingDouble(attraction -> rewardsService.getDistance(attraction, visitedLocation.location)))
	            .limit(5)
	            .collect(Collectors.toList());
	}
	
	public List<Attraction> getFiveNearAttractions(VisitedLocation visitedLocation) {
	    List<Attraction> nearByAttractions = getNearByAttractions(visitedLocation);
	    List<Attraction> fiveNearAttractions = nearByAttractions.stream()
	            .limit(5)
	            .collect(Collectors.toList());
	    return fiveNearAttractions;
	}


	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}



}
