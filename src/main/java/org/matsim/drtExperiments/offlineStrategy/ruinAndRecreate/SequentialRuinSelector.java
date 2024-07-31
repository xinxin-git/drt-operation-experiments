package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
/**
 * remove requests from the randomly selected round trip *
 */

public class SequentialRuinSelector implements RuinSelector{
    private final Random random;
    private final double proportion_to_remove;

    public SequentialRuinSelector(Random random, double proportionToRemove) {
        this.random = random;
        proportion_to_remove = proportionToRemove;
    }

    @Override
    public List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules) {
        List<GeneralRequest> openRequests = new ArrayList<>();
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()) {
            timetable.stream().filter(s -> s.getStopType() == TimetableEntry.StopType.PICKUP).forEach(s -> openRequests.add(s.getRequest()));
        }
        // TODO ist  this alltrips right?
        List<List<TimetableEntry>> allTrips = new ArrayList<>(fleetSchedules.vehicleToTimetableMap().values());
        List<GeneralRequest> requestsToBeRuined = new ArrayList<>();

        int numToRemoved = (int) (openRequests.size() * proportion_to_remove) + 1;
        int maxRemoval = 1000;
        numToRemoved = Math.min(numToRemoved, maxRemoval);
        numToRemoved = Math.min(numToRemoved, openRequests.size());

        while (requestsToBeRuined.size() < numToRemoved){
            List<TimetableEntry> selectedTrip = allTrips.get(random.nextInt(allTrips.size()));
            for (GeneralRequest openRequest : getRequestsFromTrip(selectedTrip)){
                if (requestsToBeRuined.size()  >= numToRemoved){
                    return requestsToBeRuined;
                }
                requestsToBeRuined.add(openRequest);
            }
        }
        return requestsToBeRuined;
    }

    @Override
    public double getParameter() {
        return proportion_to_remove;
    }

    public List<GeneralRequest> getRequestsFromTrip(List<TimetableEntry> trip) {
        return trip.stream()
                .map(TimetableEntry::getRequest) // Extract the GeneralRequest from each TimetableEntry
                .toList();
    }


}
