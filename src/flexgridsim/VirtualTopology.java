
package flexgridsim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import org.w3c.dom.*;

import flexgridsim.util.ModulationsMuticore;


/**
 * The virtual topology is created based on a given Physical Topology and
 * on the lightpaths specified on the XML file.
 * 
 * @author andred
 */
public class VirtualTopology {

    private long nextLightpathID = 0;
    private TreeSet<LightPath>[][] adjMatrix;
    private int adjMatrixSize;
    private Map<Long, LightPath> lightPaths;
    private PhysicalTopology pt;
    private Tracer tr = Tracer.getTracerObject();
  //private MyStatistics st = MyStatistics.getMyStatisticsObject();
    
    private static class LightPathSort implements Comparator<LightPath> {

        public int compare(LightPath lp1, LightPath lp2) {
            if (lp1.getID() < lp2.getID()) {
                return -1;
            }
            if (lp1.getID() > lp2.getID()) {
                return 1;
            }
            return 0;
        }
    }
    
    /**
     * Creates a new VirtualTopology object.
     * 
     * @param xml file that contains all simulation information
     * @param pt Physical Topology of the network
     */
    @SuppressWarnings("unchecked")
    public VirtualTopology(Element xml, PhysicalTopology pt) {
        int nodes, lightpaths;

        lightPaths = new HashMap<Long, LightPath>();

        try {
            this.pt = pt;
            if (Simulator.verbose) {
                System.out.println(xml.getAttribute("name"));
            }

            adjMatrixSize = nodes = pt.getNumNodes();

            // Process lightpaths
            adjMatrix = new TreeSet[nodes][nodes];
            for (int i = 0; i < nodes; i++) {
                for (int j = 0; j < nodes; j++) {
                    if (i != j) {
                        adjMatrix[i][j] = new TreeSet<LightPath>(new LightPathSort());
                    }
                }
            }
            NodeList lightpathlist = xml.getElementsByTagName("lightpath");
            lightpaths = lightpathlist.getLength();
            if (Simulator.verbose) {
                System.out.println(Integer.toString(lightpaths) + " lightpath(s)");
            }
            if (lightpaths > 0) {
                //TODO
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * First, creates a lightpath in the Physical Topology through the createLightpathInPT
     * function. Then, gets the lightpath's source and destination nodes, so a new
     * LightPath object can finally be created and added to the lightPaths HashMap
     * and to the adjMatrix TreeSet.
     *
     * @param links list of integers that represent the links that form the lightpath
     * @param channel list of channels to be allocated for each core
     * @param modulationLevel the modulation level
     * @return -1 if LightPath object cannot be created, or its unique identifier otherwise
     */
    public long createLightpath(Path path, int modulationLevel) {

        LightPath lp;
        int src, dst;
        long id;
        if (path.getLinks().length < 1) {
            throw (new IllegalArgumentException());
        } else {
            if (canCreateLightpath(path.getSlotList(), path.getLinks(), path.getModulation())) {
            //if (canCreateLightpath(path.getSlotList())) {
                createLightpathInPT(path.getSlotList(), modulationLevel);
    	        src = pt.getLink(path.getLinks()[0]).getSource();
    	        dst = pt.getLink(path.getLinks()[path.getLinks().length - 1]).getDestination();
    	        if(src==dst)
    	        	dst =pt.getLink(path.getLinks()[path.getLinks().length - 2]).getDestination();
                id = this.nextLightpathID; 
               // System.out.println("ID: "+id);
                lp = new LightPath(id, src, dst, path, modulationLevel);
                adjMatrix[src][dst].add(lp);
                lightPaths.put(nextLightpathID, lp);
                tr.createLightpath(lp);
                this.nextLightpathID++;
                return id; 
            }
        //	System.out.println("ruim");ailS
            return -1;

        }
    }
    
    //ADDED TO SUPPORT IMAGE ALG.
    public long createLightpath(int[] links, ArrayList<Slot> channelList, int modulationLevel) {
    	Path path = new Path(links, channelList, modulationLevel);
        LightPath lp;
        int src, dst;
        long id;
        if (path.getLinks().length < 1) {
        	System.out.println("ERRO");
            throw (new IllegalArgumentException());
        } else {
            if (canCreateLightpath(path.getSlotList())) {
            	
                createLightpathInPT(path.getSlotList());
    	        src = pt.getLink(path.getLinks()[0]).getSource();
    	        dst = pt.getLink(path.getLinks()[path.getLinks().length - 1]).getDestination();
    	        if(src==dst)
    	        	dst =pt.getLink(path.getLinks()[path.getLinks().length - 2]).getDestination();
                id = this.nextLightpathID; 
               // System.out.println("ID: "+id);
                lp = new LightPath(id, src, dst, path, modulationLevel);
                adjMatrix[src][dst].add(lp);
                lightPaths.put(nextLightpathID, lp);
                tr.createLightpath(lp);
                this.nextLightpathID++;
                return id; 
            }
        //	System.out.println("ruim");ailS
            return -1;

        }
    }
    public long createLightpath(Path path) {

        LightPath lp;
        int src, dst;
        long id;
        if (path.getLinks().length < 1) {
            throw (new IllegalArgumentException());
        } else {
            if (canCreateLightpath(path.getSlotList())) {
            	
                createLightpathInPT(path.getSlotList());
    	        src = pt.getLink(path.getLinks()[0]).getSource();
    	        dst =pt.getLink(path.getLinks()[path.getLinks().length - 1]).getDestination();
    	        if(src==dst)
    	        	dst =pt.getLink(path.getLinks()[path.getLinks().length - 2]).getDestination();
                id = this.nextLightpathID; 
               // System.out.println("ID: "+id);
                lp = new LightPath(id, src, dst, path);
                adjMatrix[src][dst].add(lp);
                lightPaths.put(nextLightpathID, lp);
                tr.createLightpath(lp);
                this.nextLightpathID++;
                return id; 
            }
        //	System.out.println("ruim");
            return -1;

        }
    }
    
    public long createLightpathProtection(Path pathBack, int modulationLevel) {
        long id;
        LightPath lp;
        int src, dst;
        if (pathBack.getLinks().length < 1||pathBack.getSlotList().isEmpty()) {
            throw (new IllegalArgumentException());

        } else {
            if (canCreateProtectionLightpath(pathBack.getSlotList())) {         
	            createProtectionLightpathInPT(pathBack.getSlotList());
	 	        src = pt.getLink(pathBack.getLinks()[0]).getSource();
	 	        dst =pt.getLink(pathBack.getLinks()[pathBack.getLinks().length - 1]).getDestination();
	 	        if(src==dst)
	 	        	dst =pt.getLink(pathBack.getLinks()[pathBack.getLinks().length -1]).getSource();
	             id = this.nextLightpathID;
	             lp = new LightPath(id, src, dst, pathBack, modulationLevel);
	             adjMatrix[src][dst].add(lp);
	             lightPaths.put(nextLightpathID, lp);
	             tr.createLightpath(lp);
	             this.nextLightpathID++;
	             return id;
            }
           // System.out.println("VT - linha 142");
            return -1;
        }

    }
    public long createLightpathProtection(Path pathBack) {
        long id;
        LightPath lp;
        int src, dst;
        if (pathBack.getLinks().length < 1||pathBack.getSlotList().isEmpty()) {
            throw (new IllegalArgumentException());

        } else {
            if (canCreateProtectionLightpath(pathBack.getSlotList())) {         
	            createProtectionLightpathInPT(pathBack.getSlotList());
	 	        src = pt.getLink(pathBack.getLinks()[0]).getSource();
	 	        dst =pt.getLink(pathBack.getLinks()[pathBack.getLinks().length - 1]).getDestination();
	 	        if(src==dst)
	 	        	dst =pt.getLink(pathBack.getLinks()[pathBack.getLinks().length -1]).getSource();
	             id = this.nextLightpathID;
	             lp = new LightPath(id, src, dst, pathBack);
	             adjMatrix[src][dst].add(lp);
	             lightPaths.put(nextLightpathID, lp);
	             tr.createLightpath(lp);
	             this.nextLightpathID++;
	             return id;
            }
           // System.out.println("VT - linha 142");
            return -1;
        }

    }
    
    public long getnextLightpathID(){
    	return this.nextLightpathID;
    }
    
    
    /**
     * First, removes a given lightpath in the Physical Topology through the removeLightpathInPT
     * function. Then, gets the lightpath's source and destination nodes, to remove it 
     * from the lightPaths HashMap and the adjMatrix TreeSet.
     * 
     * @param id the unique identifier of the lightpath to be removed
     * @return true if operation was successful, or false otherwise
     */
    public boolean removeLightPath(long id) {
        int src, dst;
        LightPath lp;

        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!lightPaths.containsKey(id)) {
                return false;
            }
            lp = lightPaths.get(id);
            removeLightpathFromPT( lp.getSlotList());
            src = lp.getSource();
            dst = lp.getDestination();
            lightPaths.remove(id);
            adjMatrix[src][dst].remove(lp);
            tr.removeLightpath(lp);

            return true;
        }
    }
    public boolean removeProtectionLightPath(long id) {
        int src, dst;
        LightPath lp;

        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!lightPaths.containsKey(id)) {
                return false;
            }
            lp = lightPaths.get(id);
            removeProtectionLightpathFromPT( lp.getSlotList());
            src = lp.getSource();
            dst = lp.getDestination();
            lightPaths.remove(id);
            adjMatrix[src][dst].remove(lp);
            tr.removeLightpath(lp);

            return true;
        }
    }
    
    /**
     * Removes a given lightpath from the Physical Topology and then puts it back,
     * but with a new route (set of links).
     *
     * @param id unique identifier of the lightpath to be rerouted
     * @param links list of integers that represent the links that form the lightpath
     * @param channel list of channels to be rerouted for each core
     * @param modulationLevel the modulation level
     * @return true if operation was successful, or false otherwise
     */
    public boolean rerouteLightPath(long id, int[] links, Path path, int modulationLevel,  int modulationLevelBack) {
        int src, dst;
        LightPath old, lp;
        if (links.length < 1) {
            throw (new IllegalArgumentException());
        } else {
            if (!lightPaths.containsKey(id)) {
                return false;
            }
            old = lightPaths.get(id);
            removeLightpathFromPT( old.getSlotList());
            if (!canCreateLightpath( path.getSlotList())) {
                createLightpathInPT( old.getSlotList());
                return false;
            }
            createLightpathInPT( path.getSlotList());
            src = pt.getLink(links[0]).getSource();
            dst = pt.getLink(links[links.length - 1]).getDestination();
            adjMatrix[src][dst].remove(old);
            lp = new LightPath(id, src, dst, path, modulationLevel);
            adjMatrix[src][dst].add(lp);
            lightPaths.put(id, lp);
            return true;
        }
    }
    
    
  
    
//    /**
//     * Says whether or not a given lightpath is idle, i.e.,
//     * all its bandwidth is available.
//     * 
//     * @param id the lightpath's unique identifier
//     * @return true if lightpath is idle, or false otherwise
//     */
//    public boolean isLightpathIdle(long id) {
//        int[] links;
//        int firstSlot, lastSlot;
//        links = getLightpath(id).getLinks();
//        firstSlot = getLightpath(id).getFirstSlot();
//        lastSlot  = getLightpath(id).getLastSlot();
//        
//        return pt.getLink(links[0]).areSlotsAvailable(firstSlot, lastSlot);
//    }
    
    /**
     * Retrieves a determined LightPath object from the Virtual Topology.
     * 
     * @param id the lightpath's unique identifier
     * @return the required lightpath
     */
    public LightPath getLightpath(long id) {
        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (lightPaths.containsKey(id)) {
                return lightPaths.get(id);
            } else {
                return null;
            }
        }
    }
    
    /**
     * Retrieves the TreeSet with all LightPath objects that
     * belong to the Virtual Topology.
     * 
     * @param src the lightpath's source node
     * @param dst the lightpath's destination node
     * @return the TreeSet with all of the lightpaths
     */
    public TreeSet<LightPath> getLightpaths(int src, int dst) {
        return new TreeSet<LightPath>(adjMatrix[src][dst]);
    }
    
    /**
     * Retrieves the adjacency matrix of the Virtual Topology.
     * 
     * @return the VirtualTopology object's adjMatrix
     */
    public TreeSet<LightPath>[][] getAdjMatrix() {
        return adjMatrix;
    }
    
    /**
     * Says whether or not a lightpath exists, based only on its source
     * and destination nodes.
     * 
     * @param src the lightpath's source node
     * @param dst the lightpath's destination node
     * @return true if the lightpath exists, or false otherwise
     */
    public boolean hasLightpath(int src, int dst) {
        //System.out.println("hasLightpath"+Integer.toString(src)+" -> "+Integer.toString(dst));
        if (adjMatrix[src][dst] != null) {
            //System.out.println("Not null");
            if (!adjMatrix[src][dst].isEmpty()) {
                //System.out.println("Not empty");
                return true;
            }
        }
        return false;
    }
    
    /**
     * Says whether or not a lightpath can be created, based only on its
     * links and slots.
     *
     * @param links list of integers that represent the links that form the lightpath
     * @param channel list of channels to be verified for each core
     * @return true if the lightpath can be created, or false otherwise
     */

    
    public boolean canCreateLightpath(ArrayList<Slot> slots) {
        try {
        	 for (Slot slot: slots) {
        		 if (!pt.getLink(slot.link).isSlotsAvailable(slot)) {
        			//System.out.println("VT Linha393 - Ocupado: Link "+slot.link +" Slot "+ slot.slot);
        			//Verify the threshold
             		return false;
        		 }
             }
        } catch (IllegalArgumentException e){
			System.out.println("Illegal argument for areSlotsAvailable");
			return false;
		}
        return true;
    }

    //
    //	Second
    public boolean canCreateLightpath(ArrayList<Slot> slots, int[] links, int modulationLevel) {
        try {
        	for (int i:links) {
        		boolean [][] dispo = pt.getLink(i).getAllocableSpectrum(modulationLevel);
        		for (Slot s:slots) {
        			if (s.link == i) {
        				if(!dispo[s.core][s.slot]) {
                     		return false;
        				}
        			}
        		}
        	}
        } catch (IllegalArgumentException e){
			System.out.println("Illegal argument for areSlotsAvailable");
			return false;
		}
        return true;
    }

    public boolean canCreateProtectionLightpath( ArrayList<Slot> slots) {
        try {
       	 for (Slot slot: slots) {
    		 if (!pt.getLink(slot.link).isProtectionSlotsAvailable(slot, pt.getOverlap())) {
    			System.out.println("VT LInha408 - Ocupado: Link "+slot.link +" Slot "+ slot.slot);
         		return false;	
    		 }
         }
        } catch (IllegalArgumentException e){
			System.out.println("Illegal argument for areSlotsAvailable");
			return false;
		}
        return true;
    }
    
    /**
     * Reserves, in the physical topology, the resources a given lightpath needs:
     * links, wavelengths and wavelength converters (if necessary).
     * 
     * @param links list of integers that represent the links that form the lightpath 
     * @param firstSlot list of wavelength values used in the lightpath links
     */
    private void createLightpathInPT( ArrayList<Slot> slots) {
        for (Slot slot: slots) {
        	pt.getLink(slot.link).reserveSlot(slot);
        }
    }
    private void createLightpathInPT( ArrayList<Slot> slots, int modulationLevel) {
        for (Slot slot: slots) {
        	pt.getLink(slot.link).reserveSlot(slot);
        	pt.getLink(slot.link).setModulationLevel(slot.core, slot.slot, modulationLevel);
        	pt.getLink(slot.link).updateNoise(slot, modulationLevel);
        }
    }
    private void createProtectionLightpathInPT(ArrayList<Slot> slots) {
        for (Slot slot: slots) {
        	pt.getLink(slot.link).reserveProtectionSlot(slot,pt.getOverlap());
        }
    }
    /**
     * Releases, in the physical topology, the resources a given lightpath was using:
     * links, wavelengths and wavelength converters (if necessary).
     * 
     * @param links list of integers that represent the links that form the lightpath
     * @param firstSlot list of wavelength values used in the lightpath links
     */
    private void removeLightpathFromPT( ArrayList<Slot> slots) {
    	 for (Slot slot: slots) {
         	pt.getLink(slot.link).releaseSlot(slot);
         }
    }
    
   private void removeProtectionLightpathFromPT( ArrayList<Slot> slots) {  

	   for (Slot slot: slots) {
        	pt.getLink(slot.link).releaseProtectionSlot(slot,pt.getOverlap());
        }
    }
    
    
    /**
     * Prints all lightpaths belonging to the Virtual Topology.
     * 
     * @return string containing all the elements of the adjMatrix TreeSet
     */
    @Override
    public String toString() {
        String vtopo = "";
        for (int i = 0; i < adjMatrixSize; i++) {
            for (int j = 0; j < adjMatrixSize; j++) {
                if (adjMatrix[i][j] != null) {
                    if (!adjMatrix[i][j].isEmpty()) {
                        vtopo += adjMatrix[i][j].toString() + "\n\n";
                    }
                }
            }
        }
        return vtopo;
    }
}