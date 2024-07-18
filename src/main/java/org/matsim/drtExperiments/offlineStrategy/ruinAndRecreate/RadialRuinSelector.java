package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.EuclideanDistanceCalculator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


public class RadialRuinSelector implements RuinSelector{
    private final Random random;
    private final double proportion_to_remove;

    private final Network network;
    public RadialRuinSelector(Random random, double proportion_to_remove, Network  network){
        this.random = random;
        this.proportion_to_remove  = proportion_to_remove;
        this.network = network;
    }
    @Override
    public List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules) {
        List<GeneralRequest> openRequests = new ArrayList<>();
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()) {
            timetable.stream().filter(s -> s.getStopType() == TimetableEntry.StopType.PICKUP).forEach(s -> openRequests.add(s.getRequest()));
        }
        //select a random request and calculate the distance between the chosen request and other requests,remove the nearest neighbors
        Id<Link> randomChosenRequest = openRequests.get(random.nextInt(openRequests.size())).getFromLinkId();
        List<GeneralRequest> sortedRequests = openRequests.stream()
                .sorted(Comparator.comparingDouble(request -> getDistance(randomChosenRequest, request.getFromLinkId())))
                .toList();

        int numToRemoved = (int) (openRequests.size() * proportion_to_remove) + 1;
        int maxRemoval = 1000;
        numToRemoved = Math.min(numToRemoved, maxRemoval);
        numToRemoved = Math.min(numToRemoved, openRequests.size());
        List<GeneralRequest> requestsToBeRuined = new ArrayList<>();
        for (int i = 0; i < numToRemoved; i++) {
            requestsToBeRuined.add(sortedRequests.get(i));
        }
        return requestsToBeRuined;
    }
    private double getDistance(Id<Link> randomChosenRequest, Id<Link> generalRequest) {
        Link centerLink = network.getLinks().get(randomChosenRequest);
        Coord centerCoordinate = centerLink.getCoord();
        Link link = network.getLinks().get(generalRequest);
        Coord coordinate = link.getCoord();

        double xDiff = centerCoordinate.getX() - coordinate.getX();
        double yDiff = centerCoordinate.getY() - coordinate.getY();
        return Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));
    }
}
