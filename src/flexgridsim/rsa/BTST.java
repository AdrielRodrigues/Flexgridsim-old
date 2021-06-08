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
public class BTST implements RSA  {
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
		
		int demandInSlots;
		Path path;
		
		ArrayList<Path> paths = new ArrayList<Path>();
		ArrayList<Integer> modulations = new ArrayList<Integer>();
		boolean mp = false;
		int mod = 5;
		int modulation = 0;
		do {
			//int d = (int) Math.ceil((flow.getRate()) / (double) Modulations.getBandwidth(mod));
			//d = (int) Math.ceil(d/2);
			demandInSlots = (int) Math.ceil((flow.getRate() / 2) / (double) Modulations.getBandwidth(mod)) + guardBand;
			MultiGraph multigraph = new MultiGraph(graph, pt, demandInSlots);
			path = getShortestPath(multigraph, flow.getSource(), flow.getDestination(),demandInSlots, mod);
			if(path!=null)
				modulation=Modulations.getModulationByDistance(getPhysicalDistance(path.getLinks()));
			else
				modulation =-1;
			mod--;
		}while(mod>-1 &&modulation!=-1 && modulation!=mod+1);
		
		if(path != null) {
			paths.add(path);
			modulations.add(modulation);
		}
		
		if(paths.isEmpty()) {
			mp = true;
			WeightedGraph g = new WeightedGraph(graph);
			for(int r = 0; r < 2; r++) { //Numero de caminhos
					mod = 5;
					modulation = 0;
					do {
						//int d = (int) Math.ceil((flow.getRate()) / (double) Modulations.getBandwidth(mod));
						//d = (int) Math.ceil(d/2);
						demandInSlots = (int) Math.ceil((flow.getRate() / 2) / (double) Modulations.getBandwidth(mod)) + guardBand;
						MultiGraph multigraph = new MultiGraph(g, pt, demandInSlots);
						path = getShortestPath(multigraph, flow.getSource(), flow.getDestination(),demandInSlots, mod);
						if(path!=null)
							modulation=Modulations.getModulationByDistance(getPhysicalDistance(path.getLinks()));
						else
							modulation =-1;
						mod--;
					}while(mod>-1 &&modulation!=-1 && modulation!=mod+1);
					
					if(path != null) {
						paths.add(path);
						modulations.add(modulation);
						int[] l = path.getLinks();
						for(int ll : l) {
							int s = pt.getLink(ll).getSource();
							int d = pt.getLink(ll).getDestination();
							g.removeEdge(s, d);
							g.removeEdge(d, s);
						}
					}
					if(paths.isEmpty())
						break;
				}
		}
		
		if(paths.isEmpty()) {
			cp.blockFlow(flow.getID());
			return;
		}
		if(paths.size() != 2 && mp) {
			cp.blockFlow(flow.getID());
			return;
		}
		if(paths.size() == 2) {
			int[] a = paths.get(0).getLinks();
			int[] b = paths.get(1).getLinks();
			int da = 0;
			int db = 0;
			for(int n:a) {
				da += pt.getLink(n).getDistance();
			}
			for(int n:b) {
				db += pt.getLink(n).getDistance();
			}
			int dc = da-db;
			//System.out.println("Working");
			if(Math.abs(dc) > 3000) {
				cp.blockFlow(flow.getID());
				return;
			}
		}
		/*
		if (path == null && !mp) {
			cp.blockFlow(flow.getID());
			return;
		} else if(!mp){ 
			if (path.getLinks()==null||path.getSlotList().isEmpty()) {
				cp.blockFlow(flow.getID());
				return;
			}
		} else if(mp) {
			for(Path pat : paths) {
				if (pat.getLinks()==null||pat.getSlotList().isEmpty()) {
					cp.blockFlow(flow.getID());
					return;
				}
			}
		}
		*/
		//if(mp) {
			
			ArrayList<Long> ids = new ArrayList<Long>();
			for(int r = 0; r < paths.size(); r++) {
				ids.add(vt.createLightpath(paths.get(r), Modulations.getModulationLevel(modulations.get(r))));
			}
			
			ArrayList<LightPath> lightpath = new ArrayList<LightPath>();
			
			int numb = 0;
			boolean error = false;
			for(Path pat : paths) {
				if (ids.get(numb) >= 0) {
					flow.setLinks(pat.getLinks(), numb);
					flow.setSlotList(pat.getSlotList(), numb);
					flow.setModulationLevel(modulations.get(numb));
					lightpath.add(vt.getLightpath(ids.get(numb)));
				} else {
					error = true;
				}
				numb++;
			}
			
			if(error) {
				cp.blockFlow(flow.getID());
			}
			
			if(!cp.acceptFlow(flow.getID(), lightpath)) {
				for(Long i : ids) {
					vt.removeLightPath(i);
				}
				cp.blockFlow(flow.getID());
				return;
			}
			return;
		}
	//}

	
	
	
	
	
	
	
	
	
	public int getPhysicalDistance(int[] links){
		if(links!=null&& links.length>0){
			int physicalDistance = 0;
			for (int i = 0; i < links.length - 1; i++) {
				physicalDistance += pt.getLink(links[i]).getDistance();
			}
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

	public Path getShortestPath(MultiGraph multigraph, int src, int dst, int demand, int modulationLevel) {
		int links[];
		int linksTemp[];
		int nowSlot=-1;
		int nowCore=-1;;
		//
		double frag = -1;
		//
		ArrayList<Integer> path = new ArrayList<Integer>();
		ArrayList<Slot> channel = new ArrayList<Slot>();
		ArrayList<Slot> channelTemp = new ArrayList<Slot>();
		//int[] priority = {0,2,4,1,3,5,6};
		//for(int j:priority) {
		for (int j = 0; j < multigraph.getNumMultiedges();j++) {	//Num cores
			for (int i = 0; i < multigraph.getNumEdges(); i++) {	//Num slots left
				ArrayList<Integer> nowpath = new ArrayList<Integer>();
				nowpath=getShortestPath(multigraph.getGraph(i,j),src,dst, demand);
				if(nowpath==null||nowpath.size()<2){
					continue;
				}else{
					if(nowpath.size()<=path.size()||path.isEmpty()){
						
						if(nowpath.size()==path.size() && !path.isEmpty()) {
							double fragt = -1;
							int core = j;
							int slot = i;
							linksTemp = null;
							linksTemp = new int[nowpath.size() - 1];
							boolean [] calculo = new boolean[320];
							for (int m = 0; m < nowpath.size() - 1; m++) {
								linksTemp[m] = pt.getLink(nowpath.get(m), nowpath.get(m + 1)).getID();
							}
							int a = 1;
							for(int lk:linksTemp) {
								boolean[] sl = new boolean[320]; 
								boolean[] sa = pt.getLink(lk).getSpectrum(core);
								for(int n = 0; n < 320; n++) {
									if(sa[n])
										sl[n] = true;
									else
										sl[n] = false;
								}
								
								if(a == 1) {
									calculo = sl;
								} else {
									for(int b = 0; b < 320; b++) {
										
										calculo[b] = calculo[b] && sl[b];
									}
								}
								a++;
							}
							fragt = fragmentation(calculo);
							
							if(fragt<=frag) {
								continue;
							} else {
								int v = 10;
							}
						}
						////////////////
						linksTemp = null;
						channelTemp.clear();
						boolean able = true;
						double fragt = -1;
						int core = j;
						int slot = i;

						linksTemp = new int[nowpath.size() - 1];
						
						//
						boolean [] calculo = new boolean[320];
						//

						for (int m = 0; m < nowpath.size() - 1; m++) {
							linksTemp[m] = pt.getLink(nowpath.get(m), nowpath.get(m + 1)).getID();
						}
						/*
						for (int l = 0; l < linksTemp.length; l++) {
							for (int m = slot; m < slot+demand; m++) {
								channelTemp.add(new Slot(core, m, linksTemp[l] ));
							}
						}*/
						
						if(!crosstalkCheck(linksTemp, core, slot, demand, modulationLevel)) {
							able = false;
						}
						
						if(able) {
							int a = 1;
							for(int lk:linksTemp) {
								boolean[] sl = new boolean[320]; 
								boolean[] sa = pt.getLink(lk).getSpectrum(core);
								for(int n = 0; n < 320; n++) {
									if(sa[n])
										sl[n] = true;
									else
										sl[n] = false;
								}
								
								if(a == 1) {
									calculo = sl;
								} else {
									for(int b = 0; b < 320; b++) {
										
										calculo[b] = calculo[b] && sl[b];
									}
								}
								a++;
							}
							fragt = fragmentation(calculo);
						}
						
						if(able) {
							path=nowpath;
							nowSlot=i;
							nowCore =j;
							frag = fragt;
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
	/*
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
	
*/	
	public boolean crosstalkCheck(int[] links, int core, int slot, int demand, int modulation) {
		for(int l:links) {
			//boolean[] dispo = pt.getLink(l).getAllocableSpectrum(modulation, 2, core);
			for(int i=slot; i<slot+demand; i++) {
				//if(!dispo[i]) {
					//return false;
				//}
			}
		}
		return true;
		
	}
	
	public double fragmentation(boolean[] calculo) {
		double lffs = 0;
		double lbfs = 0;
		int n_big = 0;
		int size_big = -1;
		int n_small = 0;
		int size_small = -1;
		int gaps = 0;
		int size = 0;
		boolean veri = false;
		ArrayList<Integer> s = new ArrayList<Integer>();
		
		for(int i = 0; i < 320; i++) {
			if(calculo[i] == true) {
				lbfs = i+1;
				if(veri == false) {
					gaps++;
				}
				size++;
				veri = true;
			}
			if(calculo[i] == false) {
				lffs = i+1;
				if(veri)
					s.add(size);
				veri = false;
				size = 0;
			}
			if(i == 319) {
				if(veri)
					s.add(size);
			}
		}
		
		for(int a:s) {
			if(a == size_big) {
				n_big++;
			}
			if(a == size_small) {
				n_small++;
			}
			//=====================//
			if(a > size_big) {
				size_big = a;
				n_big = 1;
			}
			if(a < size_small) {
				size_small = a;
				n_small = 1;
			}
			//=====================//
			if(size_big == -1) {
				size_big = a;
				n_big++;
			}
			if(size_small == -1) {
				size_small = a;
				n_small++;
			}			
		}
		
		double number = (Math.abs(n_big * size_big - n_small * size_small) + 1)/(Math.abs(size_big - size_small) + 1);
		
		double fmm = (double)((lffs/lbfs) * gaps * (number))/100;
		
		if(lbfs == 0) {
			fmm = 100;
		}
		
		return fmm;
	}
}

