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
import flexgridsim.util.WeightedGraph;


/**
 * The Class ModifiedShortestPath.
 * 
 * @author Helder
 */
public class CaPDPPM implements RSA {
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
	public CaPDPPM() {
	}
	@Override
	public void flowDeparture(Flow flow) {
		// TODO Auto-generated method stub
		
	}

	public long flowArrivalc(Flow flow) {return 1;}
	@Override
	public void flowArrival(Flow flow) {
		long id=-1;
		long idc;
		int guardBand=1;
		if(pt.getGrooming())
			guardBand=0;
		int demandInSlots = (int) (Math.ceil(flow.getRate() / (double) pt.getSlotCapacity()))+ guardBand;
		Path path, pathBack;
		
		int mod=5;
		int modulation=0;
		do{
			demandInSlots = (int) Math.ceil(flow.getRate() / (double) Modulations.getBandwidth(mod)) + guardBand;
			path = getShortestPath(graph, flow.getSource(), flow.getDestination(),	demandInSlots,false);
			if(path!=null)
				modulation=Modulations.getModulationByDistance(getPhysicalDistance(path.getLinks()));
			else
				modulation =-1;
			mod--;
		 }while(mod>-1 &&modulation!=-1 && modulation!=mod+1);
		
		
		
		// If no possible path found, block the call
		if (path == null) {
			cp.blockFlow(flow.getID());
			return;
		} else if (path.getLinks()==null||path.getSlotList().isEmpty()) {
			cp.blockFlow(flow.getID());
			return;
		}

		//id = vt.createLightpath(path, Modulations.getModulationLevel(pt.getSlotCapacity()));
		id = vt.createLightpath(path, modulation);
		//se não achar caminho de proteção lembrar de remover lightpath
		if (id >= 0) {
			flow.setLinks(path.getLinks());
			flow.setSlotList(path.getSlotList());
			flow.setModulationLevel(modulation);
			ArrayList<LightPath> lightpath = new ArrayList<LightPath>(); 
			lightpath.add(vt.getLightpath(id));
			if(!cp.acceptFlow(flow.getID(), lightpath)) {
				vt.removeLightPath(id);
				cp.blockFlow(flow.getID());
				return;
			}
			return;
		}	
		if(id<0){
			cp.blockFlow(flow.getID());
			return;
		}
	}
		
		//created a new p-cycle of protection		
/*
		int modback=5;
		int modulationback=0;
		int demandInSlotsback;
		
		
		
		do{
			demandInSlotsback = (int) Math.ceil(flow.getRate() / (double) Modulations.getBandwidth(modback)) + guardBand;
			path = getbestPathBack(graph, flow.getSource(), flow.getDestination(),	demandInSlotsback,path,false);
			if(path!=null)
				modulationback=Modulations.getModulationByDistance(getPhysicalDistance(path.getLinks()));
			else
				modulationback =-1;
			modback--;
		    }while(modback>-1 &&modulationback!=-1 && modulationback!=modback+1); 

		pathBack=getbestPathBack(graph, flow.getSource(), flow.getDestination(),demandInSlots, path, false);
		
		
		
		if (pathBack == null || pathBack.getLinks().length<1|| pathBack.getLinks()==null||pathBack.getSlotList()==null) {
			vt.removeLightPath(id);
			cp.blockFlow(flow.getID());
			return;
		} */
		/* OBS: todas os que usam poteção deve alocar a proteção com o sharing true ou false*/
/*
		idc = vt.createLightpathProtection(pathBack, Modulations.getModulationLevel(pt.getSlotCapacity()));
		if (idc >= 0) {
			flow.setLinksp(pathBack.getLinks());
			flow.setSlotListp(pathBack.getSlotList());
			vt.getLightpath(idc).setProtection();
			ArrayList<LightPath> lightpath = new ArrayList<LightPath>(); 
			lightpath.add(vt.getLightpath(id));
			ArrayList<LightPath> lightpathp = new ArrayList<LightPath>(); 
			lightpathp.add(vt.getLightpath(idc));
			cp.acceptFlow(flow.getID(),lightpath, lightpathp);
*/		
			//System.out.println("\n\n\n\n\n");
			/*return;
		}
		vt.removeLightPath(id);
		cp.blockFlow(flow.getID());
	}*/

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
	 * Finds,  the list of slots to be used
	 *
	 * @param links[]
	 * @param demandInSlots
	 * @param sharing nao esta sendo usado
	 * @return the Channel@
	 */	
	
	public  ArrayList<Slot> getSimilarSlotsInLinks(int []links, boolean sharing, int demandInSlots) {
		
		int firstSlot;
		int lastSlot;
		int core;
		for (int i = 0; i < pt.getNumSlots()-demandInSlots; i++) {
			firstSlot = i;
			lastSlot = i + demandInSlots - 1;
			core=usingSameCore(firstSlot, lastSlot, links, sharing);
		
			if(core!=-1){
				ArrayList<Slot> channel = new ArrayList<Slot>();
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
		for (int core=0; core< pt.getNumCores(); core ++){
			if(freeSlotInAllLinks(links, firstSlot, lastSlot, core, sharing)){
				return core;
			}
		}
		return -1;
	}

	public int usingDiferentCore(int firstSlot, int lastSlot, int links[], boolean sharing){
		/*for (int core=0; core< pt.getNumCores(); core ++){
			if(freeSlotInAllLinks(links, firstSlot, lastSlot, core, sharing)){
				return core;
			}
		}*/
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
	public static boolean[] arrayAnd(boolean[] array1, boolean[] array2) {
		if (array1.length != array2.length) {
			throw (new IllegalArgumentException());
		}
		boolean[] result = new boolean[array1.length];
		for (int i = 0; i < array1.length; i++) {
			result[i] = array1[i] & array2[i];
		}
		return result;
	}

	public static boolean[][] arrayAnd(boolean[][]array1, boolean[][] array2) {
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
	public static boolean contiguousSlotsAvailable(boolean[][] array, int n, int ncores) {
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
		if(Path==null)
			return null;
		//removendo os links do primeiro caminho, para que não haja links iguais 
		for (int i = 1; i < Path.getLinks().length; i++) {	
			auxg.removeEdge(pt.getLink(Path.getLink(i)).getSource(),pt.getLink(Path.getLink(i)).getDestination());
			auxg.removeEdge(pt.getLink(Path.getLink(i)).getDestination(),pt.getLink(Path.getLink(i)).getSource());
		}	
		

		path=getShortestPath(auxg,src, dst,demandInSlots, overlap);
		if(path==null||path.getSlotList().isEmpty()||path.getLinks().length<1){
			return null;
		}	
	
		return path;
	}


		
	
}
