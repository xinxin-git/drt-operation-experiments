package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TotalDelayCostCalculator implements SolutionCostCalculator {
    private final Network network;
    private static final double REJECTION_COST = 1e6;

    public TotalDelayCostCalculator(Network network) {
        this.network = network;
    }

    @Override
    public double calculateSolutionCost(FleetSchedules fleetSchedules, double now) {
        double totalDelay = 0;
        Map<Id<Person>, Double> totalDelayMap = new LinkedHashMap<>();
        TravelTime travelTime = new QSimFreeSpeedTravelTime(1);
        TravelDisutility travelCosts = new TimeAsTravelDisutility(travelTime);
        LeastCostPathCalculator router = new SpeedyALTFactory().createPathCalculator(network, travelCosts, travelTime);
        // calculate delay of every request
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()) {
            for (TimetableEntry stop : timetable) {
                if (stop.getStopType() == TimetableEntry.StopType.DROP_OFF) {
                    Id<Person> passengerId = stop.getRequest().getPassengerId();
                    // calculate the earliest arrival time
                    Link fromLink = network.getLinks().get(stop.getRequest().getFromLinkId());
                    Link toLink = network.getLinks().get(stop.getRequest().getToLinkId());
                    double earliestArrivalTime = VrpPaths.calcAndCreatePath(fromLink, toLink, stop.getRequest().getEarliestDepartureTime(), router, travelTime).getTravelTime() +
                            stop.getRequest().getEarliestDepartureTime();
                    // calculate delay
                    double actualArrivalTime = stop.getArrivalTime();
                    double delay = actualArrivalTime - earliestArrivalTime;
                    totalDelayMap.put(passengerId, delay);
                }
            }
        }
        for (double delay : totalDelayMap.values()) {
            totalDelay += delay;
        }
        // log.info("total delay = " + totalDelay);
        return totalDelay + REJECTION_COST * fleetSchedules.pendingRequests().size();
    }
}