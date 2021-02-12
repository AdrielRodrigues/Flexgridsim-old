/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim.util;

import java.util.ArrayList;
import java.util.Arrays;

import flexgridsim.Path;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;


/**
 * A multigraph that has an edge for each free slot between two node.
 * 
 * @author helder //implements Cloneable
 */
public class MultiGraph {
	private PhysicalTopology pt;
	private WeightedGraph G;
	private int demand;
	protected int numNodes;
	protected int numMultiEdges;//core
	protected int numEdges;//slots n-b+1
	protected boolean[][][][] multiedges; // adjacency matrix
	protected boolean[][][][] sharedMultiedges; // adjacency matrix
		
	/**
	 * Instantiates a new multi graph.
	 */
	public MultiGraph(){

	}
	

	/**
	 * Instantiate a spectrum graph from a weighted graph the edges from the weighted graph will be
	 * transformed in N-b+1 edges, where N is the number of slots in the Physical Topology pt.
	 *
	 * @param G the weighted graph
	 * @param pt the pt
	 * @param demand the number of requested slots
	 */

	public MultiGraph(WeightedGraph G, PhysicalTopology pt, int demand) {
		this.pt = pt;
		this.G=G;
		this.demand = demand;
		this.numNodes = G.getNumNodes();
		this.numEdges = pt.getNumSlots()-demand;//+1?
		this.numMultiEdges= pt.getNumCores();
		/*grafo com 7 cores e n-b+1 arestas*/
		this.multiedges = new boolean[this.numNodes][this.numNodes][this.numMultiEdges][this.numEdges];
		this.sharedMultiedges = new boolean[this.numNodes][this.numNodes][this.numMultiEdges][this.numEdges];
		/*grafo com 1 core e n-b+1 arestas*/;
		for (int i = 0; i < G.size(); i++) {
			for (int j = 0; j < G.size(); j++) {
				for (int k = 0; k < this.numMultiEdges; k++) {
					for (int s = 0; s < this.numEdges; s++) {
						//Normal se não compartilha slot
						
						//Na construção importa que todos sejam True
						if(!pt.getOverlap()){						
							if(G.isEdge(i,j)&&i!=j&&continuosSlotsIncore(i,j,k,s)){// se de k ate pt.getNumSlots()-demand-1
								this.multiedges[i][j][k][s] = true;
								this.sharedMultiedges[i][j][k][s] = true;
							}
							else{
								this.multiedges[i][j][k][s] = false;
								this.sharedMultiedges[i][j][k][s] = false;
							}
						}
						//se compartilha o proteção difere
						else{
							if(G.isEdge(i,j)&&i!=j&&continuosSlotsIncore(i,j,k,s)){// se de k ate pt.getNumSlots()-demand-1
								this.multiedges[i][j][k][s] = true;
							}else{
								this.multiedges[i][j][k][s] = false;
							}							
							if(G.isEdge(i,j)&&i!=j&&continuosOverlapSlotsIncore(i,j,k,s)){// se de k ate pt.getNumSlots()-demand-1
								this.sharedMultiedges[i][j][k][s] = true;
							}else{
								this.sharedMultiedges[i][j][k][s] = false;
							}
							
						}
					}
				}
			}
		}

	}
	public MultiGraph(WeightedGraph G, PhysicalTopology pt, int demand, int nodes ) {
		this.pt = pt;
		this.G=G;
		this.demand =demand;
		this.numNodes = G.getNumNodes();
		this.numEdges = pt.getNumSlots()-demand;//+1?
		this.numMultiEdges= pt.getNumCores();
		/*grafo com 7 cores e n-b+1 arestas*/
		this.multiedges = new boolean[this.numNodes][this.numNodes][this.numMultiEdges][this.numEdges];
		this.sharedMultiedges = new boolean[this.numNodes][this.numNodes][this.numMultiEdges][this.numEdges];
		/*grafo com 1 core e n-b+1 arestas*/;
		for (int i = 0; i < G.size(); i++) {
			for (int j = 0; j < G.size(); j++) {
				for (int k = 0; k < this.numMultiEdges; k++) {
					for (int s = 0; s < this.numEdges; s++) {
						//Normal se não compartilha slot
						
						if(!pt.getOverlap()){						
							if(G.isEdge(i,j)&&i!=j&&continuosSlotsIncore(i,j,k,s)){// se de k ate pt.getNumSlots()-demand-1
								this.multiedges[i][j][k][s] = true;
								this.sharedMultiedges[i][j][k][s] = true;
							}
							else{
								this.multiedges[i][j][k][s] = false;
								this.sharedMultiedges[i][j][k][s] = false;
							}
						}
						//se compartilha o proteção difere
						else{
							if(G.isEdge(i,j)&&i!=j&&continuosSlotsIncore(i,j,k,s)){// se de k ate pt.getNumSlots()-demand-1
								this.multiedges[i][j][k][s] = true;
							}else{
								this.multiedges[i][j][k][s] = false;
							}							
							if(G.isEdge(i,j)&&i!=j&&continuosOverlapSlotsIncore(i,j,k,s)){// se de k ate pt.getNumSlots()-demand-1
								this.sharedMultiedges[i][j][k][s] = true;
							}else{
								this.sharedMultiedges[i][j][k][s] = false;
							}
							
						}
					}
				}
			}
		}
		
	/*	for (Path temp : path) {
			removePath(temp);
		}*/
	}
    
	/**
	 * @return the original graph
	 */
	public WeightedGraph getOriginalGraph() {
		return G;
	} 
	
	/**
	 * @return the numEdges
	 */
	public int getNumEdges() {
		return numEdges;
	}
	/**
	 * @return the multiedges
	 */
	public int getNumMultiedges() {
		return numMultiEdges;
	}
	
	public boolean getMultiedges(int source, int destination, int core, int slot) {
		return this.multiedges[source][destination][core][slot];
	}
	/**
	 * @param multiedges the multiedges to set
	 */

	/**
	 * @return the graph generated for n-b+1 edges starting in firstEdge
	 */
	public WeightedGraph getGraph(int firstEdge, int core) {
		WeightedGraph graph;
		graph = new WeightedGraph(this.numNodes);
		for (int i = 0; i < this.numNodes; i++) {
			for (int j = 0; j < this.numNodes; j++) {
				if(this.multiedges[i][j][core][firstEdge]&&i!=j){
					graph.setWeight(i,j,this.G.getWeight(i,j));	
				}else{
					graph.setWeight(i,j,0);	
				}
			}
		}
		return graph;
	}
	
	public WeightedGraph getSharedGraph(int firstEdge, int core) {
		WeightedGraph graph;
		graph = new WeightedGraph(this.numNodes);
		for (int i = 0; i < this.numNodes; i++) {
			for (int j = 0; j < this.numNodes; j++) {
				if(this.sharedMultiedges[i][j][core][firstEdge]&&i!=j){
					graph.setWeight(i,j,this.G.getWeight(i,j));	
				}else{
					graph.setWeight(i,j,0);	
				}
			}
		}
		return graph;
	}

	/**
	 * Gets n-b+1  continuos slots in the same core.
	 *
	 * @param src 
	 * 			the node src
	 * @param dst 
	 * 			the node dst
	 * @param core 
	 * 			the core
	 * @param firstEdge 
	 * 			the first edge of set to be converted for one edge
	 * @return true if the Multiedges is available and false otherwise
	 */
		
	public boolean getMultiEdge(int src, int dst, int core, int firstEdge){
		return this.multiedges[src][dst][core][firstEdge];
	}
	/**
	 * Gets vector of links .
	 *
	 * @param nodes [] 
	 * 			the links

	 * @param core 
	 * 			the core
	 * @param firstEdge 
	 * 			the first edge of set to be converted for one edge
	 * @return true if the Multiedges is available in all links and false otherwise
	 */
	public boolean availableMultiEdge(int nodes[], int core, int firstEdge){
		for (int i = 0; i <  nodes.length-1; i++) {
			//se o caminho não esta disponivel return false
			if(!this.multiedges[nodes[i]][i+1][core][firstEdge])
				return false;
		}
		return true;
	}
	
	/**
	 * Gets Path with vector of links and Slots .
	 *
	 * @param nodes [] 
	 * 
	 * @param modulation to create the path
	 * @return Path
	 * */
	
	////// EEEEEEEERRRRRRRRRRAAAAAAADOOOOO AAAQQQQQUIIII
	
	public Path  getAvailablePath(int nodes[], int modulation) {
		int links []= new int[nodes.length-1];
		System.out.print("Multi LN 255: ");
		for (int l = 0; l < nodes.length-1; l++) {
			links[l] = pt.getLink(nodes[l], nodes[l + 1]).getID();
			System.out.print("Link: "+links[l]+ ", ");
		}
		
		ArrayList<Slot> channel = new ArrayList<Slot>();
		for (int k = 0; k < this.numMultiEdges; k++) {
			for (int s = 0; s < this.numEdges; s++) {
				for (int i = 0; i <  nodes.length-1; i++) {
				//se o caminho não esta disponivel return false
					if(this.multiedges[nodes[i]][nodes[i+1]][k][s]) {
						System.out.println("Nós: "+nodes[i]+", "+ nodes[i+1]+", "+this.multiedges[nodes[i]][nodes[i+1]][k][s]);
						for (int l = 0; l < nodes.length-1; l++) {
							for (int j = s; j < s+this.demand; j++) {
								channel.add(new Slot(s, j, links[l] ));
							}
						}
						return new Path(links,  channel, modulation);	
					}
				}
			}
		}
		return null;
	}
	/**
	 * Remove slots of link in multigraph .
	 *
	 * @param path  
	 * 			That contain:
	 * 				the links
	 * 				the core
	 * 				the slots 
	 * @return void
	 */
/*	public void  removePath(Path path) {
		int firstEdge = path.getInitSlot().slot;
		int core = path.getInitSlot().core;
		for (int j = 0; j < path.getLinks().length; j++) {
			for (int i = firstEdge-demand+1; i < firstEdge+this.demand; i++) {
				//trata as pontas, ou seja os slots iniciais e finais
				if(i>=0&&i<this.numEdges){	
					this.multiedges[pt.getLink(path.getLink(j)).getSource()][pt.getLink(path.getLink(j)).getDestination()] [core][i] = false;
					//System.out.println(this.multiedges[pt.getLink(path.getLink(j)).getSource()][pt.getLink(path.getLink(j)).getDestination()] [core][i]);
				}
			}
		}
	}
	*/
	
	
	/**
	 * Gets n-b+1  continuos slots in the same core.
	 *
	 * @param src 
	 * 			the node src
	 * @param dst 
	 * 			the node dst
	 * @param multiEdge 
	 * 			the core
	 * @param firstEdge 
	 * 			the first edge of set to be converted for one edge
	 * @return true if the edges is available and false otherwise
	 */
	public boolean continuosSlotsIncore( int src, int dst, int multiEdge, int firstEdge){
		for (int i = firstEdge; i < firstEdge+this.demand; i++) {
			if(this.G.isEdge(src, dst)){
				if(!this.pt.getLink(src, dst).getSpectrum(multiEdge, i)){
					return false;
				}
			}else{
				return false;
				}
		}
		return true;	
	}
	
	public boolean continuosOverlapSlotsIncore( int src, int dst, int multiEdge, int firstEdge){
		for (int i = firstEdge; i < firstEdge+this.demand; i++) {
			if(this.G.isEdge(src, dst)){
				if(!this.pt.getLink(src, dst).getSpectrumS(multiEdge, i)){
					return false;
				}
			}else{
				return false;
				}
		}
		return true;	
	}
	
	/**
	 * Removes a given link from the graph by simply attributing zero to its
	 * source and target coordinates within the matrix of edges.
	 * 
	 * @param source
	 *            the edge's source node
	 * @param target
	 *            the edge's destination node
	 */
	public void removeEdge(int source, int target) {
		for (int k = 0; k < this.numMultiEdges; k++) {
			for (int s = 0; s < this.numEdges; s++) {
				this.multiedges[source][target] [k][s] = false;
				this.sharedMultiedges[source][target] [k][s] = false;
			}
		}
	}
	
	/**
	 * Removes continuos slots of a  given set of link from the graph
	 * @param vector of links
	 * 
	 * @param source
	 *            the edge's source node
	 * @param target
	 *            the edge's destination node
	 */
	
	
	public void removeMultiEdge(int links[], int firstEdge, int core) {
		//System.out.println("Multigraph\n");
		//o c e o s estão considerando o grafo e eu tenho q remover no multigrafo
		for (int j = 0; j < links.length; j++) {
			for (int i = firstEdge-demand+1; i < firstEdge+this.demand; i++) {
				//trata as pontas, ou seja os slots iniciais e finais
				if(i>=0&&i<this.numEdges){	
					//System.out.print(" Link:"+links[j]+ " I:"+i);
					this.multiedges[pt.getLink(links[j]).getSource()][pt.getLink(links[j]).getDestination()] [core][i] = false;
					//System.out.println(this.multiedges[pt.getLink(links[j]).getSource()][pt.getLink(links[j]).getDestination()] [core][i]);
				}
			}
		}
	}
	
	/**
	 * Array or.
	 *
	 * @param array[][] 
	 * 		the 7 cores where each edge represents the continuity of n-b+1 edges in graph
	 * @return the boolean[] that represent OR between the cores
	 */
	/*If não puder comutar o core mecher aqui*/
	public  boolean[] arrayOr(boolean[][]array) {
		if (array.length<1||array[0].length<1) {
			throw (new IllegalArgumentException());
		}
		boolean[] result = new boolean[array[0].length];
		
		for (int j = 0; j < array[0].length; j++) {
			for (int i = 0; i < array.length; i++) {
				result[j] = result[j] || array[i][j];
			}
		}
		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MultiGraph [pt=" + pt + ", G=" + G + ", demand=" + demand
				+ ", numNodes=" + numNodes + ", numMultiEdges=" + numMultiEdges
				+ ", numEdges=" + numEdges + ", multiedges="
				+ Arrays.toString(multiedges) + "]";
	}

}