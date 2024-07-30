package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MultiRadialRuinSelector implements RuinSelector{
    private final Random random;
    private final double proportion_to_remove;

    private final Network network;
    private final double radial;
    public MultiRadialRuinSelector(Random random, double proportion_to_remove, Network  network, double radial){
        this.random = random;
        this.proportion_to_remove  = proportion_to_remove;
        this.network = network;
        this.radial = radial;
    }

    @Override
    public List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules) {
        List<GeneralRequest> openRequests = new ArrayList<>();
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()) {
            timetable.stream().filter(s -> s.getStopType() == TimetableEntry.StopType.PICKUP).forEach(s -> openRequests.add(s.getRequest()));
        }
        //
        List<GeneralRequest> requestsToBeRuined = new ArrayList<>();
        while (requestsToBeRuined.size() < openRequests.size() * proportion_to_remove){
            Id<Link> randomChosenLinkId = openRequests.get(random.nextInt(openRequests.size())).getFromLinkId();
            for (GeneralRequest openRequest : openRequests) {
                if (getDistance(randomChosenLinkId,openRequest.getFromLinkId()) <= radial){
                    if (requestsToBeRuined.size()  >= openRequests.size() * proportion_to_remove){
                        return requestsToBeRuined;
                    }
                    requestsToBeRuined.add(openRequest);
                }
            }
        }
        return requestsToBeRuined;
    }

    @Override
    public double getParameter() {
        return radial;
    }
    private double getDistance(Id<Link> randomChosenLinkId, Id<Link> generalRequestLinkId) {
        Coord centerCoordinate = network.getLinks().get(randomChosenLinkId).getToNode().getCoord();
        Coord coordinate = network.getLinks().get(generalRequestLinkId).getToNode().getCoord();
        double xDiff = centerCoordinate.getX() - coordinate.getX();
        double yDiff = centerCoordinate.getY() - coordinate.getY();
        return Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));
    }
}
