package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.*;

/**
 * select multiple center points, each of which ruin at a certain radius until the numToRemoved is reached *
 */

public class MultiRadialRuinSelector implements RuinSelector {
    private final Random random;
    private final double proportion_to_remove;
    private final Network network;
    private final double radius;

    public MultiRadialRuinSelector(Random random, double proportion_to_remove, Network network, double radius) {
        this.random = random;
        this.proportion_to_remove = proportion_to_remove;
        this.network = network;
        this.radius = radius;
    }

    @Override
    public List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules) {
        List<GeneralRequest> openRequests = new ArrayList<>();
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()) {
            timetable.stream().filter(s -> s.getStopType() == TimetableEntry.StopType.PICKUP).forEach(s -> openRequests.add(s.getRequest()));
        }
        // requestsToBeRuined as a set to avoid the same request being added repeatedly, otherwise get an error: Vehicle ID is null for some passengers
        Set<GeneralRequest> requestsToBeRuined = new LinkedHashSet<>();

        int numToRemoved = (int) (openRequests.size() * proportion_to_remove) + 1;
        int maxRemoval = 1000;
        numToRemoved = Math.min(numToRemoved, maxRemoval);
        numToRemoved = Math.min(numToRemoved, openRequests.size());

        while (requestsToBeRuined.size() < numToRemoved) {
            Id<Link> randomChosenLinkId = openRequests.get(random.nextInt(openRequests.size())).getFromLinkId();
            for (GeneralRequest openRequest : openRequests) {
                if (getDistance(randomChosenLinkId, openRequest.getFromLinkId()) <= radius) {
                    if (requestsToBeRuined.size() >= numToRemoved) {
                        break;
                    }
                    requestsToBeRuined.add(openRequest);
                }
            }
        }
        return requestsToBeRuined.stream().toList();
    }

    @Override
    public double getParameter() {
        return radius;
    }

    private double getDistance(Id<Link> randomChosenLinkId, Id<Link> generalRequestLinkId) {
        Coord centerCoordinate = network.getLinks().get(randomChosenLinkId).getToNode().getCoord();
        Coord coordinate = network.getLinks().get(generalRequestLinkId).getToNode().getCoord();
        double xDiff = centerCoordinate.getX() - coordinate.getX();
        double yDiff = centerCoordinate.getY() - coordinate.getY();
        return Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));
    }
}