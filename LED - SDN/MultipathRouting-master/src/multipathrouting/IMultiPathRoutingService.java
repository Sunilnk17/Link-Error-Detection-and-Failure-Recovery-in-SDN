package net.floodlightcontroller.multipathrouting;

import java.util.Date;
import java.util.Set;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface IMultiPathRoutingService extends IFloodlightService  {
    public void modifyLinkCost(Long srcDpid,Long dstDpid,short cost);
    public Route getRoute(long srcDpid,short srcPort,long dstDpid,short dstPort);
}
