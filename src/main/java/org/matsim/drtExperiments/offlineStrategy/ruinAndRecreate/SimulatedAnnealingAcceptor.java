package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import java.util.Random;

public class SimulatedAnnealingAcceptor implements RecreateSolutionAcceptor {
    private final double probability;
    private final Random random;

    public SimulatedAnnealingAcceptor(double probability, Random random) {
        this.probability = probability;
        this.random = random;
    }

    @Override
    public boolean acceptSolutionOrNot(double currentScore, double previousScore, int currentIteration, int totalIterations) {
        if (currentScore < previousScore) {
            return true;
        } else {
            return probability > random.nextDouble();
        }
    }

    @Override
    public double getParameter() {
        return probability;
    }

}
