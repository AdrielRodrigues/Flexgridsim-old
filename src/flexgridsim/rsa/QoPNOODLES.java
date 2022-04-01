package flexgridsim.rsa;


import java.util.ArrayList;
import org.w3c.dom.Element;

import flexgridsim.ControlPlane;
import flexgridsim.FlexGridLink;
import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.MyStatistics;
import flexgridsim.Path;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.Crosstalk;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.Modulations;
import flexgridsim.util.WeightedGraph;


/**
 * The Class ModifiedShortestPath.
 * 
 * @author Rafael
 */
public class QoPNOODLES implements RSA {
	private PhysicalTopology pt;
	private VirtualTopology vt;
	private ControlPlaneForRSA cp;
	private WeightedGraph graph;
	private Flow flowHere;
	private Flow flowFalho;
	private int minCOS;
	private int meanCOS;
	private int maxCOS;
	private MyStatistics st;
	private boolean restorationMethod;
	
	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt,
			VirtualTopology vt, ControlPlaneForRSA cp, TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();
		this.meanCOS = traffic.getMeanCOS();
		this.maxCOS = traffic.getMaxCOS();
		this.restorationMethod = false;
	}

	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt, ControlPlaneForRSA cp, TrafficGenerator traffic, MyStatistics st) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();
		this.minCOS = traffic.getMinCOS();
		this.meanCOS = traffic.getMeanCOS();
		this.maxCOS = traffic.getMaxCOS();
		this.st =  st;
		this.restorationMethod = false;
	}

	/**
	 * Instantiates a new modified shortest path.
	 */
	public QoPNOODLES() {
	}
	@Override
	public void flowDeparture(Flow flow) {
		// TODO Auto-generated method stub
		st.departureFlow(flow.getCOS());
	}

	public long flowArrivalc(Flow flow) {return 1;}
	@Override
	public void flowArrival(Flow flow) {
		flowHere = flow;

		/**/if(flow.getID() == 209) {
			flowFalho = flow;
		}/**/
		//System.out.println(flow.getID());
		/**if(flow.getID() > 9329) {
			System.out.println(flow.getID());
		}/**/
		
		long id=-1;
		long idc=-1;
		int guardBand=1;
		if(pt.getGrooming())
			guardBand=0;
		
		int demandInSlots = (int) (Math.ceil(flow.getRate() / (double) pt.getSlotCapacity()))+ guardBand;

		Path path;
		path = getRShortestPath2(graph, flow.getSource(), flow.getDestination(),	demandInSlots, false);
		
		/**if (path != null) {
			for (Slot slot: path.getSlotList()) {
				if (slot.link == 30 && slot.core == 0 && slot.slot == 123) {
					System.out.println(flow.getID());
				}
			}
		}/**/

		// If no possible path found, block the call
		if (path == null) {
			/**if(flow.getCOS() == 1) {
				System.out.println("alo");
			}/**/
			cp.blockFlow(flow.getID());
			return;
		} else if (path.getLinks() == null || path.getSlotList().isEmpty()) {
			/**if(flow.getCOS() == 1) {
			System.out.println("alo");
		}/**/
			cp.blockFlow(flow.getID());
			return;
		}

		id = vt.createLightpath(path, Modulations.getModulationLevel(pt.getSlotCapacity()), flow.getCOS(), (ControlPlane)cp);
		
		/**if(id == 9703) {
			System.out.println("alo");
		}/**/

		/*if (id == -1) {
			if(prioridadeMaior(path));
		}*/

		/**if (flowFalho != null &&  !cp.getMappedFlows().containsKey(flowFalho)) {
			System.out.println(id );
		}/**/

		if (id >= 0) { 
			flow.setLinks(path.getLinks());

			flow.setSlotList(path.getSlotList());

			ArrayList<LightPath> lightpath = new ArrayList<>(); 

			lightpath.add(vt.getLightpath(id));
			
			if(flow.getCOS()<maxCOS) {
				Path pathBack;
				
				pathBack = getbestPathBackR(graph, flow.getSource(), flow.getDestination(), demandInSlots, path, false);
				
				/**if(pathBack != null) {
					for (Slot slot: pathBack.getSlotList()) {
						if (slot.link == 30 && slot.core == 0 && slot.slot == 123) {
							System.out.println(flow.getID());
						}
					}
				}/**/
	
				if (flow.getCOS() == 1) {
					if (pathBack == null) {
						/**if(flow.getCOS() == 1) {
						System.out.println("alo");
					}/**/
						cp.blockFlow(flow.getID());
						vt.removeLightPath(id);//
						return;	
					} else if (pathBack.getLinks() == null || pathBack.getSlotList().isEmpty()) {
						/**if(flow.getCOS() == 1) {
						System.out.println("alo");
					}/**/
						cp.blockFlow(flow.getID());
						vt.removeLightPath(id);//
						return;
					}
	
				} else {
					if (pathBack == null) {
						if(!cp.acceptFlow(flow.getID(), lightpath)) {
							cp.blockFlow(flow.getID());
							vt.removeLightPath(id);//
						}
						return;
					} else {
						if (pathBack.getLinks() == null || path.getSlotList().isEmpty()) {
							if(!cp.acceptFlow(flow.getID(), lightpath)) {
								cp.blockFlow(flow.getID());
								vt.removeLightPath(id);
							}
							return;
						}
					}
				}
	
				//System.out.println("Passou!");
				/**/idc = vt.createLightpathProtection(pathBack, Modulations.getModulationLevel(pt.getSlotCapacity()));/**/
	
				//se não achar caminho de proteção lembrar de remover lightpath
				if (idc >= 0) { //PODE DAR ERRO
					flow.setLinksp(pathBack.getLinks());
					/**/flow.setSlotListp(pathBack.getSlotList());/**/
					/**/ArrayList<LightPath> lightpathp = new ArrayList<>();/**/
					/**/lightpathp.add(vt.getLightpath(idc));/**/
	
					if(!cp.acceptFlow(flow.getID(), lightpath, lightpathp)) {
						cp.blockFlow(flow.getID());
						vt.removeLightPath(id);
						vt.removeProtectionLightPath(idc);
						return;
					}
					return;
				}
			}
			if(!cp.acceptFlow(flow.getID(), lightpath)) {
				cp.blockFlow(flow.getID());
				vt.removeLightPath(id);//
				return;
			}
		}


		//It would never work

		/**else {
			for (int i = 0; i < flow.getSizeAllLinks(); i++) {
				if (flow.getLink(i) == 36/*numero aleatorio) {
					cp.blockFlow(flow.getID());
					return;
				}
			}
		}*/

		/*for (Slot p : path.getSlotList()){
			double xt=Crosstalk.getCrosstalkDB(pt.getLink(p.link).getDistance(), pt.getLink(p.link).getActiveNeighbor(p.core,p.slot), 'A');	
			//if(xt!=Double.NEGATIVE_INFINITY&&xt!=0.0)
			if(xt!=Double.NEGATIVE_INFINITY&&pt.getLink(p.link).getActiveNeighbor(p.core,p.slot)>2)
				System.out.println("xt"+xt+" Slot:"+p+"V:"+ pt.getLink(p.link).getActiveNeighbor(p.core,p.slot));
		}
		/*========== Teste ==========*
		for (Slot p : pathBack.getSlotList()){
			double xt=Crosstalk.getCrosstalkDB(pt.getLink(p.link).getDistance(), pt.getLink(p.link).getActiveNeighbor(p.core,p.slot), 'A');	
			//if(xt!=Double.NEGATIVE_INFINITY&&xt!=0.0)
			if(xt!=Double.NEGATIVE_INFINITY&&pt.getLink(p.link).getActiveNeighbor(p.core,p.slot)>2)
				System.out.println("xt"+xt+" Slot:"+p+"V:"+ pt.getLink(p.link).getActiveNeighbor(p.core,p.slot));
		}
		/*========== Teste ==========*/

		/*======== OLHAR ISSO =======*
        for (Slot p : path.getSlotList()){
			double xt=Crosstalk.getCrosstalkDB(pt.getLink(p.link).getDistance(), pt.getLink(p.link).getActiveNeighbor(p.core,p.slot));	
			//if(xt!=Double.NEGATIVE_INFINITY&&xt!=0.0)
			if(xt!=Double.NEGATIVE_INFINITY&&pt.getLink(p.link).getActiveNeighbor(p.core,p.slot)>2)
				System.out.println("xt"+xt+" Slot:"+p+"V:"+ pt.getLink(p.link).getActiveNeighbor(p.core,p.slot));
		}

		for (Slot p : pathBack.getSlotList()){
			double xt=Crosstalk.getCrosstalkDB(pt.getLink(p.link).getDistance(), pt.getLink(p.link).getActiveNeighbor(p.core,p.slot));	
			//if(xt!=Double.NEGATIVE_INFINITY&&xt!=0.0)
			if(xt!=Double.NEGATIVE_INFINITY&&pt.getLink(p.link).getActiveNeighbor(p.core,p.slot)>2)
				System.out.println("xt"+xt+" Slot:"+p+"V:"+ pt.getLink(p.link).getActiveNeighbor(p.core,p.slot));
		}        
        /*===========================*/


		//se não achar caminho de proteção lembrar de remover lightpath

		//else {



		/**if(!cp.acceptFlow(flow.getID(), lightpath)) {
					cp.blockFlow(flow.getID());
				}/*ORIGINAL*/

		/**if(!cp.acceptFlow(flow.getID(), lightpath, lightpathp)) {
					cp.blockFlow(flow.getID());
				}/*MODIFICADO*/

		// }	//cp.acceptFlow(flow.getID(),lightpath);
		//System.out.println("\n\n\n\n\n");
		//return;
		// }
		//}	

		/**/if(id<0){
			cp.blockFlow(flow.getID());
			//vt.removeLightPath(id);
			return;
		}/**/
		return;
	}


	/**
	 * Says whether or not two nodes belong to a path.
	 *
	 * @param path
	 * @param flow
	 * @return the boolean
	 */		
	public boolean belongs(int []linkpathprotection, Flow flow){
		boolean source=false;
		boolean destination =false;
		//flow.getSource() flow.getDestination()
		for(int i = 0; i < linkpathprotection.length; i++){ 
			if(pt.getLink(linkpathprotection[i]).getSource()==flow.getSource()||pt.getLink(linkpathprotection[i]).getDestination()==flow.getSource()){
				source =true;
			}
			if(pt.getLink(linkpathprotection[i]).getSource()==flow.getDestination()||pt.getLink(linkpathprotection[i]).getDestination()==flow.getDestination()){
				destination =true;
			}
			if(source&&destination)
				return true;
		}
		return false;
	}

	/**
	 * Verify if two paths are disjoint
	 * 
	 * @param vector of links
	 *           
	 * @param path 
	 *            path 
	 * @return return true in case disjoint, false other case
	 */	
	//	Diz se dois vetores são disjuntos
	/*	public  boolean disjoint(ArrayList<Flow> flows, int[] linkpath) {
		if( flows!=null&&linkpath!=null && linkpath.length!=0){
			for (Flow flow : flows){
				for(int i = 0; i < flow.getLinks().length; i++){  
					for(int j = 0; j < linkpath.length; j++){  
						if( flow.getLinks()[i]==linkpath[j])
								return false;	
					}
				}
			}
		}		
		return true;
	}*/
	public  boolean disjoint(ArrayList<LightPath> lightpaths, int[] linkpath) {
		if(lightpaths!=null&& linkpath!=null && linkpath.length!=0){
			for (LightPath lightpath : lightpaths){
				for(int i = 0; i < lightpath.getLinks().length; i++){  
					for(int j = 0; j < linkpath.length; j++){  
						if( lightpath.getLinks()[i]==linkpath[j])
							return false;	
					}
				}
			}
		}		
		return true;
	}
	/**
	 * Finds,  the list of slots to be used
	 *
	 * @param links[]
	 * @param demandInSlots
	 * @param sharing nao esta sendo usado
	 * @return the Channel@
	 */	


	public  ArrayList<Slot> getSimilarSlotsInLinks(int []links, boolean sharing, int demandInSlots) {
		ArrayList<Slot> channel = new ArrayList<Slot>();
		int firstSlot;
		int lastSlot;
		int core;
		for (int i = 0; i < pt.getNumSlots()-demandInSlots; i++) {
			firstSlot = i;
			lastSlot = i + demandInSlots - 1;
			core=usingSameCore(firstSlot, lastSlot, links, sharing);

			if(core!=-1){
				for (int j = firstSlot; j <= lastSlot; j++) {
					for (int l = 0; l < links.length; l++) {
						channel.add(new Slot(core, j, links[l] ));
					}
				}
				return channel;	
			}//else{@todo}

		}	
		return null;
	}

	public  ArrayList<Slot> getSimilarSlotsInLinks(int []links, boolean sharing, int demandInSlots, int COS) {
		ArrayList<Slot> channel = new ArrayList<Slot>();
		int firstSlot;
		int lastSlot;
		int core;
		for (int i = 0; i < pt.getNumSlots()-demandInSlots; i++) {
			firstSlot = i;
			lastSlot = i + demandInSlots - 1;
			core=usingSameCore(firstSlot, lastSlot, links, sharing);

			if(core!=-1){
				for (int j = firstSlot; j <= lastSlot; j++) {
					for (int l = 0; l < links.length; l++) {
						channel.add(new Slot(core, j, links[l] ));
					}
				}
				return channel;	
			}//else{@todo}

		}

		if (COS != maxCOS) { 
			for(int j = 0; j < pt.getNumSlots()-demandInSlots; j++) {
				firstSlot = j;
				lastSlot = j + demandInSlots - 1;
				core = liberaPath(firstSlot, lastSlot, links, sharing, COS);

				if(core!=-1) {
					for (int k = firstSlot; k <= lastSlot; k++) {
						for (int l = 0; l < links.length; l++) {
							channel.add(new Slot(core, k, links[l] ));
						}
					}
					return channel;	
				}//else{@todo}
			}
		}
		return null;
	}

	private int liberaPath(int firstSlot, int lastSlot, int links[], boolean sharing, int COS) {
		/**for (int core=0; core < pt.getNumCores(); core ++){

		}
		return 0;/**/

		for (int core=0; core < pt.getNumCores(); core ++) {
			if (liberarSlotsNosLinks2(links, firstSlot, lastSlot, core, sharing, COS)) {
				return core;
			}
		}
		return -1;
	}
	/**
	 * Says whether or not a determined set of contiguous slots are available on the same core for a set of links.
	 *
	 * @param link
	 * @param slot
	 * @param initial slot
	 * @param final slot
	 * @param core
	 * @return the boolean
	 */	

	public int usingSameCore(int firstSlot, int lastSlot, int links[], boolean sharing){
		for (int core=0; core < pt.getNumCores(); core ++){
			if(freeSlotInAllLinks(links, firstSlot, lastSlot, core, sharing)){
				return core;
			}
		}
		return -1;
	}


	/**
	 * Says whether or not a determined set of contiguous slots are available on core of set of link.
	 *
	 * @param link
	 * @param slot
	 * @param initial slot
	 * @param final slot
	 * @param core
	 * @return the boolean
	 */

	public boolean freeSlotInAllLinks(int links[], int firstSlot, int lastSlot, int core, boolean sharing) {
		for (int j = 0; j < links.length; j++) {
			if (sharing==false){
				for (int h = firstSlot; h <= lastSlot; h++) {
					if(!pt.getLink(links[j]).getSpectrum(core, h)){
						return false;
					}
				}
			}
			if (sharing==true){
				for (int h = firstSlot; h <= lastSlot; h++) {
					if(!pt.getLink(links[j]).getSpectrumS(core, h)){
						return false;
					}
				}
			}
		}
		return true;
	}

	public boolean liberarSlotsNosLinks(int links[], int firstSlot, int lastSlot, int core, boolean sharing, int COS) {
		int demand = lastSlot - firstSlot + 1 ;
		for (int j = 0; j < links.length; j++) {
			for (int h = firstSlot; h <= lastSlot; h++) {
				//if(links[j] == 30 && core == 0 && h == 123) {
				//	System.out.println();
				//}
				if (!pt.getLink(links[j]).getSpectrum(core, h)) {
					long key = vt.fluxoQueAlocou(links[j], core, h);
					if (key != -1) {
						int cos = cp.getFlow(key).getCOS();
						if (cos > COS) {//Se prioridade maior
							int demandaOcupada = cp.getFlow(key).getSlotList().size()/cp.getFlow(key).getLinks().length;
							
							if (h + demandaOcupada - 1 >= lastSlot) {
							//if (demandaOcupada >= demand - (h-firstSlot)) {//demanda menor ou igual
								if (mesmoFluxoNoPath (key, links, core, h)) {
									if (cp.removeFlow(key) != null) {
										st.setRemovedDiff(cos - 2);
										return true;
									}
								}	
								
							} else { 
								if (pt.getLink(links[j]).getSpectrum(core, h + demandaOcupada)) {
									int count = 1;
									int slot =  h + demandaOcupada + 1;
									while (pt.getLink(links[j]).getSpectrum(core, slot) && count + h + demandaOcupada <= lastSlot ) {
										count++;
										slot++;
									}
									if (count + h + demandaOcupada -1 == lastSlot) {
										if (mesmoFluxoNoPath (key, links, core, h)) {
											if (cp.removeFlow(key) != null) {
												st.setRemovedDiff(cos- 2);
												return true;
											}
										}
									}
								}
								//ver se os slots seguintes estão livres antes de testar isso aqui 			[OK]
								long key2 = vt.fluxoQueAlocou(links[j], core, h+demandaOcupada);
								if (key2 != -1) {
									int cos2 = cp.getFlow(key2).getCOS();
									if (cos2 > COS) {
										int demandaOcupada2 = cp.getFlow(key2).getSlotList().size()/cp.getFlow(key2).getLinks().length;
										
										if (h + demandaOcupada + demandaOcupada2 - 1 >= lastSlot) {
										//if (demandaOcupada + demandaOcupada2 >= demand - (h-firstSlot)) {//demanda menor ou igual
											if(mesmoFluxoNoPath (key, links, core, h) && mesmoFluxoNoPath (key2, links, core, h+demandaOcupada)) {
												if (cp.removeFlow(key) != null && cp.removeFlow(key2) != null) {
													st.setRemovedDiff(cos - 2);
													st.setRemovedDiff(cos2 - 2);
													return true;
												}
											}
										}										
									} else {
										return false;
									}
								} else {
									key2 = vt.fluxoQueAlocouP(links[j], core, h+demandaOcupada);
									if (key2 != -1) {
										if (!cp.getFlow(key2).getSlotListp().isEmpty()) {
											int cos2 = cp.getFlow(key2).getCOS();
											if (cos2 > COS) {
												int demandaOcupada2 = cp.getFlow(key2).getSlotListp().size()/cp.getFlow(key2).getLinksp().length;
												
												if (h + demandaOcupada + demandaOcupada2 - 1 >= lastSlot) {
												//if (demandaOcupada + demandaOcupada2 >= demand - (h-firstSlot)) {//demanda menor ou igual
													if(mesmoFluxoNoPath(key, links, core, h) && mesmoFluxoNoPathP (key2, links, core, h+demandaOcupada)) {
														if (cp.removeFlowProtection(key) != null && cp.removeFlowProtection(key2) != null) {
															st.setRemovedBackupDiff(cos - 2);
															st.setRemovedBackupDiff(cos2 - 2);
															return true;
														}
													}
												}
											} else {
												return false;
											}
										}
									}
								}
							}
						} else {
							return false;
						}
					} else {
						key = vt.fluxoQueAlocouP(links[j], core, h);
						if (key != -1) {
							//System.out.println("AQUi");
							int cos = cp.getFlow(key).getCOS();
							if (cos > COS) {//Se prioridade maior
  								int demandaOcupada = cp.getFlow(key).getSlotListp().size()/cp.getFlow(key).getLinksp().length;
								
								if (h + demandaOcupada - 1 >= lastSlot) {
								//if (demandaOcupada >= demand - (h-firstSlot)) {//demanda menor ou igual
									if (mesmoFluxoNoPathP (key, links, core, h)) {
										if (cp.removeFlowProtection(key) != null) {
											st.setRemovedBackupDiff(cos - 2);
											return true;
										} else {
											return false;
										}
									}
								} else {
									if (pt.getLink(links[j]).getSpectrum(core, h + demandaOcupada)) {
										int count = 1;
										int slot =  h + demandaOcupada + 1;
										while (pt.getLink(links[j]).getSpectrum(core, slot) && count + h + demandaOcupada < lastSlot ) {
											count++;
											slot++;
										}
										if (count + h + demandaOcupada -1 == lastSlot) {
											if (mesmoFluxoNoPathP (key, links, core, h)) {
												if (cp.removeFlowProtection(key) != null) {
													st.setRemovedBackupDiff(cos- 2);
													return true;
												}
											}
										}
									}
									long key2 = vt.fluxoQueAlocou(links[j], core, h+demandaOcupada); //Backup + Primário
									if (key2 != -1) {
										if (!cp.getFlow(key2).getSlotList().isEmpty()) {
											int cos2 = cp.getFlow(key2).getCOS();
											if (cos2> COS) {
												int demandaOcupada2 = cp.getFlow(key2).getSlotList().size()/cp.getFlow(key2).getLinks().length;
												
												if (h + demandaOcupada + demandaOcupada2 - 1 >= lastSlot) {
												//if (demandaOcupada + demandaOcupada2 >= demand - (h-firstSlot)) {//demanda menor ou igual
													if(mesmoFluxoNoPathP (key, links, core, h) && mesmoFluxoNoPath (key2, links, core, h+demandaOcupada)) {
														if (cp.removeFlowProtection(key) != null && cp.removeFlow(key2) != null) {
															st.setRemovedBackupDiff(cos - 2);
															st.setRemovedDiff(cos2 - 2);
															return true;
														}
													}
												}
											}
										}
									}
									key2 = vt.fluxoQueAlocouP(links[j], core, h+demandaOcupada);  //Backup + Backup
									if (key2 != -1) {
										if (!cp.getFlow(key2).getSlotListp().isEmpty()) {
											int cos2 = cp.getFlow(key2).getCOS();
											if (cos2> COS) {
												int demandaOcupada2 = cp.getFlow(key2).getSlotListp().size()/cp.getFlow(key2).getLinksp().length;
												
												if (h + demandaOcupada + demandaOcupada2 - 1 >= lastSlot) {
												//if (demandaOcupada + demandaOcupada2 >= demand - (h-firstSlot)) {//demanda menor ou igual
													if(mesmoFluxoNoPathP (key, links, core, h) && mesmoFluxoNoPathP (key2, links, core, h+demandaOcupada)) {
														if (cp.removeFlowProtection(key) != null && cp.removeFlowProtection(key2) != null) {
															st.setRemovedBackupDiff(cos - 2);
															st.setRemovedBackupDiff(cos2 - 2);
															return true;
														}
													}
												}
											}
										}
									}
								}
							} else {
								return false;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	public boolean liberarSlotsNosLinks2(int links[], int firstSlot, int lastSlot, int core, boolean sharing, int COS) {
		int demand = lastSlot - firstSlot + 1;
		ArrayList<FlowInfo> remove = new ArrayList<>();
		for(int slot=firstSlot; slot<=lastSlot; slot++) {
			int[] linksOcupados = linksOcupados(links, core, slot);
			if(linksOcupados.length == 0) {
				continue;
			}
			ArrayList<FlowInfo> info = fluxosOcupantes(linksOcupados, core, slot);
			if(containsAll(remove, info)) {
				continue;
			}
			boolean enoughDemand[][] = new boolean[info.size()][];
			boolean test = true;
			for(int i=0; i<info.size(); i++) {
				enoughDemand[i] = new boolean[info.get(i).getLinks().length];
				boolean ts = true;
				if(info.get(i).getCOS() <= COS) {
					return false;
				}
				for(int j=0; j<info.get(i).getLinks().length; j++) {
					if(info.get(i).isPrimaryPath(j)) {
						enoughDemand[i][j] = info.get(i).getLastSlot() + info.get(i).getKNextFreeSlots(j) >= lastSlot;
					} else {
						enoughDemand[i][j] = info.get(i).getLastSlotBackup() + info.get(i).getKNextFreeSlots(j) >= lastSlot;
					}
					ts &= enoughDemand[i][j];
				}
				test &= ts;
			}
			if(test) {
				ArrayList<FlowInfo> aux = (ArrayList<FlowInfo>) remove.clone();
				aux.addAll(info);
				if(resolve(links, core, slot, lastSlot, aux)) {
					remove.addAll(info);
					break;
				}
			}
			ArrayList<FlowInfo> nextFlows = new ArrayList<>();
			for(int i=0; i<enoughDemand.length; i++) {	
				for(int j=0; j<enoughDemand[i].length; j++) {
					if(!enoughDemand[i][j]) {
						int link0 = info.get(i).getLink(j);
						int core0 = info.get(i).getCore();
						int slot0;
						if(info.get(i).isPrimaryPath(j)) {
							slot0 = info.get(i).getLastSlot()+info.get(i).getKNextFreeSlots(j)+1;
						} else {
							slot0 = info.get(i).getLastSlotBackup()+info.get(i).getKNextFreeSlots(j)+1;
						}
						FlowInfo info0 = getFlowInfo(link0, core0, slot0); 
						int index0 = flowIndex(nextFlows, info0.getKey());
						if(index0 != -1) {
							nextFlows.get(index0).addLink(link0, info0.isPrimaryPath(0));
							/**int linkIndex = nextFlows.get(index0).indexOfLink(link0);
							if(info0.isPrimaryPath(0) && !info.get(index0).isPrimaryPath(linkIndex)) {
								nextFlows.get(index0).setPrimaryPath(linkIndex);
							}/**/
						} else {
							nextFlows.add(info0);
						}
					}
				}
			}
			test = true;
			for(int i=0; i<nextFlows.size(); i++) {
				if(nextFlows.get(i).getCOS() <= COS || nextFlows.get(i).getDemand()/** + nextFlows.get(i).getKNextFreeSlots(0)/**/ >= demand) {
					return false;
				}
				//if((nextFlows.get(i).getDemand() >= demand && mesmoFluxoNoPath())) {
					
				//}
				if(nextFlows.get(i).areAllPrimaryPath()) {
					test &= nextFlows.get(i).getLastSlot() >= lastSlot;
				} else {
					test &= nextFlows.get(i).getLastSlotBackup() >= lastSlot;
				}
			}
			info.addAll(nextFlows);
			if(test) {
				//ArrayList<FlowInfo> aux = (ArrayList<FlowInfo>) remove.clone();
				//aux.addAll(info);
				remove.addAll(info);
				if(resolve(links, core, slot, lastSlot, remove)) {
					break;
				}
				continue;
			}
			return false;
		}
		for(FlowInfo flow: remove) {
			if(flow.areAllPrimaryPath()) {
				if (cp.removeFlow(flow.getKey()) != null) {
					st.setRemovedDiff(flow.getCOS()- 1);
				}
			} else {
				if (cp.removeFlowProtection(flow.getKey()) != null) {
					st.setRemovedBackupDiff(flow.getCOS()- 1);
				}
			}
		}
		return true;
	}
	
	private int[] linksOcupados(int links[], int core, int slot){
		ArrayList<Integer> linkList = new ArrayList<>();
		int linksOcupados[];
		for(int i=0; i<links.length; i++) {
			if(!pt.getLink(links[i]).getSpectrum(core, slot)) {
				linkList.add(links[i]);
			}
		}
		linksOcupados = new int[linkList.size()];
		for(int j=0; j<linksOcupados.length; j++) {
			linksOcupados[j] = linkList.get(j);
		}
		return linksOcupados;
	}
	
	private	FlowInfo getFlowInfo(int link, int core, int slot) {
		FlowInfo info;
		long key;
		boolean primaryPath;
		
		if(vt.fluxoQueAlocou(link, core, slot) != -1) {
			key = vt.fluxoQueAlocou(link, core, slot);
			primaryPath = true;
		} else {
			key = vt.fluxoQueAlocouP(link, core, slot);
			primaryPath = false;
		}
		
		info = new FlowInfo(key, primaryPath, link, core);
		return info;
	}
	
	private ArrayList<FlowInfo> fluxosOcupantes(int links[], int core, int slot){
		ArrayList<FlowInfo> fluxos = new ArrayList<>();
		
		for(int link: links) {
			FlowInfo info = getFlowInfo(link, core, slot);
			int i = flowIndex(fluxos, info.getKey());
			if(i != -1) {
				fluxos.get(i).addLink(link, info.isPrimaryPath(0));
			} else {
				fluxos.add(info);
			}
		}
		
		return fluxos;
	}
	
	private int flowIndex(ArrayList<FlowInfo> fluxos, long key) {
		for(int i=0; i<fluxos.size(); i++) {
			if(fluxos.get(i).getKey() == key) {
				return i;
			}
		}
		return -1;
	}
	
	private boolean containsAll(ArrayList<FlowInfo> listaMaior, ArrayList<FlowInfo> listaMenor) {
		if(listaMaior == null) {
			return false;
		}
		
		for(FlowInfo i: listaMenor) {
			boolean encontrado = false;
			for(FlowInfo j: listaMaior) {
				if(i.getKey() == j.getKey()) {
					encontrado = true;
				}
			}
			if(!encontrado) {
				return false;
			}
		}
		return true;
	}
	
	private boolean resolve(int links[], int core, int firstSlot, int lastSlot, ArrayList<FlowInfo> info) {
		for(int link: links) {
			for(int slot=firstSlot; slot<=lastSlot; slot++) {
				if(!pt.getLink(link).getSpectrum(core, slot)) {
					FlowInfo flow = getFlowInfo(link, core, slot);
					int index = flowIndex(info, flow.getKey()); 
					if(index != -1) {
						info.get(index).addLink(link, flow.isPrimaryPath(0));
					} else {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private boolean mesmoFluxoNoPath (long key, int links[], int core, int firstSlot) {
		for (int i= 0; i < links.length; i++) {
			if (key != vt.fluxoQueAlocou(links[i], core, firstSlot)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean mesmoFluxoNoPathP (long key, int links[], int core, int firstSlot) {
		for (int i= 0; i < links.length; i++) {
			if (key != vt.fluxoQueAlocouP(links[i], core, firstSlot)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Array and.
	 *
	 * @param array1 the array1
	 * @param array2 the array2
	 * @return the boolean[]
	 */


	public  boolean[][] arrayAnd(boolean[][]array1, boolean[][] array2) {
		if (array1.length != array2.length) {
			throw (new IllegalArgumentException());
		}
		boolean[][] result = new boolean[array1.length][array1[0].length];

		for (int i = 0; i < array1.length; i++) {
			for (int j = 0; j < array1[0].length; j++) {
				result[i][j] = array1[i][j] & array2[i][j];
			}
		}
		return result;
	}

	/**
	 * Verify if the array of booleans has n contiguous true values
	 * 
	 * @param array
	 *            array to be verified
	 * @param n
	 *            number of contiguous slots
	 * @return return true in case it has n contiguous slots and false in case
	 *         it doesnt
	 */
	public boolean contiguousSlotsAvailable(boolean[][] array, int n, int ncores) {
		int j;
		for (int cores=0; cores< ncores; cores ++){
			for (int i = 0; i < array[cores].length; i++) {
				if (array[cores][i]) {
					for (j = i; j < i + n && j < array[cores].length; j++) {
						if (!array[cores][j]) {
							i = j;
							break;
						}
					}
					if (j == i + n)
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Contiguous slots available index.
	 *
	 * @param array the array
	 * @param n the n
	 * @return the int
	 */
	public static int contiguousSlotsAvailableIndex(boolean[][] array, int n, int ncores) {
		int j;
		for (int cores=0; cores< ncores; cores ++){
			for (int i = 0; i < array[cores].length; i++) {
				if (array[cores][i]) {
					for (j = i; j < i + n && j < array[cores].length; j++) {
						if (!array[cores][j]) {
							i = j;
							break;
						}
					}
					if (j == i + n)
						return i;
				}
			}
		}
		return -1;
	}


	/**
	 * Finds, from the list of unvisited vertexes, the one with the lowest
	 * distance from the initial node.
	 * 
	 * @param dist
	 *            vector with shortest known distance from the initial node
	 * @param v
	 *            vector indicating the visited nodes
	 * @return vertex with minimum distance from initial node, or -1 if the
	 *         graph is unconnected or if no vertexes were visited yet
	 */
	public int minVertex(double[] dist, boolean[] v) {
		double x = Double.MAX_VALUE;
		int y = -1; // graph not connected, or no unvisited vertices
		for (int i = 0; i < dist.length; i++) {
			if (!v[i] && dist[i] < x) {
				y = i;
				x = dist[i];
			}
		}
		return y;
	}




	// Dijkstra's algorithm to find shortest path from s to all other nodes
	/**
	 * Msp.
	 *
	 * @param G the g
	 * @param s the s
	 * @param demand the demand
	 * @param protection true if is to protect false otherwise
	 * @return the int[]
	 */

	public int[] MSP(WeightedGraph G, int s, int demand, boolean sharing) {
		final double[] dist = new double[G.size()]; // shortest known distance
		// from "s"
		final int[] pred = new int[G.size()]; // preceding node in path
		final boolean[] visited = new boolean[G.size()]; // all false initially
		for (int i = 0; i < dist.length; i++) {
			pred[i] = -1;
			dist[i] = 1000000;
		}
		dist[s] = 0;
		for (int i = 0; i < dist.length; i++) {
			final int next = minVertex(dist, visited);
			if (next >= 0) {
				visited[next] = true;

				// The shortest path to next is dist[next] and via pred[next].
				final int[] n = G.neighbors(next);
				for (int j = 0; j < n.length; j++) {
					final int v = n[j];
					final double d = dist[next] + G.getWeight(next, v);
					if (dist[v] > d) {
						dist[v] = d;
						pred[v] = next;
					}					
				}
			}
		}
		return pred;
	}





	/**
	 * Retrieves the shortest path between a source and a destination node,
	 * within a weighted graph.
	 * 
	 * @param G
	 *            the weighted graph in which the shortest path will be found
	 * @param src
	 *            the source node
	 * @param dst
	 *            the destination node
	 * @param demand
	 *            size of the demand
	 * @return the shortest path, as a vector of integers that represent node
	 *         coordinates
	 */
	public Path getShortestPath(WeightedGraph G, int src, int dst, int demand, boolean overlap) {
		int x;
		int[] sp;
		int []links;
		ArrayList<Slot> channel = new ArrayList<Slot>();
		ArrayList<Integer> path = new ArrayList<Integer>();
		final int[] pred = MSP(G, src, demand, overlap);
		if (pred == null) {
			return null;
		}
		x = dst;

		while (x != src) {
			path.add(0, x);
			x = pred[x];
			// No path
			if (x == -1) {
				return null;
			}				
		}
		path.add(0, src);

		sp = new int[path.size()];
		for (int i = 0; i < path.size(); i++) {
			sp[i] = path.get(i);
		}
		if(sp.length<2)
			return null;
		//links do primeiro caminho
		links = new int[sp.length - 1];
		for (int j = 0; j < sp.length - 1; j++) {
			links[j] = pt.getLink(sp[j], sp[j + 1]).getID();
		}
		channel=getSimilarSlotsInLinks(links,overlap, demand, flowHere.getCOS());
		if(channel==null||channel.isEmpty()){
			return null;
		}	
		return new Path(links, channel);
	}



	public Path getKShortestPath(WeightedGraph G,int src, int dst, int demand, boolean overlap){
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(G, src, dst, 3);
		if(kPaths==null)
			return null;
		int []links;
		ArrayList<Slot> channel = new ArrayList<Slot>();

		for (int i = 0; i < kPaths.length; i++) {
			if (kPaths[i].length > 1){
				links = new int[kPaths[i].length - 1];
				for (int j = 0; j < kPaths[i].length - 1; j++) {
					links[j] = pt.getLink(kPaths[i][j], kPaths[i][j + 1]).getID();
				}
				channel=getSimilarSlotsInLinks(links,overlap, demand);
				if(channel!=null){
					return new Path(links, channel);
				}	
			} else {
				continue;
			}
		}
		return null;
	}

	/**/public Path getRShortestPath(WeightedGraph G, int src, int dst, int demand, boolean overlap) {
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(G, src, dst, 3);
		if(kPaths==null)
			return null;
		int []links;
		ArrayList<Slot> channel = new ArrayList<Slot>();

		for (int i = 0; i < kPaths.length; i++) {
			if (kPaths[i].length > 1){
				links = new int[kPaths[i].length - 1];
				for (int j = 0; j < kPaths[i].length - 1; j++) {
					links[j] = pt.getLink(kPaths[i][j], kPaths[i][j + 1]).getID();
				}
				channel=getSimilarSlotsInLinks(links,overlap, demand, flowHere.getCOS());
				if(channel!=null){
					return new Path(links, channel);
				}	
			} else {
				continue;
			}
		}
		return null;
	}/**/

	/**/public Path getRShortestPath2(WeightedGraph G, int src, int dst, int demand, boolean overlap) {
		KShortestPaths kShortestPaths = new KShortestPaths();
		/**/ArrayList<int[]> KP = kShortestPaths.dijkstraKShortestPathsR(G, src, dst, 50);
		int[][] kPaths = new int[KP.size()][];
		for ( int i = 0; i < KP.size(); i++) {
			//kPaths[i] = new int[KP.get(i).length];
			kPaths[i] = KP.get(i);
		}/**/
		//int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(G, src, dst, 50);
		if(kPaths==null)
			return null;
		int []links;
		ArrayList<Slot> channel = new ArrayList<Slot>();

		for (int i = 0; i < kPaths.length; i++) {
			if (kPaths[i].length > 1){
				links = new int[kPaths[i].length - 1];
				for (int j = 0; j < kPaths[i].length - 1; j++) {
					links[j] = pt.getLink(kPaths[i][j], kPaths[i][j + 1]).getID();
				}
				channel=getSimilarSlotsInLinks(links,overlap, demand, flowHere.getCOS());
				if(channel!=null){
					return new Path(links, channel);
				}	
			} else {
				continue;
			}
		}
		return null;
	}/**/
	
	public Path getYenKShortestPath(WeightedGraph G,int src, int dst, int demand, boolean overlap){
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.yenKShortestPaths(G, src, dst, 3);
		int []links;
		ArrayList<Slot> channel = new ArrayList<Slot>();

		for (int i = 0; i < kPaths.length; i++) {
			if (kPaths[i].length > 1){
				links = new int[kPaths[i].length - 1];
				for (int j = 0; j < kPaths[i].length - 1; j++) {
					links[j] = pt.getLink(kPaths[i][j], kPaths[i][j + 1]).getID();
				}
				channel=getSimilarSlotsInLinks(links,overlap, demand);
				if(channel!=null){
					return new Path(links, channel);
				}	
			} else {
				continue;
			}
		}
		return null;
	}



	public  Path getbestPathBack(WeightedGraph G, int src, int dst, int demandInSlots, Path Path, boolean overlap) {
		WeightedGraph auxg;
		Path path;

		//criando graf aux
		auxg = new WeightedGraph(G.getNumNodes());
		for (int i = 0; i < G.getNumNodes(); i++) {
			for (int j = 0; j < G.getNumNodes(); j++) {
				auxg.setWeight(i,j,G.getWeight(i,j));
			}
		}
		//removendo os links do primeiro caminho, para que não haja links iguais 
		for (int i = 0; i < Path.getLinks().length; i++) {	
			auxg.removeEdge(pt.getLink(Path.getLink(i)).getSource(),pt.getLink(Path.getLink(i)).getDestination());
			auxg.removeEdge(pt.getLink(Path.getLink(i)).getDestination(),pt.getLink(Path.getLink(i)).getSource());
		}	

		/**path=getKShortestPath(auxg,src, dst,demandInSlots, overlap);/*ORIGINAl*/
		/**/path=getRShortestPath(auxg,src, dst,demandInSlots, overlap);/*MODIFICADO*/
		if(path==null||path.getSlotList().isEmpty()||path.getLinks().length<1){
			return null;
		}

		return path;
	}

	public  Path getbestPathBackR(WeightedGraph G, int src, int dst, int demandInSlots, Path Path, boolean overlap) {
		WeightedGraph auxg;
		Path path;

		//criando graf aux
		auxg = new WeightedGraph(G.getNumNodes());
		for (int i = 0; i < G.getNumNodes(); i++) {
			for (int j = 0; j < G.getNumNodes(); j++) {
				auxg.setWeight(i,j,G.getWeight(i,j));
			}
		}
		//removendo os links do primeiro caminho, para que não haja links iguais 
		for (int i = 0; i < Path.getLinks().length; i++) {	
			auxg.removeEdge(pt.getLink(Path.getLink(i)).getSource(),pt.getLink(Path.getLink(i)).getDestination());
			auxg.removeEdge(pt.getLink(Path.getLink(i)).getDestination(),pt.getLink(Path.getLink(i)).getSource());
		}	

		/**path=getKShortestPath(auxg,src, dst,demandInSlots, overlap);/*ORIGINAl*/
		/**path=getRShortestPath(auxg,src, dst,demandInSlots, overlap);/*MODIFICADO*/
		path = getRShortestPath2(auxg, src, dst, demandInSlots, overlap);
		if(path==null||path.getSlotList().isEmpty()||path.getLinks().length<1){
			return null;
		}	

		return path;
	}

	public class FlowInfo{
		/**
		 * Observações importantes:
		 * 		- O mesmo fluxo pode ocupar diversos links do caminho sendo roteado, com seu caminho
		 * 			primário e de backup, simultâneamente.
		 * 		- A parte das demandas terá que ser adaptada quando trabalhar com modulação adaptativa
		 *		- O valor das variáveis (first/last)SlotBackup só será válido caso o fluxo use o mesmo
		 *			core nos caminhos primário e backup. 		
		 */
		private long key;
		private int cos;
		private ArrayList<Integer> links;
		private int core;
		private int demand;
		private int firstSlot;
		private int lastSlot;
		private int firstSlotBackup;
		private int lastSlotBackup;
		private boolean[] primaryPath;
		private boolean areAllPrimaryPath;
		private int[] kNextFreeSlots;
		
		public FlowInfo(long key, boolean primaryPath, int link, int core) {
			this.key = key;
			//A demanda é a mesma para caminhos primário e backup ENQUANTO NÃO ESTIVER USANDO MODULAÇÃO ADAPTATIVA
			demand = cp.getFlow(key).getSlotList().size()/cp.getFlow(key).getLinks().length;
			this.firstSlot = -1;
			this.firstSlotBackup = -1;
			this.lastSlot = -1;
			this.lastSlotBackup = -1;
			this.cos = cp.getFlow(key).getCOS();
			this.links = new ArrayList<>();
			this.core = core;
			addLink(link, primaryPath);
		}

		private void borderSlots(boolean primaryPath) {
			Flow flow = cp.getFlow(key);
			
			if(primaryPath) {//Não faz sentido separar as demandas, porque o tamanho do SlotList é diretamente proporcional ao número de links 
				firstSlot = flow.getSlotList().get(0).getslot();
				lastSlot = flow.getSlotList().get(flow.getSlotList().size()-1).getslot();
			} else {
				firstSlotBackup = flow.getSlotListp().get(0).getslot();
				lastSlotBackup = flow.getSlotListp().get(flow.getSlotListp().size()-1).getslot();
			}
			
			boolean aux[] = this.primaryPath;
			this.primaryPath = new boolean[links.size()];
			if(aux != null) {
				for(int i=0; i<aux.length; i++) {
					this.primaryPath[i] = aux[i];
				}
			}
			this.primaryPath[links.size()-1] = primaryPath;
 		}
		
		public long getKey() {
			return key;
		}
		
		public int getCOS() {
			return cos;
		}
		
		public int[] getLinks() {
			int linkVector[] = new int[links.size()];
			int i = 0;
			for(int link:links) {
				linkVector[i] = link;
				i++;
			}
			return linkVector;
		}
		
		public int indexOfLink(int link) {
			return links.indexOf(link);
		}
		
		public int getLink(int i) {
			return links.get(i);
		}
		
		public int getCore() {
			return core;
		}
		
		public int getDemand() {
			return demand;
		}
		
		public int getFirstSlot() {
			return firstSlot;
		}
		
		public int getLastSlot() {
			return lastSlot;
		}
		
		
		public int getFirstSlotBackup(){
			return firstSlotBackup;
		}
		
		public int getLastSlotBackup(){
			return lastSlotBackup;
		}
		
		public boolean isPrimaryPath(int i) {
			return primaryPath[i];
		}
		
		public boolean[] getPrimaryPath() {
			return primaryPath;
		}
		
		public void setPrimaryPath(int i) {
			primaryPath[i] = true;
		}
		
		public boolean areAllPrimaryPath() {
			return areAllPrimaryPath;
		}
		
		public void addLink(int link, boolean primaryPath) {
			if(!links.contains(link)) {
				links.add(link);
				
				borderSlots(primaryPath);
				calculateNextFreeSlots(link);
				
				if(!this.areAllPrimaryPath && primaryPath) {
					this.areAllPrimaryPath = true;
				}
			}
			
			
		}
		
		public int getKNextFreeSlots(int i) {
			return kNextFreeSlots[i];
		}
		
		/**public void setKNextFreeSlots(int index, int k) {
			kNextFreeSlots[index] = k;
		}/**/
		
		private void calculateNextFreeSlots(int link) {
			int index = links.indexOf(link);
			int aux[] = kNextFreeSlots;
			int tam = index+1;
			kNextFreeSlots = new int[tam];
			int count = 0;
			int slot;
			if(primaryPath[index]) {
				slot = lastSlot+1;
			} else {
				slot = lastSlotBackup+1;
			}
			while(slot<=pt.getNumSlots()-1 && pt.getLink(link).getSpectrum(core, slot)) {
				count++;
				slot++;
			}
			kNextFreeSlots[index] = count;
			if(aux != null) {
				for(int i=0; i<index; i++) {
					kNextFreeSlots[i] = aux[i];
				}
			}
		}
	}

	@Override
	public void flowRerouting(Flow flow) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasRestorationMethod() {
		return this.restorationMethod;
	}


}
