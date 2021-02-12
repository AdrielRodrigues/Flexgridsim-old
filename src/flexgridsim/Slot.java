package flexgridsim;

/**
 * @author pedrom
 *
 */
public class Slot {
	/**
	 * 
	 */
	public int core;
	/**
	 * 
	 */
	public int slot;
	
	/**
	 * 
	 */
	public int label;
	/**
	 * @param core
	 * @param slot
	 */
	public int link;
	
	public Slot(int core, int slot, int link) {
		super();
		this.core = core;
		this.slot = slot;
		this.link = link;
	}
	
	public Slot(int core, int slot) {
		super();
		this.core = core;
		this.slot = slot;
	}
	
	public int getslot(){
		return slot;
	}
	
	//Only for ImageRCSA
	public void setLink(int n) {
		this.link = n;
	}
	public int getCore() {
		return core;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
	    if (obj == this)
	    {
	        return true;
	    }
	    if (obj == null)
	    {
	        return false;
	    }
	    if (obj instanceof Slot)
	    {
	    	Slot other = (Slot)obj;
	    	return other.core ==this.slot &&
	               other.slot==this.slot&&
	               other.link==this.link;
	    }
		return super.equals(obj);
	}
	


	@Override
	public String toString() {
		String Slot = "Core:"+Integer.toString(core) + " Slot:" + Integer.toString(slot)+ " Link:" + Integer.toString(link)+" " ;

		return Slot;
	}



}