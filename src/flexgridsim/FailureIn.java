package flexgridsim;

import java.util.ArrayList;

public class FailureIn extends Event {
	//private PhysicalTopology pt;
	
	private Failure ff;
	
	public FailureIn(double time, Failure ff) {
		super(time);
		this.ff = ff;
	}
	
	public Failure getFail() {
		return ff;
	}
}
