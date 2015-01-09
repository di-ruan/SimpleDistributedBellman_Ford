/**
 * record the information of a neighbor
 */

import java.util.Calendar;
import java.util.Date;


public class neighbor {
    public float cost;
    public boolean status;
    public Date time;

    public neighbor(float cost) {
        this.cost = cost;
        status = true;
        updateTime();
    }

    public void updateTime() {
        time = Calendar.getInstance().getTime();
    }
}
