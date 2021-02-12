package flexgridsim;

import java.util.ArrayList;

public class Path implements Comparable<Path> {

	private int[] links;
	private ArrayList<Slot> channelList;
	private int modulation;
	
	public Path(int[] links, ArrayList<Slot> channelList, int modulation ) {
		if (links.length < 1|| channelList.isEmpty()||modulation < 0||modulation > 5) {
			throw (new IllegalArgumentException());
		} else {
			this.channelList = channelList;
			this.links = links;
			this.modulation= modulation;
		}
	}
	
	public Path(int[] links, ArrayList<Slot> channelList) {
		if (links.length < 1|| channelList.isEmpty()) {
			throw (new IllegalArgumentException());
		} else {
			this.channelList = channelList;
			this.links = links;
			this.modulation= -1;
		}
	}

	/**
	 * Number of links that compose the path.
	 * 
	 * @return a integers
	 */
	public int getNumberOfLinks() {
		return links.length;
	}
	
	public Slot getInitSlot() {
		return channelList.get(0);
	}

	/**
	 * Number of links that compose the path.
	 * 
	 * @return a integers
	 */
	public int getModulation() {
		return modulation;
	}
	
    /**
     * @return the list of channels allocated for all cores
     */
    public ArrayList<Slot> getSlotList() {
		return channelList;
	}
  
	/**
	 * links that compose the path.
	 * 
	 * @return a vector of integers that represent fiberlinks identifiers
	 */
	public int[] getLinks() {
		return links;
	}
	/**
	 * Retrieves the LightPath's vector containing the identifier numbers of the
	 * links that compose the path.
	 * 
	 * @param i index
	 * @return a integers that represent the fiberlink identifier
	 */
	public int getLink(int i) {
		return links[i];
	}

	/**
	 * Prints all information related to a given LightPath, starting with its
	 * ID, to make it easier to identify.
	 * 
	 * @return string containing all the values of the LightPath's parameters
	 */
	@Override
	public String toString() {
		String path = "Caminho:  [Link , Core, Slot]: ";
		for (int i = 0; i < channelList.size(); i++) {
			path += "["+Integer.toString(channelList.get(i).link) + ", "+ Integer.toString(channelList.get(i).core) + ", "+ Integer.toString(channelList.get(i).slot)+ "], ";
		} 
		path += "] \n";
		path += "] \nLinks [";
		for (int i = 0; i < links.length; i++) {
			path += Integer.toString(links[i])+ ", ";
		}
		path += "]\n\n";
		return path;
	}
   // se esse é maior q outro retorn 1 senão -1 e 0 se forem iguais
	public int compareTo(Path p) {
        return (getNumberOfLinks() > p.getNumberOfLinks())?1 : (getNumberOfLinks() < p.getNumberOfLinks())?-1: 0;
    } 

}