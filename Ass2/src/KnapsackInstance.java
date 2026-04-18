import java.io.*;
import java.util.*;

public class KnapsackInstance {
    public final String name;
    public final int    n;
    public final double capacity;
    public final Item[] items;

    private KnapsackInstance(String name, int n, double capacity, Item[] items) {
        this.name     = name;
        this.n        = n;
        this.capacity = capacity;
        this.items    = items;
    }

    //Parse a problem file. Line 1: n W. Lines 2..n+1: value weight.
    public static KnapsackInstance parse(String filePath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        StringTokenizer st = new StringTokenizer(br.readLine());
        int n = Integer.parseInt(st.nextToken());
        double capacity = Double.parseDouble(st.nextToken());
        Item[] items = new Item[n];
        for (int i = 0; i < n; i++) {
            String line = br.readLine();
            if (line == null) throw new IOException("Unexpected EOF at item " + i);
            st = new StringTokenizer(line);
            double value  = Double.parseDouble(st.nextToken());
            double weight = Double.parseDouble(st.nextToken());
            items[i] = new Item(value, weight);
        }
        br.close();
        // derive name from file path
        String name = new File(filePath).getName();
        return new KnapsackInstance(name, n, capacity, items);
    }

    //Returns total value if weight <= capacity, else 0.0. 
    public double evaluate(int[] bits) {
        double totalValue  = 0.0;
        double totalWeight = 0.0;
        for (int i = 0; i < n; i++) {
            if (bits[i] == 1) {
                totalValue  += items[i].value;
                totalWeight += items[i].weight;
            }
        }
        return totalWeight <= capacity ? totalValue : 0.0;
    }

    /*
     * Repair in-place: drop least value/weight efficient selected items
     * until total weight <= capacity.
     */
    public void repair(int[] bits) {
        // Build list of selected indices sorted by efficiency ascending (worst first)
        double totalWeight = 0.0;
        for (int i = 0; i < n; i++) {
            if (bits[i] == 1) totalWeight += items[i].weight;
        }
        if (totalWeight <= capacity) return;

        // Collect selected indices
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (bits[i] == 1) selected.add(i);
        }
        // Sort by efficiency ascending (least efficient first)
        selected.sort(Comparator.comparingDouble(i -> items[i].value / items[i].weight));

        for (int idx : selected) {
            if (totalWeight <= capacity) break;
            bits[idx] = 0;
            totalWeight -= items[idx].weight;
        }
    }
}
