/**
 * Backup, Routing,Modulation Level and Spectrum Assignment
 */
package flexgridsim.rsa;
import java.util.ArrayList;
import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.Path;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.Modulations;
import flexgridsim.util.MultiGraph;
import flexgridsim.util.WeightedGraph;

/**
 * @author adriel
 *
 */
public class BRT_NOMOD implements RSA  {
	private PhysicalTopology pt;
	private VirtualTopology vt;
	private ControlPlaneForRSA cp;
	private WeightedGraph graph;
	/**
	 * 
	 */

	@Override
	public void flowDeparture(Flow flow) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt,
			VirtualTopology vt, ControlPlaneForRSA cp, TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();

		
	}

	@Override
	public void flowArrival(Flow flow) {
		long id=-1;
		int guardBand=1;
		if(pt.getGrooming())
			guardBand=0;

		int demandInSlots = (int) (Math.ceil(flow.getRate() / (double) pt.getSlotCapacity()));
		ArrayList<Path> paths = new ArrayList<Path>();
		ArrayList<Integer> demands = new ArrayList<Integer>();
		WeightedGraph none = new WeightedGraph(graph);
		int numMP = 1;
		boolean found = false;

		do{
			demands = dividir(demandInSlots, numMP);
			for(int demand : demands) {
				MultiGraph multigraph = new MultiGraph(none, pt, demand);
				Path path = getShortestPath(multigraph, flow.getSource(), flow.getDestination(),demandInSlots);
				if(path != null) {
					paths.add(path);
					for(int n : path.getLinks()){
						none.removeEdge(pt.getLink(n).getSource(), pt.getLink(n).getDestination());
					}
				}
			}
			if(paths.size() == numMP) {
				found = true;
			}
			numMP++;
		  }while(numMP <= 1 && !found);

		// If no possible path found, block the call


		if(!found) {
			cp.blockFlow(flow.getID());
			return;
		} else {
			for(Path p : paths) {
				if(p.getLinks() == null || p.getSlotList().isEmpty()) {
					cp.blockFlow(flow.getID());
					return;
				}
			}
		}
		
		ArrayList<Long> ids = new ArrayList<Long>();
		ArrayList<LightPath> lightpath = new ArrayList<>();
		
		//Check id numbers
		
		for(Path p : paths) {
			Long num = vt.createLightpath(p, Modulations.getModulationLevel(pt.getSlotCapacity()));
			if(num != -1)
				ids.add(num);
			else {
				if(!ids.isEmpty()) {
					for(Long numb : ids) {
						vt.removeLightPath(numb);
					}
				}
				cp.blockFlow(flow.getID());
				return;
			}
		}

		//Setting links and slots

		int al = 0;
		for(Path p : paths) {
			flow.setLinks(p.getLinks(), al);
			flow.setSlotList(p.getSlotList(), al);
			lightpath.add(vt.getLightpath(ids.get(al)));
			al++;
		}
		
		if(!cp.acceptFlow(flow.getID(), lightpath)) {
			cp.blockFlow(flow.getID());
			for(Long i : ids) {
				vt.removeLightPath(i);
			}
			return;
		}
	}

	
	public int getPhysicalDistance(int[] links){
		if(links!=null&& links.length>0){
			int physicalDistance = 0;
			for (int i = 0; i < links.length - 1; i++) {
				physicalDistance += pt.getLink(links[i]).getDistance();
			}
			return physicalDistance/2;
		}
		else
			return -1;
	}
	
	/**
	 * Verify if two paths are disjoint
	 * 
	 * @param vector of links
	 *           
	 * @param path 
	 *            path 
	 * @return return true in case disjoint, false other case
	 */	
//	Diz se dois vetores são disjuntos
	/*	public  boolean disjoint(ArrayList<Flow> flows, int[] linkpath) {
	if( flows!=null&&linkpath!=null && linkpath.length!=0){
		for (Flow flow : flows){
			for(int i = 0; i < flow.getLinks().length; i++){  
				for(int j = 0; j < linkpath.length; j++){  
					if( flow.getLinks()[i]==linkpath[j])
							return false;	
				}
			}
		}
	}		
	return true;
}*/
public  boolean disjoint(ArrayList<LightPath> lightpaths, int[] linkpath) {
	if(lightpaths!=null&& linkpath!=null && linkpath.length!=0){
		for (LightPath lightpath : lightpaths){
			for(int i = 0; i < lightpath.getLinks().length; i++){  
				for(int j = 0; j < linkpath.length; j++){  
					if( lightpath.getLinks()[i]==linkpath[j])
							return false;	
				}
			}
		}
	}		
	return true;
}
	/**
	 * Finds, from the list of unvisited vertexes, the one with the lowest
	 * distance from the initial node.
	 * 
	 * @param dist
	 *            vector with shortest known distance from the initial node
	 * @param v
	 *            vector indicating the visited nodes
	 * @return vertex with minimum distance from initial node, or -1 if the
	 *         graph is unconnected or if no vertexes were visited yet
	 */
	public int minVertex(double[] dist, boolean[] v) {
		double x = Double.MAX_VALUE;
		int y = -1; // graph not connected, or no unvisited vertices
		for (int i = 0; i < dist.length; i++) {
			if (!v[i] && dist[i] < x) {
				y = i;
				x = dist[i];
			}
		}
		return y;
	}
	

	
	
	// Dijkstra's algorithm to find shortest path from s to all other nodes
		/**
		 * Msp.
		 *
		 * @param G the g
		 * @param s the s
		 * @param demand the demand
		 * @param protection true if is to protect false otherwise
		 * @return the int[]
		 */
	
public int[] MSP(WeightedGraph G, int s, int demand) {
	final double[] dist = new double[G.size()]; // shortest known distance
												// from "s"
	final int[] pred = new int[G.size()]; // preceding node in path
	final boolean[] visited = new boolean[G.size()]; // all false initially
	for (int i = 0; i < dist.length; i++) {
		pred[i] = -1;
		dist[i] = 1000000;
	}

	dist[s] = 0;
	for (int i = 0; i < dist.length; i++) {
		final int next = minVertex(dist, visited);
		if (next >= 0) {
			visited[next] = true;

			// The shortest path to next is dist[next] and via pred[next].
			final int[] n = G.neighbors(next);
			for (int j = 0; j < n.length; j++) {
				final int v = n[j];
				final double d = dist[next] + G.getWeight(next, v);
				if (dist[v] > d) {
					dist[v] = d;
					pred[v] = next;
				}					
			}
		}
	}
	return pred;
}


	
	/**
	 * Retrieves the shortest path between a source and a destination node,
	 * within a weighted graph.
	 * 
	 * @param G
	 *            the weighted graph in which the shortest path will be found
	 * @param src
	 *            the source node
	 * @param dst
	 *            the destination node
	 * @param demand
	 *            size of the demand
	 * @return the shortest path, as a vector of integers that represent node
	 *         coordinates
	 */


	public ArrayList<Integer> getShortestPath(WeightedGraph G, int src, int dst, int demand) {
		int x;
		ArrayList<Integer> path = new ArrayList<Integer>();
		final int[] pred = MSP(G, src, demand);
		if (pred == null) {
			return null;
		}
		x = dst;
	
		while (x != src) {
			path.add(0, x);
			x = pred[x];
			// No path
			if (x == -1) {
				return null;
			}				
		}
		path.add(0, src);
		
		return path;
	}

	public Path getShortestPath(MultiGraph multigraph, int src, int dst, int demand) {
		int links[];
		int nowSlot=-1;
		int nowCore=-1;;
		ArrayList<Integer> path = new ArrayList<Integer>();
		ArrayList<Slot> channel = new ArrayList<Slot>();
		for (int j = 0; j < multigraph.getNumMultiedges();j++) {	//Num cores
			for (int i = 0; i < multigraph.getNumEdges(); i++) {	//Num slots left
				ArrayList<Integer> nowpath = new ArrayList<Integer>();
				nowpath=getShortestPath(multigraph.getGraph(i,j),src,dst, demand);
				if(nowpath==null||nowpath.size()<2){
					continue;
				}else{
					if(nowpath.size()<path.size()||path.isEmpty()){
						path=nowpath;
						nowSlot=i;
						nowCore =j;
					}
				}
				
			}
		}
		if(path.size()<2){
			return null;
		}
		links = new int[path.size() - 1];

		for (int j = 0; j < path.size() - 1; j++) {
			links[j] = pt.getLink(path.get(j), path.get(j + 1)).getID();
		}
		for (int l = 0; l < links.length; l++) {
			for (int j = nowSlot; j < nowSlot+demand; j++) {
				channel.add(new Slot(nowCore, j, links[l] ));
			}
		}
		return new Path(links, channel);
	}
	
	public  Path getbestPathBack( int src, int dst, int demand,Path path) {
		Path backupPath;
		MultiGraph multigraphb = new MultiGraph(graph, pt, demand);		
		for (int i = 0; i < path.getLinks().length; i++) {	
			multigraphb.removeEdge(pt.getLink(path.getLink(i)).getSource(),pt.getLink(path.getLink(i)).getDestination());
		}	
		 backupPath = getShortestPath(multigraphb, src, dst, demand);
		return backupPath;
	}
	
	
	
	public  Path getbestPathBack(MultiGraph multigraph, int src, int dst, int demand,Path path) {
		Path backupPath;
		for (int i = 0; i < path.getLinks().length; i++) {	
			multigraph.removeEdge(pt.getLink(path.getLink(i)).getSource(),pt.getLink(path.getLink(i)).getDestination());
			multigraph.removeEdge(pt.getLink(path.getLink(i)).getDestination(),pt.getLink(path.getLink(i)).getSource());
		}	
		 backupPath = getShortestPath(multigraph, src, dst, demand);
		return backupPath;
	}
	
	public ArrayList<Integer> dividir(int demandTotal, int multipaths){
		ArrayList<Integer> demands = new ArrayList<Integer>();
		demandTotal += multipaths;
		while(multipaths != 0) {
			int dm = (int) Math.ceil(demandTotal / multipaths);
			demands.add(dm);
			demandTotal -= dm;
			multipaths--;
		}
		return demands;
	}
}
