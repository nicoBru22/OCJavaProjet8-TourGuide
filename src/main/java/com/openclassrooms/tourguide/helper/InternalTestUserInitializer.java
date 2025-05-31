package com.openclassrooms.tourguide.helper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import com.openclassrooms.tourguide.user.User;

import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

public class InternalTestUserInitializer {

	public Map<String, User> initializeUsers(int numberOfUsers) {
        Map<String, User> internalUserMap = new HashMap<>();

        IntStream.range(0, numberOfUsers).forEach(i -> {
            String userName = "internalUser" + i;
            User user = new User(UUID.randomUUID(), userName, "000", userName + "@tourGuide.com");
            generateUserLocationHistory(user);
            internalUserMap.put(userName, user);
        });

        return internalUserMap;
    }

    private void generateUserLocationHistory(User user) {
        IntStream.range(0, 3).forEach(i -> {
            user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
                new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
        });
    }

    private double generateRandomLongitude() {
        return -180 + new Random().nextDouble() * 360;
    }

    private double generateRandomLatitude() {
        return -85.05112878 + new Random().nextDouble() * 170.10225756;
    }

    private Date getRandomTime() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }
}
