[![INFORMS Journal on Computing Logo](https://INFORMSJoC.github.io/logos/INFORMS_Journal_on_Computing_Header.jpg)](https://pubsonline.informs.org/journal/ijoc)

# Production planning under demand and endogenous supply uncertainty

This archive is distributed in association with the [INFORMS Journal on Computing](https://pubsonline.informs.org/journal/ijoc)
under the MIT [License](LICENSE).

The software and data in this repository are a snapshot of the software and data that were used in the research reported on in the paper
[Production planning under demand and endogenous supply uncertainty](https://doi.org/10.1287/ijoc.2023.0067) by M. Hewitt and G. Pantuso.

## Cite

To cite the contents of this repository, please cite both the paper and this repo, using their respective DOIs.

https://doi.org/10.1287/ijoc.2023.0067

https://doi.org/10.1287/ijoc.2023.0067.cd

Below is the BibTex for citing this snapshot of the repository.

```
@misc{ProdPlan,
  author =        {M. Hewitt and G. Pantuso},
  publisher =     {INFORMS Journal on Computing},
  title =         {{Production planning under demand and endogenous supply uncertainty}},
  year =          {2024},
  doi =           {10.1287/ijoc.2023.0067.cd},
  url =           {https://github.com/INFORMSJoC/2023.0067},
  note =          {Available for download at https://github.com/INFORMSJoC/2023.0067},
}  
```

## Description

This software implements a specialized Benders algorithm for solving a Production Planning Problem under Endogenous Uncertainty.

## Usage

```java Main -i path/to/file.txt -t bdscV1 -vi1 -tLim 1800 -r results.csv -off 0.003```

The following options are available
- `i` the path to the instance file
- `r` the path to the desired result file
- `t` the type of test to perform. Currently available
    - `full` to solve the full model without decomposition
    - `autobd` to solve the problem with Cplex's automatic Benders decomposition
    - `bdscV1` to solve the problem using version 1 of the algorithm.
    - `bdscV2` to solve the problem using version 2 of the algorithm.
    - `bdscV3` to solve the problem using version 3 of the algorithm.
    - `eev-d` computes the expected value of the expected value problem with respect to the demand
    - `eev-y` computes the expected value of the expected value problem with respect to the yield
    - `eev-all` computes the expected value of the expected value problem with respect to both demand and yield
- `g` the target optimality gap (default 1e-04)
- `tLim` the time limit in seconds (default 1800)
- `log` the log freequency during solution in seconds (0 corresponds to no log -- default 0).
- `random` if we want that the test is performed in a random, non replicable, manner. This entails that all the random generators (for example in the definition of RSs) in the code will not be seeded.
- `off` the offset between production levels (default 0)
- `vi1` through `vi6` a number of valid inequalities
