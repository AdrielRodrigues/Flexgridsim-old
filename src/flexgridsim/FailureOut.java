package flexgridsim;

import java.util.ArrayList;

public class FailureOut extends Event{
	//private PhysicalTopology pt;
	//private ArrayList<Integer> linkFail;
	private long id;
	private Failure ff;
	
	public FailureOut(double time, long id, Failure ff){
		super(time);
		this.ff = ff;
		this.id = id;
	}
	
	public Failure getFail() {
		return ff;
	}
	
	public long getID() {
		return this.id;
	}
}
