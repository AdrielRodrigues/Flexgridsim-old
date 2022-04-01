/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim;

import java.util.ArrayList;
import flexgridsim.InfoWriter;

import org.w3c.dom.*;

import flexgridsim.util.Distribution;

/**
 * Generates the network's traffic based on the information passed through the
 * command line arguments and the XML simulation file.
 * 
 * @author andred
 */
public class TrafficGenerator {

    private int calls;
    private double load;
    private int maxRate;
    private TrafficInfo[] callsTypesInfo;
	private double meanRate;
    private double meanHoldingTime;
    private int TotalWeight;
    private int numberCallsTypes;
    private Element xml;
    private int[] minRate;
    private double[] minSize;
    private double[] maxSize;
    private NodeList fileSizes;

    /**
     * Creates a new TrafficGenerator object.
     * Extracts the traffic information from the XML file and takes the chosen load and
     * seed from the command line arguments.
     * 
     * @param xml file that contains all information about the simulation
     * @param forcedLoad range of offered loads for several simulations
     */
    
    public TrafficGenerator(Element xml, double forcedLoad) {
        int rate, cos, weight;
        double holdingTime;

        this.xml = xml;
        calls = Integer.parseInt(xml.getAttribute("calls"));
        load = forcedLoad;
        if (load == 0) {
            load = Double.parseDouble(xml.getAttribute("load"));
        }
        maxRate = Integer.parseInt(xml.getAttribute("max-rate"));

        if (Simulator.verbose) {
            System.out.println(xml.getAttribute("calls") + " calls, " + xml.getAttribute("load") + " erlangs.");
        }

        // Process calls
        NodeList callslist = xml.getElementsByTagName("calls");
        numberCallsTypes = callslist.getLength();
        if (Simulator.verbose) {
            System.out.println(Integer.toString(numberCallsTypes) + " type(s) of calls:");
        }

        callsTypesInfo = new TrafficInfo[numberCallsTypes];

        TotalWeight = 0;
        meanRate = 0;
        meanHoldingTime = 0;

        for (int i = 0; i < numberCallsTypes; i++) {
            TotalWeight += Integer.parseInt(((Element) callslist.item(i)).getAttribute("weight"));
        }

        for (int i = 0; i < numberCallsTypes; i++) {
            holdingTime = Double.parseDouble(((Element) callslist.item(i)).getAttribute("holding-time"));
            rate = Integer.parseInt(((Element) callslist.item(i)).getAttribute("rate"));
            cos = Integer.parseInt(((Element) callslist.item(i)).getAttribute("cos"));
            weight = Integer.parseInt(((Element) callslist.item(i)).getAttribute("weight"));
            meanRate += (double) rate * ((double) weight / (double) TotalWeight);
            meanHoldingTime += holdingTime * ((double) weight / (double) TotalWeight);
            callsTypesInfo[i] = new TrafficInfo(holdingTime, rate, cos, weight);
            if (Simulator.verbose) {
                System.out.println("#################################");
                System.out.println("Weight: " + Integer.toString(weight) + ".");
                System.out.println("COS: " + Integer.toString(cos) + ".");
                System.out.println("Rate: " + Integer.toString(rate) + "Mbps.");
                System.out.println("Mean holding time: " + Double.toString(holdingTime) + " seconds.");
            }
        }
    }

    /**
     * Generates the network's traffic.
     *
     * @param events EventScheduler object that will contain the simulation events
     * @param pt the network's Physical Topology
     * @param seed a number in the interval [1,25] that defines up to 25 different random simulations
     */

	public void generateTraffic(PhysicalTopology pt, EventScheduler events, int seed) {

        // Compute the weight vector
        int[] weightVector = new int[TotalWeight];
        int aux = 0;
        for (int i = 0; i < numberCallsTypes; i++) {
            for (int j = 0; j < callsTypesInfo[i].getWeight(); j++) {
                weightVector[aux] = i;
                aux++;
            }
        }

        /* Compute the arrival time
         *
         * load = meanArrivalRate x holdingTime x bw/maxRate
         * 1/meanArrivalRate = (holdingTime x bw/maxRate)/load
         * meanArrivalTime = (holdingTime x bw/maxRate)/load
         */
        double meanArrivalTime = (meanHoldingTime * (meanRate / (double) maxRate)) / load;

      //Generate events
        int type, src, dst;
        double time = 0.0;
        int id = 0;
        int numNodes = pt.getNumNodes();
        Distribution dist1, dist2, dist3, dist4;//, dist5;
        dist1 = new Distribution(1, seed);
        dist2 = new Distribution(2, seed);
        dist3 = new Distribution(3, seed);
        dist4 = new Distribution(4, seed);

        //VERIFICAR SE HÃ� CONDIÃ‡ÃƒO PARA GERAÃ‡ÃƒO DE FALHA

        for (int j = 0; j < calls; j++) {
        	
        	//CONDIÃ‡ÃƒO DE FALHA QUE PROVAVELMENTE DEVE SER TRABALHADO COM PROBABILIDADE
        	if(Simulator.failure) {
	        	if(j == 50 || j == 100 || j == 500) {
	        		double holdingTime;
	        		type = weightVector[dist1.nextInt(TotalWeight)];
	        		
	        		src = dst = dist2.nextInt(numNodes);
	        		while(!pt.hasLink(src, dst)) {
                                    dst = dist1.nextInt(pt.getNumNodes());
	        		}
	        		
	        		Failure newFailure = new Failure(id, pt, seed, src, dst);
	        		holdingTime = dist4.nextExponential(callsTypesInfo[type].getHoldingTime()); // Maybe if I change a parameter the number changes also  
	        		Event event = new FailureIn(time, newFailure);
		            time += dist3.nextExponential(meanArrivalTime);
		            events.addEvent(event);
		            event = new FailureOut(time + holdingTime, id, newFailure);
		            events.addEvent(event);
		            id++;
	        	}
        	}
            type = weightVector[dist1.nextInt(TotalWeight)];
            src = dst = dist2.nextInt(numNodes);
            while (src == dst) {
                dst = dist2.nextInt(numNodes);
            }
            
            double holdingTime;
            //verifica se ha o atributo fileSizeValues, que indica que e utilizado esquema de batch

            /*	
			if (this.xml.hasAttribute("fileSizeValues")){
				double fileSize = dist2.nextDoubleInTheInterval(minSize[j], maxSize[j]);
				double rateInGbps = ocInGigaBits(callsTypesInfo[type].getRate());
				holdingTime = (((fileSize)/rateInGbps)*8);
		    } else {
		    */

            holdingTime = dist4.nextExponential(callsTypesInfo[type].getHoldingTime());

            /* }*/
            Flow newFLow = new Flow(id, src, dst, time, callsTypesInfo[type].getRate(), holdingTime, callsTypesInfo[type].getCOS(), time+(holdingTime*0.5));
            Event event;
            //new InfoWriter(new Flow(id, src, dst, time, callsTypesInfo[type].getRate(), holdingTime, callsTypesInfo[type].getCOS(), time+(holdingTime*0.5)));
            event = new FlowArrivalEvent(time, newFLow);
            time += dist3.nextExponential(meanArrivalTime);
            events.addEvent(event);
            event = new FlowDepartureEvent(time + holdingTime, id, newFLow);
            events.addEvent(event);
            id++;
    	}

	        /*	if (j==2000 || j==4000|| j== 6000|| j== 8000|| j== 10000) {
	        	linkfail = dist5.nextInt(pt.getNumLinks());
	        	System.out.println(linkfail);
	        }*/
	
	        /*
	         * se j = 20.000 40.000 60.000 80.000 100.000
	         * calculo a disponibilade se arrancar um link que pode ser gerado usando dist5.nextInt(numlinks)
	         * quantos caminhos foram afetados e quantos nÃ£o se recuperaram
	         **/
        
    }
    
    /**
     * Gets the calls type info.
     *
     * @return the calls type info
     */

    public TrafficInfo[] getCallsTypeInfo() {
		return callsTypesInfo;
	}
    
    /**
     * OC in giga bits.
     *
     * @param oc the oc
     * @return the double
     */
  /*  private double ocInGigaBits(int oc){
    	//System.out.println("Aquin\n\n\n\n");
        double rateInGbps = 0;
        switch(oc) {
            case 3:
                rateInGbps = 0.1; //0.1555; // 155.52Mbps
                break;
            case 12:
                rateInGbps = 0.5; //0.622; //622.08Mbps
                break;
            case 24:
                rateInGbps = 1.0; //1.244; //1244,16Mbps
                break;
            case 48:
                rateInGbps = 2.5; //2.488; //2488.32Mbps
                break;
            case 96:
                rateInGbps = 5.0; //4.975360082; //4976.00 MBps
                break;
            case 192:
                rateInGbps = 10.0; //9.950720165; //9952.00Mbps;
                break;
            default: System.out.println("invalid rate!!");
                System.exit(1);
        }

        return(rateInGbps);
     }
*/
}
