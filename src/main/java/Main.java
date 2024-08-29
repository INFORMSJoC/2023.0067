
import ilog.concert.IloException;
import models.*;
import org.apache.commons.cli.ParseException;
import utils.*;
import problems.ProductionPlanningProblemWithSalvageRevenue;

import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws ParseException {

        CommandLineInterpreter cli = new CommandLineInterpreter();
        Configuration conf = new Configuration(args, cli.getCliOptions());

        String instanceFile = conf.getInstanceFile();
        try {
            ProductionPlanningProblemWithSalvageRevenue pp = DataReader.readProblemWithSalvageRevenue(instanceFile,conf.getOffset());
            //pp.printSummary();
            if (conf.getTest().equalsIgnoreCase("full")) {
                FullModel f = new FullModel(pp);
                f.solve(conf);
                f.saveResults(conf);
                f.saveSolution(conf);
            }
            if (conf.getTest().equalsIgnoreCase("autobd")) {
                AutoBenders abd = new AutoBenders(pp);
                abd.solve(conf);
                abd.saveResults(conf);
            }
            if (conf.getTest().equalsIgnoreCase("bdscV1")) {
                MasterProblemV1 mp = new MasterProblemV1(pp);
                if(conf.addValidInequality1()){
                    mp.addValidInequality1();
                }
                if(conf.addValidInequality2()){
                    mp.addValidInequality2();
                }
                if(conf.addValidInequality3()){
                    mp.addValidInequality3();
                }
                if(conf.addValidInequality4()){
                    mp.addValidInequality4();
                }
                if(conf.addValidInequality5()){
                    mp.addValidInequality5();
                }
                if(conf.addValidInequality6()){
                    mp.addValidInequality6();
                }
                if(conf.addMinimalCoverInequalities()) {
                    mp.addMinimalCoversInequalities();
                }
                if(conf.addExtendedCoverInequalities()){
                    mp.addExtendedCoverInequalities();
                }
                mp.solve(conf);
                mp.saveResults(conf);
                mp.saveSolution(conf);
            }
            if (conf.getTest().equalsIgnoreCase("bdscV2")) {
                MasterProblemV2 mp = new MasterProblemV2(pp);
                if(conf.addValidInequality1()){
                    mp.addValidInequality1();
                }
                if(conf.addValidInequality2()){
                    mp.addValidInequality2();
                }
                if(conf.addValidInequality6()){
                    mp.addValidInequality6();
                }
                if(conf.addMinimalCoverInequalities()) {
                    mp.addMinimalCoversInequalities();
                }
                if(conf.addExtendedCoverInequalities()){
                    mp.addExtendedCoverInequalities();
                }
                mp.solve(conf);
                mp.saveResults(conf);
            }
            if (conf.getTest().equalsIgnoreCase("bdscV3")) {
                MasterProblemV3 mp = new MasterProblemV3(pp);
                if(conf.addValidInequality1()){
                    mp.addValidInequality1();
                }
                if(conf.addValidInequality2()){
                    mp.addValidInequality2();
                }
                if(conf.addValidInequality6()){
                    mp.addValidInequality6();
                }
                if(conf.addMinimalCoverInequalities()) {
                    mp.addMinimalCoversInequalities();
                }
                if(conf.addExtendedCoverInequalities()){
                    mp.addExtendedCoverInequalities();
                }
                mp.solve(conf);
                mp.saveResults(conf);
            }
            if(conf.getTest().startsWith("eev-")){

                // First it loads the appropriate EV problem
                String evInstanceFile = instanceFile;
                if(conf.getTest().equalsIgnoreCase("eev-d")){
                    evInstanceFile = evInstanceFile.replaceFirst("model","exp_demand_model");

                }
                if(conf.getTest().equalsIgnoreCase("eev-y")){
                    evInstanceFile = evInstanceFile.replaceFirst("model","exp_distsupply_model");

                }
                if(conf.getTest().equalsIgnoreCase("eev-all")){
                    evInstanceFile = evInstanceFile.replaceFirst("model","exp_demand_distsupply_model");

                }
                System.out.println("Reading EV file "+evInstanceFile);
                ProductionPlanningProblemWithSalvageRevenue ev = DataReader.readProblemWithSalvageRevenue(evInstanceFile,conf.getOffset());

                TaskManager.computeEEV(pp,ev,conf);

            }

        } catch (FileNotFoundException | IloException e) {
            e.printStackTrace();
        }
    }
}
