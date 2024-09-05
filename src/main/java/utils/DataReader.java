package utils;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import problems.ProductionPlanningProblemWithSalvageRevenue;

public class DataReader {

    /**
     * Reads an instance file and generates the corresponding instance of the Production Planning Problem.
     * @param pathToInstanceFile absolute or relative path to the instance file
     * @return an instance of the class ProductionPlanningProblem
     * @throws FileNotFoundException
     */
    public static ProductionPlanningProblemWithSalvageRevenue readProblemWithSalvageRevenue(String pathToInstanceFile, double offset) throws FileNotFoundException {
        // =====================================================
        // Reads the instance data
        // =====================================================

        Scanner scanner = new Scanner(new BufferedReader(new FileReader(pathToInstanceFile)));
        // Extracts the number of facilities
        int nFacilities = Integer.parseInt(scanner.nextLine().split(",")[1]);

        // Extracts the number of products
        int nProducts = Integer.parseInt(scanner.nextLine().split(",")[1]);

        // Extracts the total capacity
        double totalCapacities[] = new double[nFacilities];
        scanner.nextLine(); // Skips the header
        for(int f = 1; f <= nFacilities; f++) {
            totalCapacities[f-1] = Double.parseDouble(scanner.nextLine().split(",")[1]);
        }

        // Extracts leftover costs and prices
        double leftoverCosts[] = new double[nProducts];
        double salesPrices[] = new double[nProducts];
        scanner.nextLine(); // Skips the header
        for(int p = 1; p <= nProducts; p++){
            String tokens[] = scanner.nextLine().split(",");
            leftoverCosts[p-1] = Double.parseDouble(tokens[1]);
            salesPrices[p-1] = Double.parseDouble(tokens[2]);
        }

        // Extracts manufacturing costs and # production levels
        double manufacturingCosts[][] = new double[nFacilities][nProducts];
        int nProductionLevels[][] = new int[nFacilities][nProducts];
        int maxNProductionLevels = 0;
        scanner.nextLine(); // Skips the header
        for(int f = 1; f <= nFacilities; f++) {
            for (int p = 1; p <= nProducts; p++) {
                String tokens[] = scanner.nextLine().split(",");
                manufacturingCosts[f-1][p-1] = Double.parseDouble(tokens[2]);
                nProductionLevels[f-1][p-1] = Integer.parseInt(tokens[3]);
                if(nProductionLevels[f-1][p-1] > maxNProductionLevels){
                    maxNProductionLevels = nProductionLevels[f-1][p-1];
                }
            }
        }
        // Calculates the number of probability distributions for each product
        int nDistributions[] = new int[nProducts];
        int maxNDistributions = 0;
        for (int p = 1; p <= nProducts; p++) {
            int nDist = 1;
            for(int f = 1; f <= nFacilities; f++) {
                nDist = nDist * nProductionLevels[f-1][p-1];
            }
            nDistributions[p-1] = nDist;
            if(nDist > maxNDistributions){
                maxNDistributions = nDist;
            }
        }

        // Extracts production levels information
        scanner.nextLine(); // Skips the header
        double productionLevelLowerBounds[][][] = new double[nFacilities][nProducts][maxNProductionLevels];
        double productionLevelUpperBounds[][][] = new double[nFacilities][nProducts][maxNProductionLevels];
        for (int f = 1; f <= nFacilities; f++) {
            for (int p = 1; p <= nProducts; p++) {
                for (int pl = 1; pl <= nProductionLevels[f-1][p-1]; pl++) {
                    String tokens[] = scanner.nextLine().split(",");
                    productionLevelLowerBounds[f-1][p-1][pl-1] = Double.parseDouble(tokens[4]);
                    productionLevelUpperBounds[f-1][p-1][pl-1] = Double.parseDouble(tokens[5])-offset;
                }
            }
        }

        // Extracts demand scenarios
        scanner.nextLine(); // Skips the header
        ArrayList<double[]> demandScenariosList = new ArrayList<double[]>();
        ArrayList<Double> demandScenariosProbabilitiesList = new ArrayList<Double>();
        String regex = "\\d+.*"; // Regex meaning: The line starts with one or more digits (\\d+) followed by anything (.) for zero or more times (*).
        while(scanner.hasNext(Pattern.compile("\\d+.*"))){
            String token[] = scanner.nextLine().split(",");
            int scenario = Integer.parseInt(token[0]);
            double probability = Double.parseDouble(token[1]);
            double demand[] = new double[nProducts];
            for(int p = 1; p <= nProducts; p++){
                demand[p-1] = Double.parseDouble(token[1+p]);
            }
            demandScenariosList.add(scenario,demand);
            demandScenariosProbabilitiesList.add(scenario,probability);
        }
        // Transforms the Lists into arrays
        int nDemandScenarios = demandScenariosProbabilitiesList.size();
        Double[] demandScenarioProbabilitiesList = demandScenariosProbabilitiesList.toArray(new Double[0]);
        double demandScenarioProbabilities[] = Stream.of(demandScenarioProbabilitiesList).mapToDouble(Double::doubleValue).toArray();
        double demandScenarios[][] = demandScenariosList.toArray(new double[0][]);

        // Reads yields distributions
        scanner.nextLine(); // Skips the header
        int distributionProductionLevels[][][] = new int[nProducts][maxNDistributions][nFacilities];
        String distributionNames[][] = new String[nProducts][maxNDistributions];
        int nYieldDistributionScenarios[][] = new int[nProducts][maxNDistributions];
        int maxNYieldDistributionScenarios = 0;
        for(int p = 1; p <= nProducts; p++){
            for(int d = 1; d <= nDistributions[p-1]; d++){
                String token[] = scanner.nextLine().split(",");
                String name = token[0];
                distributionNames[p-1][d-1] = name;
                for(int f = 1; f <= nFacilities; f++){
                    distributionProductionLevels[p-1][d-1][f-1] = Integer.parseInt(token[1+f]);
                }
                nYieldDistributionScenarios[p-1][d-1] = Integer.parseInt(token[1+nFacilities+1]);
                if(nYieldDistributionScenarios[p-1][d-1] > maxNYieldDistributionScenarios){
                    maxNYieldDistributionScenarios = nYieldDistributionScenarios[p-1][d-1];
                }
            }
        }

        // Reads the distribution yield scenarios
        scanner.nextLine(); // Skips the header
        double yieldDistributionScenarios[][][][] = new double[nProducts][maxNDistributions][nFacilities][maxNYieldDistributionScenarios];
        double yieldDistributionProbabilities[][][] = new double[nProducts][maxNDistributions][maxNYieldDistributionScenarios];
        for(int p = 1; p <= nProducts; p++){
            for(int d = 1; d <= nDistributions[p-1]; d++){
                for(int s = 1; s <= nYieldDistributionScenarios[p-1][d-1]; s++){
                    String token[] = scanner.nextLine().split(",");
                    yieldDistributionProbabilities[p-1][d-1][s-1] = Double.parseDouble(token[3]);
                    for(int f = 1; f <= nFacilities; f++) {
                        yieldDistributionScenarios[p - 1][d - 1][f - 1][s - 1] = Double.parseDouble(token[3+f]);
                    }
                }
            }
        }

        // Merges demand and yield distributions
        int nDistributionScenarios[][] = new int[nProducts][maxNDistributions];
        int maxNScenarios = 0;
        for(int p = 1; p <= nProducts; p++){
            for(int d = 1; d <= nDistributions[p-1]; d++){
                nDistributionScenarios[p-1][d-1] = nYieldDistributionScenarios[p-1][d-1] * nDemandScenarios;
                if(nDistributionScenarios[p-1][d-1] > maxNScenarios){
                    maxNScenarios = nDistributionScenarios[p-1][d-1];
                }
            }
        }
        double probabilities[][][] = new double[nProducts][maxNDistributions][maxNScenarios];
        double yieldRealization[][][][] = new double[nProducts][maxNDistributions][nFacilities][maxNScenarios];
        double demandRealization[][][] = new double[nProducts][maxNDistributions][maxNScenarios];
        for(int p = 1; p <= nProducts; p++){
            for(int d = 1; d <= nDistributions[p-1]; d++){
                int scenarioCounter = 0;
                for(int sy = 1; sy <= nYieldDistributionScenarios[p-1][d-1]; sy++){
                    for(int sd = 1; sd <= nDemandScenarios; sd++) {
                        scenarioCounter++;
                        probabilities[p - 1][d - 1][scenarioCounter - 1] = yieldDistributionProbabilities[p-1][d-1][sy-1] * demandScenarioProbabilities[sd-1];
                        double a = demandScenarios[sd-1][p-1];
                        demandRealization[p-1][d-1][scenarioCounter-1] = a;
                        for(int f = 1 ; f <= nFacilities; f++) {
                            yieldRealization[p - 1][d - 1][f-1][scenarioCounter-1] = yieldDistributionScenarios[p-1][d-1][f-1][sy-1];
                        }
                    }
                }
            }
        }

        return new ProductionPlanningProblemWithSalvageRevenue(nFacilities, nProducts, totalCapacities, leftoverCosts, manufacturingCosts, salesPrices,
                            nProductionLevels, productionLevelLowerBounds, productionLevelUpperBounds,
                            nDistributions, maxNDistributions, distributionProductionLevels, distributionNames,
                            probabilities, nDistributionScenarios, maxNScenarios, yieldRealization, demandRealization);

    }

}
