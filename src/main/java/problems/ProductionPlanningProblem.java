package problems;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import org.jgrapht.*;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.*;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

abstract public class ProductionPlanningProblem {
    protected final int nFacilities;
    protected final int nProducts;
    protected final double[] leftoverCosts; // Cost of unsold product [nProducts]
    protected final double[][] manufacturingCosts; // Cost of producing one unit of product at each facility [nFacilities][nProducts]
    protected final double[] salesPrices; // Sales price of each product [nProducts]
    protected final int[][] nProductionLevels; // Number of production levels [nFacilities][nProducts]
    protected final int maxnProductionLevels; // Highest number of production levels
    protected final double[][][] productionLevelLowerBounds; // Production levels lower bounds [nFacilities][nProducts][maxNProductionLevels]
    protected final double[][][] productionLevelUpperBounds; // Production levels upper bounds [nFacilities][nProducts][maxNProductionLevels]
    protected final double[] totalCapacities; // Total production bound [nFacilities]

    protected final int nDistributions[]; // The number of distributions for each product [nProducts]
    protected final int maxNDistributions; // The highest number of distributions among the different products
    protected final int distributionProductionLevels[][][]; // Contains the level at a given facility corresponding to product p and distribution d [nProducts][nDistributions][nFacilities]
    protected final String distributionNames[][]; // The name of the distributuon (e.g., D1, D2, ...) [nProducts][maxNDistributions]
    protected final int nScenarios[][]; // Number of scenarios for each product and distribution [nProducts][nDistribution]
    protected final int maxNScenarios; // The highest number of scenarios across all distributions
    protected final double probabilities[][][]; // Probability of each scenario for each distribution [nProducts][nDistributions][maxNScenarios]
    protected final double yieldRealization[][][][]; // The yield realization for a given product, distribution, facility and scenario [nProducts][maxNDistributions][nFacilities][maxNScenarios]
    protected final double demandRealization[][][]; // The demand realization for a given product, distribution and scenario [nProducts][maxNDistributions][maxNScenarios]
    protected double upperBoundOnProductionRealization[][][]; // The maximum production for a given product, distribution, scenario [nProducts][maxNDistributions][maxNScenarios]
    protected double upperBoundOnProductExpectation[]; // An upper bound on the highest profit expectation for a given product [nProducts]

    protected ProductionPlanningProblem(int nFacilities, int nProducts, double[] totalCapacities, double[] leftoverCosts, double[][] manufacturingCosts, double[] salesPrices,
                                                      int[][] nProductionLevels, double[][][] productionLevelLowerBounds, double[][][] productionLevelUpperBounds,
                                                      int[] nDistributions, int maxNDistributions, int[][][] distributionProductionLevels, String[][] distributionNames,
                                                      double[][][] probabilities, int[][] nScenarios, int maxNScenarios, double[][][][] yieldRealization, double[][][] demandRealization){
        this.nFacilities = nFacilities;
        this.nProducts = nProducts;
        this.leftoverCosts = leftoverCosts;
        this.manufacturingCosts = manufacturingCosts;
        this.salesPrices = salesPrices;
        this.nProductionLevels = nProductionLevels;
        this.nDistributions = nDistributions;
        this.maxNDistributions = maxNDistributions;
        this.distributionProductionLevels = distributionProductionLevels;
        this.distributionNames = distributionNames;
        this.probabilities = probabilities;
        this.nScenarios = nScenarios;
        this.maxNScenarios = maxNScenarios;
        this.yieldRealization = yieldRealization;
        this.demandRealization = demandRealization;
        int maxNLevels = 0;
        for(int f = 1; f <= nFacilities; f++){
            for(int p = 1; p <= nProducts; p++){
                if (getnProductionLevels(f,p) > maxNLevels){
                    maxNLevels = getnProductionLevels(f,p);
                }
            }
        }
        this.maxnProductionLevels = maxNLevels;
        this.productionLevelLowerBounds = productionLevelLowerBounds;
        this.productionLevelUpperBounds = productionLevelUpperBounds;
        this.totalCapacities = totalCapacities;
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
            this.upperBoundOnProductExpectation[p - 1] = computeUpperBoundProductExpectation(p);
        }

    }


    public int getnFacilities() {
        return nFacilities;
    }

    public int getnProducts() {
        return nProducts;
    }

    public int getnProductionLevels(int facility,int product) {
        return nProductionLevels[facility-1][product-1];
    }

    public int getMaxnProductionLevels() {
        return maxnProductionLevels;
    }

    public int getMaxNScenarios() {
        return maxNScenarios;
    }

    public int getnDistributions(int product) {
        return nDistributions[product-1];
    }

    public int getMaxNDistributions() {
        return maxNDistributions;
    }

    public int getnScenarios(int product, int distribution) {
        return nScenarios[product-1][distribution-1];
    }

    public double getManufacturingCosts(int facility,int product) {
        return manufacturingCosts[facility-1][product-1];
    }

    public double getSalesPrice(int product) {
        return salesPrices[product-1];
    }

    public double getProbability(int product,int distribution,int scenario) {
        return probabilities[product-1][distribution-1][scenario-1];
    }

    public double getLeftoverCosts(int product) {
        return leftoverCosts[product-1];
    }

    /**
     * Returns the level activated for product p at facility f if distribution d is enforced.
     * The level returned is in [0,..,NLevels-1].
     * @param product
     * @param distribution
     * @param facility
     * @return
     */
    public int getDistributionProductionLevels(int product, int distribution, int facility) {
        return distributionProductionLevels[product-1][distribution-1][facility-1];
    }

    public double getYieldRealization(int product, int distribution, int facility, int scenario) {
        return yieldRealization[product-1][distribution-1][facility-1][scenario-1];
    }

    public double getDemandRealization(int product, int distribution, int scenario) {
        return demandRealization[product-1][distribution-1][scenario-1];
    }
    /**
     * Returns the lower bound for a given production level at a given facility for a given product.
     * @param facility
     * @param product
     * @param level
     * @return
     */
    public double getProductionLevelLowerBound(int facility, int product, int level) {
        return productionLevelLowerBounds[facility-1][product-1][level-1];
    }
    /**
     * Returns the upper bound for a given production level at a given facility for a given product.
     * @param facility
     * @param product
     * @param level
     * @return
     */
    public double getProductionLevelUpperBound(int facility, int product, int level) {
        return productionLevelUpperBounds[facility - 1][product - 1][level - 1];
    }
    public double getTotalCapacity(int facility) {
        return totalCapacities[facility-1];
    }

    public double getHighestYield(int product, int facility){
        double highestYield = Double.NEGATIVE_INFINITY;
        for(int d = 1; d <= getnDistributions(product); d++){
            for(int s = 1; s <= getnScenarios(product,d); s++){
                if(getYieldRealization(product,d,facility,s) > highestYield){
                    highestYield = getYieldRealization(product,d,facility,s);
                }
            }
        }
        return highestYield;
    }
    public double getHighestYieldForLevel(int product, int facility, int level){
        double highestYield = Double.NEGATIVE_INFINITY;
        for(int d = 1; d <= getnDistributions(product); d++){
            if(getDistributionProductionLevels(product,d,facility) == (level-1)) {
                for (int s = 1; s <= getnScenarios(product, d); s++) {
                    if (getYieldRealization(product, d, facility, s) > highestYield) {
                        highestYield = getYieldRealization(product, d, facility, s);
                    }
                }
            }
        }
        System.out.println(product+"-"+facility+"-"+level+" "+highestYield);
        return highestYield;
    }
    public double getHighestYield(int product, int facility,int distribution){
        double highestYield = Double.NEGATIVE_INFINITY;
        for(int s = 1; s <= getnScenarios(product,distribution); s++){
            if(getYieldRealization(product,distribution,facility,s) > highestYield){
                highestYield = getYieldRealization(product,distribution,facility,s);
            }
        }
        return highestYield;
    }

    public double getHighestYield(int facility){
        double highestYield = Double.NEGATIVE_INFINITY;
        for(int p = 1; p <= getnProducts(); p++) {
            for(int d = 1; d <= getnDistributions(p); d++) {
                for (int s = 1; s <= getnScenarios(p, d); s++) {
                    if (getYieldRealization(p, d, facility, s) > highestYield) {
                        highestYield = getYieldRealization(p, d, facility, s);
                    }
                }
            }
        }
        return highestYield;
    }

    public double getHighestSellingPrice(){
        double highestSellingPrice = Double.NEGATIVE_INFINITY;
        for(int p = 1; p <= getnProducts(); p++) {
            if(getSalesPrice(p) > highestSellingPrice){
                highestSellingPrice = getSalesPrice(p);
            }
        }
        return highestSellingPrice;
    }
    public double getExpectedYield(int product, int facility,int distribution){
        double expectedYield = 0;
        for(int s = 1; s <= getnScenarios(product,distribution); s++){
            expectedYield = expectedYield + getProbability(product,distribution,s) * getYieldRealization(product,distribution,facility,s);
        }
        return expectedYield;
    }
    public double getHighestExpectedYield(int product,int facility){
        double highestExpectedYield = Double.NEGATIVE_INFINITY;
        for(int d = 1; d <= getnDistributions(product); d++){
            if(getExpectedYield(product,facility,d) > highestExpectedYield){
                highestExpectedYield = getExpectedYield(product,facility,d);
            }
        }
        return highestExpectedYield;
    }
    public double getHighestDemand(int product){
        double highestDemand = Double.NEGATIVE_INFINITY;
        for(int d = 1; d <= getnDistributions(product); d++){
            for(int s = 1; s <= getnScenarios(product,d); s++){
                if(getDemandRealization(product,d,s) > highestDemand){
                    highestDemand = getDemandRealization(product,d,s);
                }
            }
        }
        return highestDemand;
    }
    public double getHighestDemand(int product, int distribution){
        double highestDemand = Double.NEGATIVE_INFINITY;

        for(int s = 1; s <= getnScenarios(product,distribution); s++){
            if(getDemandRealization(product,distribution,s) > highestDemand){
                highestDemand = getDemandRealization(product,distribution,s);
            }
        }
        return highestDemand;
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
     abstract double computeUpperBoundOnProductionRealization(int product, int distribution, int scenario);
    /**
     * Returns an upper bound on the total production realized for a given product, distribution and scenario.
     * @param product
     * @param distribution
     * @param scenario
     * @return
     */
    public double getUpperBoundOnProductionRealization(int product, int distribution, int scenario){
        return upperBoundOnProductionRealization[product-1][distribution-1][scenario-1];
    }
    /**
     * Computes an upper bound on the total amount of sales.
     * This is an upper bound on the w variable in the model.
     * @param product
     * @param distribution
     * @param scenario
     * @return
     */
    public double getUpperBoundOnSales(int product, int distribution, int scenario){
        return Math.min(getDemandRealization(product,distribution,scenario),getUpperBoundOnProductionRealization(product,distribution,scenario));
    }
    /**
     * Calculates an upper bound on the total oversupply.
     * This is an upper bound on the o variable in the model.
     * @param product
     * @param distribution
     * @param scenario
     * @return
     */
    public double getUpperBoundOnOversupply(int product, int distribution, int scenario){
        return getUpperBoundOnProductionRealization(product,distribution,scenario);
    }
    /**
     * Computes an upper bound on the phi_p variable for the single-cut master problem with disposal costs.
     * The bound is given by the highest expectation among the possible distributions for the given product.
     * @param product
     * @return
     */
    abstract double computeUpperBoundProductExpectation(int product);
    public double getUpperBoundProductExpectation(int product){
        return upperBoundOnProductExpectation[product-1];
    }
    public Cover getMinimumCover(int facility){
        Cover c = null;
        // Creates a graph
        DirectedWeightedMultigraph<Integer, ExtendedEdge> g = new DirectedWeightedMultigraph<>(ExtendedEdge.class);
        // Add a dummy final node p+1
        for(int p = 1; p <= nProducts+1; p++){
            g.addVertex(p);
        }
        // Add arcs also from the last p to the dummy node
        int nEdges = 0;
        for(int p = 1; p <= nProducts; p++){
            for(int l = 1; l <= getnProductionLevels(facility,p); l++) {
                ExtendedEdge e = new ExtendedEdge();
                e.setLevel(l);
                e.setProduct(p);
                g.addEdge(p, p+1,e);
                g.setEdgeWeight(e, getProductionLevelLowerBound(facility,p,l));
                nEdges++;
            }
        }

        boolean found = false;
        while(!found && (nEdges>=nProducts)) {
            DijkstraShortestPath<Integer, ExtendedEdge> dijkstra = new DijkstraShortestPath<>(g);
            GraphPath<Integer, ExtendedEdge> path = dijkstra.getPath(1, nProducts + 1);
            double pathWeight = dijkstra.getPathWeight(1, nProducts + 1);
            System.out.println("Path weight " + pathWeight);
            if (pathWeight < getTotalCapacity(facility)) {
                ExtendedEdge shortestEdge = null;
                double smallestWeight = Double.POSITIVE_INFINITY;
                for (ExtendedEdge e : path.getEdgeList()) {
                    //System.out.println("Edge from " + e.getProduct() + " weight " + getProductionLevelLowerBound(facility, e.getProduct(), e.getLevel()));
                    double weight = getProductionLevelLowerBound(facility, e.getProduct(), e.getLevel());
                    if (weight < smallestWeight) {
                        smallestWeight = weight;
                        shortestEdge = e;
                    }
                }
                //System.out.println("Removing shortest edge from " + shortestEdge.getProduct() +" level "+ shortestEdge.getLevel() + " weight " + getProductionLevelLowerBound(facility, shortestEdge.getProduct(), shortestEdge.getLevel()));
                g.removeEdge(shortestEdge);
                nEdges--;
            }else{
                System.out.println("Total capacity "+getTotalCapacity(facility));
                found = true;
                if(pathWeight < Double.POSITIVE_INFINITY) {
                    HashMap<Integer, Integer> levels = new HashMap<>();
                    for (ExtendedEdge e : path.getEdgeList()) {
                        int product = e.getProduct();
                        int level = e.getLevel();
                        if (getProductionLevelLowerBound(facility, product, e.getLevel()) > 0) {
                            levels.put(product, level);
                        }
                    }
                    c = new Cover(levels, pathWeight);
                }
            }
        }

        return c;
    }
    /**
     * ==========================================
     * Prints a compact summary of the instance.
     * ==========================================
     */
    public void printSummary(){
        System.out.println("Instance of the Production Planning Problem");
        System.out.println("nFacilities "+nFacilities);
        System.out.println("nProducts "+nProducts);
        System.out.println("Leftover Costs and sales prices");
        for(int p = 1; p <= nProducts; p++){
            System.out.println("Product "+(p-1)+" leftover cost "+leftoverCosts[p-1]+" sales price "+salesPrices[p-1]);
        }
        System.out.println("Manufacturing costs & Production levels");
        for(int f = 1; f <= nFacilities; f++) {
            for (int p = 1; p <= nProducts; p++) {
                System.out.println("Facility "+(f-1)+" product "+(p-1)+" man cost "+manufacturingCosts[f-1][p-1]+ " # production levels "+nProductionLevels[f-1][p-1]);
            }
        }
        System.out.println("Production level bounds");
        for(int f = 1; f <= nFacilities; f++) {
            for (int p = 1; p <= nProducts; p++) {
                for(int pl = 1; pl <= nProductionLevels[f-1][p-1]; pl++) {
                    System.out.println("Facility " + (f - 1) + " product " + (p - 1) + " level " + (pl-1) +" ["+productionLevelLowerBounds[f - 1][p - 1][pl-1]+","+productionLevelUpperBounds[f-1][p-1][pl-1]+"]");
                }
            }
        }
        System.out.println("Total capacities");
        for(int f = 1; f <= nFacilities; f++) {
            System.out.println("Facility " + (f - 1) +" "+totalCapacities[f - 1]);
        }

        System.out.println("Distributions");
        for(int p = 1; p <= nProducts; p++){
            System.out.println("Product "+p);
            for(int d = 1; d <= nDistributions[p-1]; d++){
                System.out.println("Distribution "+d+" name "+ distributionNames[p-1][d-1]+" nScenarios "+nScenarios[p-1][d-1]);
                System.out.println("Production level requirements ");
                for(int f= 1; f <= nFacilities; f++){
                    System.out.print(" F"+f+" "+distributionProductionLevels[p-1][d-1][f-1]);
                }
                System.out.println();
            }
        }
        System.out.println("Distribution scenarios");
        for(int p = 1; p <= nProducts; p++){
            System.out.println("Product "+p);
            for(int d = 1; d <= nDistributions[p-1]; d++){
                System.out.println("Distribution "+d+" name "+distributionNames[p-1][d-1]);
                for(int s = 1; s <= nScenarios[p-1][d-1]; s++) {
                    System.out.println("Scenario "+s+ " probability "+probabilities[p-1][d-1][s-1]+" demand realization "+demandRealization[p-1][d-1][s-1]);
                    System.out.println("Yield realizations");
                    for (int f = 1; f <= nFacilities; f++) {
                        System.out.print("F" + f + " " + yieldRealization[p - 1][d - 1][f - 1][s-1]+ " ");
                    }
                    System.out.println();
                }
            }
        }
    }
    private class ExtendedEdge extends DefaultWeightedEdge{
        private int product;
        private int level;

        public void setLevel(int level) {
            this.level = level;
        }

        public void setProduct(int product) {
            this.product = product;
        }

        public int getProduct() {
            return product;
        }

        public int getLevel() {
            return level;
        }
    }
}
