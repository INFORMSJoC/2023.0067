package problems;

import java.util.HashMap;
import java.util.Set;

public class Cover {

    private final HashMap<Integer,Integer> levels;
    private final double totalWeight;


    public Cover(HashMap<Integer, Integer> levels, double totalWeight) {
        this.levels = levels;
        this.totalWeight = totalWeight;
    }
    public Set<Integer> getProducts(){
        return levels.keySet();
    }
    public int getLevel(int product){
        return levels.get(product);
    }

    public double getTotalWeight() {
        return totalWeight;
    }

}
