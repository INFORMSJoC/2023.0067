
package problems;

import utils.Version;

import java.util.function.DoubleUnaryOperator;

/**
 * An instance of this class represents an instance of the production planning problem and contains its data.
 */
public class ProductionPlanningProblemWithSalvageRevenue extends ProductionPlanningProblem{


    public ProductionPlanningProblemWithSalvageRevenue(int nFacilities, int nProducts, double[] totalCapacities, double[] leftoverCosts, double[][] manufacturingCosts, double[] salesPrices,
                                                       int[][] nProductionLevels, double[][][] productionLevelLowerBounds, double[][][] productionLevelUpperBounds,
                                                       int[] nDistributions, int maxNDistributions, int[][][] distributionProductionLevels, String[][] distributionNames,
                                                       double[][][] probabilities, int[][] nScenarios, int maxNScenarios, double[][][][] yieldRealization, double[][][] demandRealization){

        super(nFacilities, nProducts, totalCapacities, leftoverCosts, manufacturingCosts, salesPrices,
                nProductionLevels, productionLevelLowerBounds, productionLevelUpperBounds,
                nDistributions, maxNDistributions, distributionProductionLevels, distributionNames,
                probabilities, nScenarios, maxNScenarios, yieldRealization, demandRealization);

        this.upperBoundOnProductionRealization = new double[nProducts][maxNDistributions][maxNScenarios];
        for(int p = 1; p <= nProducts; p++){
            for(int d = 1; d <= getnDistributions(p); d++){
                for(int s = 1; s <= getnScenarios(p,d); s++){
                    this.upperBoundOnProductionRealization[p-1][d-1][s-1] = computeUpperBoundOnProductionRealization(p,d,s);
                }
            }
        }
        this.upperBoundOnProductExpectation = new double[nProducts];
        for(int p = 1; p <= nProducts; p++){
            this.upperBoundOnProductExpectation[p-1] = computeUpperBoundProductExpectation(p);
        }
    }



    /**
     * ==========
     * Bounds
     * ==========
    **/

    /**
     * Computes an upper bound on the total production realized for a given product, distribution and scenario.
     * @param product
     * @param distribution
     * @param scenario
     * @return
     */
     double computeUpperBoundOnProductionRealization(int product, int distribution, int scenario){
        double totalProduction = 0;
        for(int f = 1; f <= getnFacilities(); f++){
            // Calculates the largest production level upper bound
            int level = getDistributionProductionLevels(product,distribution,f) + 1;
            double highestProductionLevel = getProductionLevelUpperBound(f,product,level);
            totalProduction = totalProduction + getYieldRealization(product,distribution,f,scenario) * Math.min(highestProductionLevel,getTotalCapacity(f));
        }
        return totalProduction;
    }

    /**
     * Computes an upper bound on the phi_p variable for the single-cut master problem.
     * The bound is given by the highest expectation among the possible distributions for the given product.
     * @param product
     * @return
     */
     double computeUpperBoundProductExpectation(int product){
        double upperBound = Double.NEGATIVE_INFINITY;
        // We start by calculating, for each distribution and scenario,
        // the production of p is upper bounded as follows
        for(int d = 1; d <= getnDistributions(product); d++){
            double distributionExpectation = 0;
            for(int s = 1; s <= getnScenarios(product,d); s++) {

                double maxProduction = getUpperBoundOnProductionRealization(product,d,s);

                double scenarioProfit = getSalesPrice(product) * Math.min(maxProduction,getDemandRealization(product,d,s))
                        + getLeftoverCosts(product) * Math.max(0, maxProduction - getDemandRealization(product,d,s));

                distributionExpectation = distributionExpectation + getProbability(product,d,s) * scenarioProfit;
            }
            if(distributionExpectation > upperBound){
                upperBound = distributionExpectation;
            }
        }

        return upperBound;
    }

}
