package data.scripts.util;

import com.fs.starfarer.api.combat.ShipAPI;
import java.util.List;

public class TrainWagon {
	
	//Based on KT_SinuousSegment by Sinosauropteryx (Kingdom of Terra mod)
	
    public ShipAPI ship = null;
    public TrainWagon nextSegment = null;
    public TrainWagon previousSegment = null;

    public static void setup(TrainWagon[] segments, List<ShipAPI> ships, String[] args){

        for (int f = 0; f < segments.length; f++){
            // Iterates through SinuousSegment array and connects them in order
            segments[f] = new TrainWagon();
            if (f > 0){
                segments[f].previousSegment = segments[f-1];
                segments[f-1].nextSegment = segments[f];
            }

            // Assigns each module to a segment based on its station slot name
            for (ShipAPI s : ships) {
                s.ensureClonedStationSlotSpec();

                if (s.getStationSlot() != null && s.getStationSlot().getId().equals(args[f])) {
                    segments[f].ship = s;

                    // First module: Assigns mothership as its previousSegment
                    if (f == 0){
                        segments[f].previousSegment = new TrainWagon();
                        segments[f].previousSegment.ship = s.getParentStation();
                        segments[f].previousSegment.nextSegment = segments[f];
                    }
                }
            }
        }
    }

    public TrainWagon(){
    }

    public TrainWagon(ShipAPI newShip) {
        ship = newShip;
        previousSegment = new TrainWagon();
        previousSegment.ship = ship.getParentStation();
        previousSegment.nextSegment = this;
    }

    public TrainWagon(ShipAPI newShip, TrainWagon newPrevious){
        ship = newShip;
        previousSegment = newPrevious;
        previousSegment.nextSegment = this;
    }

}
