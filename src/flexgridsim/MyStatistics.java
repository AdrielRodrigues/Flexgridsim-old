package flexgridsim;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import flexgridsim.util.Modulations;

/**
 * The Class MyStatistics.
 */
public class MyStatistics {
	private static MyStatistics singletonObject;
	private OutputManager plotter;
	private PhysicalTopology pt;
	private TrafficGenerator traffic;
    private int minNumberArrivals;
    private int numberArrivals;
    private int arrivals;
    private int departures;
    private int accepted;
    private int blocked;
    private int requiredBandwidth;
    private int blockedBandwidth;
    private int numNodes;
    private int[][] arrivalsPairs;
    private int[][] blockedPairs;
    private int[][] requiredBandwidthPairs;
    private int[][] blockedBandwidthPairs;
    private double load;
    private double ectotal;
    private double ectrans;
    private double simTime;
    private double dataTransmitted;
    private double avgBitsPerSymbol;
    private int avgBitsPerSymbolCount;
    // Diff
    private int numClasses;
    private int[] arrivalsDiff;
    private int[] blockedDiff;
    private int[] requiredBandwidthDiff;
    private int[] blockedBandwidthDiff;
    private int[][][] arrivalsPairsDiff;
    private int[][][] blockedPairsDiff;
    private int[][][] requiredBandwidthPairsDiff;
    private int[][][] blockedBandwidthPairsDiff;
    private int[][] numberOfUsedTransponders;;
    
    // Failure
    private int failures;
    //TODO private int redirectedBandwidth;
    private int[][] failuresPairs;
    //
    private int[] modulations;
    private int connections;
    
    
    /**
     * A private constructor that prevents any other class from instantiating.
     */
    private MyStatistics() {
    	
    	connections = 0;
    	modulations = new int[6];
    	for(int i = 0; i < 6; i++)
    		modulations[i] = 0;
    	
        numberArrivals = 0;

        arrivals = 0;
        departures = 0;
        failures = 0;
        accepted = 0;
        blocked = 0;

        requiredBandwidth = 0;
        blockedBandwidth = 0;
    }
    
    public int getblocked() {
    	return this.blocked;
    }
    
    /**
     * Creates a new MyStatistics object, in case it does'n exist yet.
     * 
     * @return the MyStatistics singletonObject
     */
    public static synchronized MyStatistics getMyStatisticsObject() {
        if (singletonObject == null) {
            singletonObject = new MyStatistics();
        }
        return singletonObject;
    }
    
    /**
     * Throws an exception to stop a cloned MyStatistics object from
     * being created.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    /**
     * Attributes initializer.
     *
     * @param plotter the graph plotter
     * @param pt the pt
     * @param traffic 
     * @param numNodes number of nodes in the network
     * @param numClasses number of classes of service
     * @param minNumberArrivals minimum number of arriving events
     * @param load the load of the network
     */
    public void statisticsSetup(OutputManager plotter, PhysicalTopology pt, TrafficGenerator traffic, int numNodes, int numClasses, int minNumberArrivals, double load) {
    	this.plotter = plotter;
    	this.pt = pt;
    	this.traffic = traffic;
        this.numNodes = numNodes;
        this.load =  (int)load;
      //htop
      // this.load=load/1000;
        this.arrivalsPairs = new int[numNodes][numNodes];
        this.blockedPairs = new int[numNodes][numNodes];
        this.failuresPairs = new int [numNodes][numNodes];
        this.requiredBandwidthPairs = new int[numNodes][numNodes];
        this.blockedBandwidthPairs = new int[numNodes][numNodes];
        this.avgBitsPerSymbol = 0;
        this.avgBitsPerSymbolCount = 0;
        this.minNumberArrivals = minNumberArrivals;
        numberOfUsedTransponders = new int[numNodes][numNodes];
        //Diff
        this.numClasses = numClasses;
        this.arrivalsDiff = new int[numClasses];
        this.blockedDiff = new int[numClasses];
        this.requiredBandwidthDiff = new int[numClasses];
        this.blockedBandwidthDiff = new int[numClasses];
        for (int i = 0; i < numClasses; i++) {
            this.arrivalsDiff[i] = 0;
            this.blockedDiff[i] = 0;
            this.requiredBandwidthDiff[i] = 0;
            this.blockedBandwidthDiff[i] = 0;
        }
        this.arrivalsPairsDiff = new int[numNodes][numNodes][numClasses];
        this.blockedPairsDiff = new int[numNodes][numNodes][numClasses];
        this.requiredBandwidthPairsDiff = new int[numNodes][numNodes][numClasses];
        this.blockedBandwidthPairsDiff = new int[numNodes][numNodes][numClasses];
        this.ectotal = 0;
        this.ectrans = 0;
        this.simTime = 0;
        this.dataTransmitted = 0;
    }
	/**
	 * Calculate last statistics for the graph generation.
	 */
	public void calculateLastStatistics(){
		//bandwidth block graph
		avgBitsPerSymbol = avgBitsPerSymbol/avgBitsPerSymbolCount;
		plotter.addDotToGraph("avgbps", load, avgBitsPerSymbol);
		plotter.addDotToGraph("mbbr", load, ((float) blockedBandwidth) / ((float) requiredBandwidth));
		plotter.addDotToGraph("bp", load, ((float) blocked) / ((float) arrivals) * 100);
		int count = 0;
        float bbr, jfi, sum1 = 0, sum2 = 0;
        if (blocked == 0) {
            bbr = 0;
        } else {
            bbr = ((float) blockedBandwidth) / ((float) requiredBandwidth) * 100;
        }
        for (int i = 0; i < numNodes; i++) {
            for (int j = i + 1; j < numNodes; j++) {
                if (i != j) {
                    if (blockedPairs[i][j] == 0) {
                        bbr = 0;
                    } else {
                        bbr = ((float) blockedBandwidthPairs[i][j]) / ((float) requiredBandwidthPairs[i][j]) * 100;
                    }
                    count++;
                    sum1 += bbr;
                    sum2 += bbr * bbr;
                }
            }
        }
        jfi = (sum1 * sum1) / ((float) count * sum2);
        plotter.addDotToGraph("jfi", load, jfi);
    	//POWE CONSUPTION
    	double ecoxc = 0;
    	for (int i = 0; i < pt.getNumNodes(); i++) {
    		//OXCs consume 150 Watts and each port 85 Watts. 
			ecoxc += pt.getNodeDegree(i) * 85 + 150;
		}
    	// (*tempo de operação)
    	ecoxc=ecoxc*simTime;
    	//EDFAs have a constant power consumption of 200 Watts. (*tempo de operação)
    	double ecedfa = pt.getNumLinks() * (60*pt.getNumCores()+140) ;
    	 //(*tempo de operação)
    	 ecedfa= ecedfa*simTime;
    	
    	//converte para kj
    	ectotal=  (ectrans + ecoxc + ecedfa);
    	
    	
    	//The data transmitted is given by the product of the transmission rate of the flow (TRFlow) and the flow duration(Mbits).
    	plotter.addDotToGraph("data",load, dataTransmitted);
    	// Energy consumption (Joules)
    	plotter.addDotToGraph("pc",load, ectotal/(simTime*1000));//tira o mil vira kjoules
    	//energy efficiency  (Mb/Joule) is defined as	the ratio between the total data transmitted, and the total energy consumed in the network
    	plotter.addDotToGraph("ee",load, dataTransmitted/(ectotal/1000));
//?
    	plotter.addDotToGraph("ee2",load, (((float) blockedBandwidth) / ((float) requiredBandwidth)) / (ectotal/(simTime*1000)));
    	
    	if(plotter.verify("percent")) {
    		String name = plotter.getNameByTag("percent");
    		new InfoWriter(load, modulations, connections, name);
    	}
    	
    	
	}
	
	/**
	 * Calculate periodical statistics.
	 */
	public void calculatePeriodicalStatistics(ControlPlane cp){
		//fragmentation graph
		double fragmentationMean = 0;
		double averageCrosstalk = 0;
    	for (int i = 0; i < pt.getNumLinks(); i++) {
    		try {
    			fragmentationMean += pt.getLink(i).getFragmentationRatio(traffic.getCallsTypeInfo(), pt.getSlotCapacity());//pt.getSlotCapacity());
    			averageCrosstalk += pt.getLink(i).averageCrosstalk();
    		} catch (NullPointerException e) {
    			
    		}
		}
    	averageCrosstalk /= pt.getNumLinks();
    	plotter.addDotToGraph("avgcrosstalk", load, averageCrosstalk);
    	fragmentationMean = fragmentationMean / pt.getNumLinks();
		//System.out.println("Frag:"+fragmentationMean);
    	if (Double.compare(fragmentationMean, Double.NaN)!=0){
    		plotter.addDotToGraph("fragmentation",load, fragmentationMean);
    	}
    	double meanTransponders = 0;
    	for (int i = 0; i < numberOfUsedTransponders.length; i++) {
			for (int j = 0; j < numberOfUsedTransponders[i].length; j++) {
				if (numberOfUsedTransponders[i][j]>0){
					meanTransponders += numberOfUsedTransponders[i][j];
				}
			}
		}
//    	meanTransponders = meanTransponders / size;
    	if (Double.compare(meanTransponders, Double.NaN)!=0){
    		plotter.addDotToGraph("transponders",load, meanTransponders);
    	}
    	
    	
    	//double xtps = 0;
    	double xtpsP = 0;
    	//int linksXtps = 0;
    	int linksXtpsP = 0;
    	//double crossIndecibeisA =0;
    	//double crossIndecibeisB =0;
    	//double crossIndecibeisC =0;
    	//double crossIndecibeisD =0;
    	for (int i = 0; i < pt.getNumLinks(); i++) {
    		try {
	    		//crossIndecibeisA +=  pt.getLink(i).getCrossTalkDB('A');
	    		//crossIndecibeisB +=  pt.getLink(i).getCrossTalkDB('B');
	    		//crossIndecibeisC +=  pt.getLink(i).getCrossTalkDB('C');
	    		//crossIndecibeisD +=  pt.getLink(i).getCrossTalkDB('D');
		    			
    			/* Crosstalk velho*/
            //	double xt = pt.getLink(i).getCrossTalkPerSlot();
    			double xtP = pt.getLink(i).getCrossTalkPerSlotb();
    		//	if (xt!=-1){
    			//	xtps += xt;
    			//	linksXtps++;
    			//}
    			if (xtP!=-1){
    				xtpsP += xtP;
    				linksXtpsP++;
    			}
    		} catch (NullPointerException e) {
    			
    		}
		}
    	/* Crosstalk em Decibeis*/
    	//plotter.addDotToGraph("decibeisA",load, crossIndecibeisA/ pt.getNumLinks());
    	//plotter.addDotToGraph("decibeisB",load, crossIndecibeisB/ pt.getNumLinks());
    	//plotter.addDotToGraph("decibeisC",load, crossIndecibeisC/ pt.getNumLinks());
    	//plotter.addDotToGraph("decibeisD",load, crossIndecibeisD/ pt.getNumLinks());
	
    	
    /*	if (xtps!=0)
    		plotter.addDotToGraph("xtpsok",load, xtps/ linksXtps);*/
    	
    	/* Crosstalk de Proteção*/
    	if (xtpsP!=0)
    		plotter.addDotToGraph("xtps",load, xtpsP/ linksXtpsP);
    	
    	double pcima = 0;
    	double time = 0;
    	int s = cp.getActiveFlows().size();
    	int c = 1;
    	for(Flow flow : cp.getActiveFlows().values()) {
    		pcima += flow.getDuration() * (flow.getRate()/pt.getSlotCapacity());
    		if(c == s)
    			time = flow.getTime();
    		c++;
    	}
    	
    	double su = pcima/(time * pt.getNumCores() * pt.getNumSlots());
    	
    	plotter.addDotToGraph("su", load, su);
    	
    	
	}
	
    /**
     * Adds an accepted flow to the statistics.
     * 
     * @param flow the accepted Flow object
     * @param lightpath lightpath of the flow
     */
    public void acceptFlow(Flow flow, ArrayList<LightPath> lightpath,  ArrayList<LightPath> lightpath2) {
        if (this.numberArrivals > this.minNumberArrivals){
	        this.accepted++;    
        	int links =  flow.getSizeAllLinks()+1;
        	int linksp = flow.getSizeAllLinksp();
        	int linkst =  links+1+ linksp;
            int slotsocuped= ( links + linksp-1)*(int)(Math.ceil(flow.getRate() / (double) pt.getSlotCapacity()));
            plotter.addDotToGraph("modulation",load, flow.getModulationLevel());
            plotter.addDotToGraph("hops",load, links);
            plotter.addDotToGraph("hopsback",load, linksp);
            plotter.addDotToGraph("slotsoccupied",load, linkst*slotsocuped);
            //taxa em bytes * duracao
            dataTransmitted += flow.getRate();
            ectrans += flow.getDuration() * (flow.getSlotList().size()-1)* Modulations.getPowerConsumption(flow.getModulationLevel());
            numberOfUsedTransponders[flow.getSource()][flow.getDestination()]++;            
        }
    }
    public void acceptFlow(Flow flow, ArrayList<LightPath> lightpath) {
        if (this.numberArrivals > this.minNumberArrivals){
        	this.avgBitsPerSymbol+=Modulations.bitsPerSymbol(flow.getModulationLevel());
        	this.avgBitsPerSymbolCount++;
	        this.accepted++;
	        int links =  flow.getSizeAllLinks()+1;
        	int linksp = flow.getSizeAllLinksp();
        	int linkst =  links+1+ linksp;
            int slotsocuped= ( links-1)*(int)(Math.ceil(flow.getRate() / (double) pt.getSlotCapacity()));
        	int n = 0;
            for(LightPath lp:lightpath) {
        		plotter.addDotToGraph("modulation",load, flow.getModulationLevel(n));
        		n++;
            }
            plotter.addDotToGraph("hops",load, links);
            plotter.addDotToGraph("slotsoccupied",load, linkst*slotsocuped);
            //taxa em Mbs * duracao
            dataTransmitted += flow.getRate();
            ectrans += flow.getDuration() *( flow.getSlotList().size()-1) * Modulations.getPowerConsumption(flow.getModulationLevel());
            numberOfUsedTransponders[flow.getSource()][flow.getDestination()]++;
            
            n = 0;
            for(LightPath lp:lightpath) {
        		modulations[flow.getModulationLevel(n)]++;
        		connections++;
        		n++;
            }
        }
    }
    /**
     * Groomed flow.
     *
     * @param flow the flow
     */
    public void groomedFlow(Flow flow){
    	if (this.numberArrivals > this.minNumberArrivals){
            //taxa em bytes * duracao
            dataTransmitted += flow.getRate();
           	ectrans += flow.getDuration() * (flow.getSlotList().size()-1) *Modulations.getPowerConsumption(flow.getModulationLevel());
        }
    }
    /**
     * Adds a blocked flow to the statistics.
     * 
     * @param flow the blocked Flow object
     */
    public void blockFlow(Flow flow) {
        if (this.numberArrivals > this.minNumberArrivals) {
	        this.blocked++;
            int cos = flow.getCOS();
            this.blockedDiff[cos]++;
            this.blockedBandwidth += flow.getRate();
            this.blockedBandwidthDiff[cos] += flow.getRate();
            this.blockedPairs[flow.getSource()][flow.getDestination()]++;
            this.blockedPairsDiff[flow.getSource()][flow.getDestination()][cos]++;
            this.blockedBandwidthPairs[flow.getSource()][flow.getDestination()] += flow.getRate();
            this.blockedBandwidthPairsDiff[flow.getSource()][flow.getDestination()][cos] += flow.getRate();
        }
    }
    
    /**
     * Adds an event to the statistics.
     * 
     * @param event the Event object to be added
     */
    public void addEvent(Event event, ControlPlane cp) {
    	simTime = event.getTime();
        try {
            if (event instanceof FlowArrivalEvent) {
                this.numberArrivals++;
                if (this.numberArrivals > this.minNumberArrivals) {
                    int cos = ((FlowArrivalEvent) event).getFlow().getCOS();
                    this.arrivals++;
                    this.arrivalsDiff[cos]++;
                    this.requiredBandwidth += ((FlowArrivalEvent) event).getFlow().getRate();
                    this.requiredBandwidthDiff[cos] += ((FlowArrivalEvent) event).getFlow().getRate();
                    this.arrivalsPairs[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()]++;
                    this.arrivalsPairsDiff[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()][cos]++;
                    this.requiredBandwidthPairs[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()] += ((FlowArrivalEvent) event).getFlow().getRate();
                    this.requiredBandwidthPairsDiff[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()][cos] += ((FlowArrivalEvent) event).getFlow().getRate();
                }
                if (Simulator.verbose && Math.IEEEremainder((double) arrivals, (double) 10000) == 0) {
                    System.out.println(Integer.toString(arrivals));
                }
            }
            else if (event instanceof FlowDepartureEvent) {
                if (this.numberArrivals > this.minNumberArrivals) {
                    this.departures++;
                }
                Flow f = ((FlowDepartureEvent)event).getFlow();
                if (f.isAccepeted()){
                	this.numberOfUsedTransponders[f.getSource()][f.getDestination()]--;
                }
            }
            else if (event instanceof FailureIn) {
            	Failure ff = ((FailureIn) event).getFail();
            	this.failures++;
            	this.failuresPairs[ff.getSrc()][ff.getDst()]++;
            }
            else if (event instanceof FailureOut) {
            	//TODO
            }
            
            if (this.numberArrivals % 100 == 0){
            	calculatePeriodicalStatistics(cp);
            	
            }
            if (this.numberArrivals % 5000 == 0){
            	
//            	System.out.println(event.getTime()+","+BFR);
            }
        }
        
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * This function is called during the simulation execution, but only if
     * verbose was activated.
     * 
     * @return string with the obtained statistics
     */
    public String fancyStatistics() {
        float acceptProb, blockProb, bbr;
        if (accepted == 0) {
            acceptProb = 0;
        } else {
            acceptProb = ((float) accepted) / ((float) arrivals) * 100;
        }
        if (blocked == 0) {
            blockProb = 0;
            bbr = 0;
        } else {
            blockProb = ((float) blocked) / ((float) arrivals) * 100;
            bbr = ((float) blockedBandwidth) / ((float) requiredBandwidth) * 100;
        }

        String stats = "Arrivals \t: " + Integer.toString(arrivals) + "\n";
        stats += "Required BW \t: " + Integer.toString(requiredBandwidth) + "\n";
        stats += "Departures \t: " + Integer.toString(departures) + "\n";
        stats += "Accepted \t: " + Integer.toString(accepted) + "\t(" + Float.toString(acceptProb) + "%)\n";
        stats += "Blocked \t: " + Integer.toString(blocked) + "\t(" + Float.toString(blockProb) + "%)\n";
        stats += "BBR     \t: " + Float.toString(bbr) + "%\n";
        stats += "\n";
        stats += "Blocking probability per s-d pair:\n";
        for (int i = 0; i < numNodes; i++) {
            for (int j = i + 1; j < numNodes; j++) {
                stats += "Pair (" + Integer.toString(i) + "->" + Integer.toString(j) + ") ";
                stats += "Calls (" + Integer.toString(arrivalsPairs[i][j]) + ")";
                if (blockedPairs[i][j] == 0) {
                    blockProb = 0;
                    bbr = 0;
                } else {
                    blockProb = ((float) blockedPairs[i][j]) / ((float) arrivalsPairs[i][j]) * 100;
                    bbr = ((float) blockedBandwidthPairs[i][j]) / ((float) requiredBandwidthPairs[i][j]) * 100;
                }
                stats += "\tBP (" + Float.toString(blockProb) + "%)";
                stats += "\tBBR (" + Float.toString(bbr) + "%)\n";
            }
        }

        return stats;
    }
    
    /**
     * Prints all the obtained statistics, but only if verbose was not activated.
     *
     */
    public void printStatistics() {
        int count = 0;
        float bp, bbr, jfi, sum1 = 0, sum2 = 0;
        float bpDiff[], bbrDiff[];

        if (blocked == 0) {
            bp = 0;
            bbr = 0;
        } else {
            bp = ((float) blocked) / ((float) arrivals) * 100;
            bbr = ((float) blockedBandwidth) / ((float) requiredBandwidth) * 100;
        }
        bpDiff = new float[numClasses];
        bbrDiff = new float[numClasses];
        for (int i = 0; i < numClasses; i++) {
            if (blockedDiff[i] == 0) {
                bpDiff[i] = 0;
                bbrDiff[i] = 0;
            } else {
                bpDiff[i] = ((float) blockedDiff[i]) / ((float) arrivalsDiff[i]) * 100;
                bbrDiff[i] = ((float) blockedBandwidthDiff[i]) / ((float) requiredBandwidthDiff[i]) * 100;
            }
        }
        System.out.println("MBP " + Float.toString(bp));
        for (int i = 0; i < numClasses; i++) {
            System.out.println("MBP-" + Integer.toString(i) + " " + Float.toString(bpDiff[i]));
        }
        System.out.println("MBBR " + Float.toString(bbr));
        for (int i = 0; i < numClasses; i++) {
            System.out.println("MBBR-" + Integer.toString(i) + " " + Float.toString(bbrDiff[i]));
        }

        for (int i = 0; i < numNodes; i++) {
            for (int j = i + 1; j < numNodes; j++) {
                if (i != j) {
                    System.out.print(Integer.toString(i) + "-" + Integer.toString(j) + " ");
                    System.out.print("A " + Integer.toString(arrivalsPairs[i][j]) + " ");
                    if (blockedPairs[i][j] == 0) {
                        bp = 0;
                        bbr = 0;
                    } else {
                        bp = ((float) blockedPairs[i][j]) / ((float) arrivalsPairs[i][j]) * 100;
                        bbr = ((float) blockedBandwidthPairs[i][j]) / ((float) requiredBandwidthPairs[i][j]) * 100;
                    }
                    count++;
                    sum1 += bbr;
                    sum2 += bbr * bbr;
                    System.out.print("BP " + Float.toString(bp) + " ");
                    System.out.println("BBR " + Float.toString(bbr));
                }
            }
        }
        jfi = (sum1 * sum1) / ((float) count * sum2);
        System.out.println("JFI " + Float.toString(jfi));
        //Diff
        for (int c = 0; c < numClasses; c++) {
            count = 0;
            sum1 = 0;
            sum2 = 0;
            for (int i = 0; i < numNodes; i++) {
                for (int j = i + 1; j < numNodes; j++) {
                    if (i != j) {
                        if (blockedPairsDiff[i][j][c] == 0) {
                            bp = 0;
                            bbr = 0;
                        } else {
                            bp = ((float) blockedPairsDiff[i][j][c]) / ((float) arrivalsPairsDiff[i][j][c]) * 100;
                            bbr = ((float) blockedBandwidthPairsDiff[i][j][c]) / ((float) requiredBandwidthPairsDiff[i][j][c]) * 100;
                        }
                        count++;
                        sum1 += bbr;
                        sum2 += bbr * bbr;
                    }
                }
            }
            jfi = (sum1 * sum1) / ((float) count * sum2);
            System.out.println("JFI-" + Integer.toString(c) + " " + Float.toString(jfi));
        }
    }
	
    
    /**
     * Terminates the singleton object.
     */
    public void finish()
    {
        singletonObject = null;
    }
}
