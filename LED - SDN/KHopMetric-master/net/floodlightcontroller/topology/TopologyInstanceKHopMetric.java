/**
 * Copyright (C) 2013 Luca Prete, Simone Visconti, Andrea Biancini, Fabio Farina - www.garr.it - Consortium GARR
 * 
 * This is an extended, modified version of the original TopologyInstance
 * file provided with Floodlight 0.90
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Implementation of the Floodlight TopologyInstance for KHopMetric service.
 * A representation of a network topology.  Used internally by 
 * {@link TopologyManager}
 * 
 * @author Luca Prete <luca.prete@garr.it>
 * @author Andrea Biancini <andrea.biancini@garr.it>
 * @author Fabio Farina <fabio.farina@garr.it>
 * 
 * @version 0.90
 * @see net.floodlightcontroller.topology.TopologyManagerKHopMetric
 * 
 */

package net.floodlightcontroller.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.floodlightcontroller.core.annotations.LogMessageCategory;
import net.floodlightcontroller.routing.BroadcastTree;
import net.floodlightcontroller.routing.Link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.asu.emit.qyan.alg.control.YenTopKShortestPathsAlg;
import edu.asu.emit.qyan.alg.model.Graph;
import edu.asu.emit.qyan.alg.model.Pair;
import edu.asu.emit.qyan.alg.model.Path;
import edu.asu.emit.qyan.alg.model.Vertex;
import edu.asu.emit.qyan.alg.model.abstracts.BaseVertex;

@LogMessageCategory("Network Topology")
public class TopologyInstanceKHopMetric extends TopologyInstance{

    public static final short LT_SH_LINK = 1;
    public static final short LT_BD_LINK = 2;
    public static final short LT_TUNNEL  = 3; 

    public static final int MAX_LINK_WEIGHT = 10000;
    public static final int MAX_PATH_WEIGHT = Integer.MAX_VALUE - MAX_LINK_WEIGHT - 1;
    public static final int PATH_CACHE_SIZE = 1000;

    protected static Logger log = LoggerFactory.getLogger(TopologyInstanceKHopMetric.class);
    
    protected Map<Pair<Long, Long>, List<Pair<Path, Boolean>>> cacheMap = null;
	
    public Map<Pair<Long, Long>, List<Pair<Path, Boolean>>> getCache() {
		return cacheMap;
	}

	public void setCache(
			Map<Pair<Long, Long>, List<Pair<Path, Boolean>>> cacheMap) {
		this.cacheMap = cacheMap;
	}

	public TopologyInstanceKHopMetric() {
    	super();
    	cacheMap = new HashMap<Pair<Long, Long>, List<Pair<Path, Boolean>>>();
    }
    
    public TopologyInstanceKHopMetric(Map<Long, Set<Short>> switchPorts, Map<NodePortTuple, Set<Link>> switchPortLinks){
    	super(switchPorts, switchPortLinks);
    	cacheMap = new HashMap<Pair<Long, Long>, List<Pair<Path, Boolean>>>();
    }
    
    public TopologyInstanceKHopMetric(Map<Long, Set<Short>> switchPorts, Set<NodePortTuple> blockedPorts, Map<NodePortTuple, Set<Link>> switchPortLinks, Set<NodePortTuple> broadcastDomainPorts, Set<NodePortTuple> tunnelPorts){
        super(switchPorts, blockedPorts, switchPortLinks, broadcastDomainPorts, tunnelPorts);
        cacheMap = new HashMap<Pair<Long, Long>, List<Pair<Path, Boolean>>>();
    }

    /**
     * @author Srinivasan Ramasubramanian
     *
     * This algorithm computes the depth first search (DFS) traversal of the
     * switches in the network, computes the lowpoint, and creates clusters
     * (of strongly connected components).
     *
     * The computation of strongly connected components is based on
     * Tarjan's algorithm.  For more details, please see the Wikipedia
     * link below.
     *
     * http://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
     *
     * The initialization of lowpoint and the check condition for when a
     * cluster should be formed is modified as we do not remove switches that
     * are already part of a cluster.
     *
     * A return value of -1 indicates that dfsTraverse failed somewhere in the middle
     * of computation.  This could happen when a switch is removed during the cluster
     * computation procedure.
     *
     * @param parentIndex: DFS index of the parent node
     * @param currIndex: DFS index to be assigned to a newly visited node
     * @param currSw: ID of the current switch
     * @param dfsList: HashMap of DFS data structure for each switch
     * @param currSet: Set of nodes in the current cluster in formation
     * @return long: DSF index to be used when a new node is visited
     */
    
    private Graph fromClusterToGraph(Cluster c, Long root, Map<Link, Integer> linkCost){
    	Graph graph = new Graph();
    	graph.set_vertex_num(c.getNodes().size());
    	for(Long node : c.getNodes()){
    		BaseVertex vertex = new Vertex(node);
			graph._vertex_list.add(vertex);
			graph._id_vertex_index.put(vertex.get_id(), vertex);
    	}
    	for(Long nodeFrom : c.getLinks().keySet()){
    		for(Link l : c.getLinks().get(nodeFrom)){
    			long start_vertex_id = l.getSrc();
				long end_vertex_id = l.getDst();
				double weight = 0;
				if (linkCost == null || linkCost.get(l)==null) weight= 1;
	            else weight = linkCost.get(l);
				graph.add_edge(start_vertex_id, end_vertex_id, weight, l.getSrcPort(), l.getDstPort());
    		}
    	}
    	return graph;
    }
    
    private Path getFromCache(Long srcSwitch, Long dstSwitch){
//    	long startOfCalc = System.nanoTime();
//    	long endOfCalc = 0;
//    	log.debug("Starting to search in cache for srcNode" + srcSwitch + " - dstNode: " + dstSwitch + ". Time: " + startOfCalc);
    	List<Pair<Path, Boolean>> cachedElement = cacheMap.get(new Pair<Long, Long>(srcSwitch, dstSwitch));
    	if (cachedElement == null || cachedElement.size() <= 0) return null;
    	for(Pair<Path, Boolean> path : cachedElement){
    		if(path.second()) {
//    			endOfCalc = System.nanoTime();
//    	    	log.debug("Calculation of cache search for srcNode " + srcSwitch + " - dstNode " + "dstNode " + dstSwitch + ". Time: " + endOfCalc + ". Time lasted " + (endOfCalc-startOfCalc));
    			return path.first();
    		}
    	}
    	return null;
    }
    
    protected BroadcastTree getBestPath(Cluster c, Long root, Map<Link, Integer> linkCost, boolean isDstRooted){
//    	long startOfCalc = System.nanoTime();
//    	log.debug("Starting to look for a braodcast tree for node " + root + ". Time: " + startOfCalc);
    	HashMap<Long, Link> nexthoplinks = new HashMap<Long, Link>();
        HashMap<Long, Integer> cost = new HashMap<Long, Integer>();
    	for(Long node : c.getNodes()){
    		nexthoplinks.put(node, null);
    		cost.put(node, MAX_PATH_WEIGHT);
    		cost.put(root, 0);
    		if(node.equals(root)) continue;
    		Path path; 
    		if((path = getFromCache(root, node)) == null) {
    			List<Pair<Path, Boolean>> l = calculateKShortestPath(c, root, node, linkCost);
    			cacheMap.put(new Pair<Long, Long>(root, node), l);
    			path = l.get(0).first();
    		} 		
    		//lo prendo da p e lo aggiungo ai nexthoplinks e cost
    		long dstSwitch = path.get_vertices().get(path.get_vertices().size()-1).get_id();
    		long srcSwitch = path.get_vertices().get(path.get_vertices().size()-2).get_id();
    		//log.debug("--> " + dstSwitch + " " + srcSwitch);
    		Link lastLink = null;
    		for(Link link : c.getLinks().get(dstSwitch)){
    			if(link.getDst() == srcSwitch){
    				lastLink = link;
    				//log.debug(link);
    			}
    		}
    		cost.put(node, (int)path.get_weight());
    		nexthoplinks.put(node, lastLink);
    	}
        BroadcastTree ret = new BroadcastTree(nexthoplinks, cost);
//      log.debug("Ora per il nodo " + root + " sto usando " + ret);
//      long endOfCalc = System.nanoTime();
//    	log.debug("Ended to retreive broadcast tree for node " + root + ". Time: " + endOfCalc + ". Time lasted " + (endOfCalc-startOfCalc));
        return ret;
    }
    
    public void printCache(){
    	log.debug("######## CONTENT OF THE CACHE ########");
    	for(Entry<Pair<Long, Long>, List<Pair<Path, Boolean>>> cacheElement : cacheMap.entrySet()){
			log.debug("Coppia nodi: " + cacheElement.getKey().o1 + " e " + cacheElement.getKey().o2);
    		for(Pair<Path, Boolean> pairPath : cacheElement.getValue()){
    			log.debug("valido: " + pairPath.o2 + ", path: " + pairPath.o1);
			}
		}
    }
    
    protected List<Pair<Path,Boolean>> calculateKShortestPath(Cluster c, Long root, Long node, Map<Link, Integer> linkCost){
//    	long startOfCalc = System.nanoTime();
//    	System.out.println("Starting to calculate KSP for node " + root + ". Time: " + startOfCalc);
    	Graph graph = fromClusterToGraph(c, root, linkCost);
    	YenTopKShortestPathsAlg yenAlg = new YenTopKShortestPathsAlg(graph);
    	List<Path> pathList = yenAlg.get_shortest_paths(graph.get_vertex(root), graph.get_vertex(node), 3);
    	List<Pair<Path, Boolean>> finalPathList = new ArrayList<Pair<Path, Boolean>>();
    	for(Path path : pathList){
    		finalPathList.add(new Pair<Path, Boolean>(path, true));
    	}
//    	long endOfCalc = System.nanoTime();
//    	System.out.println("Calculation of KSP for node " + root + ". Time: " + endOfCalc + ". Time lasted: " + (endOfCalc-startOfCalc));
    	return finalPathList;
    }
    
    protected BroadcastTree dijkstra(Cluster c, Long root, Map<Link, Integer> linkCost, boolean isDstRooted) {
    	long startOfCalc = System.nanoTime();
    	log.debug(startOfCalc + " - Inizio calcolo broadcast tree per " + root);
    	BroadcastTree bct = getBestPath(c, root, linkCost, isDstRooted);
        long endOfCalc = System.nanoTime();
        log.debug(endOfCalc + " - Fine calcolo broadcast tree per " + root);
        return bct;
    }
}