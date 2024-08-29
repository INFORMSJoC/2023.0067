package utils;

import ilog.concert.IloException;
import models.MasterProblemV1;
import models.OptimalitySubproblemV1;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import problems.ProductionPlanningProblemWithSalvageRevenue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TaskManager {

    public static void computeEEV(ProductionPlanningProblemWithSalvageRevenue pp, ProductionPlanningProblemWithSalvageRevenue ev, Configuration conf) throws IloException {
        // Solves the EV problem
        //FullModelWithSalvageRevenue evp = new FullModelWithSalvageRevenue(ev);
        MasterProblemV1 evp = new MasterProblemV1(ev);
        evp.addValidInequality6();
        evp.solve(conf);

        // Gets the EV solution
        double[][] X = evp.getFirstStageXSolution();
        int[][][] Y = evp.getFirstStageYSolution();

        // Computes the EEV.
        double eev = 0;
        // First we compute the first-stage cost.
        for (int p = 1; p <= pp.getnProducts(); p++){
            for(int f = 1; f <= pp.getnFacilities(); f++){
                eev = eev - pp.getManufacturingCosts(f,p) * X[p-1][f-1];
            }
        }
        // Then we compute the second-stage cost
        for (int p = 1; p <= pp.getnProducts(); p++) {
            // First, we identify the distribution enforced by the EV solution on the product
            int nDistributions = 0;
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
                    nDistributions++;
                    for (int s = 1; s <= pp.getnScenarios(p, d); s++) {
                        OptimalitySubproblemV1 optimalitySubproblem = new OptimalitySubproblemV1(p, d, s, X, pp);
                        double objectiveValue = optimalitySubproblem.solve();
                        eev = eev + (pp.getProbability(p,d,s) * objectiveValue);
                    }
                }
            }
            if (nDistributions != 1) {
                throw new IllegalArgumentException("The EV problem enforces more than one distributions.");
            }
        }
        System.out.println("EEV = "+eev);
        saveEEVResults(conf, pp, eev,evp.getSolutionTime(),evp.getGap());

    }

    public static void saveEEVResults(Configuration config, ProductionPlanningProblemWithSalvageRevenue pp,double eev, double evSolutionTime,double evGap){
        File f = new File(config.getResultsFile());
        boolean file_exists = f.exists();
        try (FileWriter fw = new FileWriter(f,true); CSVPrinter printer = new CSVPrinter(fw, CSVFormat.DEFAULT)) {
            if(!file_exists){
                printer.printRecord("version","n_products","n_facilities","max_n_distributions","max_n_scenarios","experiment","eev","ev_solution_time","ev_gap","instance_file","experiment_time");
            }
            printer.printRecord(config.getVersion(),pp.getnProducts(),pp.getnFacilities(),pp.getMaxNDistributions(),pp.getMaxNScenarios(), config.getTest(),eev,evSolutionTime,evGap,config.getInstanceFile(),config.getTestTime());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
