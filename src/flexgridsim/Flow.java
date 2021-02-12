/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim;


import java.util.ArrayList;


/**
 * The Flow class defines an object that can be thought of as a flow
 * of data, going from a source node to a destination node. 
 * 
 * @author andred, pedrom
 */
public class Flow {

    private long id;
    private int src;
    private int dst;
    private int bw;
    private double duration;
    private int cos;
    private ArrayList<int[]> links;
    private ArrayList<int[]> linksp;
    private ArrayList<ArrayList<Slot>> slotList;
    private ArrayList<ArrayList<Slot>> slotListp;
    private double deadline;
    private boolean accepted;
    private double time;
    private ArrayList<Integer> modulationLevel;
    private ArrayList<Integer> modulationLevelBack;

	/**
	 * Creates a new Flow object.
	 *
	 * @param id            unique identifier
	 * @param src           source node
	 * @param dst           destination node
	 * @param time 			the time
	 * @param bw            bandwidth required (Mbps)
	 * @param duration      duration time (seconds)
	 * @param cos           class of service
	 * @param deadline 		the deadline
	 */
    public Flow(long id, int src, int dst, double time, int bw, double duration, int cos, double deadline) {
        if (id < 0 || src < 0 || dst < 0 || bw < 1 || duration < 0 || cos < 0) {
            throw (new IllegalArgumentException());
        } else {
            this.id = id;
            this.src = src;
            this.dst = dst;
            this.bw = bw;
            this.duration = duration;
            this.cos = cos;
            this.deadline = deadline;
            this.accepted = false;
            this.time = time;
            this.modulationLevel = new ArrayList<Integer>();
            this.modulationLevelBack  = new ArrayList<Integer>();
            this.links = new ArrayList<int []>();
            this.linksp = new ArrayList<int []>();
            this.slotList = new ArrayList<ArrayList<Slot>> ();
            this.slotListp = new ArrayList<ArrayList<Slot>> ();
        }
    }
    
    /**
     * Gets the time.
     *
     * @return the time
     */
    public double getTime() {
		return time;
	}


	/**
     * Retrieves the unique identifier for a given Flow.
     * 
     * @return the value of the Flow's id attribute
     */
    public long getID() {
        return id;
    }
    
    /**
     * Retrieves the source node for a given Flow.    	
     * 
     * @return the value of the Flow's src attribute
     */
    public int getSource() {
        return src;
    }
    
    /**
     * Retrieves the destination node for a given Flow.
     * 
     * @return the value of the Flow's dst attribute
     */
    public int getDestination() {
        return dst;
    }
    
    /**
     * Retrieves the required bandwidth for a given Flow.
     * 
     * @return the value of the Flow's bw attribute.
     */
    public int getRate() {
        return bw;
    }
    
    /**
     * Assigns a new value to the required bandwidth of a given Flow.
     * 
     * @param bw new required bandwidth 
     */
    public void setRate(int bw) {
        this.bw = bw;
    }
    
    /**
     * Retrieves the duration time, in seconds, of a given Flow.
     * 
     * @return the value of the Flow's duration attribute
     */
    public double getDuration() {
        return duration;
    }
    
    /**
     * Retrieves a given Flow's "class of service".
     * A "class of service" groups together similar types of traffic
     * (for example, email, streaming video, voice,...) and treats
     * each type with its own level of service priority.
     * 
     * @return the value of the Flow's cos attribute
     */
    public int getCOS() {
        return cos;
    }
    

    
    /**
     * @return the list of channels allocated for all cores
     */
    public ArrayList<Slot> getSlotList() {
		return slotList.get(0);
	}
    //for multipath
    public ArrayList<Slot> getSlotList(int path) {
		return slotList.get(path);
	}
    public ArrayList<Slot> getSlotListp() {
		return slotListp.get(0);
	}
    //for multipath
    public ArrayList<Slot> getSlotListp(int path) {
		return slotListp.get(path);
	}
	/**
	 * @param channel modify the channel allocated for the flow
	 */
	public void setSlotList(ArrayList<Slot> slotList) {
		this.slotList.add(0, slotList);
	}
	public void setSlotList(ArrayList<Slot> slotList, int path) {
		this.slotList.add(path, slotList);
	}
	public void setSlotListp(ArrayList<Slot> slotListp) {
		this.slotListp.add(0, slotListp);
	}
	public void setSlotListp(ArrayList<Slot> slotList, int path) {
		this.slotListp.add(path, slotList);
	}
	/**
     * Gets the links used by the flow.
     *
     * @return the links
     */
    
    public int[] getLinks() {
		return links.get(0);
	}
	   //for multipath
    public int[] getLinks(int path) {
		return links.get(path);
	}
    
    
    
    
    
   // Media de tamanho de todos os caminhos para calcular número de saltos
    
    public int getSizeAllLinks() {
    	int i=0; 
    	for (int len[]: links) 
    		i+=len.length;
		return i;
	}
    
    public int getSizeAllLinksp() {
    	int i=0;
    	for (int len[]: linksp) 
    		i+=len.length;
		return i;
	}
    
    //public int getNLinks() {
    //	return this.links.size();
    //}
    
    /**
     * Gets the link used by the flow.
     *
     * @param i the i
     * @return the link
     */
    public int getLink(int l) {
		return links.get(0)[l];
	}
	   //for multipath
    public int getLink(int l, int path) {
		return links.get(path)[l];
	}

	/**
	 * Sets the links used by the flow.
	 *
	 * @param links the new links
	 */

   
	public void setLinks(int[] links) {
		this.links.add(0, links);
	}
	
	   //for multipath
	public void setLinks(int[] links, int path) {
		this.links.add(path, links);
	}
	/**
     * Gets the links used by the flow.
     *
     * @return the links
     */

    
    public int[] getLinksp() {
		return linksp.get(0);
	}
	   //for multipath
    public int[] getLinksp(int path) {
		return linksp.get(path);
	}    
    /**
     * Gets the link used by the flow.
     *
     * @param i the i
     * @return the link
     */
    public int getLinkp(int l) {
		return links.get(0)[l];
	}  
    //for multipath
    public int getLinkp(int l, int path) {
		return links.get(path)[l];
	}

    public void initFlow() {
    	this.links = new ArrayList<int []>();
    	this.slotList = new ArrayList<ArrayList<Slot>> ();
    }
    
	/**
	 * Sets the links used by the flow.
	 *
	 * @param links the new links
	 */
    //for multipath
	public void setLinksp(int[] linksp, int path) {
		this.linksp.add(path, linksp);
	}
	public void setLinksp(int[] linksp) {
		this.linksp.add(0, linksp);
	}
    /**
     * Prints all information related to a given Flow.
     * 
     * @return string containing all the values of the flow's parameters
     */
    public String toString() {
        String flow = Long.toString(id) + ": " + Integer.toString(src) + "->" + Integer.toString(dst) + " rate: " + Integer.toString(bw) + " duration: " + Double.toString(duration) + " cos: " + Integer.toString(cos);
        return flow;
    }
  
	/**
	 * Gets the deadline.
	 *
	 * @return the deadline
	 */
	public double getDeadline() {
		return deadline;
	}

	/**
	 * Sets the deadline.
	 *
	 * @param deadline the new deadline
	 */
	public void setDeadline(double deadline) {
		this.deadline = deadline;
	}
	
	  
    /**
     * Creates a string with relevant information about the flow, to be
     * printed on the Trace file.
     * 
     * @return string with values of the flow's parameters
     */
    
    public String toTrace()
    {
    	String trace = Long.toString(id) + " " + Integer.toString(src) + " " + Integer.toString(dst) + " " + Integer.toString(bw) + " " + Double.toString(duration) + " " + Integer.toString(cos);
    	return trace;
    }

	/**
	 * Checks if is accepeted.
	 *
	 * @return true, if is accepeted
	 */
	public boolean isAccepeted() {
		return accepted;
	}

	/**
	 * Sets the accepeted.
	 *
	 * @param accepeted the new accepeted
	 */
	public void setAccepeted(boolean accepeted) {
		this.accepted = accepeted;
	}

	/**
	 * Gets the modulation level.
	 *
	 * @return the modulation level
	 */
	public int getModulationLevel() {
		return modulationLevel.get(0);
		//return -1;
	}
	public int getModulationLevel(int i) {
		return modulationLevel.get(i);
	}


	/**
	 * Sets the modulation level.
	 *
	 * @param modulationLevel the new modulation level
	 */
	public void setModulationLevel(int modulationLevel) {
		this.modulationLevel.add(modulationLevel);
	}

	/**
	 * Gets the modulation level.
	 *
	 * @return the modulation level
	 */
	public int getModulationLevelBack() {
		return modulationLevelBack.get(0);
	}
	public int getModulationLevelBack(int i) {
		return modulationLevelBack.get(i);
	}
	/**
	 * Sets the modulation level.
	 *
	 * @param modulationLevel the new modulation level
	 */
	public void setModulationLevelBack(int modulationLevelBack) {
		this.modulationLevelBack.add(modulationLevelBack);
	}
	
	public int sizeOfLinks() {
		return links.size();
	}
	
	public String printLinks() {
		String frase = "";
		if(isAccepeted()){
			int[] a = getLinks();
			int t = a.length;
			for(int n = 0; n < t; n++) {
				frase = frase + a[n];
				if(n != t -1) {
					frase = frase + ",";
				}
			}
		}
		return frase;
	}
	public String printLinksp() {
		String frase = "";
		if(isAccepeted()){
			int[] a = getLinksp();
			int t = a.length;
			for(int n = 0; n < t; n++) {
				frase = frase + a[n];
				if(n != t -1) {
					frase = frase + ",";
				}
			}
		}
		return frase;
	}
    
}
