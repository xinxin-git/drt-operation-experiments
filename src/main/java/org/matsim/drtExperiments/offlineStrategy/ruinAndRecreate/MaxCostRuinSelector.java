package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.OnlineVehicleInfo;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;
import org.matsim.drtExperiments.offlineStrategy.LinkToLinkTravelTimeMatrix;

import java.util.*;
import java.util.stream.Collectors;

public class MaxCostRuinSelector implements RuinSelector {
    private final double proportion_to_remove;
    Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap;
    private final Network network;
    LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix;

    public MaxCostRuinSelector(double proportionToRemove, Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap, Network network, LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix) {
        this.proportion_to_remove = proportionToRemove;
        this.onlineVehicleInfoMap = onlineVehicleInfoMap;
        this.network = network;
        this.linkToLinkTravelTimeMatrix = linkToLinkTravelTimeMatrix;
    }

    @Override
    public List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules) {
        Map<GeneralRequest, Double> request2TravelTimeMap = new LinkedHashMap<>();
        List<List<TimetableEntry>> allTours = new ArrayList<>(fleetSchedules.vehicleToTimetableMap().values());

        for (List<TimetableEntry> tour : allTours){
            //find the vehicleId of this tour
            Id<DvrpVehicle> vehicleId = getVehicleIdByTimetable(tour,fleetSchedules.vehicleToTimetableMap());
            //calculate the total travel time of a tour
            double totalTravelTimeOfATour = calculateTravelTime(tour,vehicleId);
            for (GeneralRequest openRequest : getRequestsFromTour(tour)){
                List<TimetableEntry> copyTour = FleetSchedules.copyTimetable(tour);
                //remove the request and calculate the new travel time
                List<TimetableEntry> newTour = removeRequestFromTour(copyTour, openRequest);
                double newTravelTime  = calculateTravelTime(newTour,vehicleId);
                double travelTimeDifference = totalTravelTimeOfATour - newTravelTime;
                request2TravelTimeMap.put(openRequest,travelTimeDifference);
            }
        }
        //sorted
        List<GeneralRequest> sortedOpenRequests = request2TravelTimeMap.entrySet().stream()
                .sorted(Map.Entry.<GeneralRequest, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        List<GeneralRequest> requestsToBeRuined = new ArrayList<>();
        int numToRemoved = (int) (sortedOpenRequests.size() * proportion_to_remove) + 1;
        int maxRemoval = 1000;
        numToRemoved = Math.min(numToRemoved, maxRemoval);
        numToRemoved = Math.min(numToRemoved, sortedOpenRequests.size());

        for (int i = 0; i < numToRemoved; i++) {
            requestsToBeRuined.add(sortedOpenRequests.get(i));
        }
        return requestsToBeRuined;
    }

    @Override
    public double getParameter() {
        return proportion_to_remove;
    }

    public List<GeneralRequest> getRequestsFromTour(List<TimetableEntry> tour) {
        return tour.stream()
                .filter(s -> s.getStopType() == TimetableEntry.StopType.PICKUP)
                .map(TimetableEntry::getRequest)
                .toList();
    }

    private double calculateTravelTime(List<TimetableEntry> tour, Id<DvrpVehicle> vehicleId) {
        double totalTravelTime = 0.;
        //current position of the vehicle and current time
        Link currentLink = onlineVehicleInfoMap.get(vehicleId).currentLink();
        double currentTime = onlineVehicleInfoMap.get(vehicleId).divertableTime();

        TimetableEntry firstStop = tour.get(0);
        Id<Link> firstStopLinkId = firstStop.getLinkId();
        Link fistStopLink = network.getLinks().get(firstStopLinkId);

        totalTravelTime += linkToLinkTravelTimeMatrix.getTravelTime(currentLink,fistStopLink,currentTime);

        for (int i = 1; i < tour.size(); i++) {
            TimetableEntry previousStop = tour.get(i-1);
            Id<Link> previousStopLinkId = previousStop.getLinkId();
            Link previousStopLink = network.getLinks().get(previousStopLinkId);

            TimetableEntry currentStop = tour.get(i);
            Id<Link> currentStopLinkId = currentStop.getLinkId();
            Link currentStopLink = network.getLinks().get(currentStopLinkId);

            totalTravelTime += linkToLinkTravelTimeMatrix.getTravelTime(previousStopLink,currentStopLink,currentTime);
        }
        return totalTravelTime;
    }

    private List<TimetableEntry> removeRequestFromTour(List<TimetableEntry> tour, GeneralRequest requestToRemove) {
        return tour.stream()
                .filter(entry -> !entry.getRequest().equals(requestToRemove))
                .collect(Collectors.toList());
    }

    public Id<DvrpVehicle> getVehicleIdByTimetable(List<TimetableEntry> tour, Map<Id<DvrpVehicle>, List<TimetableEntry>> vehicleToTimetableMap) {
        for (Map.Entry<Id<DvrpVehicle>, List<TimetableEntry>> entry : vehicleToTimetableMap.entrySet()) {
            if (entry.getValue().equals(tour)) {
                return entry.getKey();
            }
        }
        return null;
    }
}

