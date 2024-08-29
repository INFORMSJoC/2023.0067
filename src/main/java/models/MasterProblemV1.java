package models;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import problems.ProductionPlanningProblemWithSalvageRevenue;
import utils.Configuration;
import utils.TestableMasterProblemWSR;

public class MasterProblemV1 extends TestableMasterProblemWSR {

    // This class inherits the model, the y and the phi variables from the parent class.
    // It remains to specify the x variables.


    /**
     * Constructs an instance of the model for the Production Planning Problem
     * @param pp an instance of the production planning problem
     * @throws IloException
     */
    public MasterProblemV1(ProductionPlanningProblemWithSalvageRevenue pp) throws IloException {

        // Initializes the final fields of the parent class.
        super(pp, "bdscV1");

        // ========================================
        // Creates the objective function
        IloLinearNumExpr objective = model.linearNumExpr();
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                objective.addTerm(-pp.getManufacturingCosts(f,p), x[p-1][f-1]);
            }
            objective.addTerm(1,phi[p-1]);
        }
        model.addMaximize(objective);

        // ======================================
        // Creates the constraints
        // (b.1) Production quantities lower bounds
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                IloLinearNumExpr lhs = model.linearNumExpr();
                for(int l = 1; l <= pp.getnProductionLevels(f,p); l++){
                   lhs.addTerm(pp.getProductionLevelLowerBound(f,p,l),y[p-1][f-1][l-1]);
                }
                lhs.addTerm(-1,x[p - 1][f - 1]);
                model.addLe(lhs, 0);
            }
        }
        // (b.2) Production quantities upper bounds
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                IloLinearNumExpr rhs = model.linearNumExpr();
                for(int l = 1; l <= pp.getnProductionLevels(f,p); l++){
                    rhs.addTerm(Math.min(pp.getProductionLevelUpperBound(f,p,l),pp.getTotalCapacity(f)),y[p-1][f-1][l-1]);
                }
                model.addLe(x[p - 1][f - 1], rhs);
            }
        }
        // (b.3) Total production bound
        for(int f = 1; f <= pp.getnFacilities(); f++){
            IloLinearNumExpr lhs = model.linearNumExpr();
            for (int p = 1; p <= pp.getnProducts(); p++) {
                lhs.addTerm(1, x[p - 1][f - 1]);
            }
            model.addLe(lhs, pp.getTotalCapacity(f));
        }
        // (c) Choice of one production level
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                IloLinearNumExpr lhs = model.linearNumExpr();
                for(int l = 1; l <= pp.getnProductionLevels(f,p); l++){
                    lhs.addTerm(1,y[p-1][f-1][l-1]);
                }
                model.addEq(lhs, 1);
            }
        }

        // Bound on Phi
        for (int p = 1; p <= pp.getnProducts(); p++){
            model.addLe(phi[p-1], pp.getUpperBoundProductExpectation(p));
        }

    }


    public void addValidInequality1() throws IloException {
        for (int p = 1; p <= pp.getnProducts(); p++){
            IloLinearNumExpr lhs = model.linearNumExpr();
            lhs.addTerm(1,phi[p-1]);
            for(int f = 1; f <= pp.getnFacilities(); f++){
                lhs.addTerm(-pp.getSalesPrice(p) * pp.getHighestYield(p,f),x[p-1][f-1]);
            }
            model.addLe(lhs, 0);
        }
        super.experiment_name = super.experiment_name+"+VI1";
    }
    public void addValidInequality2() throws IloException {
        for (int p = 1; p <= pp.getnProducts(); p++){
            double rhs = (pp.getSalesPrice(p)-pp.getLeftoverCosts(p))*pp.getHighestDemand(p);

            IloLinearNumExpr lhs = model.linearNumExpr();
            lhs.addTerm(1,phi[p-1]);
            for(int f = 1; f <= pp.getnFacilities(); f++){
                lhs.addTerm(-pp.getLeftoverCosts(p) * pp.getHighestYield(p,f),x[p-1][f-1]);
            }
            model.addLe(lhs, rhs);
        }
        super.experiment_name = super.experiment_name+"+VI2";
    }
    public void addValidInequality3() throws IloException {
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++) {
                IloLinearNumExpr lhs = model.linearNumExpr();
                lhs.addTerm(1, phi[p - 1]);
                for (int f = 1; f <= pp.getnFacilities(); f++) {
                    lhs.addTerm(-pp.getSalesPrice(p) * pp.getHighestYield(p, f, d), x[p - 1][f - 1]);
                }
                for(int f = 1; f <= pp.getnFacilities(); f++) {
                    lhs.addTerm( pp.getUpperBoundProductExpectation(p), y[p - 1][f - 1][pp.getDistributionProductionLevels(p,d,f)]);
                }
                model.addLe(lhs, pp.getUpperBoundProductExpectation(p) * pp.getnFacilities());
            }
        }
        super.experiment_name = super.experiment_name+"+VI3";
    }
    public void addValidInequality4() throws IloException {
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++) {
                double rhs = (pp.getSalesPrice(p) - pp.getLeftoverCosts(p)) * pp.getHighestDemand(p,d);
                rhs = rhs + pp.getUpperBoundProductExpectation(p) * pp.getnFacilities();

                IloLinearNumExpr lhs = model.linearNumExpr();
                lhs.addTerm(1, phi[p - 1]);
                for (int f = 1; f <= pp.getnFacilities(); f++) {
                    lhs.addTerm(-pp.getLeftoverCosts(p) * pp.getHighestYield(p, f,d), x[p - 1][f - 1]);
                }
                for(int f = 1; f <= pp.getnFacilities(); f++) {
                    lhs.addTerm( pp.getUpperBoundProductExpectation(p), y[p - 1][f - 1][pp.getDistributionProductionLevels(p,d,f)]);
                }
                model.addLe(lhs, rhs);
            }
        }
        super.experiment_name = super.experiment_name+"+VI4";
    }
    public void addValidInequality5() throws IloException {
        IloLinearNumExpr lhs = model.linearNumExpr();
        for (int p = 1; p <= pp.getnProducts(); p++){
            lhs.addTerm(1, phi[p - 1]);
        }
        double rhs = 0;
        for (int f = 1; f <= pp.getnFacilities(); f++) {
            rhs = rhs + pp.getHighestSellingPrice() * pp.getHighestYield(f) * pp.getTotalCapacity(f);
        }

        model.addLe(lhs, rhs);

        super.experiment_name = super.experiment_name+"+VI5";
    }
    public void addValidInequality6() throws IloException {
        for (int p = 1; p <= pp.getnProducts(); p++){
            IloLinearNumExpr lhs = model.linearNumExpr();
            lhs.addTerm(1,phi[p-1]);
            for(int f = 1; f <= pp.getnFacilities(); f++){
                lhs.addTerm(-pp.getSalesPrice(p) * pp.getHighestExpectedYield(p,f),x[p-1][f-1]);
            }
            model.addLe(lhs, 0);
        }
        super.experiment_name = super.experiment_name+"+VI6";
    }
    @Override
    public void configure(Configuration config) throws IloException {
        model.setParam(IloCplex.Param.TimeLimit, config.getTimeLimit());
        model.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, config.getTargetGap());
        model.setParam(IloCplex.Param.MIP.Strategy.LBHeur,config.useLocalBranching());

        model.use(new CutsCallback());
        model.use(new ProgressCallback(config));
    }
    private class CutsCallback extends IloCplex.LazyConstraintCallback {
        public CutsCallback(){

        }

        @Override
        protected void main() throws IloException {
            // Stores the LP root relaxation time
            if(getNodeId().toString().equalsIgnoreCase("Node0") && rootNodeRelaxationTime < 0){
                rootNodeRelaxationTime = (System.nanoTime() - start)/1e9;
                rootNodeLPbound = getObjValue();
            }

            nCallbackCalls++;
            System.out.println("Callback call #"+nCallbackCalls+ ". # cuts added so far "+nCuts);
            long cbStart = System.nanoTime();

            double X[][] = getX();
            double Y[][][] = getY();


            for (int p = 1; p <= pp.getnProducts(); p++) {
                // First, we identify the distribution enforced on the product
                for (int d = 1; d <= pp.getnDistributions(p); d++) {
                    boolean enforced = true;
                    for(int f = 1; f <= pp.getnFacilities(); f++){
                        int level = pp.getDistributionProductionLevels(p,d,f);
                        if(Y[p-1][f-1][level] < 0.5){
                            enforced = false;
                            break;
                        }
                    }
                    if(enforced) {
                        // If the distribution is enforced we proceed to generate an optimality cut
                        //System.out.println("Product "+p+" distribution enforced "+d);

                        // We start creating the cut, in case late we need it
                        IloLinearNumExpr firstCutLHS = model.linearNumExpr();
                        firstCutLHS.addTerm(1, phi[p - 1]);
                        double firstCutRHS = 0;
                        // First we compute the expected profit for this distribution
                        // to make the optimality test
                        double expectedProfit = 0;

                        for (int s = 1; s <= pp.getnScenarios(p, d); s++) {
                            OptimalitySubproblemV1 optimalitySubproblem = new OptimalitySubproblemV1(p, d, s, X, pp);
                            double objectiveValue = optimalitySubproblem.solve();
                            expectedProfit = expectedProfit + (pp.getProbability(p,d,s) * objectiveValue);

                            // We add terms to the cut
                            if(pp.getDemandRealization(p,d,s) <= optimalitySubproblem.getTotalProduction()){
                                firstCutRHS = firstCutRHS
                                        + (pp.getProbability(p,d,s) * (pp.getSalesPrice(p) - pp.getLeftoverCosts(p)) * pp.getDemandRealization(p,d,s));
                                for(int f = 1; f <= pp.getnFacilities(); f++) {
                                    firstCutLHS.addTerm(-pp.getProbability(p, d, s) * pp.getLeftoverCosts(p) * pp.getYieldRealization(p,d,f,s),x[p-1][f-1]);
                                }
                            }else{
                                for(int f = 1; f <= pp.getnFacilities(); f++) {
                                    firstCutLHS.addTerm(-pp.getProbability(p, d, s) * pp.getSalesPrice(p) * pp.getYieldRealization(p,d,f,s),x[p-1][f-1]);
                                }
                            }
                        }
                        firstCutRHS = firstCutRHS +  pp.getUpperBoundProductExpectation(p) * pp.getnFacilities();
                        for(int f = 1; f <= pp.getnFacilities(); f++) {
                            firstCutLHS.addTerm( pp.getUpperBoundProductExpectation(p), y[p - 1][f - 1][pp.getDistributionProductionLevels(p,d,f)]);
                        }

                        if (getPhi(p) <= expectedProfit + 1e-9) {
                            //System.out.println("Node optimal.");
                        }else{
                            IloRange firstCut = add(model.le(firstCutLHS, firstCutRHS));
                            nCuts++;
                        }
                    }
                }
            }
            System.out.println("# cuts after cut loop "+nCuts);
            long cbEnd = System.nanoTime();
            totalCallbackTime = totalCallbackTime + (cbEnd-cbStart)/1e9;
        }

        private double[][] getX() throws IloException {
            double X[][] = new double[pp.getnProducts()][pp.getnFacilities()];
            for (int p = 1; p <= pp.getnProducts(); p++){
                for(int f = 1; f <= pp.getnFacilities(); f++){
                    X[p-1][f-1] = getValue(x[p-1][f-1]);
                }
            }
            return X;
        }

        private double[][][] getY() throws IloException {
            double Y[][][] = new double[pp.getnProducts()][pp.getnFacilities()][pp.getMaxnProductionLevels()];
            for (int p = 1; p <= pp.getnProducts(); p++){
                for(int f = 1; f <= pp.getnFacilities(); f++){
                    for(int l = 1; l <= pp.getnProductionLevels(f,p); l++){
                        Y[p-1][f-1][l-1] = getValue(y[p-1][f-1][l-1]);
                    }
                }
            }
            return Y;
        }

        private double getPhi(int p) throws IloException {
            return getValue(phi[p-1]);
        }


    }
}
