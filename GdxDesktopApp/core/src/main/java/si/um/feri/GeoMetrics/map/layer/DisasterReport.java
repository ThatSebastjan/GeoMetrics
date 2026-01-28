package si.um.feri.GeoMetrics.map.layer;

import si.um.feri.GeoMetrics.map.GeoPoint;

public class DisasterReport {
    public final String type;
    public final int severity;
    public final GeoPoint location;

    public DisasterReport(String type, int severity, GeoPoint location){
        this.type = type;
        this.severity = severity;
        this.location = location;
    }
}
