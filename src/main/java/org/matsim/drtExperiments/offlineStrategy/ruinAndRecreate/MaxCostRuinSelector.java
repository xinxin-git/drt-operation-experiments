package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import com.graphhopper.jsprit.core.algorithm.recreate.InsertionData;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.OnlineVehicleInfo;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;
import org.matsim.drtExperiments.offlineStrategy.InsertionCalculator;

import java.util.*;
import java.util.stream.Collectors;

public class MaxCostRuinSelector implements RuinSelector {
    private final double proportion_to_remove;
    private final Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap;
    private final InsertionCalculator insertionCalculator;


    public MaxCostRuinSelector(double proportionToRemove,
                               Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap,
                               InsertionCalculator insertionCalculator) {
        this.proportion_to_remove = proportionToRemove;
        this.onlineVehicleInfoMap = onlineVehicleInfoMap;
        this.insertionCalculator = insertionCalculator;
    }

    @Override
    public List<GeneralRequest> selectRequestsToBeRuined(FleetSchedules fleetSchedules) {
        List<GeneralRequest> openRequests = new ArrayList<>();
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()) {
            timetable.stream().filter(s -> s.getStopType() == TimetableEntry.StopType.PICKUP).forEach(s -> openRequests.add(s.getRequest()));
        }
        //calculate the insertion cost openRequests and sorted
        Map<InsertionCalculator.InsertionData, GeneralRequest> insertionDataToGeneralRequestMap = new HashMap<>();
        for (GeneralRequest openRequest : openRequests) {
            for (OnlineVehicleInfo vehicleInfo : onlineVehicleInfoMap.values()) {
                InsertionCalculator.InsertionData insertionData = insertionCalculator.computeInsertionData(vehicleInfo, openRequest, fleetSchedules);
                insertionDataToGeneralRequestMap.put(insertionData, openRequest);
            }
        }
        List<InsertionCalculator.InsertionData> sortedInsertionDataList = insertionDataToGeneralRequestMap.keySet().stream()
                .sorted(Comparator.comparingDouble(InsertionCalculator.InsertionData::cost).reversed())
                .toList();

        Set<GeneralRequest> requestsToBeRuined = new HashSet<>();
        int numToRemoved = (int) (openRequests.size() * proportion_to_remove) + 1;
        int maxRemoval = 1000;
        numToRemoved = Math.min(numToRemoved, maxRemoval);
        numToRemoved = Math.min(numToRemoved, openRequests.size());

        // find the  corresponding request of the insertionData and put it into the requestsToBeRuined list
        for (int i = 0; i < numToRemoved; i++) {
            InsertionCalculator.InsertionData insertionData = sortedInsertionDataList.get(i);
            GeneralRequest request = insertionDataToGeneralRequestMap.get(insertionData);
            requestsToBeRuined.add(request);
        }
        return requestsToBeRuined.stream().toList();
    }

    @Override
    public double getParameter() {
        return proportion_to_remove;
    }
}
