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
public class Globecom implements RSA  {
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
		
		if (flow.getID() == 300) {
			int a = 30;
		}
		
		long id=-1;
		int guardBand=1;

		if(pt.getGrooming())
			guardBand=0;
		
		int demandInSlots = 0; // Inicializa variável
		Path path;

		int mod=5; // Modulação testada a cada iteração
		int modulation=0;
		do{
			demandInSlots = (int) Math.ceil(flow.getRate() / (double) Modulations.getBandwidth(mod)) + guardBand;
			MultiGraph multigraph = new MultiGraph(graph, pt, demandInSlots);
			path = getShortestPath(multigraph, flow.getSource(), flow.getDestination(),demandInSlots);
			if(path!=null)
				modulation=Modulations.getModulationByDistance(getPhysicalDistance(path.getLinks()));
			else
				modulation =-1;
			mod--;
		}while(mod>-1 && modulation!=-1 && modulation!=mod+1);

		if (path == null) {
			cp.blockFlow(flow.getID());
			return;
		} else if (path.getLinks()==null||path.getSlotList().isEmpty()) {
			cp.blockFlow(flow.getID());
			return;
		}
		
		id = vt.createLightpath(path, modulation);
		
		ArrayList<LightPath> lightpath = new ArrayList<LightPath>();
		
		if (id >= 0) {
			flow.setLinks(path.getLinks());
			flow.setSlotList(path.getSlotList());
			flow.setModulationLevel(modulation);
			lightpath.add(vt.getLightpath(id));
		}
		if(id<0){
			cp.blockFlow(flow.getID());
			return;
		}
		
		if(!cp.acceptFlow(flow.getID(), lightpath)) {
			vt.removeLightPath(id);
			cp.blockFlow(flow.getID());
			return;
		}	
		return;
	}

	
	public int getPhysicalDistance(int[] links){
		if(links!=null&& links.length>0){
			int physicalDistance = 0;
			for (int i = 0; i < links.length - 1; i++) {
				physicalDistance += pt.getLink(links[i]).getDistance();
			}
			//return physicalDistance/2;
			return physicalDistance;
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
	
	// Primeiro
	public Path getShortestPath(MultiGraph multigraph, int src, int dst, int demand) {
		int links[];
		int nowSlot=-1;
		int nowCore=-1;
		double frag = 0;
		int inter = 0;
		ArrayList<Integer> path = new ArrayList<Integer>(); //ArrayList para facilitar a troca de valores
		ArrayList<Slot> channel = new ArrayList<Slot>();
		
		for (int j = 0; j < multigraph.getNumMultiedges();j++) {	//Num cores
			for (int i = 0; i < multigraph.getNumEdges(); i++) {	//Num slots left
				ArrayList<Integer> nowpath = new ArrayList<Integer>();
				nowpath = getShortestPath(multigraph.getGraph(i,j),src,dst, demand);
				if(nowpath == null || nowpath.size() < 2){
					continue;
				}else{
					if(nowpath.size() < path.size() || path.isEmpty()){
						path = nowpath;
						nowSlot = i;
						nowCore = j;
						for (int link:nowpath) {
							inter += calculateCross(link, j, i, demand);
							frag += calculateFrag(link, j, i, demand);
						}
					} else if (nowpath.size() == path.size()) {
						int nowInter = 0;
						double nowFrag = 0; 
						for (int link:nowpath) {
							nowInter += calculateCross(link, j, i, demand);
							nowFrag += calculateFrag(link, j, i, demand);
						}
						if (nowInter < inter || nowFrag < frag) {
							path = nowpath;
							nowSlot = i;
							nowCore = j;
							inter = nowInter;
							frag = nowFrag;
						}
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
	
	public double calculateFrag (int link, int core, int slot, int demand) {
		boolean[] spectrum = pt.getLink(link).getSpectrum(core);
		boolean[] s = new boolean[pt.getNumSlots()];
		
		for (int j = 0; j < pt.getNumSlots(); j++) {
			if (spectrum[j])
				s[j] = true;
			else
				s[j] = false;
		}
		
		for (int j = slot; j < slot + demand; j++) {
			s[j] = false;
		}
		
		int lf = 0; // last frequency
		int lb = 0; // last blank
		int gaps = 0;
		int nBig = 0;
		int sBig = 0;
		int nSmall = 0;
		int sSmall = 320;
		
		boolean veri = false;
		int size = 0;
		ArrayList<Integer> space = new ArrayList<Integer>();
		
		for(int j = 0; j < pt.getNumSlots(); j++) {
			if(s[j] == true) {
				lb = j + 1;
				if(veri == false) {
					gaps++;
				}
				size++;
				veri = true;
			}
			if(s[j] == false) {
				lf = j + 1;
				if(veri) {
					space.add(size);
					if (size >= sBig)
						sBig = size;
					if (size <= sSmall)
						sSmall = size;
				}
				veri = false;
				size = 0;
			}
			if(j == pt.getNumSlots()-1) {
				if(veri) {
					space.add(size);
					if (size >= sBig)
						sBig = size;
					if (size <= sSmall)
						sSmall = size;
				}
			}
		}
		
		for(int a:space) {
			if (a == sBig)
				nBig++;
			if (a == sSmall)
				nSmall++;
		}
		
		if (lb == 0)
			return 1;

		double cima = (double)(Math.abs(nBig * sBig - nSmall * sSmall) + 1);
		double baixo = (double)(Math.abs(sBig - sSmall) + 1);
		double number = cima/baixo;
		//double number = (double)(Math.abs(nBig * sBig - nSmall * sSmall) + 1)/(double)(Math.abs(sBig - sSmall) + 1);
		
		double primeira = (double) lf/lb;
		double segunda = (double)primeira * gaps;
		double terceira = (double) segunda * number;
		double fmm = (double) terceira/100;
		
		//double fmm = (double)(((lf/lb) * (double)gaps * (number))/(double)100);
		
		return fmm;
		
	}
	
	public int calculateCross(int link, int core, int slot, int demand) {
		boolean[][] spectrum = pt.getLink(link).getSpectrum();
		// 0
		if (core == 0) {
			int n = 0;
			for (int i = slot; i < slot + demand; i++) {
				// Cores 1, 5 e 6
				if (!spectrum[1][i]) {
					n++;
				}
				if (!spectrum[5][i]) {
					n++;
				}
				if (!spectrum[6][i]) {
					n++;
				}
			}
			return n;
		} else if (core == 5) {
			int n = 0;
			for (int i = slot; i < slot + demand; i++) {
				// Cores 4, 6 e 0
				if (!spectrum[0][i]) {
					n++;
				}
				if (!spectrum[4][i]) {
					n++;
				}
				if (!spectrum[6][i]) {
					n++;
				}
			}
			return n;
		} else if (core == 6){
			int n = 0;
			for (int i = slot; i < slot + demand; i++) {
				for (int j = 0; j < 6; j++) {
					if (!spectrum[j][i]) {
						n++;
					}
				}
			}
			return n;
		} else {
			int n = 0;
			for (int i = slot; i < slot + demand; i++) {
				if (!spectrum[6][i]) {
					n++;
				}
				if (!spectrum[core-1][i]) {
					n++;
				}
				if (!spectrum[core+1][i]) {
					n++;
				}
			}
			return n;
		}
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
	
	
}
