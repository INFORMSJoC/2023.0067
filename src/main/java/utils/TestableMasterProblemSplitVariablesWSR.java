package utils;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import models.OptimalitySubproblemV1;
import problems.Cover;
import problems.ProductionPlanningProblemWithSalvageRevenue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class TestableMasterProblemSplitVariablesWSR extends Testable<ProductionPlanningProblemWithSalvageRevenue>{
    protected final IloCplex model;
    protected final IloIntVar y[][][];
    protected final IloNumVar x[][][];
    protected final IloNumVar phi[];

    public TestableMasterProblemSplitVariablesWSR(ProductionPlanningProblemWithSalvageRevenue pp, String experimentName) throws IloException {
        super(pp,experimentName);

        model = new IloCplex();

        // Creates the y decision variables
        this.y = new IloIntVar[pp.getnProducts()][pp.getnFacilities()][pp.getMaxnProductionLevels()];
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                for(int l = 1; l <= pp.getnProductionLevels(f,p); l++){
                    y[p-1][f-1][l-1] = model.boolVar("y_"+p+"_"+f+"_"+l);
                }
            }
        }
        // Creates the x decision variables
        this.x = new IloNumVar[pp.getnProducts()][pp.getnFacilities()][pp.getMaxnProductionLevels()];
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                for(int l = 1; l <= pp.getnProductionLevels(f,p); l++) {
                    x[p - 1][f - 1][l-1] = model.numVar(0, Double.POSITIVE_INFINITY, "x_" + p + "_" + f+"_"+l);
                }
            }
        }
        // Creates the phi decision variables
        this.phi = new IloNumVar[pp.getnProducts()];
        for (int p = 1; p <= pp.getnProducts(); p++){
            this.phi[p-1] = model.numVar(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,"phi_"+p);
        }
    }

    public abstract void addValidInequality1() throws IloException;
    public abstract void addValidInequality2() throws IloException;
    public abstract void addValidInequality3() throws IloException;
    public abstract void addValidInequality4() throws IloException;
    public abstract void addValidInequality5() throws IloException;
    public abstract void addValidInequality6() throws IloException;
    /**
     * Generates and adds to the model all minimal cover inequalities.
     * @throws IloException
     */
    public void addMinimalCoversInequalities() throws IloException {
        for(int f = 1; f <= pp.getnFacilities(); f++){
            Cover c = pp.getMinimumCover(f);
            int nCoversAdded = 0;
            if(!(c == null)){
                System.out.println("Cover size "+ c.getProducts().size());
                Set<Integer> coverProducts = c.getProducts();
                IloLinearNumExpr lhs = model.linearNumExpr();

                for (int p: coverProducts){
                    lhs.addTerm(1, y[p-1][f-1][c.getLevel(p)-1]);
                }
                model.addLe(lhs,coverProducts.size()-1);
                nCoversAdded++;

                // Generates alternative minimal covers
                // by replacing each product with an alternative one
                for (int p: coverProducts){
                    System.out.println("Looking for alternatives to "+p);
                    List<Integer> alternativeProducts = new ArrayList<Integer>();
                    for(int pr = 1; pr <= pp.getnProducts(); pr++) {
                        if(!coverProducts.contains(pr)) {
                            System.out.println("Trying with "+pr);
                            // Gets the cover weight
                            System.out.println("cover weight "+c.getTotalWeight());
                            double totalWeight = c.getTotalWeight();
                            // Subtracts the weight of p
                            totalWeight= totalWeight - pp.getProductionLevelLowerBound(f,p,c.getLevel(p));
                            // Adds the weight of pr at the same level of p
                            totalWeight = totalWeight + pp.getProductionLevelLowerBound(f,pr,c.getLevel(p));
                            System.out.println("new cover weight "+c.getTotalWeight());
                            // Checks if it violates the capacity
                            if(totalWeight > pp.getTotalCapacity(f)){
                                System.out.println("capacity  "+pp.getTotalCapacity(f));
                                alternativeProducts.add(pr);
                            }

                        }
                    }
                    // Creates a minimal cover for each alternative product
                    for(int pr:alternativeProducts){
                        IloLinearNumExpr alternativeMClhs = model.linearNumExpr();
                        // Adds the term for the alternative product to p
                        alternativeMClhs.addTerm(1, y[pr - 1][f - 1][c.getLevel(p) - 1]);
                        for(int prod: coverProducts){
                            // Now adds terms for all other products in the cover, except for p
                            if(prod != p) {
                                alternativeMClhs.addTerm(1, y[prod - 1][f - 1][c.getLevel(prod) - 1]);
                            }
                        }
                        // Adds the constraint
                        model.addLe(alternativeMClhs, coverProducts.size() - 1);
                        nCoversAdded++;
                    }

                }

            }
            System.out.println("Added "+nCoversAdded+" minimal covers for facility "+f);
        }
        super.experiment_name = super.experiment_name+"+MCI";

    }
    /**
     * Generates and adds to the model extended cover inequalities.
     * It uses the same procedure of the method for the minimal covers,
     * but with the difference that (i) minimal covers are not added
     * (ii) for each minimal covers adds an extended cover.
     * @throws IloException
     */
    public void addExtendedCoverInequalities() throws IloException {

        for(int f = 1; f <= pp.getnFacilities(); f++){
            // Finds the first available minimal cover.
            Cover c = pp.getMinimumCover(f);
            int nCoversAdded = 0;
            if(!(c == null)){
                System.out.println("Cover size "+ c.getProducts().size());
                Set<Integer> coverProducts = c.getProducts();

                // From the minimal cover generates an extended cover
                IloLinearNumExpr lhs = model.linearNumExpr();
                for (int p = 1; p <= pp.getnProducts(); p++){
                    // A term is added for each product of the cover
                    // at their respective production levels.
                    if(coverProducts.contains(p)) {
                        lhs.addTerm(1, y[p - 1][f - 1][c.getLevel(p) - 1]);
                    }else{
                        // Then it adds all other products at all production levels.
                        for(int l = 1; l <= pp.getnProductionLevels(f,p); l++){
                            lhs.addTerm(1, y[p - 1][f - 1][l - 1]);
                        }
                    }
                }
                model.addLe(lhs,coverProducts.size()-1);
                nCoversAdded++;

                // Generates alternative minimal covers
                // by replacing each product with alternative ones
                for (int p: coverProducts){
                    System.out.println("Looking for alternatives to "+p);
                    List<Integer> alternativeProducts = new ArrayList<Integer>();
                    for(int pr = 1; pr <= pp.getnProducts(); pr++) {
                        if(!coverProducts.contains(pr)) {
                            System.out.println("Trying with "+pr);
                            // Gets the cover weight
                            System.out.println("cover weight "+c.getTotalWeight());
                            double totalWeight = c.getTotalWeight();
                            // Subtracts the weight of p
                            totalWeight= totalWeight - pp.getProductionLevelLowerBound(f,p,c.getLevel(p));
                            // Adds the weight of pr at the same level of p
                            totalWeight = totalWeight + pp.getProductionLevelLowerBound(f,pr,c.getLevel(p));
                            System.out.println("new cover weight "+c.getTotalWeight());
                            // Checks if it violates the capacity
                            if(totalWeight > pp.getTotalCapacity(f)){
                                System.out.println("capacity  "+pp.getTotalCapacity(f));
                                alternativeProducts.add(pr);
                            }

                        }
                    }
                    // Creates a minimal cover for each alternative product
                    for(int pr:alternativeProducts){
                        IloLinearNumExpr alternativeMClhs = model.linearNumExpr();
                        // Adds a term for the alternative product
                        // at the same level of the focal product
                        alternativeMClhs.addTerm(1, y[pr - 1][f - 1][c.getLevel(p) - 1]);
                        for(int prod: coverProducts){
                            // Adds one term for each product in the original cover
                            // other than the focal product p
                            if(prod != p) {
                                alternativeMClhs.addTerm(1, y[prod - 1][f - 1][c.getLevel(prod) - 1]);
                            }
                            // Then it adds all other products at all production levels.
                            // The other products are those different from p, from its replacement pr, and from the
                            // other products in the cover.
                            for(int product = 1; product <= pp.getnProducts(); product++){
                                if( (product != p) && (product != pr) && (! coverProducts.contains(product) ) ){
                                    for(int l = 1; l <= pp.getnProductionLevels(f,product); l++){
                                        alternativeMClhs.addTerm(1, y[product - 1][f - 1][l - 1]);
                                    }
                                }
                            }
                            model.addLe(alternativeMClhs, coverProducts.size() - 1);
                            nCoversAdded++;
                        }
                    }

                }

            }
            System.out.println("Added "+nCoversAdded+" extended covers for facility "+f);
        }
        super.experiment_name = super.experiment_name+"+ECI";


    }

    public abstract void configure(Configuration config) throws IloException;

    public boolean solve(Configuration config) throws IloException {
        configure(config);

        start = System.nanoTime();
        boolean hasSolution = model.solve();
        end = System.nanoTime();
        if(hasSolution) {
            gap = model.getMIPRelativeGap();
            bestInteger = model.getObjValue();
        }
        bestBound = model.getBestObjValue();
        nNodesExplored = model.getNnodes();
        System.out.println("Status "+model.getStatus().toString());
        return hasSolution;
    }
    public void printSolution() throws IloException {

        System.out.println("Production level decisions");
        for (int p = 1; p <= pp.getnProducts(); p++) {
            System.out.println("Product " + p);
            for (int f = 1; f <= pp.getnFacilities(); f++) {
                System.out.println("Facility " + f);
                for (int l = 1; l <= pp.getnProductionLevels(f, p); l++) {
                    System.out.print(" L" + l + "=" + model.getValue(y[p - 1][f - 1][l - 1]));

                    System.out.println();
                    System.out.println("Quantity " + model.getValue(x[p - 1][f - 1][l-1]));
                }
            }
        }
        System.out.println("Objective value : "+model.getObjValue());

    }
    @Override
    public double[][] getFirstStageXSolution() throws IloException {

        double X[][] = new double[pp.getnProducts()][pp.getnFacilities()];
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                X[p-1][f-1] = 0;
                for(int l = 1; l <= pp.getnProductionLevels(f,p); l++) {
                    X[p-1][f-1] = X[p-1][f-1] + model.getValue(x[p - 1][f - 1][l-1]);
                }
            }
        }
        return X;
    }
    @Override
    public int[][][] getFirstStageYSolution() throws IloException {

        int Y[][][] = new int[pp.getnProducts()][pp.getnFacilities()][pp.getMaxnProductionLevels()];
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                for(int l = 1; l <= pp.getnProductionLevels(f,p); l++){
                    if(model.getValue(y[p-1][f-1][l-1]) > 0.5) {
                        Y[p - 1][f - 1][l - 1] = 1;
                    }else{
                        Y[p - 1][f - 1][l - 1] = 0;
                    }
                }
            }
        }

        return Y;
    }
    public double[][] getSecondStageOversupply() throws IloException{
        int[][][] Y = getFirstStageYSolution();
        double[][] X = getFirstStageXSolution();
        double o[][] = new double[pp.getnProducts()][pp.getMaxNScenarios()];
        for(int p = 1; p <= pp.getnProducts(); p++) {
            for (int d = 1; d <= pp.getnDistributions(p); d++) {
                boolean enforced = true;
                for (int f = 1; f <= pp.getnFacilities(); f++) {
                    int level = pp.getDistributionProductionLevels(p, d, f);
                    if (Y[p - 1][f - 1][level] < 0.5) {
                        enforced = false;
                        break;
                    }
                }
                if(enforced){
                    for (int s = 1; s <= pp.getnScenarios(p, d); s++) {
                        OptimalitySubproblemV1 osp = new OptimalitySubproblemV1(p, d, s, X, pp);
                        o[p-1][s-1] = osp.getOversupply();
                    }
                }
            }

        }
        return o;

    }
    public double[][] getSecondStageSales() throws IloException{
        int[][][] Y = getFirstStageYSolution();
        double[][] X = getFirstStageXSolution();
        double w[][] = new double[pp.getnProducts()][pp.getMaxNScenarios()];
        for(int p = 1; p <= pp.getnProducts(); p++) {
            for (int d = 1; d <= pp.getnDistributions(p); d++) {
                boolean enforced = true;
                for (int f = 1; f <= pp.getnFacilities(); f++) {
                    int level = pp.getDistributionProductionLevels(p, d, f);
                    if (Y[p - 1][f - 1][level] < 0.5) {
                        enforced = false;
                        break;
                    }
                }
                if(enforced){
                    for (int s = 1; s <= pp.getnScenarios(p, d); s++) {
                        OptimalitySubproblemV1 osp = new OptimalitySubproblemV1(p, d, s, X, pp);
                        w[p-1][s-1] = osp.getSales();
                    }
                }
            }

        }
        return w;
    }

}
