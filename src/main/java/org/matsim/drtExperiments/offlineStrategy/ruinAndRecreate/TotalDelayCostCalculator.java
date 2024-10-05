package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;
import org.matsim.drtExperiments.offlineStrategy.LinkToLinkTravelTimeMatrix;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TotalDelayCostCalculator implements SolutionCostCalculator {
    private static final Logger log = LogManager.getLogger(TotalDelayCostCalculator.class);
    private final Network network;
    private final LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix;
    private static final double REJECTION_COST = 1e6;


    public TotalDelayCostCalculator(Network network, LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix){
        this.network = network;
        this.linkToLinkTravelTimeMatrix = linkToLinkTravelTimeMatrix;
    }
    @Override
    public double calculateSolutionCost(FleetSchedules fleetSchedules, double now) {
        double totalDelay = 0;
        Map<Id<Person>, Double> totalDelayMap = new LinkedHashMap<>();
        //calculate delay of every request
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()) {
            for (TimetableEntry stop : timetable) {
                if (stop.getStopType() == TimetableEntry.StopType.DROP_OFF){
                    Id<Person> passengerId = stop.getRequest().getPassengerId();
                    double earliestArrivalTime = calculateEarliestArrivalTime(stop.getRequest());
                    double actualArrivalTime = stop.getArrivalTime();
                    double delay = actualArrivalTime - earliestArrivalTime;
                    totalDelayMap.put(passengerId,delay);
                }
            }
        }
        for (double delay : totalDelayMap.values()) {
            totalDelay += delay;
        }
        log.info("total delay = " + totalDelay);
        return totalDelay + REJECTION_COST * fleetSchedules.pendingRequests().size();
    }

    public double calculateEarliestArrivalTime(GeneralRequest generalRequest){
        Id<Link> fromLinkId = generalRequest.getFromLinkId();
        Id<Link> toLinkId = generalRequest.getToLinkId();
        Link fromLink = network.getLinks().get(fromLinkId);
        Link toLink = network.getLinks().get(toLinkId);
        return linkToLinkTravelTimeMatrix.getTravelTime(fromLink,toLink,generalRequest.getEarliestDepartureTime()) + generalRequest.getEarliestDepartureTime();
    }

}