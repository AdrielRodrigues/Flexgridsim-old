package flexgridsim.util;

import java.util.ArrayList;

/**
 * The Class KShortestPaths.
 */
public class KShortestPaths {
	/**
 * Yen k shortest paths.
 *
 * @param graph the graph
 * @param src the src
 * @param dst the dst
 * @param K the k
 * @return the int[][]
 */

	public int[][] yenKShortestPaths(WeightedGraph graph, int src, int dst, int K){
		ArrayList<CostedPath> A = new ArrayList<CostedPath>();
		A.add(new CostedPath(graph, Dijkstra2.getShortestPath(graph, src, dst)));
		ArrayList<CostedPath> B = new ArrayList<CostedPath>();
		ArrayList<Integer> removedNodes = new ArrayList<Integer>();
		
		for (int k = 1; k <= K; k++) {
			for (int i = 0; i <= A.get(k-1).size()-1; i++) {
				
				int spurNode = A.get(k-1).get(i);
				
				CostedPath rootPath = A.get(k-1).subpath(0, i);
				
				for (CostedPath p:A) {
					if (rootPath.equals(p.subpath(0, i))){
						graph.markEdgeRemoved(i, i+1);
					}
				}
				for (Integer rootPathNode : rootPath) {
					if (rootPathNode != spurNode){
						removedNodes.add(rootPathNode);
					}
				}
				CostedPath spurPath = new CostedPath(graph, Dijkstra2.getShortestPath(graph, spurNode, dst));
				
				CostedPath totalPath = rootPath.concat(spurPath);
				
				B.add(totalPath);
				
				graph.restoreEdgesRemoved();
				
				removedNodes.clear();
			}
			CostedPath minCost = minCost(B);
			A.add(minCost);
			B.remove(minCost);
		}
		

		int[][] kPaths = new int[K][];
		//System.out.print("KSP=");
		for (int i = 0; i < K; i++) {
			kPaths[i] = new int[A.get(i).size()];
			for (int j = 0; j < A.get(i).size(); j++) {
				kPaths[i][j] = A.get(i).get(j);
				//System.out.print(kPaths[i][j]+"-");
			}
			//System.out.println();
		}
		return kPaths;
	}
	
	/**
	 * Dijkstra k shortest paths.
	 *
	 * @param graph the graph
	 * @param s the s
	 * @param t the t
	 * @param K the k
	 * @return the int[][]
	 */
	public int[][] dijkstraKShortestPaths(WeightedGraph graph, int s, int t, int K){
		ArrayList<CostedPath> P = new ArrayList<CostedPath>();
		//ArrayList<Integer> visitedNodes = new ArrayList<Integer>();
		int count[] = new int[graph.getNumNodes()]; 
		ArrayList<CostedPath> B = new ArrayList<CostedPath>();
		CostedPath Ps = new CostedPath(graph);
		Ps.add(s);
		//visitedNodes.add(s);
		B.add(Ps);
		while (!B.isEmpty() && count[t] < K){
//			while (P.contains(minCost(B))) {
	//			B.remove(minCost(B));
		//	}
			CostedPath Pu = minCost(B);
			int u = Pu.get(Pu.size()-1);
			B.remove(Pu);
			count[u]++;
			if (u==t){
				P.add(Pu);
			} /**/else {/**/
				if (count[u]<=K){
					final int[] n = graph.neighbors(u);
					for (int v : n) {
						/**/if(!Pu.contains(v)) {/**/
							CostedPath Pv = new CostedPath(graph, Pu);
							Pv.add(v);
							B.add(Pv);
						/**/}/**/
					}
				}
			/**/}/**/
		}
		/**if(P.isEmpty())
			return null;/*Original*/
                
        /**/if(P.size()<K)
            return null;/*Modificação*/
                
                int[][] kPaths = new int[K][];
//		System.out.print("KSP=");
                
		for (int i = 0; i < K; i++) {
			kPaths[i] = new int[P.get(i).size()];
			for (int j = 0; j < P.get(i).size(); j++) {
				kPaths[i][j] = P.get(i).get(j);
//				System.out.print(kPaths[i][j]+"-");
			}
//			System.out.println();
		}
		return kPaths;
	}
	
	
	/**
	 * Min cost.
	 *
	 * @param paths the paths
	 * @return the k path
	 */
	public static CostedPath minCost(ArrayList<CostedPath> paths){
		if (paths.isEmpty()){
			return null;
		}
		CostedPath minCost = paths.get(0);
		for (CostedPath p : paths) {
			if (p.getCost()<minCost.getCost()){
				minCost = p;
			}
		}
		return minCost;
	}
	/**
	 * The Class KPath.
	 */
	class CostedPath extends ArrayList<Integer>{
		/**
		 * 
		 */
		private static final long serialVersionUID = -3721778618427546839L;
		int cost;
		WeightedGraph graph;
		public CostedPath(WeightedGraph graph){
			super();
			this.graph = graph;
		}
		public CostedPath(WeightedGraph graph, CostedPath path){
			super();
			this.graph = graph;
			cost = 0;
			for (Integer u : path) {
				this.add(u);
			}
		}
		public CostedPath(WeightedGraph graph, int[] path){
			super();
			this.graph = graph;
			for (Integer u : path) {
				this.add(u);
			}
		}
		public int getCost(){
			return cost;
		}
		public boolean add(Integer v){
			if (this.isEmpty()){
				super.add(v);
			} else {
				Integer u = this.get(this.size()-1);
				cost += graph.getWeight(u, v);
				super.add(v);
			}
			return true;
		}
		public boolean equals(CostedPath p){
			if (this.isEmpty() || p.isEmpty()){
				return false;
			}
			if (this.size()!=p.size()){
				return false;
			}
			for (int i = 0; i < this.size(); i++) {
				if (this.get(i)!=p.get(i)){
					return false;
				}
			}
			return true;
		}
		public CostedPath subpath(int i, int j){
			CostedPath subpath = new CostedPath(graph);
			for (int k = i; k < j; k++) {
				subpath.add(this.get(k));
			}
			return subpath;
		}
		
		public CostedPath concat(CostedPath p){
			CostedPath result = new CostedPath(graph, this);
			for (Integer u : p) {
				result.add(u);
			}
			return result;
		}
		public void print(){
			for (Integer i : this) {
				System.out.print(i+",");
			}
			System.out.println();
		}
	}
	
}
