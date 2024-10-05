package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TotalRidingTimeCostCalculator implements SolutionCostCalculator{
    private static final Logger log = LogManager.getLogger(TotalRidingTimeCostCalculator.class);
    private static final double REJECTION_COST = 1e6;
    @Override
    public double calculateSolutionCost(FleetSchedules fleetSchedules, double now) {
        double totalRidingTime = 0;
        Map<Id<Person>, Double> personDepartureTimeMap = new LinkedHashMap<>();
        Map<Id<Person>, Double> personArrivalTimeMap = new LinkedHashMap<>();
        Map<Id<Person>, Double> personRidingTimeMap = new LinkedHashMap<>();
        //initialized departure time of all passengers
        for (Id<Person> personId: fleetSchedules.requestIdToVehicleMap().keySet()){
            personDepartureTimeMap.put(personId,now);
        }
        //update departure or arrival time of passengers
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()){
            for (TimetableEntry stop : timetable){
                Id<Person> passengerId = stop.getRequest().getPassengerId();
                if (stop.getStopType() == TimetableEntry.StopType.PICKUP){
                    personDepartureTimeMap.put(passengerId, stop.getDepartureTime());
                } else {
                    personArrivalTimeMap.put(passengerId, stop.getArrivalTime());
                }
            }
        }
        //calculate total riding time
        for (Id<Person> personId : personDepartureTimeMap.keySet()) {
            if (personArrivalTimeMap.containsKey(personId)) {
                double departure = personDepartureTimeMap.get(personId);
                double arrivalTime = personArrivalTimeMap.get(personId);
                double ridingTime = arrivalTime - departure;
                personRidingTimeMap.put(personId, ridingTime);
            }
        }
        for (double ridingTime : personRidingTimeMap.values()) {
            totalRidingTime += ridingTime;
        }
        log.info("total riding time = " + totalRidingTime);
        return totalRidingTime + REJECTION_COST * fleetSchedules.pendingRequests().size();
    }
}
