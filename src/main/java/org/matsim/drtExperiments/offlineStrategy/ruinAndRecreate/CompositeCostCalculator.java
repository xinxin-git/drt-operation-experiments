package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.api.core.v01.network.Network;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.offlineStrategy.LinkToLinkTravelTimeMatrix;

public class CompositeCostCalculator implements SolutionCostCalculator{
    private final Network network;
    private final LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix;
    private final double totalDrivingTime_share;
    private final double totalRidingTime_share;
    private final double totalDelay_share;

    public CompositeCostCalculator(Network network, LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix,
                                   double totalDrivingTimeShare, double totalRidingTimeShare, double totalDelayShare){
        this.network = network;
        this.linkToLinkTravelTimeMatrix = linkToLinkTravelTimeMatrix;
        totalDrivingTime_share = totalDrivingTimeShare;
        totalRidingTime_share = totalRidingTimeShare;
        totalDelay_share = totalDelayShare;
    }
    @Override
    public double calculateSolutionCost(FleetSchedules fleetSchedules, double now) {
        SolutionCostCalculator totalDrivingTime = new DefaultSolutionCostCalculator();
        SolutionCostCalculator totalRidingTime = new TotalRidingTimeCostCalculator();
        SolutionCostCalculator totalDelay = new TotalDelayCostCalculator(network,linkToLinkTravelTimeMatrix);
        // Multiplying the total driving time by 3 to adjust the magnitude, because the driving time is on a different scale compared to other costs
        return totalDrivingTime_share* 3 * totalDrivingTime.calculateSolutionCost(fleetSchedules,now) +
                totalRidingTime_share * totalRidingTime.calculateSolutionCost(fleetSchedules,now) +
                totalDelay_share * totalDelay.calculateSolutionCost(fleetSchedules,now);
    }
}
