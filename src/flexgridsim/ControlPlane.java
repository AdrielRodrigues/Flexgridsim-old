/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim;
//

//import com.opencsv.CSVWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
//

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import flexgridsim.rsa.ControlPlaneForRSA;
import flexgridsim.rsa.RSA;
import flexgridsim.util.Decibel;
import flexgridsim.util.ModulationsMuticore;

/**
 * The Control Plane is responsible for managing resources and
 * connection within the network.
 */
public class ControlPlane implements ControlPlaneForRSA {

    private RSA rsa;
    private PhysicalTopology pt;
    private VirtualTopology vt;
    private Map<Flow, ArrayList<LightPath>> mappedFlows; // Flows and lightpath that have been accepted into the network
    private Map<Flow, ArrayList<LightPath>> mappedPFlows; // Flows and lightpath protection that have been accepted into the network
    private Map<Long, Flow> activeFlows; // Flows that have been accepted or that are waiting for a decision

    private Map<Long, Failure> failure; // Keep id and failure of which links have failed

    private Map<Long, ArrayList<LightPath>> protection;
    private Tracer tr = Tracer.getTracerObject();
    private MyStatistics st = MyStatistics.getMyStatisticsObject();
    
    /*
    private List<String[]> linhas;
    private String[] c = {"id", "src", "dst", "accepted", "path", "protection"};
    */

	/**
	 * Creates a new ControlPlane object.
	 *
	 * @param xml the xml
	 * @param eventScheduler the event scheduler
	 * @param rsaModule the name of the RSA class
	 * @param pt the network's physical topology
	 * @param vt the network's virtual topology
	 * @param traffic the traffic
	 * @param ff network failure
	 */
    @SuppressWarnings("deprecation")
	public ControlPlane(Element xml, EventScheduler eventScheduler, String rsaModule, PhysicalTopology pt, VirtualTopology vt, TrafficGenerator traffic) {
        @SuppressWarnings("rawtypes")
		Class RSAClass;
        mappedFlows = new HashMap<Flow, ArrayList<LightPath>>();
        mappedPFlows = new HashMap<Flow, ArrayList<LightPath>>();
        activeFlows = new HashMap<Long, Flow>();
        failure = new HashMap<Long, Failure>();
        protection = new HashMap<Long, ArrayList<LightPath>>();
        this.pt = pt;
        this.vt = vt;
        
        //
        //String[] cabecalho = {"id", "src", "dst"};
        //linhas = new ArrayList<>();
        //
        
        try {
            RSAClass = Class.forName(rsaModule);
            rsa = (RSA) RSAClass.newInstance();
            rsa.simulationInterface(xml, pt, vt, this, traffic);
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    /**
     * Deals with an Event from the event queue.
     * If it is of the FlowArrivalEvent kind, adds it to the list of active flows.
     * If it is from the FlowDepartureEvent, removes it from the list.
     * 
     * @param event the Event object taken from the queue 
     */

    public void newEvent(Event event) {
        if (event instanceof FlowArrivalEvent) {
        	
        	Flow flow = ((FlowArrivalEvent) event).getFlow();
        	
        	//if(flow.getID() == 7000) {
        		//System.out.println("stop");
        	//}
        	
            newFlow(flow);
            rsa.flowArrival(flow);
            
            //String.valueOf(flow.getID()), String.valueOf(flow.getSource()), String.valueOf(flow.getDestination()), String.valueOf(flow.isAccepeted()), 
            //linhas.add(new String[] {String.valueOf(flow.getID()), String.valueOf(flow.getSource()), String.valueOf(flow.getDestination()), String.valueOf(flow.isAccepeted()), flow.printLinks(), flow.printLinksp() });
            
            //System.out.println(flow.printLinks());
            
            
        } else if (event instanceof FlowDepartureEvent) {
            Flow removedFlow = removeFlow(((FlowDepartureEvent) event).getID());
            rsa.flowDeparture(removedFlow); // Useless
        } else if (event instanceof FailureIn) {
        	newFailure(((FailureIn) event).getFail());
        	rerouteFlowFailed(((FailureIn) event).getFail());
        } else if (event instanceof FailureOut) {
        	removeFailure(((FailureOut)event).getFail());
        }
    }

    /**
     * Retrieves a Flow object from the list of active flows.
     * 
     * @param id the unique identifier of the Flow object
     * @return the required Flow object
     */
    public Flow getFlow(long id) {
        return activeFlows.get(id);
    }
    
    /**
     * Adds a given active Flow object to a determined Physical Topology.
     * 
     * @param id unique identifier of the Flow object
     * @param lightpath the Path, or list of LighPath objects
     * @return true if operation was successful, or false if a problem occurred
     */
    public boolean acceptFlow(long id, ArrayList<LightPath> lightpath) {
        Flow flow;
        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
            	throw (new IllegalArgumentException());
            }
            flow = activeFlows.get(id);

            if (!canAddPathToPT(flow, lightpath)) {
                return false;
            }
            
            for(int i = 0; i < flow.getLinks().length; i++) {
            	int[] vetor = flow.getLinks();
            	if(!pt.getLink(vetor[i]).isLinkActive()) {
            		this.blockFlow(id);
            		
            		long idtemp = pt.getLink(vetor[i]).getWhichBlocked();
            		Failure ff =  failure.get(idtemp);
            		ff.addFlowsFailed(flow);
            		
            		return false;
            	}
            }
            
            for(int i = 0; i < flow.getLinks().length; i++) {
            	int[] vetor = flow.getLinks();
            	pt.getLink(vetor[i]).insertFlow(flow.getID(), flow);
            	
            }
            
            addPathToPT(flow, lightpath);
            
            
            mappedFlows.put(flow, lightpath);
            tr.acceptFlow(flow, lightpath);
            st.acceptFlow(flow, lightpath);
            flow.setAccepeted(true);
            return true;
        }
    }
    //pode passar null como parametro e excluir o aceita fluxo acima 
    public boolean acceptFlow(long id, ArrayList<LightPath> lightpaths, ArrayList<LightPath> lightpathsProtection) {
        Flow flow;

        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
            	throw (new IllegalArgumentException());
            }
            flow = activeFlows.get(id);
            if (!canAddPathToPT(flow, lightpaths)) {
            	System.out.println("FaLse");
                return false;
            }
        	
        	for (LightPath lightpathprotection: lightpathsProtection) {	
	        	if(!protection.containsKey(lightpathprotection.getID())){     
	        		ArrayList<LightPath> light = new ArrayList<LightPath>();
	        		addProtectionPathToPT(flow, lightpathprotection);
	        		light.add(lightpaths.get(lightpathsProtection.indexOf(lightpathprotection)));
	        		//path de proteção com id x, protege um conjunto light de paths primarios
	        		protection.put(lightpathprotection.getID(), light);
	        	}else{	
	        		protection.get(lightpathprotection.getID()).add(lightpaths.get(lightpathsProtection.indexOf(lightpathprotection)));
	        	}
        	}
    		//add primary path
        	addPathToPT(flow, lightpaths);
        	mappedFlows.put(flow, lightpaths);
        	mappedPFlows.put(flow, lightpathsProtection);
            tr.acceptFlow(flow, lightpaths);
            st.acceptFlow(flow, lightpaths,  lightpathsProtection);
            flow.setAccepeted(true);
            return true;
        }
    }

    /**
     * Removes a given Flow object from the list of active flows. //correto
     * 
     * @param id the unique identifier of the Flow to be removed
     * 
     * @return the flow object
     */

    private Flow removeFlow(long id) {
        Flow flow;
        ArrayList<LightPath> lightpaths;
        ArrayList<LightPath> lightpathsProtection;

        if (activeFlows.containsKey(id)) { 	// Check by id
            flow = activeFlows.get(id); 	// Return flow

            if (mappedFlows.containsKey(flow)) { 		// Check the existence
                lightpaths = mappedFlows.get(flow);		// *******************
                removePathFromPT(flow, lightpaths);		// *******************
                mappedFlows.remove(flow);				//
                
                for(int i = 0; i < flow.getLinks().length; i++) {
                	int[] vetor = flow.getLinks();
                	pt.getLink(vetor[i]).removeLinkFlow(flow.getID());
                }
                                
            } else {
            	return flow;
            }
            
            // It's about protection
            
        	if (mappedPFlows.containsKey(flow)) {
               // System.out.println("aqui entra");
        		lightpathsProtection = mappedPFlows.get(flow);
            	for (LightPath lightpathprotection: lightpathsProtection) {
	               //remove lightpath do fluxo
            		protection.get(lightpathprotection.getID()).remove(lightpaths.get(lightpathsProtection.indexOf(lightpathprotection)));
	                //remove array de lightpath se estiver vazio
	                if(protection.get(lightpathprotection.getID()).isEmpty()){
	                	//If the lightpath to be not protecting other flow remove it
	                	removeProtectionPathFromPT(flow, lightpathprotection);
	                }
        		}
                mappedPFlows.remove(flow);
        	}
            activeFlows.remove(id);
            return flow;
        }
        return null;
    }

    

    /**
     * Removes a given Flow object from the list of active flows.
     * 
     * @param id unique identifier of the Flow object
     * @return true if operation was successful, or false if a problem occurred
     */
    public boolean blockFlow(long id) {
        Flow flow;

        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
                return false;
            }
            flow = activeFlows.get(id);
            if (mappedFlows.containsKey(flow)) {
                return false;
            }
            activeFlows.remove(id);
            tr.blockFlow(flow);
            st.blockFlow(flow);
            return true;
        }
    }
    
    /**
     * Removes a given Flow object from the Physical Topology and then
     * puts it back, but with a new route (set of LightPath objects). 
     * 
     * @param id unique identifier of the Flow object
     * @param lightpath list of LightPath objects, which form a Path
     * @return true if operation was successful, or false if a problem occurred
     */
    
    public boolean rerouteFlow(long id, ArrayList<LightPath> lightpath) {
        Flow flow;
        ArrayList<LightPath> oldPath;
        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
                return false;
            }
            flow = activeFlows.get(id);
            if (!mappedFlows.containsKey(flow)) {
                return false;
            }
            oldPath = mappedFlows.get(flow);
            removePathFromPT(flow, oldPath);
            if (!canAddPathToPT(flow, lightpath)) {
                addPathToPT(flow, oldPath);
                return false;
            }
            addPathToPT(flow, lightpath);
            mappedFlows.put(flow, lightpath);
            //tr.flowRequest(id, true);
            return true;
        }
    }
    
    /**
     * Reroute flows which were using the link at the moment of the failure
     * 
     * */
    
    public boolean rerouteFlowFailed(Failure ff) {

    	//TODO add statistic information, check the disjoint(?), swap protection path and primary path 
    	Map<Long, Flow> flows = new HashMap<Long, Flow>();
        ArrayList<LightPath> lightpathProtection;
        ArrayList<LightPath> oldPath;

    	for(int i : ff.getLinkFail()) {
    		Map<Long, Flow> temp = new HashMap<Long, Flow>();
    		temp = pt.getLink(i).getFlowsHere();
    		for(Flow tt : temp.values()) {
    			flows.put(tt.getID(), tt);
    		}
    	}

    	for(Flow flow : flows.values()) {
    		if(mappedPFlows.containsKey(flow)) {
    			if (mappedFlows.containsKey(flow)) { 		// Check the existence
                    oldPath = mappedFlows.get(flow);		// *******************                    
                    removePathFromPT(flow, oldPath);

                    lightpathProtection = mappedPFlows.get(flow);
                    removeProtectionPathFromPT(flow, lightpathProtection);
                    addPathToPT(flow, lightpathProtection);
                    mappedFlows.put(flow, lightpathProtection);

                    for(int i = 0; i < flow.getLinks().length; i++) {
                    	int[] vetor = flow.getLinks();
                    	pt.getLink(vetor[i]).removeLinkFlow(flow.getID());
                    }
                    
                    // Necessary (?)
                    int[] lastPath = flow.getLinks();
                    flow.setLinks(flow.getLinksp());
                    flow.setLinksp(lastPath);
                    
                    ArrayList<Slot> old = flow.getSlotList();
                    flow.setSlotList(flow.getSlotListp());
                    flow.setSlotListp(old); // Probably it is not right but I need to save the old path
                    
                    //return true;
                }
    		} else {
    	        ArrayList<LightPath> lightpaths;

    	        if (activeFlows.containsKey(flow.getID())) { 	// Check by id
    	            flow = activeFlows.get(flow.getID()); 	// Return flow

    	            if (mappedFlows.containsKey(flow)) { 		// Check the existence
    	                lightpaths = mappedFlows.get(flow);		// *******************
    	                removePathFromPT(flow, lightpaths);		// *******************
    	                mappedFlows.remove(flow);				//
    	            }
    			blockFlow(flow.getID());
    	        }
    		}
    	}
    	// If there are flows = false
    	return true;
    }
    
    /**
     * Adds a given Flow object to the HashMap of active flows.
     * The HashMap also stores the object's unique identifier (ID). 
     * 
     * @param flow Flow object to be added
     */
    private void newFlow(Flow flow) {
        activeFlows.put(flow.getID(), flow);
    }
    
    /**
     * @author rafael, adriel
     * 
     * @param fail the one which have failed
     * 
     * @return void vazio
     */

    private void newFailure(Failure ff) {
    	if(ff.generateFailure()) {
    		failure.put(ff.getID(), ff);
    	}
    }
    // Maybe we should get info about failures and add it 
    private void removeFailure(Failure ff) {
    	if(failure.containsKey(ff.getID())) {
    		ff.removeFailure();
			failure.remove(ff.getID());
    	}
    }

    /**
     * Removes a given Flow object from a Physical Topology. 
     * 
     * @param flow the Flow object that will be removed from the PT
     * @param lightpaths a list of LighPath objects
     */
    
    private void removePathFromPT(Flow flow, LightPath lightpath) {
        for (Slot slot: lightpath.getSlotList()) {
         	pt.getLink(slot.link).releaseSlot(slot);
         }
        vt.removeLightPath(lightpath.getID());
    }
    
    private void removeProtectionPathFromPT(Flow flow, LightPath lightpath) {
        for (Slot slot: lightpath.getSlotList()) {
         	pt.getLink(slot.link).releaseProtectionSlot(slot,pt.getOverlap());
         }
        vt.removeProtectionLightPath(lightpath.getID());
    }
    
    private void removePathFromPT(Flow flow, ArrayList<LightPath> lightpaths) {
    	int n = 0;
    	for (LightPath lightpath: lightpaths) {
	        for (Slot slot: lightpath.getSlotList()) {
	         	pt.getLink(slot.link).releaseSlot(slot);
	        }
	        //updateNoise(flow.getSlotList(), flow.getModulationLevel(n));
	        vt.removeLightPath(lightpath.getID());
	        n++;
    	}
    }
    
    private void removeProtectionPathFromPT(Flow flow, ArrayList<LightPath> lightpaths) {
    	for (LightPath lightpath: lightpaths) {
	        for (Slot slot: lightpath.getSlotList()) {
	         	pt.getLink(slot.link).releaseProtectionSlot(slot,pt.getOverlap());
	         }
	        vt.removeProtectionLightPath(lightpath.getID());
    	}
    }
    
    
    /**
     * Says whether or not a given Flow object can be added to a 
     * determined Physical Topology, based on the amount of bandwidth the
     * flow requires opposed to the available bandwidth.
     * 
     * @param flow the Flow object to be added 
     * @param lightpaths list of LightPath objects the flow uses
     * @return true if Flow object can be added to the PT, or false if it can't
     */

    private boolean canAddPathToPT(Flow flow, ArrayList<LightPath> lightpaths) {
		for (LightPath lightpath: lightpaths) {
			for (Slot slot: lightpath.getSlotList()) {
				 if (pt.getLink(slot.link).isSlotsAvailable(slot)) {
			 		return false;
				 }
			}  
		}
        return true;
    }
    
    private boolean canAddProtectionPathToPT(Flow flow, ArrayList<LightPath> lightpaths) {	
    	for (LightPath lightpath: lightpaths) {
			for (Slot slot: lightpath.getSlotList()) {
				if (pt.getLink(slot.link).isProtectionSlotsAvailable(slot, pt.getOverlap())) {
					return false;
				}
			 } 
    	}
        return true;
   }
    
    /**
     * Adds a Flow object to a Physical Topology.
     * This means adding the flow to the network's traffic,
     * which simply decreases the available bandwidth.
     * 
     * @param flow the Flow object to be added 
     * @param lightpaths list of LightPath objects the flow uses
     */
    
    public void addPathToPT(Flow flow, LightPath lightpath) {
        for (Slot slot: lightpath.getSlotList()) {
        	pt.getLink(slot.link).reserveSlot(slot);
        }
        //TODO
        //updateNoise(lightpath.getSlotList(), flow.getModulationLevel());
    }
    
    public void addProtectionPathToPT(Flow flow, LightPath lightpath) {
		for (Slot slot: lightpath.getSlotList()) {
			pt.getLink(slot.link).reserveProtectionSlot(slot, pt.getOverlap());
		}      
		
    }
    public void addPathToPT(Flow flow, ArrayList<LightPath> lightpaths) {
    	int n = 0;
    	for (LightPath lightpath: lightpaths) {
	        for (Slot slot: lightpath.getSlotList()) {
	        	pt.getLink(slot.link).reserveSlot(slot);
	        }
	        //TODO
	    	//updateNoise(lightpath.getSlotList(), flow.getModulationLevel(n));
	    	n++;
    	}
    }
    
    public void addProtectionPathToPT(Flow flow, ArrayList<LightPath> lightpaths) {
		for (LightPath lightpath: lightpaths) {
			for (Slot slot: lightpath.getSlotList()) {
				pt.getLink(slot.link).reserveProtectionSlot(slot, pt.getOverlap());
			}      
		}
    }
    
    /**
     * Retrieves a Path object, based on a given Flow object.
     * That's possible thanks to the HashMap mappedFlows, which
     * maps a Flow to a Path.
     * 
     * @param flow Flow object that will be used to find the Path object
     * @return Path object mapped to the given flow 
     */
    public ArrayList<LightPath> getPath(Flow flow) {
        return mappedFlows.get(flow);
    }
    
    /**
     * Retrieves the complete set of Flow/Path pairs listed on the
     * mappedFlows HashMap.
     * 
     * @return the mappedFlows HashMap
     */
    public Map<Flow, ArrayList<LightPath>> getMappedFlows() {
        return mappedFlows;
    }
    
    
    /**
     * 
     * Return LightPaths Protected by LightPath  path
     * 
     * @return flows
     */
    
    public  ArrayList<LightPath> getLightPathsProtectedByLightPath(LightPath path) {
        return protection.get(path);
    }

    /**
     * Groom flow.
     * @param flow 
     *
     * @param lp the lp
     * @param demandInSlots 
     */
    public void groomFlow(Flow flow, LightPath lp, int demandInSlots){
//    	boolean canGroom = true;
//    	for (int linkID : lp.getLinks()) {
//			if (!pt.getLink(linkID).areSlotsAvailable(lp.getSlotList())){
//				canGroom = false;
//				break;
//			}
//		}
//    	if (canGroom){
//    		lp. += demandInSlots;
//    		for (int linkID : lp.getLinks()) {
//                pt.getLink(linkID).reserveSlots(lp.getSlotList());
//            }
//    		st.groomedFlow(flow);
//    	}
    }
 
    /*public void writeCSV(int seed, double forcedLoad) throws IOException{
    	Writer writer = Files.newBufferedWriter(Paths.get("info"+seed+"-"+forcedLoad+".csv"));
        CSVWriter csvWriter = new CSVWriter(writer);

        csvWriter.writeNext(c);
        csvWriter.writeAll(linhas);

        csvWriter.flush();
        writer.close();
    }*/
    public Map<Long, Flow> getActiveFlows(){
    	return activeFlows;
    }
   
}
