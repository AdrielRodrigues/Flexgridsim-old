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
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.Modulations;
import flexgridsim.util.WeightedGraph;


/**
 * The Class ModifiedShortestPath.
 * 
 * @author Helder
 */
public class min_frag implements RSA {
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
	public min_frag() {
	}
	@Override
	public void flowDeparture(Flow flow) {
		// TODO Auto-generated method stub
	}

	public long flowArrivalc(Flow flow) {return 1;}
	@Override
	public void flowArrival(Flow flow) {
		long id=-1;
		int guardBand=1;
		
		Path path;
		
		int mod=5;
		int modulation=0;
		do {			
			int demandInSlots = (int) Math.ceil(flow.getRate() / (double) Modulations.getBandwidth(mod)) + guardBand;
			path = getKShortestPath(graph, flow.getSource(), flow.getDestination(),	demandInSlots,false);
			if(path!=null)
				modulation=Modulations.getModulationByDistance(getPhysicalDistance(path.getLinks()));
			else
				modulation =-1;
			mod--;
		}while(mod>-1 &&modulation!=-1 && modulation!=mod+1);
		
		/*
		int demandInSlots = (int) (Math.ceil(flow.getRate() / (double) pt.getSlotCapacity()))+ guardBand;
		Path path;
		path = getKShortestPath(graph, flow.getSource(), flow.getDestination(),	demandInSlots,false);
		*/
		// If no possible path found, block the call
		if (path == null) {
			cp.blockFlow(flow.getID());
			return;
		} else if (path.getLinks() == null || path.getSlotList().isEmpty()) {
			cp.blockFlow(flow.getID());
			return;
		}
	
		//id = vt.createLightpath(path, Modulations.getModulationLevel(pt.getSlotCapacity()));
	id = vt.createLightpath(path, modulation);
		
		if (id >= 0) {
			flow.setLinks(path.getLinks());
			flow.setSlotList(path.getSlotList());
			ArrayList<LightPath> lightpath = new ArrayList<>(); 
			lightpath.add(vt.getLightpath(id));
			//flow.setModulationLevel(Modulations.getModulationLevel(pt.getSlotCapacity()));
			flow.setModulationLevel(modulation);
			
			if(!cp.acceptFlow(flow.getID(), lightpath)) {
				cp.blockFlow(flow.getID());
			}
			return;
		}	
		if(id<0){
			cp.blockFlow(flow.getID());
			vt.removeLightPath(id);
			return;
		}
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
				
				channel = allocate(links, demand);
				if(channel!=null){
					return new Path(links, channel);
				}	
			} else {
				continue;
			}
		}
		return null;
	}
	
	public ArrayList<Slot> allocate(int[] path, int demandInSlots) {
		int[] cores = {0, 1, 2, 3, 4, 5, 6};
		double[] cps = new double[7];
		double[] frag = new double[7];
		
		for(int core:cores) {
			frag[core] = fragmentation_metric(demandInSlots, core, path);
		}
		
		int[] cores_sort = ordenar(frag);
		
		for(int core:cores_sort) {
			cps[core] = cps(core, path);
		}
		
		//COMPARA FRAGMENTATION
		ArrayList<int[]> EF = new ArrayList<int[]>();
		
		for(int i = 0; i < cores_sort.length; i++) {
			if(i == 6) {
				//Try to fit
				int n = findExactFit(demandInSlots, cores_sort[i], path);
				if(n != -1 && n+demandInSlots<=320) {
					int[] add = {cores_sort[i], n};
					EF.add(add);
					continue;
				}
			} else {
				if(cps(cores_sort[i], path) >= cps(cores_sort[i+1], path)) {
					int n = findExactFit(demandInSlots, cores_sort[i+1], path);
					if(n != -1 && n+demandInSlots<=320) {
						int[] add = {cores_sort[i+1], n};
						EF.add(add);
						continue;
					}
					else {
						n = findExactFit(demandInSlots, cores_sort[i], path);
						if(n != -1 && n+demandInSlots<=320) {
							int[] add = {cores_sort[i], n};
							EF.add(add);
							continue;
						}
					}
				} else {
					int n = findExactFit(demandInSlots, cores_sort[i], path);
					if(n != -1 && n+demandInSlots<=320) {
						int[] add = {cores_sort[i], n};
						EF.add(add);
						continue;
					}
					else {
						n = findExactFit(demandInSlots, cores_sort[i+1], path);
						if(n != -1 && n+demandInSlots<=320) {
							int[] add = {cores_sort[i+1], n};
							EF.add(add);
							continue;
						}
					}
				}
			}
		}
		
		for(int[] Exact:EF) {
			double cps2 = cps2(Exact[0], path, Exact[1], demandInSlots);
			if(cps2 <= cps[Exact[0]]) {
				ArrayList<Slot> channel = new ArrayList<Slot>();
				for(int link:path) {
					for(int s = Exact[1]; s < Exact[1] + demandInSlots; s++) {
						channel.add(new Slot(Exact[0], s, link));
					}
				}
				return channel;
			}
		}
		return null;
	}


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
	
	
	
	
	
	
public int findExactFit(int demandInSlots, int core, int[] path) {
		
		boolean[] spectrum = new boolean[pt.getNumSlots()];
		
		int number_hops = 0;

		for(int link:path) {
			int s = 0;
			for(boolean fs:pt.getLink(link).getSpectrum(core)) {
				if(number_hops == 0) {
					spectrum[s] = fs;
				} else {
					spectrum[s] = spectrum[s] && fs;
				}
				s++;
			}
			number_hops++;
		}
		
		int counter = 0;
		int first = -1;
		
		ArrayList<int[]> index = new ArrayList<int[]>();
		
		for(int i = 0; i < pt.getNumSlots(); i++) {
			if(spectrum[i]) {
				if(counter == 0)
					first = i;
				counter++;
			} else {
				if(counter == demandInSlots) {
					return first;
				} else {
					int[] lista = new int[2];
					lista[0] = first; lista[1] = counter;
					index.add(lista);
					counter = 0;
				}
			}
			if(i == pt.getNumSlots() - 1) {
				if(counter == demandInSlots)
					return first;
				else {
					int[] lista = new int[2];
					lista[0] = first; lista[1] = counter;
					index.add(lista);
				}				
			}
		}
		
		for(int[] l:index) {
			if(l[1] >= demandInSlots) {
				return l[0];
			}
		}
		
		return -1;
	}

	public int[] ordenar(double[] aocs) {
		int[] cores = {0,1,2,3,4,5,6};
			
		for(int i = 0; i < 6; i++) {
			for(int j = i+1; j < 7; j++) {
				if(aocs[j] < aocs[i]) {
					int aux = cores[i];
					cores[i] = cores[j];
					cores[j] = aux;
				}
			}
		}
		
		return cores;
	}
	
	public double cps(int core1, int[] links) {
		int aoc = 0;
		int numFreeSlots = 0;
		for(int link:links) {
			boolean[][] spectrum = pt.getLink(link).getSpectrum();
			//Only works for 7-core fiber
			for(int i=0; i<pt.getNumSlots(); i++) {
				if(!spectrum[core1][i]) {
					if(core1==6) {
						if(!spectrum[0][i]) {
							aoc++;
						}
					} else {
						if(!spectrum[core1+1][i]) {
							aoc++;
						}
					}
				}
				if(core1!=6) {
					if(spectrum[core1][i])
						numFreeSlots++;
					if(spectrum[core1+1][i])
						numFreeSlots++;
				} else {
					if(spectrum[core1][i])
						numFreeSlots++;
					if(spectrum[0][i])
						numFreeSlots++;
				}
			}
		}
		int usedSlots = links.length * pt.getNumSlots() * 2 - numFreeSlots;
		if(usedSlots == 0)
			return 0;
		else
			return (double)aoc/usedSlots;
	}
	public double cps2(int core1, int[] links, int first, int demand) {
		int aoc = 0;
		int numFreeSlots = 0;
		for(int link:links) {
			boolean[][] sp = pt.getLink(link).getSpectrum();
			boolean[][] spectrum = new boolean[pt.getNumCores()][pt.getNumSlots()];
			for(int i = 0; i < pt.getNumCores(); i++) {
				for(int j = 0; j < pt.getNumSlots(); j++) {
					if(sp[i][j])
						spectrum[i][j] = true;
					else
						spectrum[i][j] = false;
				}
			}
			for(int i = 0; i < demand; i++) {
				spectrum[core1][first+i] = false;
			}
			//Only works for 7-core fiber
			for(int i=0; i<pt.getNumSlots(); i++) {
				if(!spectrum[core1][i]) {
					if(core1==6) {
						if(!spectrum[0][i]) {
							aoc++;
						}
					} else {
						if(!spectrum[core1+1][i]) {
							aoc++;
						}
					}
				}
				if(core1!=6) {
					if(spectrum[core1][i])
						numFreeSlots++;
					if(spectrum[core1+1][i])
						numFreeSlots++;
				} else {
					if(spectrum[core1][i])
						numFreeSlots++;
					if(spectrum[0][i])
						numFreeSlots++;
				}
			}
		}
		int usedSlots = links.length * pt.getNumSlots() * 2 - numFreeSlots;
		return (double)aoc/usedSlots;
	}
	
	public double fragmentation_metric(int demand, int core, int[] links) {
		boolean[] spectrum = new boolean[pt.getNumSlots()];
		for(int i = 0; i < pt.getNumSlots(); i++)
				spectrum[i] = true;
		for(int link:links) {
			boolean[] spc = pt.getLink(link).getSpectrum(core);
			
			for(int i = 0; i < pt.getNumSlots(); i++) {
				spectrum[i] = spectrum[i] && spc[i];
			}
		}
		int counter = 0;
		int n = 0;
		int free = 0;
		for(int i = 0; i < pt.getNumSlots(); i++) {
			if(spectrum[i]) {
				free++;
				counter++;
				if(i == pt.getNumSlots()-1) {
					if(counter >= demand) {
						n++;
					}
				}
			} else {
				if(counter >= demand) {
					n++;
					counter = 0;
				}
			}
		}
		return (double)(demand * n)/(free);
	}
	public int getPhysicalDistance(int[] links){
		if(links!=null&& links.length>0){
			int physicalDistance = 0;
			for (int i = 0; i < links.length; i++) {
				physicalDistance += pt.getLink(links[i]).getDistance();
			}
			return physicalDistance;
		}
		else
			return -1;
	}
}
