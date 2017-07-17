package net.floodlightcontroller.multipathrouting;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.LinkedList;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.routing.RouteId;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.multipathrouting.LinkWithCost;
import net.floodlightcontroller.restserver.IRestApiService;

import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class MultiPathRouting implements IFloodlightModule ,ITopologyListener, IMultiPathRoutingService{
    protected static Logger logger;
    protected IFloodlightProviderService floodlightProvider;
    protected ITopologyService topologyService;
    protected IRestApiService restApi;
    protected final int ROUTE_LIMITATION = 10;
    protected HashMap<Long, HashSet<LinkWithCost>> dpidLinks;
    protected int pathCount = 0;    


    protected class FlowCacheLoader extends CacheLoader<FlowId,Route> {
        MultiPathRouting mpr;
        FlowCacheLoader(MultiPathRouting mpr) {
            this.mpr = mpr;
        }

        @Override
        public Route load(FlowId fid) {
            return mpr.buildFlowRoute(fid);
        }
    }
    private final FlowCacheLoader flowCacheLoader = new FlowCacheLoader(this);
    protected LoadingCache<FlowId,Route> flowcache;
    
    protected class PathCacheLoader extends CacheLoader<RouteId,MultiRoute> {
        MultiPathRouting mpr;
        PathCacheLoader(MultiPathRouting mpr) {
            this.mpr = mpr;
        }

        @Override
        public MultiRoute load(RouteId rid) {
            return mpr.buildMultiRoute(rid);
        }
    }
    private final PathCacheLoader pathCacheLoader = new PathCacheLoader(this);
    protected LoadingCache<RouteId,MultiRoute> pathcache;
    
    
    //
    //
    //ITopologyListener
    //
    //
    @Override
    public void topologyChanged(List<LDUpdate> linkUpdates){
        for (LDUpdate update : linkUpdates){
            if (update.getOperation().equals(ILinkDiscovery.UpdateOperation.LINK_REMOVED) || update.getOperation().equals(ILinkDiscovery.UpdateOperation.LINK_UPDATED)) {
                LinkWithCost srcLink = new LinkWithCost(update.getSrc(), update.getSrcPort(), update.getDst(), update.getDstPort(),1);
                LinkWithCost dstLink = srcLink.getInverse();
                if (update.getOperation().equals(ILinkDiscovery.UpdateOperation.LINK_REMOVED)){
                    removeLink(srcLink);
                    removeLink(dstLink);
                    clearRoutingCache();
                }
                else if (update.getOperation().equals(ILinkDiscovery.UpdateOperation.LINK_UPDATED)){
                    addLink(srcLink);
                    addLink(dstLink);
                }
            }
            
        } 

    }
    public void clearRoutingCache(){
         flowcache.invalidateAll();
         pathcache.invalidateAll();
    }
    public void removeLink(LinkWithCost link){
        Long dpid = link.getSrcDpid();
        if( null == dpidLinks.get(dpid)){
            return;
        }
        else{
            dpidLinks.get(dpid).remove(link);
            if( 0 == dpidLinks.get(dpid).size())
                dpidLinks.remove(dpid);
        }

    }
    public void addLink(LinkWithCost link){    
        Long dpid = link.getSrcDpid();
        if (null == dpidLinks.get(dpid)){
            HashSet<LinkWithCost> links = new HashSet<LinkWithCost>();
            links.add(link);
            dpidLinks.put(dpid,links);
        }
        else{
            dpidLinks.get(dpid).add(link);
        }
    }
    public Route buildFlowRoute(FlowId fid){
        Long srcDpid = fid.getSrc();
        Long dstDpid = fid.getDst();
        short srcPort = fid.getSrcPort();
        short dstPort = fid.getDstPort();

        List<NodePortTuple> nptList;
        NodePortTuple npt;
        MultiRoute routes = null;
        Route result = null;
        try{
            routes = pathcache.get(new RouteId(srcDpid,dstDpid));
        }catch (Exception e){
            logger.error("error {}",e.toString());
        }
        if( 0 == routes.getRouteSize())
            result = null;
        else
            result = routes.getRoute();
        
        if (result != null) {
            nptList= new ArrayList<NodePortTuple>(result.getPath());
        } else {
            nptList = new ArrayList<NodePortTuple>();
        }

        npt = new NodePortTuple(srcDpid, srcPort);
        nptList.add(0, npt); 
        npt = new NodePortTuple(dstDpid, dstPort);
        nptList.add(npt); 

        result = new Route(new RouteId(srcDpid,dstDpid), nptList);
        return result;
    }

    public MultiRoute buildMultiRoute(RouteId rid){
        return computeMultiPath(rid);
    }

    public MultiRoute computeMultiPath(RouteId rid){
        Long srcDpid = rid.getSrc();
        Long dstDpid = rid.getDst();
        MultiRoute routes = new MultiRoute();
        if( srcDpid == dstDpid)
            return routes;
        if( null == dpidLinks.get(srcDpid) || null == dpidLinks.get(dstDpid))
            return routes;
        HashMap<Long, HashSet<LinkWithCost>> previous = new HashMap<Long, HashSet<LinkWithCost>>();
        HashMap<Long, HashSet<LinkWithCost>> links = dpidLinks;
        HashMap<Long, Integer> costs = new HashMap<Long, Integer>();
        for(Long dpid : links.keySet()){
            costs.put(dpid,Integer.MAX_VALUE);
            previous.put(dpid,new HashSet<LinkWithCost>());
        }
        PriorityQueue<NodeCost> nodeq = new PriorityQueue<NodeCost>();
        HashSet<Long> seen = new HashSet<Long>();
        nodeq.add(new NodeCost(srcDpid,0));
        NodeCost node;
        while( null != nodeq.peek()){
            node = nodeq.poll();
            if(node.getDpid() ==  dstDpid)
                break;
            int cost = node.getCost();
            seen.add(node.getDpid());
            for (LinkWithCost link: links.get(node.getDpid())) {
                Long dst = link.getDstDpid();
                int totalCost = link.getCost() + cost;
                if( true == seen.contains(dst))
                    continue;

                if( totalCost < costs.get(dst) ){
                    costs.put(dst,totalCost);
                    previous.get(dst).clear();
                    previous.get(dst).add(link.getInverse());

                    NodeCost ndTemp = new NodeCost(dst,totalCost);
                    nodeq.remove(ndTemp);
                    nodeq.add(ndTemp);
                }
                else if( totalCost == costs.get(dst) ){
                    //multiple path
                    previous.get(dst).add(link.getInverse());
                }
            }

        }
        LinkedList<NodePortTuple> switchPorts = new LinkedList<NodePortTuple>();
        pathCount = 0;
        generateMultiPath(routes,srcDpid,dstDpid,dstDpid,previous,switchPorts);
        return routes;
    }
    public void generateMultiPath(MultiRoute routes, Long srcDpid, Long dstDpid, Long current, HashMap<Long, HashSet<LinkWithCost>> previous,LinkedList<NodePortTuple> switchPorts)
    {   if (pathCount >=ROUTE_LIMITATION)
            return ; 
        if( current == srcDpid){
            pathCount++;
            Route result = new Route(new RouteId(srcDpid,dstDpid), new LinkedList<NodePortTuple>(switchPorts));
            routes.addRoute(result);
            return ;
        }
        HashSet<LinkWithCost> links = previous.get(current);
        for(LinkWithCost link: links){
            NodePortTuple npt = new NodePortTuple(link.getDstDpid(), link.getDstPort());
            NodePortTuple npt2 = new NodePortTuple(link.getSrcDpid(), link.getSrcPort());
            switchPorts.addFirst(npt2);
            switchPorts.addFirst(npt);
            generateMultiPath(routes,srcDpid, dstDpid, link.getDstDpid(), previous,switchPorts);
            switchPorts.removeFirst();
            switchPorts.removeFirst();

        }
        return ;
    }

    private void updateLinkCost(long srcDpid,long dstDpid,int cost){
        if( null != dpidLinks.get(srcDpid)){
            for(LinkWithCost link: dpidLinks.get(srcDpid)){
                if(link.getSrcDpid() == srcDpid && link.getDstDpid() == dstDpid){
                    link.setCost(cost);
                    return;
                }
            }
        }
    }

    //
    //IMultiPathRoutingService implement
    //
    //
    @Override
    public Route getRoute(long srcDpid,short srcPort,long dstDpid,short dstPort){
        // Return null the route source and desitnation are the
        // same switchports.
        if (srcDpid == dstDpid && srcPort == dstPort)
            return null;

        FlowId id = new FlowId(srcDpid,srcPort,dstDpid,dstPort);
        Route result = null;
        try{
            result = flowcache.get(id);
        }catch (Exception e){
            logger.error("error {}",e.toString());
        }
        
        if (result == null && srcDpid != dstDpid) return null;
        return result;
        
    }
    @Override
    public void modifyLinkCost(Long srcDpid,Long dstDpid,short cost){
        updateLinkCost(srcDpid,dstDpid,cost);
        updateLinkCost(dstDpid,srcDpid,cost);
        clearRoutingCache();
    }


    //
    //
    //IFloodlightModule
    //
    //


    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IMultiPathRoutingService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>,
        IFloodlightService> m =
            new HashMap<Class<? extends IFloodlightService>,
                IFloodlightService>();
        m.put(IMultiPathRoutingService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
        new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(ITopologyService.class);
//      l.add(IRestApiService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        topologyService    = context.getServiceImpl(ITopologyService.class);
        restApi = context.getServiceImpl(IRestApiService.class);
        logger = LoggerFactory.getLogger(MultiPathRouting.class);
        dpidLinks = new HashMap<Long, HashSet<LinkWithCost>>();

        flowcache = CacheBuilder.newBuilder().concurrencyLevel(4)
                    .maximumSize(1000L)
                    .build(
                            new CacheLoader<FlowId,Route>() {
                                public Route load(FlowId fid) {
                                    return flowCacheLoader.load(fid);
                                }
                            });
        pathcache = CacheBuilder.newBuilder().concurrencyLevel(4)
                    .maximumSize(1000L)
                    .build(
                            new CacheLoader<RouteId,MultiRoute>() {
                                public MultiRoute load(RouteId rid) {
                                    return pathCacheLoader.load(rid);
                                }
                            });
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        topologyService.addListener(this);
        //restApi.addRestletRoutable(new MultiPathRoutingWebRoutable());
    }

}
