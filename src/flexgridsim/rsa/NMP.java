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
//import flexgridsim.util.Crosstalk;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.Modulations;
import flexgridsim.util.WeightedGraph;


/**
 * The Class ModifiedShortestPath.
 * 
 * @author adriel
 */
public class NMP implements RSA {
	private PhysicalTopology pt;
	private VirtualTopology vt;
	private ControlPlaneForRSA cp;
	private WeightedGraph graph;

	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt,
			ControlPlaneForRSA cp, TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();

	}

	/**
	 * Instantiates a new modified shortest path.
	 */
	public NMP() {
	}
	@Override
	public void flowDeparture(Flow flow) {
		// TODO Auto-generated method stub
	}

	public long flowArrivalc(Flow flow) {return 1;}
	@Override
	public void flowArrival(Flow flow) {
		boolean found = false;
		int numPT = 1;
		ArrayList<Integer> demands = new ArrayList<Integer>();
		ArrayList<Path> paths = new ArrayList<Path>();
		WeightedGraph none = new WeightedGraph(graph);
		
		long id=-1;
		int guardBand=1;
		if(pt.getGrooming())
			guardBand=0;
		
		int demandInSlots = (int) (Math.ceil(flow.getRate() / (double) pt.getSlotCapacity()))+ guardBand;
		
		do {
			demands = dividir(demandInSlots, numPT);
			for(int de : demands) {
				Path path;
				path = getKShortestPath(none, flow.getSource(), flow.getDestination(),	de, false);
				if(path != null) {
					paths.add(path);
					for(int n : path.getLinks()){
						none.removeEdge(pt.getLink(n).getSource(), pt.getLink(n).getDestination());
					}
				}
			}
			if(paths.size() == numPT) {
				found = true;
			}
			numPT++;
		}while(numPT <= 2 && !found); // max number of paths
		
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
	
	/**
	 * Says whether or not two nodes belong to a path.
	 *
	 * @param path
	 * @param flow
	 * @return the boolean
	 */		
	public boolean belongs(int []linkpathprotection, Flow flow){
		boolean source=false;
		boolean destination =false;
		//flow.getSource() flow.getDestination()
		for(int i = 0; i < linkpathprotection.length; i++){ 
			if(pt.getLink(linkpathprotection[i]).getSource()==flow.getSource()||pt.getLink(linkpathprotection[i]).getDestination()==flow.getSource()){
				source =true;
			}
			if(pt.getLink(linkpathprotection[i]).getSource()==flow.getDestination()||pt.getLink(linkpathprotection[i]).getDestination()==flow.getDestination()){
				destination =true;
			}
			if(source&&destination)
				return true;
		}
		return false;
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
	 * Finds,  the list of slots to be used
	 *
	 * @param links[]
	 * @param demandInSlots
	 * @param sharing nao esta sendo usado
	 * @return the Channel@
	 */	

	
	public  ArrayList<Slot> getSimilarSlotsInLinks(int []links, boolean sharing, int demandInSlots) {
		ArrayList<Slot> channel = new ArrayList<Slot>();
		int firstSlot;
		int lastSlot;
		int core;
		for (int i = 0; i < pt.getNumSlots()-demandInSlots; i++) {
			firstSlot = i;
			lastSlot = i + demandInSlots - 1;
			core=usingSameCore(firstSlot, lastSlot, links, sharing);
		
			if(core!=-1){
				for (int j = firstSlot; j <= lastSlot; j++) {
					for (int l = 0; l < links.length; l++) {
						channel.add(new Slot(core, j, links[l] ));
					}
				}
				return channel;	
			}//else{@todo}
	
		}	
		return null;
	}
	/**
	 * Says whether or not a determined set of contiguous slots are available on the same core for a set of links.
	 *
	 * @param link
	 * @param slot
	 * @param initial slot
	 * @param final slot
	 * @param core
	 * @return the boolean
	 */	
	
	public int usingSameCore(int firstSlot, int lastSlot, int links[], boolean sharing){
		for (int core=0; core < pt.getNumCores(); core ++){
			if(freeSlotInAllLinks(links, firstSlot, lastSlot, core, sharing)){
				return core;
			}
		}
		return -1;
	}

	
/**
 * Says whether or not a determined set of contiguous slots are available on core of set of link.
 *
 * @param link
 * @param slot
 * @param initial slot
 * @param final slot
 * @param core
 * @return the boolean
 */
	
	public boolean freeSlotInAllLinks(int links[], int firstSlot, int lastSlot, int core, boolean sharing) {
		for (int j = 0; j < links.length; j++) {
			if (sharing==false){
				for (int h = firstSlot; h <= lastSlot; h++) {
					if(!pt.getLink(links[j]).getSpectrum(core, h)){
						return false;
					}
				}
			}
			if (sharing==true){
				for (int h = firstSlot; h <= lastSlot; h++) {
					if(!pt.getLink(links[j]).getSpectrumS(core, h)){
						return false;
					}
				}
			}
		}
		return true;
	}
	
	
	
	/**
	 * Array and.
	 *
	 * @param array1 the array1
	 * @param array2 the array2
	 * @return the boolean[]
	 */


	public  boolean[][] arrayAnd(boolean[][]array1, boolean[][] array2) {
		if (array1.length != array2.length) {
			throw (new IllegalArgumentException());
		}
		boolean[][] result = new boolean[array1.length][array1[0].length];
		
		for (int i = 0; i < array1.length; i++) {
			for (int j = 0; j < array1[0].length; j++) {
				result[i][j] = array1[i][j] & array2[i][j];
			}
		}
		return result;
	}

	/**
	 * Verify if the array of booleans has n contiguous true values
	 * 
	 * @param array
	 *            array to be verified
	 * @param n
	 *            number of contiguous slots
	 * @return return true in case it has n contiguous slots and false in case
	 *         it doesnt
	 */
	public boolean contiguousSlotsAvailable(boolean[][] array, int n, int ncores) {
		int j;
		for (int cores=0; cores< ncores; cores ++){
			for (int i = 0; i < array[cores].length; i++) {
				if (array[cores][i]) {
					for (j = i; j < i + n && j < array[cores].length; j++) {
						if (!array[cores][j]) {
							i = j;
							break;
						}
					}
					if (j == i + n)
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Contiguous slots available index.
	 *
	 * @param array the array
	 * @param n the n
	 * @return the int
	 */
	public static int contiguousSlotsAvailableIndex(boolean[][] array, int n, int ncores) {
		int j;
		for (int cores=0; cores< ncores; cores ++){
			for (int i = 0; i < array[cores].length; i++) {
				if (array[cores][i]) {
					for (j = i; j < i + n && j < array[cores].length; j++) {
						if (!array[cores][j]) {
							i = j;
							break;
						}
					}
					if (j == i + n)
						return i;
				}
			}
		}
		return -1;
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
		
	public int[] MSP(WeightedGraph G, int s, int demand, boolean sharing) {
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
	public Path getShortestPath(WeightedGraph G, int src, int dst, int demand, boolean overlap) {
		int x;
		int[] sp;
		int []links;
		ArrayList<Slot> channel = new ArrayList<Slot>();
		ArrayList<Integer> path = new ArrayList<Integer>();
		final int[] pred = MSP(G, src, demand, overlap);
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
		
		sp = new int[path.size()];
		for (int i = 0; i < path.size(); i++) {
			sp[i] = path.get(i);
		}
		if(sp.length<2)
			return null;
		//links do primeiro caminho
		links = new int[sp.length - 1];
		for (int j = 0; j < sp.length - 1; j++) {
			links[j] = pt.getLink(sp[j], sp[j + 1]).getID();
		}
		channel=getSimilarSlotsInLinks(links,overlap, demand);
		if(channel==null||channel.isEmpty()){
			return null;
		}	
		return new Path(links, channel);
	}
	
	
	
	public Path getKShortestPath(WeightedGraph G,int src, int dst, int demand, boolean overlap){
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(G, src, dst, 3);
		if(kPaths==null)
			return null;
		int []links;
		ArrayList<Slot> channel = new ArrayList<Slot>();
		
		for (int i = 0; i < kPaths.length; i++) {
			if (kPaths[i].length > 1){
				links = new int[kPaths[i].length - 1];
				for (int j = 0; j < kPaths[i].length - 1; j++) {
					links[j] = pt.getLink(kPaths[i][j], kPaths[i][j + 1]).getID();
				}
				channel=getSimilarSlotsInLinks(links,overlap, demand);
				if(channel!=null){
					return new Path(links, channel);
				}	
			} else {
				continue;
			}
		}
		return null;
	}
	
	/**public Path getRShortestPath(WeightedGraph G, int src, int dst, int demand, boolean overlap) {
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] lPaths = kShortestPaths.dijkstraKShortestPaths(G, src, dst, 1);
		int[] kPaths = lPaths[0];
		if(kPaths==null)
			return null;
		int []links;
		ArrayList<Slot> channel = new ArrayList<Slot>();
		
		links = new int[kPaths.length - 1];
		for (int i = 0; i < kPaths.length - 1; i++) {
			links[i] = pt.getLink(kPaths[i], kPaths[i+1]).getID();
		}
		channel=getSimilarSlotsInLinks(links,overlap, demand);
		if(channel!=null){
			return new Path(links, channel);
		}	
		
		return null;
	}/**/
	
	public Path getYenKShortestPath(WeightedGraph G,int src, int dst, int demand, boolean overlap){
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.yenKShortestPaths(G, src, dst, 3);
		int []links;
		ArrayList<Slot> channel = new ArrayList<Slot>();
		
		for (int i = 0; i < kPaths.length; i++) {
			if (kPaths[i].length > 1){
				links = new int[kPaths[i].length - 1];
				for (int j = 0; j < kPaths[i].length - 1; j++) {
					links[j] = pt.getLink(kPaths[i][j], kPaths[i][j + 1]).getID();
				}
				channel=getSimilarSlotsInLinks(links,overlap, demand);
				if(channel!=null){
					return new Path(links, channel);
				}	
			} else {
				continue;
			}
		}
		return null;
	}

	
	
	public  Path getbestPathBack(WeightedGraph G, int src, int dst, int demandInSlots, Path Path, boolean overlap) {
		WeightedGraph auxg;
		Path path;
		
		//criando graf aux
		auxg = new WeightedGraph(G.getNumNodes());
		for (int i = 0; i < G.getNumNodes(); i++) {
			for (int j = 0; j < G.getNumNodes(); j++) {
				auxg.setWeight(i,j,G.getWeight(i,j));
			}
		}
		//removendo os links do primeiro caminho, para que não haja links iguais 
		for (int i = 0; i < Path.getLinks().length; i++) {	
			auxg.removeEdge(pt.getLink(Path.getLink(i)).getSource(),pt.getLink(Path.getLink(i)).getDestination());
			auxg.removeEdge(pt.getLink(Path.getLink(i)).getDestination(),pt.getLink(Path.getLink(i)).getSource());
		}	
		
		/**/path=getKShortestPath(auxg,src, dst,demandInSlots, overlap);/*ORIGINAl*/
		/**path=getRShortestPath(auxg,src, dst,demandInSlots, overlap);/*MODIFICADO*/
		if(path==null||path.getSlotList().isEmpty()||path.getLinks().length<1){
			return null;
		}	
	
		return path;
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
