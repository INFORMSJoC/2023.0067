package utils;
import org.apache.commons.cli.*;

import java.time.LocalDateTime;

public class Configuration {

    private final String instanceFile;
    private String resultsFile = "results.txt";
    private final String test;
    private double logFrequency = 0;
    private double timeLimit = 1800;
    double targetGap = 1e-04;
    private boolean deterministic = true;
    private final String testTime = LocalDateTime.now().toString();
    private boolean validInequality1 = false;
    private boolean validInequality2 = false;
    private boolean validInequality3 = false;
    private boolean validInequality4 = false;
    private boolean validInequality5 = false;
    private boolean validInequality6 = false;
    private boolean minimalCoverInequalities = false;
    private boolean extendedCoverInequalities = false;
    private boolean localBranching = false;
    private double offset = 0;


    public Configuration(String[] args, Options options) throws ParseException {
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(options, args);

        // Reads the instance file. This argument is required.
        this.instanceFile = cmd.getOptionValue("i");

        // Reads the name of the results file.
        if(cmd.hasOption("r")){
            String value = cmd.getOptionValue("r");
            if(value == null){
                throw new IllegalArgumentException("Please specify the results files.");
            }else{
                resultsFile = value;
                System.out.println("Results will be saved to "+resultsFile);
            }
        }

        // Reads the test to perform. This argument is required.
        this.test = cmd.getOptionValue("t");

        // Reads the log frequency.
        if(cmd.hasOption("log")){
            logFrequency = Double.parseDouble(cmd.getOptionValue("log"));
            if(logFrequency <= 0){
                throw new IllegalArgumentException("Invalid log frequency.");
            }
        }
        System.out.println("Log frequency "+logFrequency+".");

        // Reads the time limit.
        if(cmd.hasOption("tLim")){
            timeLimit = Double.parseDouble(cmd.getOptionValue("tLim"));
            if(timeLimit <= 0){
                throw new IllegalArgumentException("Invalid time limit");
            }else{
                System.out.println("Time limit set to "+timeLimit+".");
            }
        }

        // Reads the offset.
        if(cmd.hasOption("off")){
            offset = Double.parseDouble(cmd.getOptionValue("off"));
            if(offset <= 0){
                throw new IllegalArgumentException("Invalid offset");
            }
        }
        System.out.println("Offset "+offset+".");

        // Reads the target optimality gap.
        if(cmd.hasOption("g")){
            targetGap = Double.parseDouble(cmd.getOptionValue("g"));
            if(targetGap <= 0){
                throw new IllegalArgumentException("Invalid target gap");
            }
        }
        System.out.println("Target gap "+targetGap+".");

        // Reads whether the tests should be deterministic.
        // If true (default) all random number generators will be seeded.
        if(cmd.hasOption("random")){
            deterministic = false;
            System.out.println("Tests will not be deterministic.");
        }

        // Reads the valid inequality to add.
        validInequality1 = cmd.hasOption("vi1");
        if(validInequality1){
            System.out.println("Using VI1");
        }
        validInequality2 = cmd.hasOption("vi2");
        if(validInequality2){
            System.out.println("Using VI2");
        }
        validInequality3 = cmd.hasOption("vi3");
        if(validInequality3){
            System.out.println("Using VI3");
        }
        validInequality4 = cmd.hasOption("vi4");
        if(validInequality4){
            System.out.println("Using VI4");
        }
        validInequality5 = cmd.hasOption("vi5");
        if(validInequality5){
            System.out.println("Using VI5");
        }

        validInequality6 = cmd.hasOption("vi6");
        if(validInequality6){
            System.out.println("Using VI6");
        }

        extendedCoverInequalities = cmd.hasOption("eci");
        if(extendedCoverInequalities){
            System.out.println("Using extended cover inequalities");
        }

        minimalCoverInequalities = cmd.hasOption("mci");
        if(minimalCoverInequalities){
            System.out.println("Using minimal cover inequalities");
        }

        localBranching = cmd.hasOption("localBranching");
        if(localBranching){
            System.out.println("Using Local Branching");
        }


    }

    public String getInstanceFile() {
        return instanceFile;
    }

    public String getResultsFile() {
        return resultsFile;
    }

    public String getTest() {
        return test;
    }

    public double getLogFrequency() {
        return logFrequency;
    }

    public double getTargetGap() {
        return targetGap;
    }

    public double getTimeLimit() {
        return timeLimit;
    }

    public String getTestTime() {
        return testTime;
    }

    public boolean isDeterministic() {
        return deterministic;
    }

    public String getVersion(){
        return "NA";
    }

    public boolean addValidInequality1() {
        return validInequality1;
    }

    public boolean addValidInequality2() {
        return validInequality2;
    }

    public boolean addValidInequality3() {
        return validInequality3;
    }

    public boolean addValidInequality4() {
        return validInequality4;
    }

    public boolean addValidInequality5() {
        return validInequality5;
    }

    public boolean addValidInequality6() {
        return validInequality6;
    }

    public boolean addMinimalCoverInequalities() {
        return minimalCoverInequalities;
    }

    public boolean addExtendedCoverInequalities() {
        return extendedCoverInequalities;
    }

    public boolean useLocalBranching() {
        return localBranching;
    }

    public double getOffset() {
        return offset;
    }
}
