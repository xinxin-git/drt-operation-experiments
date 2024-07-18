package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

public class SimpleAnnealingThresholdAcceptor implements RecreateSolutionAcceptor {
    // Parameters for the annealing acceptor in input arguments
    private final double initialThreshold;
    private final double halfLife;
    public SimpleAnnealingThresholdAcceptor(double initialThreshold, double halfLife){
        this.initialThreshold = initialThreshold;
        this.halfLife = halfLife;

    }
    @Override
    public boolean acceptSolutionOrNot(double currentScore, double previousScore, int currentIteration, int totalIterations) {
        double x = (double) currentIteration / (double) totalIterations;
        double threshold = initialThreshold * Math.exp(-Math.log(2) * x / halfLife);
        return currentScore < (1 + threshold) * previousScore;
    }

    @Override
    public double getParameter() {
        return initialThreshold;
    }

}
