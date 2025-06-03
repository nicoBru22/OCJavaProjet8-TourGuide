package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.helper.InternalTestUserInitializer;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.util.ArrayList;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	
	private static final Logger logger = LogManager.getLogger();
	
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	private final ExecutorService executorService = Executors.newFixedThreadPool(100);
	private static final String tripPricerApiKey = "test-server-api-key";
    private final Map<String, User> internalUserMap = new HashMap<>();
    private final InternalTestUserInitializer internalTestUserInitializer = new InternalTestUserInitializer();

    /**
     * Constructeur de la classe TourGuideService.
     * 
     * Initialise les services GPS et de récompenses, configure la locale par défaut à US,
     * initialise les utilisateurs de test si le mode test est activé,
     * démarre le tracker et ajoute un hook d'arrêt pour stopper le tracking proprement à la fermeture de l'application.
     * 
     * @param gpsUtil le service GPS utilisé pour récupérer les attractions et localisations
     * @param rewardsService le service de gestion des récompenses
     */
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

	/**
	 * Retourne la liste des récompenses obtenues par l'utilisateur.
	 *
	 * @param user l'utilisateur dont on souhaite obtenir les récompenses
	 * @return la liste des UserReward associés à l'utilisateur
	 */
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	/**
	 * Récupère la dernière localisation visitée de l'utilisateur.
	 * Si l'utilisateur n'a pas encore de localisation enregistrée,
	 * lance un suivi asynchrone pour récupérer la localisation actuelle et attend le résultat.
	 *
	 * @param user l'utilisateur dont on souhaite obtenir la localisation
	 * @return la dernière VisitedLocation connue de l'utilisateur
	 * @throws InterruptedException si l'attente du résultat asynchrone est interrompue
	 * @throws ExecutionException si le calcul asynchrone échoue
	 */
	public VisitedLocation getUserLocation(User user) throws InterruptedException, ExecutionException {
	    VisitedLocation visitedLocation;
	    if (user.getVisitedLocations().size() > 0) {
	        visitedLocation = user.getLastVisitedLocation();
	    } else {
	        visitedLocation = trackUserLocation(user).get();
	    }
	    return visitedLocation;
	}


	/**
	 * Récupère un utilisateur à partir de son nom d'utilisateur.
	 *
	 * @param userName Le nom d'utilisateur recherché.
	 * @return L'objet {@link User} correspondant au nom d'utilisateur, ou {@code null} si aucun utilisateur trouvé.
	 */
	public User getUser(String userName) {
		User User = internalUserMap.get(userName);
		return User;
	}

	/**
	 * Récupère la liste de tous les utilisateurs présents dans la map interne.
	 *
	 * @return Une liste contenant tous les objets {@link User}.
	 */
	public List<User> getAllUsers() {
		List<User> userList = internalUserMap.values().stream()
				.collect(Collectors.toList());
		return userList;
	}

	/**
	 * Ajoute un nouvel utilisateur à la map interne s'il n'existe pas déjà.
	 *
	 * @param user L'objet {@link User} à ajouter.
	 */
	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	/**
	 * Récupère une liste de propositions de voyages (deals) adaptées à l'utilisateur.
	 * <p>
	 * Cette méthode calcule d'abord le total des points de récompense accumulés par l'utilisateur,
	 * puis utilise ces points ainsi que les préférences de l'utilisateur pour obtenir des offres
	 * de voyage auprès du service {@code tripPricer}.
	 * Les offres reçues sont ensuite associées à l'utilisateur via {@code user.setTripDeals}.
	 * </p>
	 *
	 * @param user L'utilisateur pour lequel on souhaite récupérer les offres de voyage.
	 * @return Une liste de {@link Provider} représentant les offres de voyages disponibles.
	 */
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream()
				.mapToInt(i -> i.getRewardPoints())
				.sum();
		
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		
		return providers;
	}

	/**
	 * Suit la localisation d'un utilisateur de façon asynchrone.
	 * <p>
	 * Cette méthode effectue les étapes suivantes de manière non bloquante :
	 * <ul>
	 *   <li>Récupère la localisation actuelle de l'utilisateur via {@code gpsUtil.getUserLocation}.</li>
	 *   <li>Ajoute cette localisation à la liste des lieux visités de l'utilisateur.</li>
	 *   <li>Calcule les récompenses associées à cette localisation en appelant
	 *       {@code rewardsService.calculateRewardsAsync}.</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Le résultat retourné est un {@link CompletableFuture} contenant la localisation visitée
	 * une fois toutes ces opérations terminées.
	 * </p>
	 *
	 * @param user L'utilisateur dont on veut suivre la localisation.
	 * @return Un {@code CompletableFuture<VisitedLocation>} représentant la localisation suivie,
	 *         complété après ajout aux lieux visités et calcul des récompenses.
	 */
	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
	    CompletableFuture<VisitedLocation> futureVisitedLocation = CompletableFuture.supplyAsync(() -> {
	        return gpsUtil.getUserLocation(user.getUserId());
	    }, executorService);
	    
	    CompletableFuture<VisitedLocation> visitedLocationToAdd = futureVisitedLocation.thenApply(visitedLocation -> {
	        user.addToVisitedLocations(visitedLocation);
	        return visitedLocation;
	    });
	    
	    CompletableFuture<VisitedLocation> result = visitedLocationToAdd.thenCompose(visitedLocation ->
	        rewardsService.calculateRewardsAsync(user)  // méthode déjà asynchrone qui renvoie CompletableFuture<Void>
	        .thenApply(v -> visitedLocation)         // on retourne visitedLocation après calcul des récompenses
		);
	    
	    return result;
	}


	/**
	 * Retourne la liste des attractions triées par distance croissante
	 * par rapport à la position donnée d'un utilisateur.
	 *
	 * @param visitedLocation la localisation visitée de l'utilisateur (latitude et longitude)
	 * @return une liste d'objets Attraction triée du plus proche au plus éloigné par rapport à la localisation de l'utilisateur
	 */
	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
		List<Attraction> listAllAttraction = gpsUtil.getAttractions();
		List<Attraction> listNearByAttraction = listAllAttraction.parallelStream()
				.sorted(Comparator.comparingDouble(attraction -> rewardsService.getDistance(attraction, visitedLocation.location)))
	            .collect(Collectors.toList());
		logger.info("Nombre d'attraction récupérée : {}", listNearByAttraction);
		return listNearByAttraction;
	}
	
	/**
	 * Récupère les 5 attractions touristiques les plus proches de l'utilisateur,
	 * en fonction de sa position actuelle, et retourne leurs informations dans une liste de maps.
	 *
	 * <p>Chaque élément de la liste retournée est une map contenant :
	 * <ul>
	 *   <li>le nom de l'attraction ({@code "attractionName"})</li>
	 *   <li>les coordonnées de l'attraction ({@code "attractionLat"}, {@code "attractionLong"})</li>
	 *   <li>les coordonnées de l'utilisateur ({@code "userLatitude"}, {@code "userLongitude"})</li>
	 *   <li>la distance entre l'utilisateur et l'attraction en miles ({@code "distanceMiles"})</li>
	 * </ul>
	 *
	 * @param visitedLocation la localisation actuelle de l'utilisateur (latitude et longitude)
	 * @return une liste de 5 maps contenant les données des attractions les plus proches
	 */
	public List<Map<String, Object>> getFiveNearAttractions(VisitedLocation visitedLocation) {
		double userLatitude = visitedLocation.location.latitude;
		double userLongitude = visitedLocation.location.longitude;
	    List<Attraction> nearByAttractions = getNearByAttractions(visitedLocation);
	    List<Attraction> fiveNearAttractions = nearByAttractions.stream()
	            .limit(5)
	            .collect(Collectors.toList());
	 
	            List<Map<String, Object>> result = new ArrayList<>();

	            for (Attraction attraction : fiveNearAttractions) {

	                Map<String, Object> attractionInfo = new HashMap<>();
	                attractionInfo.put("attractionName", attraction.attractionName);
	                attractionInfo.put("attractionLat", attraction.latitude);
	                attractionInfo.put("attractionLong", attraction.longitude);
	                attractionInfo.put("userLongitude", userLongitude);
	                attractionInfo.put("userLatitude", userLatitude);
	                double distanceMiles = rewardsService.getDistance(attraction, visitedLocation.location);
	                attractionInfo.put("distanceMiles", distanceMiles);

	                result.add(attractionInfo);
	            }
        logger.info("Les 5 attractions proche : {}", result);
        return result;
	}
	
	/**
	 * Ajoute un hook de shutdown à la JVM.
	 * <p>
	 * Ce hook exécute un thread lors de l'arrêt de la JVM afin d'appeler la méthode
	 * {@code stopTracking()} sur l'objet {@code tracker}. Cela permet d'arrêter proprement
	 * le suivi en cours avant que l'application ne se termine.
	 * </p>
	 * <p>
	 * Le hook sera déclenché lors d'un arrêt normal de l'application, comme un arrêt manuel,
	 * un arrêt du système, ou une fermeture via un signal (ex : Ctrl+C).
	 * </p>
	 */
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}
	
	



}
