package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

public class SimpleAnnealingThresholdAcceptor implements RecreateSolutionAcceptor {

    @Override
    public boolean acceptSolutionOrNot(double currentScore, double previousScore, int currentIteration, int totalIterations,
                                       double initialThreshold, double halfLife) {
        // Parameters for the annealing acceptor in input arguments

        double x = (double) currentIteration / (double) totalIterations;
        double threshold = initialThreshold * Math.exp(-Math.log(2) * x / halfLife);

        return currentScore < (1 + threshold) * previousScore;
    }
}
