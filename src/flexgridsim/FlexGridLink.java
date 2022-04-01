package flexgridsim;

import java.util.ArrayList;
import java.util.*;
import flexgridsim.util.Crosstalk;
import flexgridsim.util.Decibel;
import flexgridsim.util.ModulationsMuticore;
import flexgridsim.util.Modulations;

/**
 * This class is based on the WDMLink but it's adapted to RSA operations for
 * contiguous slots allocation.
 * 
 * @author adriel
 */
public class FlexGridLink {

	private int id;
	private int src;
	private int dst;
	private double delay; //posso achar esse valor usando uma regra de 3 com o valor de weight - vice-versa
	private int slots;
	private boolean[][] freeSlots; //se for usado pelo caminho ou protecao é false
	private boolean[][] SlotCanBeShared;// se o slot é usado pra proteger ele é true caso seja usado pelo caminho ele é false
	private int[][] timeSharing;//conta quantos caminhos estão usando ele
	private double weight;
	private int[][] modulationLevel;
	private int distance;
	private int cores;
	
	private boolean linkAtivo;//true se não houver falhas no link
	
	private double [] FMM;
	
	//Pedro's
	private double [][] noise;
	
	private Map<Long, Flow> flowsHere;
	private long whichBlockedThisLink;
	
	private int[][] ava;

	/**
	 * Creates a new Fiberlink object.
	 *
	 * @param id
	 *            unique identifier
	 * @param src
	 *            source node
	 * @param dst
	 *            destination node
	 * @param cores
	 *            number of fiber cores
	 * @param delay
	 *            propagation delay (miliseconds)
	 * @param slots
	 *            number of slots available
	 * @param weight
	 *            optional link weight
	 * @param distance
	 *            the distance
	 */
	
	public FlexGridLink(int id, int src, int dst, int cores, double delay, int slots, double weight, int distance) {
		if (id < 0 || src < 0 || dst < 0 || slots < 1) {
			throw (new IllegalArgumentException());
		} else {
			this.id = id;
			this.src = src;
			this.dst = dst;
			this.delay = delay;
			this.slots = slots;
			this.weight = weight;
			this.cores = cores;
			this.freeSlots = new boolean[cores][slots];
			this.SlotCanBeShared = new boolean[cores][slots];
			this.timeSharing = new int[cores][slots];
			this.linkAtivo = true; // MUDANÇA
			this.modulationLevel = new int[cores][slots];
			
			this.noise = new double[cores][slots];
			this.FMM = new double[cores];
			
			this.ava = new int[cores][slots];
			
			this.distance = distance;
			for (int i = 0; i < cores; i++) {
				for (int j = 0; j < slots; j++) {
					this.freeSlots[i][j] = true;
					this.SlotCanBeShared[i][j] = true;
					this.timeSharing[i][j] = 0;//quantos vezes foram usados pelo p-cycle
					this.modulationLevel[i][j] = -1;
					
					//this.noise[i][j] = -60;
					this.noise[i][j] = 0;
					this.ava[i][j] = this.slots - j;
				}
				this.FMM[i] = 0;
			}
			flowsHere = new HashMap<Long, Flow>();
		}
	}
	
	public double rateUsed() {
		int used = 0;
		
		for (int i = 0; i < this.cores; i++) {
			for (int j = 0; j < this.slots; j++) {
				if (!this.freeSlots[i][j]) {
					used++;
				}
			}
		}
		return used;
	}

	public void setWhichBlocked(long id) {
		this.whichBlockedThisLink = id;
	}
	public long getWhichBlocked() {
		return this.whichBlockedThisLink;
	}
	/**
	 * Puts flows that use this link
	 * 
	 * @param id identifier
	 * @param flow flow that uses link
	 * 
	 * @return void
	 *
	 **/

	public boolean insertFlow(long id, Flow flow) {
		flowsHere.put(id, flow);
		return true;
	}
	public Flow removeLinkFlow(long id) {
		return flowsHere.remove(id);
	}
	public Map<Long, Flow> getFlowsHere(){
		return flowsHere;
	}
	public boolean zerarLink() {
		flowsHere = null;
		if(flowsHere == null)
			return true;
		else
			return false;
	}

	/**
	 * Gets the number of free slots in the link.
	 *
	 * @return the free slots
	 */
	public boolean[][] getSpectrum() {
		return freeSlots;
	}
	//getSpectrumS tem que receber um parametro que é o link se esse link ja estiver nos caminhos ja usados return false se não retorna true
	//quando criar um caminho de proteção e puder compartilhar slot, o slot deve receber o id do lightPath para poder acessar os links e quendo remover remove o ligthpath
	/**
	 * Gets the number of free slots in the link with can  be shared.
	 *
	 * @return the free slots
	 */
	public boolean[][] getSpectrumS() {
		return SlotCanBeShared;
	}

	/**
	 * Gets the number of time with the slot was shared.
	 *
	 * @return the free slots
	 */
	public int[][] getUsed() {
		return timeSharing;
	}

	/**
	 * Gets the number of free slots in the link.
	 * 
	 * @param core
	 *
	 * @return the free slots
	 */
	public boolean[] getSpectrum(int core) {
		return freeSlots[core];
	}
	//retorna se slot esta vazio
	public boolean getSpectrum(int core, int slot) {
		return freeSlots[core][slot];
	}
	/**
	 * Gets the number of free slots in the link.
	 * 
	 * @param core
	 *
	 * @return the free slots
	 */
	public boolean[] getSpectrumS(int core) {
		return SlotCanBeShared[core];
	}
	//retorna se slot esta vazio
	public boolean getSpectrumS(int core, int slot) {
		return SlotCanBeShared[core][slot];
	}
	
	/**
	 * Gets the number of free slots in the link.
	 * 
	 * @param core
	 *
	 * @return the free slots
	 */
	public int[] getUsed(int core) {
		return timeSharing[core];
	}

	/**
	 * Retrieves the unique identifier for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's id attribute
	 */
	public int getID() {
		return this.id;
	}

	/**
	 * Retrieves the source node for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's src attribute
	 */
	public int getSource() {
		return this.src;
	}

	/**
	 * Retrieves the destination node for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's dst attribute
	 */
	public int getDestination() {
		return this.dst;
	}

	/**
	 * Retrieves the number of available slots for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's slots attribute
	 */
	public int getSlots() {
		return this.slots;
	}

	/**
	 * Retrieves the weight for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's weight attribute
	 */
	public double getWeight() {
		return this.weight;
	}

	/**
	 * Retrieves the propagation delay for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's delay attribute
	 */
	public double getDelay() {
		return this.delay;
	}

	/**
	 * Says whether or not a determined set of contiguous slots are available.
	 * 
	 * @param slot
	 *            array of channels for each core to be checked contains first
	 *            and last slot to be allocated
	 * 
	 * @return true if the slots are available
	 */
	//OBS: vista grossa para o link( so passo o slot do link)
	public Boolean isSlotsAvailable(Slot slot) {
			if (slot.link<0||slot.core < 0 || slot.slot < 0 || slot.core >= cores || slot.slot >= slots) {
				throw (new IllegalArgumentException());
			} else {
				if (!freeSlots[slot.core][slot.slot]) {
					//System.out.println("Core: "+ slot.core+"Slot: "+ slot.slot+ "Link: "+slot.link);
					return false;
				} 
			}
		return true;
	}

/**
 * 
 * @param slot
 * @param sharing
 * @return
 **/
	public Boolean isProtectionSlotsAvailable(Slot slot, boolean sharing) {
		if (slot.link<0||slot.core < 0 || slot.slot < 0 || slot.core >= cores || slot.slot >= slots) {
			throw (new IllegalArgumentException());
		} else {
			if(sharing){
				//System.out.println(sharing);
				if (!SlotCanBeShared[slot.core][slot.slot]) {
					//System.out.println("FL- linha 234: "+ "ID:"+id);
					return false;
				} 
			}else{
				if (!freeSlots[slot.core][slot.slot]) {
					//System.out.println("Core: "+ slot.core+"Slot: "+ slot.slot+ "Link: "+slot.link);
					return false;
				} 		
			}
			
		}
	return true;
}
	

	/**
	 * Says whether or not a determined set of contiguous slots are available.
	 * 
	 * @param channel
	 *            array of channels for each core to be checked contains first
	 *            and last slot to be allocated
	 * 
	 * @return true if the slots are available
	 */


	
	public Boolean areSlotsAvailable(ArrayList<Slot> slotList) {
	
		for (int i = 0; i < slotList.size(); i++) {
			if (slotList.get(i).core < 0 || slotList.get(i).slot < 0 || slotList.get(i).core >= cores || slotList.get(i).slot >= slots) {
				throw (new IllegalArgumentException());
			} else {
				for (Slot channel : slotList) {
					if (!freeSlots[channel.core][channel.slot]) {
						return false;
					} 
				}
			}
		}
		return true;
	}
	
	
	
	public Boolean areSlotsAvailableS(ArrayList<Slot> slotList) {
		for (int i = 0; i < slotList.size(); i++) {
			if (slotList.get(i).core < 0 || slotList.get(i).slot < 0 || slotList.get(i).core >= cores || slotList.get(i).slot >= slots) {
				throw (new IllegalArgumentException());
			} else {
				for (Slot channel : slotList) {
					if (!SlotCanBeShared[channel.core][channel.slot]) {
						return false;
					} 
				}
			}
		}
		return true;
	}

	/**
	 * Gets the num free slots.
	 *
	 * @return the num free slots
	 */
//Slot de proteção gera crosstalk

	public int getNumFreeSlots() {
		int numFreeSlots = 0;
		for (int i = 0; i < cores; i++) {
			for (int j = 0; j < slots; j++) {
				if (freeSlots[i][j]) {
					numFreeSlots++;
				}
			} 
		}
		return numFreeSlots;
	}
	//Slot de proteção não gera crosstalk
	public int getNumFreeSlotsP() {
		int numFreeSlots = 0;
		for (int i = 0; i < cores; i++) {
			for (int j = 0; j < slots; j++) {
				if (freeSlots[i][j]||timeSharing[i][j]>0) {
					numFreeSlots++;
				}
			} 
		}
		return numFreeSlots;
	}
	/**
	 * By attributing false to a given slot inside the freSlots array, this
	 * function "reserves" a set of contiguous slots.
	 * 
	 * @param slot
	 *            array of channels for each core to be checked contains first
	 *            and last slot to be allocated
	 * 
	 * @return true if operation was successful, or false otherwise
	 */
	public boolean reserveSlot(Slot slot) {
		try {
				if (slot.link < 0||slot.core < 0 || slot.slot < 0 || slot.core >= cores || slot.slot >= slots) {
					throw (new IllegalArgumentException());
				}
				if (isSlotsAvailable(slot)) {
					freeSlots[slot.core][slot.slot] = false;
					SlotCanBeShared[slot.core][slot.slot] = false;
					ava[slot.core][slot.slot] = 0;
					fixBefore(slot.core, slot.slot);
					//System.out.println("FL- l 342 "+ id+" : "+slot.core+ ""+ slot.slot);
					return true;
				} else {
					return false;
				}
		} catch (IllegalArgumentException e) {
			System.out.println("Illegal argument for reserveSlots");
			return false;
		}

	}
	
	public void fixBefore(int core, int slot) {
		int sequence = 1;
		while(slot > -1 && ava[core][slot] != 0) {
			ava[core][slot] = sequence;
			sequence++;
			slot--;
		}
	}

	public boolean reserveProtectionSlot(Slot slot,  boolean SlotShare) {
		try {
			if (slot.link < 0||slot.core < 0 || slot.slot < 0 || slot.core >= cores || slot.slot >= slots) {
				throw (new IllegalArgumentException());
			}
			if(SlotShare){
				if (isProtectionSlotsAvailable(slot, SlotShare)) {
					freeSlots[slot.core][slot.slot] = false;
					SlotCanBeShared[slot.core][slot.slot] = true;	
					timeSharing[slot.core][slot.slot] ++;
					return true;
				}else {
					return false;
				}	
			}else{
				if (isProtectionSlotsAvailable(slot, SlotShare)) {
					freeSlots[slot.core][slot.slot] = false;
					SlotCanBeShared[slot.core][slot.slot] = false;	
					timeSharing[slot.core][slot.slot] ++;
					return true;
				}else {
					return false;
				}	

				
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Illegal argument for reserveSlots");
			return false;
		}

	}

	/**
	 * By attributing true to a given set of slots inside the freeSlots array,
	 * this function "releases" a set of slots.
	 * 
	 * @param slot
	 *            array of channels for each core to be checked contains first
	 *            and last slot to be allocated
	 */
	public void releaseSlot(Slot slot) {
		if (slot.core < 0 || slot.slot < 0 || slot.core >= cores || slot.slot >= slots) {
				throw (new IllegalArgumentException());
		}
		try{
			freeSlots[slot.core][slot.slot] = true;
			SlotCanBeShared[slot.core][slot.slot] = true;
		} catch (IllegalArgumentException e) {
			System.out.print("Slots para soltar:");
			System.out.print(" ("+slot.core+","+slot.slot+")"+" ");
			System.out.println();
			printSpectrum();
			System.out.println();
		}
	}
	

	public void releaseProtectionSlot(Slot slot, boolean SlotShare) {
		if (slot.core < 0 || slot.slot < 0 || slot.core >= cores || slot.slot >= slots) {
				throw (new IllegalArgumentException());
		}
		try{
			if(SlotShare){
				if(timeSharing[slot.core][slot.slot]==1){
					freeSlots[slot.core][slot.slot] = true;
					timeSharing[slot.core][slot.slot]--;
				}else{
					timeSharing[slot.core][slot.slot]--;
				}
			}else{
				freeSlots[slot.core][slot.slot] = true;
				SlotCanBeShared[slot.core][slot.slot] = true;
				timeSharing[slot.core][slot.slot] --;
			}
		} catch (IllegalArgumentException e) {
			System.out.print("Slots para soltar:");
			System.out.print(" ("+slot.core+","+slot.slot+")"+" ");
			System.out.println();
			printSpectrum();
			System.out.println();
		}
	}
	
	
	/**
	 * Sets the modulation level.
	 *
	 * @param slot
	 *            the slot
	 * @param modulationLevel
	 *            the modulation level
	 */
	public void setModulationLevel(int core, int slot, int modulationLevel) {
		this.modulationLevel[core][slot] = modulationLevel;
	}

	/**
	 * Gets the modulation level.
	 *
	 * @param slot
	 *            the slot
	 * @return the modulation level
	 */
	public int getModulationLevel(int core, int slot) {
		return this.modulationLevel[core][slot];
	}

	/**
	 * Gets the distance.
	 *
	 * @return the distance
	 */
	public int getDistance() {
		return distance;
	}

	
	/**
	 * Gets the CrossTalk desconsidering slots of protection
	 * @return CrossTalk
	 */
	
	public int getActiveNeighbor(int core, int slot){
		int neighbor=0;
		if(core<this.cores-1&&core>0){
			if (!freeSlots[core+1][slot] && timeSharing[core+1][slot]==0&&core+1!=this.cores-1){
				neighbor++;
			}
			if (!freeSlots[core-1][slot] && timeSharing[core-1][slot]==0){
				neighbor++;
			}
			if (!freeSlots[this.cores-1][slot] && timeSharing[this.cores-1][slot]==0){
				neighbor++;
			}
		}
		if(core==this.cores-1){
			for (int i = 0; i < cores-1; i++) {
				if (!freeSlots[i][slot] && timeSharing[i][slot]==0){
					neighbor++;
				}
			}
		}
		if(core==0){
			if (!freeSlots[core+1][slot] && timeSharing[core+1][slot]==0){
				neighbor++;
			}
			if (!freeSlots[this.cores-2][slot] && timeSharing[this.cores-2][slot]==0){
				neighbor++;
			}
			if (!freeSlots[this.cores-1][slot] && timeSharing[this.cores-1][slot]==0){
				neighbor++;
			}
		}
		if(core==this.cores-2){
			if (!freeSlots[core-1][slot]&& timeSharing[this.cores-1][slot]==0){
				neighbor++;
			}
			if (!freeSlots[0][slot]&& timeSharing[this.cores-1][slot]==0){
				neighbor++;
			}
			if (!freeSlots[this.cores-1][slot]&& timeSharing[this.cores-1][slot]==0){
				neighbor++;
			}
		}
		return neighbor;
		
	}
	

	public double getCrossTalkDB(char typeFiber){
		double xt, xtdb=0.0;
		int qntd=0;
		if (cores==1 || getNumFreeSlotsP() == slots*cores){
			return -1;
		}
		for (int i = 0; i < freeSlots.length; i++) {
			for (int j = 0; j < freeSlots[i].length; j++) {
				xt=Crosstalk.getCrosstalkDB(this.distance, getActiveNeighbor(i,j), typeFiber);	
				xtdb +=xt;	
				qntd++;
			}
		}	
		xtdb= xtdb/qntd;
		if(qntd!=0)
			return xtdb;
		else{
			return 0;
		}
	}
	
	
	/**
	 * Gets the fragmentation ratio, a metric that states the potential of each
	 * free contiguous set of slots by telling the number of traffic calls it
	 * could fit in. then calculating the mean of that
	 *
	 * @param trafficCalls
	 *            the traffic calls
	 * @param slotCapacity
	 *            the slot capacity
	 * @return the fragmentation ratio
	 */
	public double getFragmentationRatio(TrafficInfo[] trafficCalls, double slotCapacity) {
		ArrayList<Double> fragmentsPotential = new ArrayList<Double>();
		for (int j = 0; j < this.freeSlots.length; j++) {
			for (int i = 0; i < this.freeSlots[j].length - 1; i++) {
				if (this.freeSlots[0][i] == true) {
					i++;
					int fragmentSize = 1;
					while (freeSlots[j][i] == true && i < freeSlots[j].length - 2) {
						fragmentSize++;
						i++;
					}
					double counter = 0;
					for (TrafficInfo call : trafficCalls) {
						if (call.getRate() / slotCapacity >= fragmentSize) {
							counter++;
						}
					}
					fragmentsPotential.add(counter / trafficCalls.length);
				}
			}
		}
		
		double sum = 0;
		for (Double potential : fragmentsPotential) {
			sum += potential.doubleValue();
		}

		return sum / fragmentsPotential.size();
	}
	
	
	
	
	/**
	 * Gets the CrossTalk Per Slot,
	 * @return CrossTalk Per Slot
	 */
	//Slot de proteção gera crosstalk
	//freeslot é falso se é usado para proteção
	
	public double getCrossTalkPerSlot(){
		if (cores==1 || getNumFreeSlots() == slots*cores){
			return -1;
		}
		int aoc=0;
		for (int i = 0; i < freeSlots.length; i++) {
			for (int j = 0; j < freeSlots[i].length; j++) {
				if (!freeSlots[i][j]){
					if (i==0){
						if (!freeSlots[this.cores-1][j]){
							aoc++;
						}
						if (!freeSlots[1][j]){
							aoc++;
						}
					} else if (i==cores-1){
						if (!freeSlots[0][j]){
							aoc++;
						}
						if (!freeSlots[cores-2][j]){
							aoc++;
						}
					} else {
						if (!freeSlots[i+1][j]){
							aoc++;
						}
						if (!freeSlots[i-1][j]){
							aoc++;
						}
					}
				}
			}
		}
		
		//printSpectrum();
		double usedSlots = (slots*cores-getNumFreeSlots());
		//System.out.println("1- AOC"+ aoc+"Usados"+usedSlots);
		return aoc/usedSlots;
	}
	/**
	 * Gets the CrossTalk Per Slot considering slots of protection
	 * @return CrossTalk Per Slot
	 */
	//Slot de proteção NÃO gera crosstalk	
	public double getCrossTalkPerSlotb(){
		if (cores==1 || getNumFreeSlotsP() == slots*cores){
			return -1;
		}
		int aoc=0;
		for (int i = 0; i < freeSlots.length; i++) {
			for (int j = 0; j < freeSlots[i].length; j++) {
				if (!freeSlots[i][j]&&timeSharing[i][j]==0){
					
					if (i==0){
						if (!freeSlots[this.cores-1][j]&&timeSharing[this.cores-1][j]==0){
							aoc++;
						}
						if (!freeSlots[1][j]&&timeSharing[1][j]==0){
							aoc++;
						}
					} else if (i==cores-1){
						if (!freeSlots[0][j]&&timeSharing[0][j] ==0){
							aoc++;
						}
						if (!freeSlots[cores-2][j]&&timeSharing[cores-2][j]==0){
							aoc++;
						}
					} else {
						if (!freeSlots[i+1][j]&&timeSharing[i+1][j]==0){
							aoc++;
						}
						if (!freeSlots[i-1][j]&&timeSharing[i-1][j]==0){
							aoc++;
						}
					}
				}
			}
		}
		
		//printSpectrum();
		double usedSlots = (slots*cores-(getNumFreeSlotsP()));
		//System.out.println("AOC"+ aoc+"Usados"+usedSlots);
		//System.out.println("Used1: " +  (slots*cores-getNumFreeSlots())+" Used: "+usedSlots+" Livre: "+getNumFreeSlots()+" Livre+proteção:"+getNumFreeSlotsP());
		return aoc/usedSlots;
	}

	
	/**
	 * Prints all information related to the FlexGridLink object.
	 * 
	 * @return string containing all the values of the link's parameters.
	 */
	@Override
	public String toString() {
		String link = Long.toString(id) + ": " + Integer.toString(src) + "->" + Integer.toString(dst) + " delay: "
				+ Double.toString(delay) + " slots: " + Integer.toString(slots) + " weight:" + Double.toString(weight)+ "\n";
		for (int i = 0; i < freeSlots.length; i++) {
			link+="core"+i+":";
			for (int j = 0; j < freeSlots[i].length; j++) {
				if (freeSlots[i][j]){
					link += "1|";
				}else{
					link += "0|";
				}
			}
			link += "\n";
         }
		return link;
	}
	
	/**
	 * It sets the FlexGridLink as (un)available
	 * @param set the availability
	 * @return void
	 */
	public void setLinkAtivo(boolean set) {
		this.linkAtivo = set;
	}

	/**
	 * Retrieves the availability
	 * @return boolean something
	 * */
	public boolean isLinkActive() {
		return this.linkAtivo;
	}

	/**
	 * Print spectrum.
	 */

	public void printSpectrum() {
		//System.out.println("----------------------------------------------------");
		for (int i = 0; i < freeSlots.length; i++) {
			
			for (int j = 0; j < freeSlots[i].length; j++) {
				
				if (freeSlots[i][j])
					System.out.print(1+"|");
				else 
					System.out.print(0+"|");
			}	
			System.out.println();
		}
	}
	
	public double[][] getNoise(){
		return this.noise;
	}
	public double getSlotNoise(Slot slot){
		return noise[slot.core][slot.slot];
	}

	public double averageCrosstalk() {
		double average = 0;
		for (int i = 0; i < this.noise.length; i++) {
			for (int j = 0; j < this.noise[i].length; j++) {
				average += this.noise[i][j];
			}
		}
		return average / (this.noise.length * noise[0].length);
	}

	
	public void updateNoise(ArrayList<Slot> slotList, int modulation) {
		
		for (Slot s : slotList) {
			this.noise[s.core][s.slot] = -60;
			for (int i = 0; i <getCoupledFibersInUse(s.core, s.slot).size(); i++) {
				this.noise[s.core][s.slot] = Decibel.add(this.noise[s.core][s.slot],ModulationsMuticore.interCoreXT(modulation));
			}
			this.noise[s.core][s.slot] = Decibel.add(this.noise[s.core][s.slot],ModulationsMuticore.inBandXT[modulation]);
		}
	}
public void updateNoise(Slot s, int modulation) {
		//this.noise[s.core][s.slot] = -60;
		this.noise[s.core][s.slot] = 0;
		for (int i = 0; i <getCoupledFibersInUse(s.core, s.slot).size(); i++) {
			//this.noise[s.core][s.slot] = Decibel.add(this.noise[s.core][s.slot],ModulationsMuticore.interCoreXT(modulation));
			this.noise[s.core][s.slot] += 2;
		}
		//this.noise[s.core][s.slot] = Decibel.add(this.noise[s.core][s.slot],ModulationsMuticore.inBandXT[modulation]);
		this.noise[s.core][s.slot] += Modulations.in[modulation]; 
	}
	
	public ArrayList<Slot> getCoupledFibersInUse(int i, int j) {
		ArrayList<Slot> coupledFibers = new ArrayList<Slot>();
		if (i == 0) {
			if (!this.freeSlots[this.cores - 2][j]) {
				coupledFibers.add(new Slot(this.cores - 2, j));
			}
			if (!this.freeSlots[1][j]) {
				coupledFibers.add(new Slot(1, j));
			}
			if(!this.freeSlots[6][j]) {
				coupledFibers.add(new Slot(6, j));
			}
		} else if (i == cores - 2) {
			if (!this.freeSlots[0][j]) {
				coupledFibers.add(new Slot(0, j));
			}
			if (!this.freeSlots[cores - 3][j]) {
				coupledFibers.add(new Slot(this.cores - 3, j));
			}
			if(!this.freeSlots[6][j]) {
				coupledFibers.add(new Slot(6, j));
			}
		} else if(i == 6){
			for(int c = 0; c < 6; c++) {
				if(!this.freeSlots[c][j]) {
					coupledFibers.add(new Slot(c, j));
				}
			}
		}else {
			if (!this.freeSlots[i + 1][j]) {
				coupledFibers.add(new Slot(i + 1, j));
			}
			if (!this.freeSlots[i - 1][j]) {
				coupledFibers.add(new Slot(i - 1, j));
			}
			if(!this.freeSlots[6][j]) {
				coupledFibers.add(new Slot(6, j));
			}
		}
		return coupledFibers;
	}

		public boolean[][] getAllocableSpectrum(int modulation) {
			/* A intenção é fazer toda a verificação de possibilidade de alocação
			   dentro do link para a modulação aplicada */
			boolean[][] freeSlots = new boolean[cores][slots];
			for (int i = 0; i < freeSlots.length; i++) {
				for (int j = 0; j < freeSlots[i].length; j++) {
					if(this.freeSlots[i][j] && !allocationAffectsCoupledFibers(i, j, modulation/*, power*/)) { //SNR
						freeSlots[i][j] = true;
					} else {
						freeSlots[i][j] = false;
					}
				}
			}
			return freeSlots;
		}
		
		public boolean allocationAffectsCoupledFibers(int i, int j, int modulation) {
			for (Slot s : getCoupledFibersInUse(i, j)) {
				//if (Decibel.subtract(power, totalNoise) < ModulationsMuticore.getSNRThreshold(modulation)) {
				//if(totalNoise > Modulations.threshold[this.modulationLevel[s.core][s.slot]]) {
					//return true;
				//}
			}
			return false;
		}	
}
