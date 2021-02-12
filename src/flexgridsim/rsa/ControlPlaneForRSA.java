/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Map;

import flexgridsim.*;

/**
 * This is the interface that provides several methods for the
 * RSA Class within the Control Plane.
 * 
 * @author andred, pedrom
 */
public interface ControlPlaneForRSA {

    /**
     * Accept flow.
     *
     * @param id the id
     * @param lightpaths the lightpaths
     * @return true, if successful
     */
    public boolean acceptFlow(long id, ArrayList<LightPath> lightpaths);

    /**
     * Block flow.
     *
     * @param id the id
     * @return true, if successful
     */
    public boolean blockFlow(long id);

    /**
     * Reroute flow.
     *
     * @param id the id
     * @param lightpaths the lightpaths
     * @return true, if successful
     */
    public boolean rerouteFlow(long id, ArrayList<LightPath> lightpaths);
    
    /**
     * Gets the flow.
     *
     * @param id the id
     * @return the flow
     */
    public Flow getFlow(long id);
    
    /**
     * Gets the path.
     *
     * @param flow the flow
     * @return the path
     */
    public ArrayList<LightPath> getPath(Flow flow);
    
    
    /**
     * Gets the mapped flows.
     *
     * @return the mapped flows
     */
    public Map<Flow, ArrayList<LightPath>> getMappedFlows();

    
    //public  ArrayList<Flow> getLightPath(LightPath path);
    
    public  ArrayList<LightPath> getLightPathsProtectedByLightPath(LightPath path);
	/**
	 * @param flow
	 * @param lp
	 * @param bestDemandInSlots
	 */
	public void groomFlow(Flow flow, LightPath lp, int bestDemandInSlots);

	public boolean acceptFlow(long id, ArrayList<LightPath> lightpath, ArrayList<LightPath> lightpath2);
}
