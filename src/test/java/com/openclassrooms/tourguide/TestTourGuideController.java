package com.openclassrooms.tourguide;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tripPricer.Provider;

@SpringBootTest
@AutoConfigureMockMvc
public class TestTourGuideController {

    @MockBean
    private TourGuideService tourGuideService;

    @Autowired
    private MockMvc mockMvc;

    private String USER_NAME;
    private User mockUser;
    private VisitedLocation mockVisitedLocation;

    @BeforeEach
    public void setUp() {
        USER_NAME = "john";
        mockUser = new User(UUID.randomUUID(), USER_NAME, "123", "john@email.com");
        mockVisitedLocation = new VisitedLocation(mockUser.getUserId(), new Location(33.817595, -117.922008), new Date());
    }

    @Test
    public void getIndexTest() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string("Greetings from TourGuide!"));
    }

    @Test
    public void getLocationTest() throws Exception {
        when(tourGuideService.getUser(USER_NAME)).thenReturn(mockUser);
        when(tourGuideService.getUserLocation(mockUser)).thenReturn(mockVisitedLocation);

        mockMvc.perform(get("/getLocation").param("userName", USER_NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.location.latitude").value(33.817595))
            .andExpect(jsonPath("$.location.longitude").value(-117.922008));
    }

    @Test
    public void testGetNearbyAttractions() throws Exception {
        List<Attraction> attractions = Arrays.asList(
            new Attraction("Attraction1", "City1", "State1", 1.0, 2.0),
            new Attraction("Attraction2", "City2", "State2", 1.1, 2.1)
        );

        when(tourGuideService.getUser(USER_NAME)).thenReturn(mockUser);
        when(tourGuideService.getUserLocation(mockUser)).thenReturn(mockVisitedLocation);
        when(tourGuideService.getNearByAttractions(mockVisitedLocation)).thenReturn(attractions);

        mockMvc.perform(get("/getNearbyAttractions").param("userName", USER_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attractionName").value("Attraction1"))
                .andExpect(jsonPath("$[1].city").value("City2"));
    }

    @Test
    public void testGetFiveNearbyAttractions() throws Exception {
        List<Map<String, Object>> nearbyAttractions = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "Attraction1");
        map1.put("distance", 10.5);
        nearbyAttractions.add(map1);

        when(tourGuideService.getUser(USER_NAME)).thenReturn(mockUser);
        when(tourGuideService.getUserLocation(mockUser)).thenReturn(mockVisitedLocation);
        when(tourGuideService.getFiveNearAttractions(mockVisitedLocation, mockUser)).thenReturn(nearbyAttractions);

        mockMvc.perform(get("/getFiveNearbyAttractions").param("userName", USER_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Attraction1"))
                .andExpect(jsonPath("$[0].distance").value(10.5));
    }

    @Test
    public void testGetRewards() throws Exception {
        User user = new User(UUID.randomUUID(), USER_NAME, "123", "email@test.com");
        Location location = new Location(33.817595, -117.922008);
        VisitedLocation visitedLocation = new VisitedLocation(user.getUserId(), location, new Date());
        Attraction attraction1 = new Attraction("Attraction1", "City1", "State1", 33.81, -117.92);
        Attraction attraction2 = new Attraction("Attraction2", "City2", "State2", 33.82, -117.93);

        UserReward reward1 = new UserReward(visitedLocation, attraction1, 10);
        UserReward reward2 = new UserReward(visitedLocation, attraction2, 20);

        List<UserReward> rewards = Arrays.asList(reward1, reward2);

        when(tourGuideService.getUser(USER_NAME)).thenReturn(user);
        when(tourGuideService.getUserRewards(user)).thenReturn(rewards);

        mockMvc.perform(get("/getRewards").param("userName", USER_NAME))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2));
    }


    @Test
    public void testGetTripDeals() throws Exception {
        List<Provider> providers = Arrays.asList(
            new Provider(UUID.randomUUID(), "Trip1", 100.0),
            new Provider(UUID.randomUUID(), "Trip2", 200.0)
        );

        when(tourGuideService.getUser(USER_NAME)).thenReturn(mockUser);
        when(tourGuideService.getTripDeals(mockUser)).thenReturn(providers);

        mockMvc.perform(get("/getTripDeals").param("userName", USER_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Trip1"))
                .andExpect(jsonPath("$[1].price").value(200.0));
    }
}
