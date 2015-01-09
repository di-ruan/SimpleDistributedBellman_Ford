/**
 * record the information of a reachable client
 */

public class distance {
    public float cost;
    public String firstHop;

    public distance(float cost, String firstHop) {
        this.cost = cost;
        this.firstHop = firstHop;
    }
}
