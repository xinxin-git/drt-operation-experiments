package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.*;

/**
 * remove requests from the randomly selected round trip *
 */

public class SequentialRuinSelector implements RuinSelector {
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

        List<List<TimetableEntry>> allTours = new ArrayList<>(fleetSchedules.vehicleToTimetableMap().values());
        Set<GeneralRequest> requestsToBeRuined = new LinkedHashSet<>();

        int numToRemoved = (int) (openRequests.size() * proportion_to_remove) + 1;
        int maxRemoval = 1000;
        numToRemoved = Math.min(numToRemoved, maxRemoval);
        numToRemoved = Math.min(numToRemoved, openRequests.size());

        while (requestsToBeRuined.size() < numToRemoved) {
            List<TimetableEntry> selectedTrip = allTours.get(random.nextInt(allTours.size()));
            for (GeneralRequest openRequest : getRequestsFromTrip(selectedTrip)) {
                if (requestsToBeRuined.size() >= numToRemoved) {
                    break;
                }
                requestsToBeRuined.add(openRequest);
            }
        }
        return requestsToBeRuined.stream().toList();
    }

    @Override
    public double getParameter() {
        return proportion_to_remove;
    }

    public List<GeneralRequest> getRequestsFromTrip(List<TimetableEntry> trip) {
        return trip.stream()
                .filter(s -> s.getStopType() == TimetableEntry.StopType.PICKUP)
                .map(TimetableEntry::getRequest)
                .toList();
    }


}
