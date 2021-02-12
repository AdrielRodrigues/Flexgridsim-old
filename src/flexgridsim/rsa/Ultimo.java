package flexgridsim.rsa;
import java.util.ArrayList;
import org.w3c.dom.Element;
import flexgridsim.Flow;
import flexgridsim.Path;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.Modulations;
import flexgridsim.util.WeightedGraph;
import flexgridsim.util.Yen;



/**
 * @author helder
 *
 */
public class Ultimo  implements RSA  {
	private PhysicalTopology pt;
	private VirtualTopology vt;
	private ControlPlaneForRSA cp;
	private WeightedGraph graph;
	
	public Ultimo() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public void flowDeparture(Flow flow) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt,
			VirtualTopology vt, ControlPlaneForRSA cp, TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();	
	}
	public long flowArrivalc(Flow flow) {return 1;}

	@Override
	public void flowArrival(Flow flow) {
		final int MAXK =4;
		final int guardBand= getGrooming();//Quantidade de banda de guarda necessária
		
		System.out.println("Ultimo linha 48");
		System.out.println("Fonte: "+flow.getSource());
		System.out.println("Destino: "+flow.getDestination());
		System.out.println("Taxa: "+flow.getRate());
		
		/* Caminhos para roteamento*/

		int linksKroutes[][] = getKShortestPath(graph, flow.getSource(), flow.getDestination(),MAXK);
		int modulation[] = new int [linksKroutes.length];// modulação mínima
		
		//encontro a menor modulação possível para cada rota possivel
		for (int m = 0; m < linksKroutes.length; m++)  {
			modulation[m] = Modulations.getModulationByDistance(getPhysicalDistance(linksKroutes[m]));
		    //System.out.println(" M"+ modulation[m]); 
		}
		//Descubro quantos k pretendo usar
		int demandInSlotsToAllRate[] = new int [linksKroutes.length];
		int demandInSlotsToKoutes[] = new int [linksKroutes.length];;
		

		for (int k = 0; k < linksKroutes.length; k++)  {		
			//Quantidade de slot necessária para acomodar toda taxa em um caminho
			demandInSlotsToAllRate[k] = (int) ((Math.ceil(flow.getRate() / (double) Modulations.getBandwidth(modulation[k]))));
			//Quantidade de slot necessária para acomodar toda taxa em k caminhos
			demandInSlotsToKoutes[k] = (demandInSlotsToAllRate[k]/(k+1));//precisa ser > 1 para funcionar
		}

		/*****
		 * ****Inserir a banda de guarda em demandInSlotsToAllRate
		 */
		Path path = null;
		//SinglePath
		for (int k = 0; k < linksKroutes.length; k++)  {
			//encontrar demandInSlotsToAllRate para um dos k disponivel
			path = findPath(linksKroutes,demandInSlotsToAllRate[k]+ guardBand,modulation);
			if(path!=null) {
				break;
			}
		}
		if(path==null) {
		//Multipath
			for (int k = 0; k < linksKroutes.length; k++)  {
				path = findMultiPath(linksKroutes,demandInSlotsToKoutes[k],k,  guardBand);
				if(path!=null) {
					break;
				}
			}
		}
		//System.out.println("DemandaEmSlot: "+demandInSlots[k]);
		/*Preciso criar alguma forma de selecionar slots nos caminhos ja encontrados*/
		/*posso usar quaisquer dos caminhos*/
		//ja tenho a quantidade de slots necessário dependendendo da rota usada
  
	    
	    
	    System.exit(1);
	}
	/**
	 * @param links
	 * @param slotList
	 * @param modulation
	 * @param flow
	 * @return true if the connection was successfully established; false otherwise
	 */
	public boolean establishConnection(int[] links, ArrayList<Slot> slotList, int modulation, Flow flow){
	/*	long id = vt.createLightpath(links, slotList ,0);
		if (id >= 0) {
			LightPath lps = vt.getLightpath(id);
			flow.setLinks(links);
			flow.setSlotList(slotList);
			cp.acceptFlow(flow.getID(), lps);
			return true;
		} else {
			return false;
		}*/
		return false;
	}
	
	

	/**
	 * Return Guard band used
	 * 
	 */
	private int getGrooming() {
		if(pt.getGrooming())
			return 0;
		else
			return 1;
	}
	/**
	 * Convert nodes in links
	 * @param nodes vector
	 *            
	 * @return  Links vector
	 */
	private int [] nodesToLink(int [] vetaux){
		int[] links= new int[vetaux.length-1];
        for (int j = 0; j < vetaux.length-1; j++) {
        	links[j]= pt.getLink(vetaux[j], vetaux[j+1]).getID();
        }
		return links;
	}
	/**
	 * Finds k routes between two nodes in WeightedGraph G
	 * 
	 * Retorna matriz em que cada linha contem os Links do caminho
	 * 
	 * @param WeightedGraph
	 *  
	 * @param source
	 *            initial node
	 * @param destination
	 *            final node
	 * @param k
	 *            number of paths
	 * @return k Links 
	 */
	private int[][] getKShortestPath(WeightedGraph G, int source, int destination, int k) {
		// Cria matriz de nodes
		Yen kShortestPath = new Yen();
		int[][] kPaths = kShortestPath.ksp(G, source, destination, k);
		if(kPaths==null)
			return null;
		
		// Cria matriz de links
    	int linkskPaths[][] = new int[k][];
    	for (int i = 0; i < kPaths.length; i++) {
    		 linkskPaths[i]= nodesToLink(kPaths[i]);
    	}
		return linkskPaths;
	}
	

	/**
	 * @return the distance between two nodes using given path
	 * 
	 * @param vector of nodes

	 * @return return distance
	 */	

	public int getPhysicalDistance(int[] links){
		if(links!=null&& links.length>0){
			int physicalDistance = 0;
			for (int i = 0; i < links.length; i++) {
				physicalDistance += pt.getLink(links[i]).getDistance();
			}
			return physicalDistance;
		}
		else
			return -1;
	}
	/**
	 * @return And between vector
	 * 
	 * @param vector of boolean
	 * @param vector of boolean

	 * @return  vector of boolean
	 */	
	private void linksAnd(boolean[][] link1, boolean[][] link2, boolean[][] res){
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[0].length; j++) {
				res[i][j] = link1[i][j] & link2[i][j];
			}
		}
	}
	/**
	 * @return a boolean matriz that represent slots and cores available in path
	 * 
	 * @param vector of links
	 * @param vector of boolean
	 * @return a boolean matriz 
	 */		
	private boolean[][] availableSpectrumPath(int[] KLinkPaths){
		boolean[][] spectrum = new boolean[pt.getNumCores()][pt.getNumSlots()];
			for (int i = 0; i < spectrum.length; i++) {
				for (int j = 0; j < spectrum[i].length; j++) {
					spectrum[i][j]=true;
				}
			}
			for (int i = 0; i < KLinkPaths.length; i++) {
				linksAnd(pt.getLink(KLinkPaths[i]).getSpectrum(), spectrum, spectrum);
			}	
		return spectrum;
	}
	
   //passo vetor de boleanos do caminho, primeiro e ultimo slot a testar e o nucleo.\
	// retorna v se o caminho nos slots estiverem livre
	private boolean testAvailability(int demandInSlots, int core, int first, boolean[][] spectrum) {
		for (int i = first; i <= demandInSlots+first; i++) {
			if(spectrum[core][i]==false)
				return false;		
		}
		return true;
	}
	//SinglePath

	//Encontro um caminho a partir de k rotas e demanda requisitada
	private Path findPath(int linksKroutes[][], int demandInSlots, int[] modulation) {
		boolean spectrumVector[][];
		for (int l = 0; l < linksKroutes.length; l++) {
			spectrumVector = availableSpectrumPath(linksKroutes[l]);
			for (int c = 0; c <pt.getNumCores() ; c++) {
				for (int i = 0; i < pt.getNumSlots()-demandInSlots; i++) {	
					if(testAvailability(demandInSlots,c,i ,spectrumVector )) {
						//System.out.println("deu");
						return createPath(linksKroutes[l], c, i,  demandInSlots, modulation[l]);
					}
				}
			}
		}
		return null;
	}
	
	//MultiPath

	//Encontro um caminho a partir de k rotas e demanda requisitada
	//posso variar k rotas 
	private Path findMultiPath(int linksKroutes[][], int demandInSlots, int k, int guardBand) {
		//descubro se os caminhos tem links em comum.
		//se tiver passo o And para garantir q não usem mesmo slot.
			//depois encontro outros caminhos
		//caso contrario encontro caminhos individuais
		//
		boolean spectrumVector[][];
		for (int l = 0; l < linksKroutes.length; l++) {
			spectrumVector = availableSpectrumPath(linksKroutes[l]);
			for (int c = 0; c <pt.getNumCores() ; c++) {
				for (int i = 0; i < pt.getNumSlots()-demandInSlots; i++) {	
					if(testAvailability(demandInSlots,c,i ,spectrumVector )) {
						//System.out.println("deu");
						return null;//createPath(linksKroutes[l], c, i,  demandInSlots);
					}
				}
			}
		}
		return null;
	}
	
	private Path createPath(int[] links, int core,int firstslot, int demand, int modulation) {
		//se for nulo?????????????
		ArrayList<Slot> channel = new ArrayList<Slot>();
		for (int l = 0; l < links.length; l++) {
			for (int j = firstslot; j < firstslot+demand; j++) {
				channel.add(new Slot(core, j, links[l]) );
			}
		}
		//System.out.println(links.length);
		//System.out.println(channel.size());

		return new Path(links, channel, modulation);
	}	
	
	
}
	
	
	