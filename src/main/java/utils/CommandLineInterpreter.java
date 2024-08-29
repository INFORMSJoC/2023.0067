package utils;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CommandLineInterpreter {
    private final Options cliOptions;

    public CommandLineInterpreter() {
        cliOptions = new Options();

        // Creates the header and footer to print in the help message.
        String header = "Production Planning under Endogenous Uncertainty."
                +"Giovanni Pantuso. All rights reserved.";
        String footer = "Please report any issue at gp@math.ku.dk.";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ProductionPlanning", header, cliOptions, footer, true);

        Option instanceFile_opt = new Option("i", true, "The path to the instance file.");
        instanceFile_opt.setRequired(true);
        instanceFile_opt.setLongOpt("instanceFile");
        cliOptions.addOption(instanceFile_opt);

        Option resultsFile_opt = new Option("r", true, "The path to the results file.");
        resultsFile_opt.setLongOpt("resultsFile");
        cliOptions.addOption(resultsFile_opt);

        Option test_opt = new Option("t", true, "Specifies the type of test to perform.\n " +
                "Current alternatives are:\n" +
                "-full: solves the extensive MIP formulation of the problem.\n" +
                "-bd: solves the problem using multicut Benders decomposition.");
        test_opt.setRequired(true);
        test_opt.setLongOpt("test");
        cliOptions.addOption(test_opt);

        Option offset_opt = new Option("off", true, "The production level offset.");
        offset_opt.setLongOpt("offset");
        cliOptions.addOption(offset_opt);

        Option gap_opt = new Option("g", true, "The target optimality gap.");
        gap_opt.setLongOpt("targetGap");
        cliOptions.addOption(gap_opt);

        Option timeLimit_opt = new Option("tLim", true, "The time limit.");
        timeLimit_opt.setLongOpt("timeLimit");
        cliOptions.addOption(timeLimit_opt);

        Option logFrequency_opt = new Option("log", true, "The log frequency in seconds (0 corresponds to no logs).");
        logFrequency_opt.setLongOpt("logFrequency");
        cliOptions.addOption(logFrequency_opt);

        Option valid_inequality1_opt = new Option("vi1", false, "Whether valid inequality 1 is used.");
        valid_inequality1_opt.setLongOpt("validInequality1");
        cliOptions.addOption(valid_inequality1_opt);

        Option valid_inequality2_opt = new Option("vi2", false, "Whether valid inequality 2 is used.");
        valid_inequality2_opt.setLongOpt("validInequality2");
        cliOptions.addOption(valid_inequality2_opt);

        Option valid_inequality3_opt = new Option("vi3", false, "Whether valid inequality 3 is used.");
        valid_inequality3_opt.setLongOpt("validInequality3");
        cliOptions.addOption(valid_inequality3_opt);

        Option valid_inequality4_opt = new Option("vi4", false, "Whether valid inequality 4 is used.");
        valid_inequality4_opt.setLongOpt("validInequality4");
        cliOptions.addOption(valid_inequality4_opt);

        Option valid_inequality5_opt = new Option("vi5", false, "Whether valid inequality 5 is used.");
        valid_inequality5_opt.setLongOpt("validInequality5");
        cliOptions.addOption(valid_inequality5_opt);

        Option valid_inequality6_opt = new Option("vi6", false, "Whether valid inequality 6 is used.");
        valid_inequality6_opt.setLongOpt("validInequality6");
        cliOptions.addOption(valid_inequality6_opt);

        Option cover_inequalities_opt = new Option("mci", false, "Whether minimal cover inequalities should be added.");
        cover_inequalities_opt.setLongOpt("minimalCoverInequalities");
        cliOptions.addOption(cover_inequalities_opt);

        Option extended_inequalities_opt = new Option("eci", false, "Whether extended cover inequalities should be added.");
        extended_inequalities_opt.setLongOpt("extendedCoverInequalities");
        cliOptions.addOption(extended_inequalities_opt);

        Option local_branching_opt = new Option("localBranching", false, "Whether local branching is used.");
        cliOptions.addOption(local_branching_opt);

    }

    /**
     * Returns the Options object containing the CLI configuration.
     * @return
     */
    public Options getCliOptions() {
        return cliOptions;
    }
}
