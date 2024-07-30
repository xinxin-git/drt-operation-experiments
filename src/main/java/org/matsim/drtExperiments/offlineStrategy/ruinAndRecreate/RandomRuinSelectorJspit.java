package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * random ruin strategy heuristic from Jsprit, the number to removed is not determined *
 */
public class RandomRuinSelectorJspit implements RuinSelector{
    private final Random random;
    private final double max_proportion_to_remove;
    private final double min_proportion_to_remove;
    private double proportion_to_remove;

    public RandomRuinSelectorJspit(Random random,double max_proportion_to_remove,double min_proportion_to_remove){
        this.random = random;
        if (max_proportion_to_remove < min_proportion_to_remove) {
            throw new IllegalArgumentException("maxProportionToRemove must be greater than or equal to minProportionToRemove");
        }
        this.max_proportion_to_remove = max_proportion_to_remove;
        this.min_proportion_to_remove = min_proportion_to_remove;

    }
    private double CalcuProportionToRemove(){
        double proportion = min_proportion_to_remove + (max_proportion_to_remove - min_proportion_to_remove) * random.nextDouble();
        proportion_to_remove = proportion;
        return proportion;
    }

    @Override
    public List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules) {
        List<GeneralRequest> openRequests = new ArrayList<>();
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()) {
            timetable.stream().filter(s -> s.getStopType() == TimetableEntry.StopType.PICKUP).forEach(s -> openRequests.add(s.getRequest()));
        }

        Collections.shuffle(openRequests, random);
        int numToRemoved = (int) (openRequests.size() * CalcuProportionToRemove()) + 1;
        int maxRemoval = 1000;
        numToRemoved = Math.min(numToRemoved, maxRemoval);
        numToRemoved = Math.min(numToRemoved, openRequests.size());
        List<GeneralRequest> requestsToBeRuined = new ArrayList<>();
        for (int i = 0; i < numToRemoved; i++) {
            requestsToBeRuined.add(openRequests.get(i));
        }
        return requestsToBeRuined;
    }

    @Override
    public double getParameter() {
        return proportion_to_remove;
    }

}
