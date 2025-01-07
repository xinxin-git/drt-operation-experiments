package org.matsim.drtExperiments.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;


import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DrtPerformanceAnalysis {
    private static final Logger log = LogManager.getLogger(DrtPerformanceAnalysis.class);

    public static void main(String[] args) throws IOException {
        //customer performance
        String networkFile = "D:/Module/Masterarbeit/drt-operation-experiments/scenarios/kelheim/kelheim-v3.0-drt-network.xml.gz";
        String customerInputFilePath = "D:/Module/Masterarbeit/drt-operation-experiments/scenarios/kelheim/output/delay/fleet35/it5000+0.2/output_drt_legs_drt.csv";
        String vehicleInputFilePath = "D:/Module/Masterarbeit/drt-operation-experiments/scenarios/kelheim/output/delay/fleet35/it5000+0.2/drt_vehicle_stats_drt.csv";
        String outputFilePath = "D:/Module/Masterarbeit/drt-operation-experiments/scenarios/kelheim/output/delay/fleet35/it5000+0.2/drt_performance_analysis.csv";

        Network network = NetworkUtils.readNetwork(networkFile);
        TravelTime travelTimes = new QSimFreeSpeedTravelTime(1);
        TravelDisutility travelCosts = new TimeAsTravelDisutility(travelTimes);
        LeastCostPathCalculator router = new SpeedyALTFactory().createPathCalculator(network, travelCosts, travelTimes);

        double totalTravelTime = 0;
        double totalRidingTime = 0;
        double totalDelay = 0;
        int requestCount = 0;

        List<List<Object>> updatedRecords = new ArrayList<>();

        try {
            FileReader customerFileReader = new FileReader(customerInputFilePath);
            CSVParser parser = CSVFormat.Builder.create()
                    .setDelimiter(';')
                    .setHeader("departureTime", "personId", "vehicleId", "fromLinkId",
                            "fromX", "fromY", "toLinkId", "toX", "toY",
                            "waitTime", "arrivalTime", "travelTime",
                            "travelDistance_m", "directTravelDistance_m",
                            "fareForLeg", "latestDepartureTime", "latestArrivalTime")
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(customerFileReader);

            List<CSVRecord> customerRecords = parser.getRecords();
            List<String> headers = new ArrayList<>(parser.getHeaderNames());
            headers.add("delay");
            headers.add("travelDelay_share");

            for (CSVRecord record : customerRecords) {
                double departureTime = Double.parseDouble(record.get("departureTime"));
                double waitTime = Double.parseDouble(record.get("waitTime"));
                double arrivalTime = Double.parseDouble(record.get("arrivalTime"));
                double travelTime = Double.parseDouble(record.get("travelTime"));

                Link fromLink = network.getLinks().get(Id.createLinkId(record.get("fromLinkId")));
                Link toLink = network.getLinks().get(Id.createLinkId(record.get("toLinkId")));
                double directTravelTime = VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router, travelTimes).getTravelTime();
                double earliestArrivalTime = directTravelTime + departureTime;
                double delay = arrivalTime - earliestArrivalTime;
                double travelDelay_share = travelTime / directTravelTime;

                List<Object> updatedRecord = new ArrayList<>();
                record.forEach(updatedRecord::add);
                updatedRecord.add(String.valueOf((int) Math.round(delay)));
                updatedRecord.add(travelDelay_share);
                updatedRecords.add(updatedRecord);

                double ridingTime = travelTime - waitTime;
                totalRidingTime += ridingTime;
                totalTravelTime += travelTime;
                totalDelay += delay;
                requestCount++;
            }

            String outputFilePathWithDelay = "D:/Module/Masterarbeit/drt-operation-experiments/scenarios/kelheim/output/delay/fleet35/it5000+0.2/output_drt_legs_drt_withDelay.csv";
            CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(outputFilePathWithDelay),
                    CSVFormat.Builder.create()
                            .setDelimiter(';')
                            .setHeader(headers.toArray(new String[0]))
                            .build());
            for (List<Object> updatedRecord : updatedRecords) {
                csvPrinter.printRecord(updatedRecord);
            }
            log.info("output_drt_legs_drt_with_delay.csv file is updated");


            double travelTime_mean = totalTravelTime / requestCount;
            double ridingTime_mean = totalRidingTime / requestCount;
            double delay_mean = totalDelay / requestCount;
            log.info("travelTime_mean = " + String.format("%.2f", travelTime_mean) +
                    "; ridingTime_mean = " + String.format("%.2f", ridingTime_mean) +
                    "; delay_mean = " + String.format("%.2f", delay_mean));

            // vehicle Performance
            double vehicleTotalDistance = 0;
            FileReader vehicleFileReader = new FileReader(vehicleInputFilePath);
            CSVParser vehicleParser = CSVFormat.Builder.create()
                    .setDelimiter(';')
                    .setHeader("runId", "iteration", "vehicles", "totalDistance", "totalEmptyDistance",
                            "emptyRatio", "totalPassengerDistanceTraveled", "averageDrivenDistance",
                            "averageEmptyDistance", "averagePassengerDistanceTraveled", "d_p/d_t",
                            "l_det", "minShareIdleVehicles", "minCountIdleVehicles")
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(vehicleFileReader);
            List<CSVRecord> vehicleRecords = vehicleParser.getRecords();
            for (CSVRecord record : vehicleRecords) {
                vehicleTotalDistance = Double.parseDouble(record.get("totalDistance"));
            }
            double fuelCost = 0.08 * vehicleTotalDistance / 1000;
            log.info("vehicleFuelCost = " + fuelCost);

            // write result in a csv file
            CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputFilePath), CSVFormat.TDF);
            List<String> titleRow = Arrays.asList("travelTime_mean", "ridingTime_mean", "delay_mean", "preis_mean");
            List<String> valueRow = Arrays.asList(
                    String.format("%.2f", travelTime_mean),
                    String.format("%.2f", ridingTime_mean),
                    String.format("%.2f", delay_mean));
            tsvWriter.printRecord(titleRow);
            tsvWriter.printRecord(valueRow);
            tsvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
