package com.openclassrooms.tourguide;

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

@RestController
public class TourGuideController {

	private Logger logger = LogManager.getLogger();
	
	@Autowired
	TourGuideService tourGuideService;
	
    @GetMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @GetMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) throws InterruptedException, ExecutionException {
    	User actualUser = tourGuideService.getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(actualUser);
    	return visitedLocation;
    }
    
    //  TODO: Change this method to no longer return a List of Attractions.
 	//  Instead: Get the closest five tourist attractions to the user - no matter how far away they are.
 	//  Return a new JSON object that contains:
    	// Name of Tourist attraction, 
        // Tourist attractions lat/long, 
        // The user's location lat/long, 
        // The distance in miles between the user's location and each of the attractions.
        // The reward points for visiting each Attraction.
        //    Note: Attraction reward points can be gathered from RewardsCentral
    @GetMapping("/getNearbyAttractions") 
    public List<Attraction> getNearbyAttractions(@RequestParam String userName) throws InterruptedException, ExecutionException {
    	User actualUser = tourGuideService.getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(actualUser);
    	List<Attraction> listAllNearAttraction = tourGuideService.getNearByAttractions(visitedLocation);
    	logger.info("Récupération des attractions selon distance réussie.");
    	return listAllNearAttraction;
    }
    
    @GetMapping("/getFiveNearbyAttractions") 
    public List<Map<String, Object>> getFiveNearbyAttractions(@RequestParam String userName) throws InterruptedException, ExecutionException {
    	User actualUser = tourGuideService.getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(actualUser);
    	List<Map<String, Object>> listFiveNearAttraction = tourGuideService.getFiveNearAttractions(visitedLocation);
    	return listFiveNearAttraction;
    }
    
    @GetMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	User actualUser = tourGuideService.getUser(userName);
    	List<UserReward> listUserReward = tourGuideService.getUserRewards(actualUser);
    	return listUserReward;
    }
       
    @GetMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	User actualUser = tourGuideService.getUser(userName);
    	List<Provider> listProvider = tourGuideService.getTripDeals(actualUser);
    	return listProvider;
    }
   

}