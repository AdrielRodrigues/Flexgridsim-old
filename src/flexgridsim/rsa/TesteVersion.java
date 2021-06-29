/**
 * Backup, Routing,Modulation Level and Spectrum Assignment
 */
package flexgridsim.rsa;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.w3c.dom.Element;

import flexgridsim.FlexGridLink;
import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.Path;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.Modulations;
import flexgridsim.util.MultiGraph;
import flexgridsim.util.Spaces;
import flexgridsim.util.WeightedGraph;

/**
 * @author adriel
 *
 */
public class TesteVersion implements RSA  {
	private PhysicalTopology pt;
	private VirtualTopology vt;
	private ControlPlaneForRSA cp;
	private WeightedGraph graph;

	private int [][][][]fromTo;
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

		fromTo = getKShortestPath(graph);
	}

	/***************************************************
	 * - Calcula todos os caminhos entre os nós
	 * - Adiciona guardBand
	 * - Calcula a distância entre os pontos
	 * - Aplica a melhor modulação para o caminho
	 * - Armazena o espectro de todos os links em matriz
	 * - Calcula a Fragmentação
	 * - Calcula o Crosstalk
	 * - Aloca para os melhores espaços
	 ***************************************************/

	@Override
	public void flowArrival(Flow flow) {
		int guardBand=1;
		int demandInSlots;
		int modulation = 0;
		long idFlow = flow.getID();
		
		// Para cada caminho encontrado para o roteamento
		try {
			for (int[] nodes:fromTo[flow.getSource()][flow.getDestination()]) {
				int[] p = nodesToLink(nodes);
				
				int distance = getPhysicalDistance(p);
				modulation=Modulations.getModulationByDistance(distance);
				demandInSlots = (int) Math.ceil(flow.getRate() / (double) Modulations.getBandwidth(modulation)) + guardBand;
				
				FileWriter fStream;
				if (modulation >= 4) {
					
					try {
						File file = new File("modulation.csv");
						if(!file.exists()) {
							fStream = new FileWriter(file, true);
							fStream.append("idFlow, distance, modulation, demandInSlots");
							fStream.append("\n");
						} else {
							fStream = new FileWriter(file, true);
						}
						fStream.append(Long.toString(idFlow) + ", " + Integer.toString(distance) + ", " + Integer.toString(modulation) + "," + Integer.toString(demandInSlots));
						fStream.append("\n");
						fStream.close();
					} catch (IOException e) {
						System.out.println("Error writing the graph file");
					} catch (IndexOutOfBoundsException e){
						System.out.println("Não calculou valor");
					}
				}
			}
		}catch(NullPointerException e) {
			System.out.println(flow.getID());
		}
		return;
	}
	
	public double [] calculateFrag(boolean [][] spectrum) {
		double [] fragCores = new double[pt.getNumCores()];
		
		for (int i = 0; i < pt.getNumCores(); i++) {
			
			int lastOcu = -1;
			int lastFre = -1;
			int gaps = 0;
			int sSmall = -1;
			int sBig = -1;
			int nSmall = 0;
			int nBig = 0;
			
			boolean actual = true;
			
			for (int j = pt.getNumSlots() - 1; j >= 0; j--) {
				if (lastOcu == -1) {
					if(!spectrum[i][j]) {
						//Ultimo Usado
						lastOcu = j;
					}
				}
				if (lastFre == -1) {
					if(spectrum[i][j]) {
						//Ultimo Usado
						lastFre = j;
					}
				}
				
			}
			boolean veri = false;
			int size = 0;
			ArrayList<Integer> s = new ArrayList<Integer>();
			
			for(int j = 0; j < pt.getNumSlots(); j++) {
				if(spectrum[i][j] == true) {
					if(veri == false) {
						gaps++;
					}
					size++;
					veri = true;
				}
				if(spectrum[i][j] == false) {
					if(veri)
						s.add(size);
					veri = false;
					size = 0;
				}
				if(i == pt.getNumSlots()-1) {
					if(veri)
						s.add(size);
				}
			}
			for(int a:s) {
				if(a == sBig) {
					nBig++;
				}
				if(a == sSmall) {
					nSmall++;
				}
				//=====================//
				if(a > sBig) {
					sBig = a;
					nBig = 1;
				}
				if(a < sSmall) {
					sSmall = a;
					nSmall = 1;
				}
				//=====================//
				if(sBig == -1) {
					sBig = a;
					nBig++;
				}
				if(sSmall == -1) {
					sSmall = a;
					nSmall++;
				}			
			}
			//fdfdga
			double number = (Math.abs(nBig * sBig - nSmall * sSmall) + 1)/(Math.abs(sBig - sSmall) + 1);
			
			double fmm = (double)((lastOcu/lastFre) * gaps * (number))/100;
			
			if(lastFre == -1) { // Não tem livre
				fmm = 100;
			}
			fragCores[i] = fmm;
		}
		
		return fragCores;
	}
	
	public int calculateCross(boolean [][] spectrum, int core, int slot, int demand) {
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
	
	public Path allocateTwo (int[] links, boolean [][] spectrum, int demand) {
		ArrayList<Slot> channel = new ArrayList<Slot>();
		
		/*****************************************************************************************
		 * Calcula os espaços vazios.
		 * Tanto espaços exatos como maiores.
		 * Para evitar organização na fila, os espaços exatos são verificados e posteriormente
		 * os espaços maiores são verificados, em caso de indisponibilidade dos elementos exatos.
		 *****************************************************************************************/
		
		ArrayList<Spaces> exactSpaces = new ArrayList<Spaces>();
		ArrayList<Spaces> biggerSpaces = new ArrayList<Spaces>();
		
		// Para cada núcleo, adiciona o espaço
		for (int i = 0; i < pt.getNumCores(); i++) {
			int tamanho = 0;
			int start = -1;
			
			for (int j = 0; j < pt.getNumSlots(); j++) {
				if(spectrum[i][j]) {
					if (start == -1) {
						start = j;
					}
					tamanho++;
					if (j == pt.getNumSlots() - 1) {
						if (tamanho == demand) {
							exactSpaces.add(new Spaces(i, start, tamanho, true));
						} else if (tamanho > demand) {
							biggerSpaces.add(new Spaces(i, start, tamanho, false));
						}	
					}
				} else {
					if (tamanho == demand) {
						exactSpaces.add(new Spaces(i, start, tamanho, true));
					} else if (tamanho > demand) {
						biggerSpaces.add(new Spaces(i, start, tamanho, false));
					}
					tamanho = 0;
					start = -1;
				}
			}
		}
		
		// Armazena a fragmentação em um vetor de cada núcleo
		// TODO
		double[] fragList = calculateFrag(spectrum);
		
		Spaces atual = null;
		int cross = 999;		// Inicialização da variável
		double frag = 999.99;	// Inicialização da variável
		
		for (Spaces s:exactSpaces) {
			
			// Variável de controle
			boolean find = false;
			
			// Definir uma matriz secundária para cálculo
			boolean [][] spectrum2 = new boolean[pt.getNumCores()][pt.getNumSlots()];
			for (int i = 0; i < pt.getNumCores(); i++) {
				for (int j = 0; j < pt.getNumSlots(); j++) {
					spectrum2[i][j] = spectrum[i][j];
				}
			}
			
			// Simular alocação de slots
			for (int i = s.getSlot(); i < s.getSlot() + demand; i++) {
				spectrum2[s.getCore()][i] = false;
			}
			
			// Calcular fragmentação após a alocação
			double atualFrag = calculateFrag(spectrum2)[s.getCore()];
			
			// TODO: Talvez mudar para verificar somente os vizinhos da requisição
			int atualCross = calculateCross(spectrum2, s.getCore(), s.getSlot(), demand);
			
			if (atual == null) {// Ainda não foi encontrado um espaço disponível
				atual = s;
				cross = atualCross;
				frag = atualFrag;
			} else {
				if (atualCross < cross && (atualFrag < frag || Math.abs(cross - atualCross) < demand)) {
					atual = s;
					cross = atualCross;
					frag = atualFrag;	
				}
			}
		}
		
		if (atual == null) {
			for (Spaces s:biggerSpaces) {
				
				// Definir uma matriz secundária para cálculo
				boolean [][] spectrum2 = new boolean[pt.getNumCores()][pt.getNumSlots()];
				for (int i = 0; i < pt.getNumCores(); i++) {
					for (int j = 0; j < pt.getNumSlots(); j++) {
						spectrum2[i][j] = spectrum[i][j];
					}
				}
				for (int i = s.getSlot(); i < s.getSlot() + demand; i++) {
					spectrum2[s.getCore()][i] = false;
				}
				
				double atualFrag = calculateFrag(spectrum2)[s.getCore()];
				
				// TODO: Talvez mudar para verificar somente os vizinhos da requisição
				int atualCross = calculateCross(spectrum2, s.getCore(), s.getSlot(), demand);
				
				if (atual == null) {// Ainda não foi encontrado um espaço disponível
					atual = s;
					cross = atualCross;
					frag = atualFrag;
				} else {
					if (atualCross < cross && (atualFrag < frag || Math.abs(cross - atualCross) < demand)) {
						atual = s;
						cross = atualCross;
						frag = atualFrag;	
					}
				}
			}
		}
		
		/** TODO
		 * 
		 * Refazer a alocação
		 * 
		 **/
		
		for (int i = 0; i < pt.getNumCores(); i++) {
			for (int j = 0; j < pt.getNumSlots() - demand + 1; j++) {
				if (check(spectrum, i, j, demand)) {
					
					for (int l = 0; l < links.length; l++) {
						for (int k = j; k < j+demand; k++) {
							channel.add(new Slot(i, k, links[l]));
						}
					}
					return new Path(links, channel);
				}
			}
		}
		return null;
	}
	
	public boolean check(boolean[][] spectrum, int core, int slot, int demand) {
		for (int i = slot; i < slot + demand; i++) {
			if (!spectrum[core][i]) {
				return false;
			}
		}
		return true;
	}
	
	public int[] nodesToLink(int[] nodes) {
		int [] links = new int[nodes.length - 1];
		for (int i = 0; i < nodes.length - 1; i++) {
			links[i] = pt.getLink(nodes[i], nodes[i+1]).getID();
		}
		return links;
	}
	
	public int getPhysicalDistance(int[] links){
		if(links!=null && links.length>0){
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
		ArrayList<Integer> path = new ArrayList<Integer>();
		ArrayList<Slot> channel = new ArrayList<Slot>();
		ArrayList<Slot> channelTemp = new ArrayList<Slot>();
		int[] priority = {0,2,4,1,3,5,6};
		for(int j:priority) {
		//for (int j = 0; j < multigraph.getNumMultiedges();j++) {	//Num cores
			for (int i = 0; i < multigraph.getNumEdges(); i++) {	//Num slots left
				ArrayList<Integer> nowpath = new ArrayList<Integer>();
				nowpath=getShortestPath(multigraph.getGraph(i,j),src,dst, demand);
				if(nowpath==null||nowpath.size()<2){
					continue;
				}else{
					if(nowpath.size()<path.size()||path.isEmpty()){
						linksTemp = null;
						channelTemp.clear();
						boolean able = true;
						int core = j;
						int slot = i;

						linksTemp = new int[nowpath.size() - 1];

						for (int m = 0; m < nowpath.size() - 1; m++) {
							linksTemp[m] = pt.getLink(nowpath.get(m), nowpath.get(m + 1)).getID();
						}
						if(nowpath.isEmpty()) {
							System.out.println("a");
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
							path=nowpath;
							nowSlot=i;
							nowCore =j;
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
	
	public int[][][][] getKShortestPath(WeightedGraph G){
		int [][][][] connections = new int [G.getNumNodes()][G.getNumNodes()][][];
		KShortestPaths kShortestPaths = new KShortestPaths();
		for (int i = 0; i < G.getNumNodes(); i++) {
			for (int j = 0; j < G.getNumEdges(); j++) {
				int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(G, i, j, 17);
				if(kPaths!=null)
					connections[i][j] = kPaths;
			}
		}
		return connections;
	}
		
	public Path allocate(int[] links, int demand) {
		ArrayList<Slot> channel = new ArrayList<Slot>();

		channel = getSimilarSlotsInLinks(links,false, demand);
		if(channel!=null){
			return new Path(links, channel);
		}	
		return null;
	}
	
	public ArrayList<Slot> getSimilarSlotsInLinks(int []links, boolean sharing, int demandInSlots) {
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
	
	public int usingSameCore(int firstSlot, int lastSlot, int links[], boolean sharing){
		for (int core=0; core < pt.getNumCores(); core ++){
			if(freeSlotInAllLinks(links, firstSlot, lastSlot, core, sharing)){
				return core;
			}
		}
		return -1;
	}
	
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
		/*
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
	}*/
}
