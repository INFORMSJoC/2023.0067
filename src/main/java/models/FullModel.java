package models;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import problems.ProductionPlanningProblemWithSalvageRevenue;
import utils.Configuration;
import utils.Testable;

public class FullModel extends Testable {
    private final ProductionPlanningProblemWithSalvageRevenue pp;
    private final IloCplex model;
    private final IloIntVar y[][][];
    private final IloNumVar x[][];
    private final IloIntVar delta[][];
    private final IloNumVar w[][][];
    private final IloNumVar o[][][];
    private final IloNumVar mu[][][];
    private final IloNumVar rho[][][];
    /**
     * Constructs an instance of the model for the Production Planning Problem
     * @param pp an instance of the production planning problem
     * @throws IloException
     */
    public FullModel(ProductionPlanningProblemWithSalvageRevenue pp) throws IloException {
        // Initializes the super class Testable which needs
        // a name for the experiment.
        super(pp, "full");
        this.pp = pp;
        model = new IloCplex();

        // Creates the decision variables
        this.y = new IloIntVar[pp.getnProducts()][pp.getnFacilities()][pp.getMaxnProductionLevels()];
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                for(int l = 1; l <= pp.getnProductionLevels(f,p); l++){
                    y[p-1][f-1][l-1] = model.boolVar("y_"+p+"_"+f+"_"+l);
                }
            }
        }
        this.x = new IloNumVar[pp.getnProducts()][pp.getnFacilities()];
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                x[p-1][f-1] = model.numVar(0,Double.POSITIVE_INFINITY,"x_"+p+"_"+f);
            }
        }
        this.delta = new IloIntVar[pp.getnProducts()][pp.getMaxNDistributions()];
        this.w = new IloNumVar[pp.getnProducts()][pp.getMaxNDistributions()][pp.getMaxNScenarios()];
        this.o = new IloNumVar[pp.getnProducts()][pp.getMaxNDistributions()][pp.getMaxNScenarios()];
        this.mu = new IloNumVar[pp.getnProducts()][pp.getMaxNDistributions()][pp.getMaxNScenarios()];
        this.rho = new IloNumVar[pp.getnProducts()][pp.getMaxNDistributions()][pp.getMaxNScenarios()];
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                delta[p-1][d-1] = model.boolVar("del_"+p+"_"+d);
                for(int s = 1; s <= pp.getnScenarios(p,d); s++){
                    w[p-1][d-1][s-1] = model.numVar(0,Double.POSITIVE_INFINITY,"w_"+p+"_"+d+"_"+s);
                    o[p-1][d-1][s-1] = model.numVar(0,Double.POSITIVE_INFINITY,"o_"+p+"_"+d+"_"+s);
                    mu[p-1][d-1][s-1] = model.numVar(0,Double.POSITIVE_INFINITY,"mu_"+p+"_"+d+"_"+s);
                    rho[p-1][d-1][s-1] = model.numVar(0,Double.POSITIVE_INFINITY,"rho_"+p+"_"+d+"_"+s);
                }
            }
        }
        // ========================================
        // Creates the objective function
        IloLinearNumExpr objective = model.linearNumExpr();
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                objective.addTerm(-pp.getManufacturingCosts(f,p), x[p-1][f-1]);
            }
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                for(int s = 1; s <= pp.getnScenarios(p,d); s++){
                    objective.addTerm(pp.getProbability(p,d,s)*pp.getSalesPrice(p),mu[p-1][d-1][s-1]);
                    objective.addTerm(pp.getProbability(p,d,s)*pp.getLeftoverCosts(p),rho[p-1][d-1][s-1]);
                }
            }
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
                    rhs.addTerm(pp.getProductionLevelUpperBound(f,p,l),y[p-1][f-1][l-1]);
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
        // (d) Enforcement of a distribution
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                IloLinearNumExpr lhs = model.linearNumExpr();
                for(int f = 1; f <= pp.getnFacilities(); f++){
                    int level = pp.getDistributionProductionLevels(p,d,f);
                    lhs.addTerm(1,y[p-1][f-1][level]);
                }
                lhs.addTerm(-pp.getnFacilities(),delta[p - 1][d - 1]);
                model.addGe(lhs,0);
            }
        }

        // (e) For each product exactly one distribution
        for (int p = 1; p <= pp.getnProducts(); p++){
            IloLinearNumExpr lhs = model.linearNumExpr();
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                lhs.addTerm(1,delta[p-1][d-1]);
            }
            model.addEq(lhs, 1);
        }

        // (f) Production quantities
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                for(int s = 1; s <= pp.getnScenarios(p,d);s++){
                    IloLinearNumExpr rhs = model.linearNumExpr();
                    for(int f = 1; f <= pp.getnFacilities(); f++){
                        rhs.addTerm(pp.getYieldRealization(p,d,f,s) ,x[p-1][f-1]);
                    }
                    IloLinearNumExpr lhs = model.linearNumExpr();
                    lhs.addTerm(1,w[p-1][d-1][s-1]);
                    lhs.addTerm(1,o[p-1][d-1][s-1]);
                    model.addEq(lhs, rhs);
                }
            }
        }
        // (g) Demand satisfaction
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                for(int s = 1; s <= pp.getnScenarios(p,d);s++){
                    model.addLe(w[p - 1][d - 1][s - 1], pp.getDemandRealization(p, d, s));
                }
            }
        }
        // Linearization constraints
        // (h) mu and w
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                for(int s = 1; s <= pp.getnScenarios(p,d);s++){
                    model.addLe(mu[p - 1][d - 1][s - 1], w[p - 1][d - 1][s - 1]);
                }
            }
        }
        // (i) mu and delta
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                for(int s = 1; s <= pp.getnScenarios(p,d);s++){
                    IloLinearNumExpr rhs = model.linearNumExpr();
                    rhs.addTerm(pp.getUpperBoundOnSales(p,d,s) , delta[p-1][d-1]);
                    model.addLe(mu[p-1][d-1][s-1],rhs);
                }
            }
        }
        // (j) mu, w and delta
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                for(int s = 1; s <= pp.getnScenarios(p,d);s++){
                    IloLinearNumExpr lhs = model.linearNumExpr();
                    lhs.addTerm(1 , mu[p-1][d-1][s-1]);
                    lhs.addTerm(-1 , w[p-1][d-1][s-1]);
                    lhs.addTerm(-pp.getUpperBoundOnSales(p,d,s) , delta[p-1][d-1]);
                    model.addGe(lhs, -pp.getUpperBoundOnSales(p,d,s) );
                }
            }
        }
        // (k) rho and o
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                for(int s = 1; s <= pp.getnScenarios(p,d);s++){
                    model.addLe(rho[p-1][d-1][s-1],o[p-1][d-1][s-1]);
                }
            }
        }
        // (l) rho and delta
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                for(int s = 1; s <= pp.getnScenarios(p,d);s++){
                    IloLinearNumExpr rhs = model.linearNumExpr();
                    rhs.addTerm(pp.getUpperBoundOnOversupply(p,d,s) , delta[p-1][d-1]);
                    model.addLe(rho[p-1][d-1][s-1], rhs );
                }
            }
        }
        // (m) rho, o and delta
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                for(int s = 1; s <= pp.getnScenarios(p,d);s++){
                    IloLinearNumExpr lhs = model.linearNumExpr();
                    lhs.addTerm(1,rho[p - 1][d - 1][s - 1]);
                    lhs.addTerm(-1,o[p - 1][d - 1][s - 1]);
                    lhs.addTerm(-pp.getUpperBoundOnOversupply(p,d,s) , delta[p-1][d-1]);
                    model.addGe(lhs, -pp.getUpperBoundOnOversupply(p,d,s));
                }
            }
        }

    }

    public void solve(Configuration config) throws IloException {
        model.setParam(IloCplex.Param.TimeLimit, config.getTimeLimit());
        model.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, config.getTargetGap());
        model.setParam(IloCplex.Param.MIP.Strategy.LBHeur,config.useLocalBranching());

        model.use(new ProgressCallback(config));
        start = System.nanoTime();
        boolean hasSolution = model.solve();
        end = System.nanoTime();
        if(hasSolution) {
            gap = model.getMIPRelativeGap();
            bestInteger = model.getObjValue();
        }
        bestBound = model.getBestObjValue();
        nNodesExplored = model.getNnodes();
    }

    public void printSolution() throws IloException {

        System.out.println("Production level decisions");
        for (int p = 1; p <= pp.getnProducts(); p++) {
            System.out.println("Product " + p);
            for (int f = 1; f <= pp.getnFacilities(); f++) {
                System.out.println("Facility " + f);
                for (int l = 1; l <= pp.getnProductionLevels(f, p); l++) {
                    System.out.print(" L" + l + "=" + model.getValue(y[p - 1][f - 1][l - 1]));
                }
                System.out.println();
                System.out.println("Quantity " + model.getValue(x[p - 1][f - 1]));
            }
        }
        System.out.println("Distributions ");
        for (int p = 1; p <= pp.getnProducts(); p++){
            System.out.println("Product "+p);
            for(int d = 1; d <= pp.getnDistributions(p); d++){
                System.out.println(" D"+d+"="+model.getValue(delta[p-1][d-1]));
                /*
                for(int s = 1; s <= pp.getnScenarios(p,d); s++){
                    System.out.println("Scenario "+s+" w:"+ model.getValue(w[p-1][d-1][s-1])+" o: "+ model.getValue(o[p-1][d-1][s-1])+" mu: "+ model.getValue(mu[p-1][d-1][s-1])+" rho: "+ model.getValue(rho[p-1][d-1][s-1]));
                }
                */
            }
        }
        System.out.println("Objective value : "+model.getObjValue());

    }
    @Override
    public double[][] getFirstStageXSolution() throws IloException {

        double X[][] = new double[pp.getnProducts()][pp.getnFacilities()];
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                X[p-1][f-1] = model.getValue(x[p-1][f-1]);
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
        double over[][] = new double[pp.getnProducts()][pp.getMaxNScenarios()];
        for(int p = 1; p <= pp.getnProducts(); p++) {
            for (int d = 1; d <= pp.getnDistributions(p); d++) {
                for (int f = 1; f <= pp.getnFacilities(); f++) {
                    if (model.getValue(delta[p-1][d-1]) > 0.5) {
                        for (int s = 1; s <= pp.getnScenarios(p, d); s++) {
                            over[p-1][s-1] = model.getValue(o[p-1][d-1][s-1]);
                        }
                    }
                }
            }
        }
        return over;

    }

    public double[][] getSecondStageSales() throws IloException{
        double sales[][] = new double[pp.getnProducts()][pp.getMaxNScenarios()];
        for(int p = 1; p <= pp.getnProducts(); p++) {
            for (int d = 1; d <= pp.getnDistributions(p); d++) {
                for (int f = 1; f <= pp.getnFacilities(); f++) {
                    if (model.getValue(delta[p-1][d-1]) > 0.5) {
                        for (int s = 1; s <= pp.getnScenarios(p, d); s++) {
                            sales[p-1][s-1] = model.getValue(w[p-1][d-1][s-1]);
                        }
                    }
                }
            }
        }
        return sales;
    }
}
