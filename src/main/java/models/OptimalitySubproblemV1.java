package models;

import ilog.concert.IloException;
import problems.ProductionPlanningProblemWithSalvageRevenue;

public class OptimalitySubproblemV1 {
    private final ProductionPlanningProblemWithSalvageRevenue pp;
    private final int p;
    private final int d;
    private final int s;
    private final double X[][];
    private final double totalProduction;
    /**
     * Constructs an instance of the model for the Production Planning Problem
     * @param pp an instance of the production planning problem
     * @throws IloException
     */
    public OptimalitySubproblemV1(int p, int d, int s, double X[][], ProductionPlanningProblemWithSalvageRevenue pp)  {
        this.pp = pp;
        this.p = p;
        this.d = d;
        this.s = s;
        this.X = X;
        this.totalProduction = computeTotalProduction();
    }

    public double solve() {

        double objective = pp.getSalesPrice(p) * totalProduction;
        if(pp.getDemandRealization(p,d,s) <= totalProduction){
            objective = pp.getSalesPrice(p) * pp.getDemandRealization(p,d,s) + pp.getLeftoverCosts(p) * (totalProduction - pp.getDemandRealization(p,d,s));
        }

        return objective;
    }


    private double computeTotalProduction(){
        double total_production = 0;
        for(int f = 1; f <= pp.getnFacilities(); f++){
            total_production = total_production + (pp.getYieldRealization(this.p,this.d,f,this.s) * X[this.p-1][f-1]);
        }
        return total_production;
    }

    public double getTotalProduction(){
        return totalProduction;
    }

    public double getSales(){
        return Math.min(totalProduction,pp.getDemandRealization(p,d,s));
    }

    public double getOversupply(){
        return totalProduction-getSales();
    }


}
