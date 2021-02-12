/**
 * 
 */

package flexgridsim.util;
/**
 * @author helder
 * 
 */
public class Crosstalk {

	/*Routing, Spectrum, and Core and/or
Mode Assignment on Space-Division
Multiplexing Optical Networks [Invited]*/
	/*An Investigation on Crosstalk in Multi-Core Fibers by Introducing
Random Fluctuation along Longitudinal Direction*/

	public static double getCrosstalkDB(int transmissionDistance, int activeNeighbor, char typeFiber) {
		double crosstalk=0, exp=0;
		exp = Math.exp(-(activeNeighbor + 1) * getpowerCouplingCoefficient(activeNeighbor + 1, typeFiber) * (transmissionDistance));
		crosstalk = (activeNeighbor * (1 - exp)) / (1 + activeNeighbor * exp);
		crosstalk = Math.log(crosstalk);
		return crosstalk;
	}
	

	public static double getpowerCouplingCoefficient(int activeNeighbor, char fiber) {
		double coupling=0;
		double powerConversionEfficiency= getPowerConversionEfficiency(fiber);
		double couplingLength = getCouplingLength(fiber);
		coupling = Math.pow(powerConversionEfficiency, activeNeighbor) / Math.pow(couplingLength, activeNeighbor);
		return coupling;
	}
	
	public static double getPowerConversionEfficiency(char fiber){
		switch (fiber){
		case 'A':
			return 2.8 * Math.pow(10, -13);
		case 'B': 
			return 3.4 *  Math.pow(10, -10);
		case 'C': 
			return 5.6 *  Math.pow(10, -9);
		case 'D': 
			return 7.6 *  Math.pow(10, -7);
		default:
			return 2.8 *  Math.pow(10, -13);
		}
	}
	public static double getCouplingLength(char fiber){
		switch (fiber){
		case 'A':
			return 4.5* Math.pow(10, -3);
		case 'B': 
			return 4.4* Math.pow(10, -3);
		case 'C': 
			return 5.3* Math.pow(10, -3);
		case 'D': 
			return 7.8* Math.pow(10, -3);
		default:
			return 4.5* Math.pow(10, -3);
		}
	}
	
	
	/**************************segundo jeito***************/
	
	public static double getCrosstalkDB(int transmissionDistance, int activeNeighbor) {
		double k = 2*Math.pow(10, -5); // de 2*10-5 ate 3,5*10-3
		double beta = 4*Math.pow(10, 6);// around the 1550 nm frequency
		double R = 50; //50 to 80 mm
		double D = 45; //μm for the 7-core and 35 μm for the 19-core. 
		double h = (2* Math.pow(k, 2)*R)/ beta*D;
		double exp = Math.exp(-(activeNeighbor + 1) * 2* h*transmissionDistance);
		return (activeNeighbor - activeNeighbor*exp )/ (1+activeNeighbor*exp);	
	}


	
}