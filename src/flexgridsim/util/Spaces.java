package flexgridsim.util;

public class Spaces {
	
	private int core;
	private int slot;
	private int tamanho;
	private boolean match;
	
	public Spaces (int core, int slot, int tamanho, boolean match) {
		this.core = core;
		this.slot = slot;
		this.tamanho = tamanho;
		this.match = match;
	}
	
	public int getCore () {
		return this.core;
	}
	
	public int getSlot () {
		return this.slot;
	}
	
	public int getTamanho () {
		return this.tamanho;
	}
	
	public boolean match () {
		return this.match;
	}
}
