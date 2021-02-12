package flexgridsim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import flexgridsim.Flow;

public class InfoWriter {
	public InfoWriter(FlexGridLink link) {
		FileWriter fStream;
		try {
			File file = new File("links.csv");
			if(!file.exists()) {
				fStream = new FileWriter(file, true);
				fStream.append("id, src, dst, cores, delay, slots, weight, distance");
			} else {
				fStream = new FileWriter(file, true);
			}
			fStream.append(Integer.toString(link.getID()) + ", " + Integer.toString(link.getSource()) + ", " + Integer.toString(link.getDestination()) +  ", " + "7" + ", " + Double.toString(link.getDelay()) + ", " + "320" + ", " + Double.toString(link.getWeight()) + ", " + Double.toString(link.getDistance()));
			fStream.append("\n");
			fStream.close();
		} catch (IOException e) {
			System.out.println("Error writing the graph file");
		} catch (IndexOutOfBoundsException e){
			System.out.println("Não calculou valor");
		}
	}

	public InfoWriter(Flow flow) {
		FileWriter fStream;
		try {
			File file = new File("flows.csv");
			if(!file.exists()) {
				fStream = new FileWriter(file, true);
				fStream.append("id, src, dst, time, rate, holdingTime, COS, deadline");
				fStream.append("\n");
			} else {
				fStream = new FileWriter(file, true);
			}
			fStream.append(Long.toString(flow.getID()) + ", " + Integer.toString(flow.getSource()) + ", " + Integer.toString(flow.getDestination()) +  ", " + Double.toString(flow.getTime()) + ", " + Double.toString(flow.getRate()) + ", " + "HoldingTime" + ", " + Double.toString(flow.getCOS()) + ", " + "Deadline");
			fStream.append("\n");
			fStream.close();
		} catch (IOException e) {
			System.out.println("Error writing the graph file");
		} catch (IndexOutOfBoundsException e){
			System.out.println("Não calculou valor");
		}
	}
	
	public InfoWriter(double load, int[] modulations, int connections, String docName) {
		FileWriter fStream;
		try {
			File file = new File(docName);
			if(!file.exists()) {
				fStream = new FileWriter(file, true);
				fStream.append("#\t" + "BPSK" 
						+"\t"+ "QPSK" 
						+"\t"+ "8QAM" 
						+"\t"+ "16QAM" 
						+"\t"+ "32QAM" 
						+"\t"+ "64QAM");
				fStream.append("\n");
			} else {
				fStream = new FileWriter(file, true);
			}
			//x = (mod * 100)/conec;
			double bpsk = (double)(modulations[0] * 100) / connections;
			double qpsk = (double)(modulations[1] * 100) / connections;
			double qam8 = (double)(modulations[2] * 100) / connections;
			double qam16 = (double)(modulations[3] * 100) / connections;
			double qam32 = (double)(modulations[4] * 100) / connections;
			double qam64 = (double)(modulations[5] * 100) / connections;

			fStream.append(load + "\t" + Double.toString(bpsk) 
					+ "\t" + Double.toString(qpsk) 
					+ "\t" + Double.toString(qam8) 
					+ "\t" + Double.toString(qam16) 
					+ "\t" + Double.toString(qam32) 
					+ "\t" + Double.toString(qam64));
			fStream.append("\n");
			fStream.close();
		} catch (IOException e) {
			//System.out.println("Error writing the graph file");
		} catch (IndexOutOfBoundsException e){
			//System.out.println("Não calculou valor");
		}
	}
}