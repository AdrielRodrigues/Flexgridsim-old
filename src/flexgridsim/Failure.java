package flexgridsim;
import java.util.ArrayList;
//import flexgridsim.util.Distribution;
/**
 * @author
 * 
 */

public class Failure {
	
	private long id;
	private int src, dst;
	private ArrayList<Integer> linkFail = new ArrayList<Integer>(); // Because links are represented by a integer but it also is a pair of links in a linkVector
	private ArrayList<Flow> flowsFail = new ArrayList<Flow>(); // Its one array per failure
	private PhysicalTopology pt;
	private boolean yesorno; // Think about how to include this as a option on the algorithm
	
	 /**
	 * @param pt
	 * 
	 */
	
	public Failure(long id, PhysicalTopology pt, int seed, int src, int dst) {
		this.id = id; // Identification of a Flow/Failure
		this.pt = pt;
		this.yesorno = true;
		
		this.src = src;
		this.dst = dst;
		
		for(int i = 0; i < pt.getNumLinks(); i++) {
			if(pt.getLink(i).getSource() == src && pt.getLink(i).getDestination() == dst
					|| pt.getLink(i).getSource() == dst && pt.getLink(i).getDestination() == src) {
				this.linkFail.add(i); // There is a pair of links that represents only one in a topology
			}
		}
	}

	public long getID() {
		return this.id;
	}

	public boolean generateFailure() {
		
		if(pt.getLink(src, dst).isLinkActive()) {
			for(int i=0; i<this.linkFail.size(); i++) {
				pt.getLink((Integer)this.linkFail.get(i)).setLinkAtivo(false); // LinkVector
				pt.getLink((Integer)this.linkFail.get(i)).setWhichBlocked(this.id);
			}
			
			//pt.getLink(src, dst).setLinkAtivo(false);
			//pt.getLink(dst, src).setLinkAtivo(false);
			if(yesorno) {
				pt.removeGraph(src, dst);
				pt.removeGraph(dst, src);
			}

			return true;
		}
		
		return false;
		
		/*pt.getLink(src, dst).setLinkAtivo(false); // It's not really necessary actually
		pt.getLink(dst, src).setLinkAtivo(false);

		for(int i = 0; i < pt.getNumLinks(); i++) {
			if(pt.getLink(i).getSource() == src && pt.getLink(i).getDestination() == dst
					|| pt.getLink(i).getSource() == dst && pt.getLink(i).getDestination() == src) {
				pt.getLink(i).setLinkAtivo(false);
				this.linkFail.add(i);
			}
		}*/
	}

	// Before I'll have already checked
	public void removeFailure() {
		pt.getLink(src, dst).setLinkAtivo(true);
		pt.getLink(dst, src).setLinkAtivo(true);
		
		for(int i=0; i<this.linkFail.size()-1; i++) {//fazer esse for pra reestabelecer o link
			pt.getLink(i).setLinkAtivo(true);
		}
		
		if(yesorno) {
			pt.fixGraph(src, dst);
			pt.fixGraph(dst, src);
		}
		
	}
	
	public ArrayList<Integer> getLinkFail() {
		return linkFail;
	}
	
	public void addFlowsFailed(Flow flow){
		this.flowsFail.add(flow);
		//System.out.println("F");
	}
	public void removeFlowsFailed(Flow flow) {
		this.flowsFail.remove(flow);
	}
	
	
	/**
	 * It will say if I need to find another path or I just want this to be blocked
	 * */
	
	/*
	public void removeLinkGraph() {
		pt.removeGraph(src, dst);
		pt.removeGraph(dst, src);
	}
	
	public void insLinkGraph() {
		pt.fixGraph(src, dst);
		pt.fixGraph(dst, src);
	}*/
	
	public void showFlowsFailed() {
		//System.out.println("====== FLOWS-FAIL ======");
		for(int i = 0; i < this.flowsFail.size(); i++) {
			System.out.println("Falha " + i + ": " + this.flowsFail.get(i).getID() + " SRC -> DST : " + this.flowsFail.get(i).getSource() + " -> " +  this.flowsFail.get(i).getDestination());
			//System.out.println("==== LINKS DESSE FLUXO ====");
			System.out.println("N links: " + this.flowsFail.get(i).getLinks().length);
			for(int j=0; j<flowsFail.get(i).getLinks().length; j++) {
				int[] vetor = flowsFail.get(i).getLinks();
				System.out.printf("Link %d: %d\n", j, vetor[j]);
			}
		}
	}

	/*Encontro os fluxos que falharam
	public ArrayList<Flow> flowsFailed(int seed, ArrayList<Flow> flowsActive){
		ArrayList<Flow> flowsFail = new ArrayList<Flow>();
		for (Flow flow : flowsActive){
			for(int i = 0; i < flow.getLinks().length; i++){  
				if( flow.getLinks()[i]== generateFailure(seed))
					flowsFail.add(flow)	;
			}	
		}
		return flowsFail;
	}*/
	public int getSrc() {
		return this.src;
	}
	public int getDst() {
		return this.dst;
	}
}
