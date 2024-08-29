package utils;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import problems.ProductionPlanningProblem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class Testable<T extends ProductionPlanningProblem> {
    protected final T pp;
    protected String experiment_name;
    protected long start;
    protected long end;
    protected int nCuts = 0;
    protected int nCallbackCalls = 0;
    protected double bestInteger = Double.NEGATIVE_INFINITY;
    protected double bestBound = Double.POSITIVE_INFINITY;
    protected double gap = Double.POSITIVE_INFINITY;
    protected double totalCallbackTime = 0;
    protected double rootNodeRelaxationTime = -1;
    protected double rootNodeLPbound = Double.POSITIVE_INFINITY;
    protected int nNodesExplored = -1;

    public Testable(T pp, String experiment_name) {
        this.pp = pp;
        this.experiment_name = experiment_name;
    }

    public double getSolutionTime(){
        return (end-start)/1e9;
    }

    public double getBestInteger(){
        return bestInteger;
    }

    public double getBestBound(){
        return bestBound;
    }

    public double getGap(){
        return gap;
    }

    public double getTotalCallbackTime(){
        return totalCallbackTime;
    }

    public double getRootNodeRelaxationTime() {
        return rootNodeRelaxationTime;
    }

    public int getnNodesExplored() {
        return nNodesExplored;
    }

    public double getRootNodeLPbound() {
        return rootNodeLPbound;
    }

    public void saveResults(Configuration config){
        File f = new File(config.getResultsFile());
        boolean file_exists = f.exists();
        try (FileWriter fw = new FileWriter(f,true); CSVPrinter printer = new CSVPrinter(fw, CSVFormat.DEFAULT)) {
            if(!file_exists){
                printer.printRecord("version","n_products","n_facilities","max_n_distributions","max_n_scenarios","experiment",
                        "gap","best_integer", "best_bound", "solution_time","root_lp_time","root_lp_bound",
                        "n_nodes","n_cuts","callback_time","n_callback_calls","instance_file","experiment_time");
            }
            printer.printRecord(config.getVersion(),pp.getnProducts(),pp.getnFacilities(),pp.getMaxNDistributions(),pp.getMaxNScenarios(),experiment_name,gap,bestInteger, bestBound, getSolutionTime(),getRootNodeRelaxationTime(),getRootNodeLPbound(),getnNodesExplored(),nCuts, totalCallbackTime,nCallbackCalls,config.getInstanceFile(),config.getTestTime());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void storeResults(Configuration config){
        String dbName = config.getResultsFile();
        if(!dbName.endsWith(".db")){
            dbName = dbName + ".db";
        }
        try(Connection connection = DriverManager.getConnection("jdbc:sqlite:"+dbName)) {
            // create a database connection
            System.out.println("Connected to db "+dbName);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            //statement.executeUpdate("drop results if not exists results");
            statement.executeUpdate("create table if not exists results (" +
                    "VERSION string , NPRODUCTS integer, NFACILITIES integer, MAXNDIST integer, MAXNSCENARIOS integer, EXPERIMENT string, " +
                    "BEST_GAP double, BEST_INTEGER double, " +
                    "BEST_BOUND double, TIME double, ROOT_LP_TIME double, ROOT_LP_BOUND double, " +
                    "NODES_EXPL integer, OCUTS integer, CALLBACKTIME double, " +
                    "NCALLBACKCALLS integer, INSTANCE_FILE string, TIMESTAMP string)");


            statement.executeUpdate("insert into results values('"
                    +config.getVersion()+"','"+pp.getnProducts()+"','"+pp.getnFacilities()+"','"+pp.getMaxNDistributions()+"','"+pp.getMaxNScenarios()+"','"+experiment_name+"','"+gap
                    +"','"+ bestInteger+"','"+bestBound+"','"+getSolutionTime()+"','"+getRootNodeRelaxationTime()+"','"+getRootNodeLPbound()+"','"+getnNodesExplored()+"','"+nCuts
                    +"','"+totalCallbackTime+"','"+nCallbackCalls+"','"+config.getInstanceFile()+"','"+config.getTestTime()+"')");

            System.out.println("Stored results.");
            statement.close();
            System.out.println("Connection closed");
        }
        catch(SQLException ex){
            System.err.println(ex.getMessage());
        }
    }


    public void saveSolution(Configuration config){
        File directory = new File("solutions");
        if(!directory.exists()){
            directory.mkdir();
        }
        File file = new File(directory.getAbsolutePath()+File.separator+"sol_"+config.getVersion()+"_"+pp.getnProducts()+"_"+pp.getnFacilities()+"_"+pp.getMaxNDistributions()+"_"+pp.getMaxNScenarios()+"_"+experiment_name+"_"+config.getTestTime()+".txt");
        try (FileWriter fw = new FileWriter(file,true); PrintWriter printer = new PrintWriter(fw, true)) {
            double[][] X = getFirstStageXSolution();
            int Y[][][] = getFirstStageYSolution();
            double w[][] = getSecondStageSales();
            double[][] o = getSecondStageOversupply();
            printer.println("First-stage production plan");
            for (int p = 1; p <= pp.getnProducts(); p++){
                for(int f = 1; f <= pp.getnFacilities(); f++){
                    for(int l = 1; l <= pp.getnProductionLevels(f,p); l++){
                        if(Y[p-1][f-1][l-1] > 0.5){
                            printer.println("Product "+p+" facility "+f+" level "+l+" lower bound "+pp.getProductionLevelLowerBound(f,p,l)+ " upper bound "+pp.getProductionLevelUpperBound(f,p,l)+" quantity "+X[p-1][f-1]);
                        }
                    }
                }
            }
            printer.println("Second-stage sales decisions");
            for (int p = 1; p <= pp.getnProducts(); p++){
                int distribution = -1;
                for (int d = 1; d <= pp.getnDistributions(p); d++) {
                    for (int f = 1; f <= pp.getnFacilities(); f++) {
                        int level = pp.getDistributionProductionLevels(p, d, f);
                        if (Y[p - 1][f - 1][level] < 0.5) {
                            distribution = d;
                        }
                    }
                }
                for(int s = 1; s <= pp.getMaxNScenarios(); s++){
                    printer.println("Product "+p+" scenario "+s+" sales "+w[p-1][s-1]+" oversupply "+o[p-1][s-1]+" demand "+pp.getDemandRealization(p,distribution,s));
                }
            }
        } catch (IOException | IloException e) {
            e.printStackTrace();
        }

    }

    public abstract double[][] getFirstStageXSolution() throws IloException;
    public abstract int[][][] getFirstStageYSolution() throws IloException;
    public abstract double[][] getSecondStageSales() throws IloException;
    public abstract double[][] getSecondStageOversupply() throws IloException;
    protected class ProgressCallback extends IloCplex.MIPInfoCallback{
        private final File logFile;
        private double lastLogTime = 0;
        private final double logFrequency; // The interval in seconds between logs;

        public ProgressCallback(Configuration config) {
             this.logFile = new File("log_"+config.getTestTime()+".log");
            this.logFrequency = config.getLogFrequency();
        }

        @Override
        protected void main() throws IloException {
            // Checks if the logfile exists
            if (this.logFrequency > 0) {
                boolean logFileExists = logFile.exists();
                try (FileWriter fw = new FileWriter(logFile, true); CSVPrinter printer = new CSVPrinter(fw, CSVFormat.DEFAULT)) {
                    // If the logfile is just created we print some information about the problem.
                    if (!logFileExists) {
                        printer.printRecord("n_products", "n_facilities", "max_n_distributions", "max_n_scenarios", "experiment");
                        printer.printRecord(pp.getnProducts(), pp.getnFacilities(), pp.getMaxNDistributions(), pp.getMaxNScenarios(), experiment_name);
                        printer.printRecord("time", "best_integer", "best_bound", "best_gap", "n_nodes_explored", "n_cuts");
                    }
                    // We log progress information

                    // Calculates elapsed time
                    double elapsedTime = (System.nanoTime() - start) / 1e9;

                    // We log at intervals of 100 seconds
                    if ((elapsedTime - lastLogTime) > logFrequency) {
                        lastLogTime = elapsedTime;
                        double logIncumbent = Double.NEGATIVE_INFINITY;
                        if (hasIncumbent()) {
                            logIncumbent = getIncumbentObjValue();
                        }
                        double logBestBound = getBestObjValue();
                        double logBestGap = getMIPRelativeGap();
                        int logNNodes = getNnodes();
                        printer.printRecord(elapsedTime, logIncumbent, logBestBound, logBestGap, logNNodes, nCuts);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }




}
