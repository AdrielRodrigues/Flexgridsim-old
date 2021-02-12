/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim;

/**
 * The Optical Cross-Connects (OXCs) can switch the optical signal coming
 * in on a wavelenght of an input fiber link to the same wavelength in an
 * output fiber link. The OXC may also switch the optical signal on an
 * incoming wavelength of an input fiber link to some other wavelength on
 * an output fiber link.
 * 
 * The OXC object has grooming input and output ports.
 * Traffic grooming is the process of grouping many small data flows
 * into larger units, so they can be processed as single units.
 * Grooming in OXCs has the objective of minimizing the cost of the network.
 * 
 * @author andred
 */
public class OXC {

    private int id;
    private int groomingInputPorts;
    private int groomingOutputPorts;
    private int freeGroomingInputPorts;
    private int freeGroomingOutputPorts;
    private int wvlConverters;
    private int freeWvlConverters;
    private int wvlConversionRange;
    
    /**
     * Creates a new OXC object. All its attributes must be given
     * given by parameter, except for the free grooming input and output
     * ports, that, at the beginning of the simulation, are the same as 
     * the total number of grooming input and output ports.
     * 
     * @param id the OXC's unique identifier
     * @param groomingInputPorts total number of grooming input ports
     * @param groomingOutputPorts total number of grooming output ports
     * @param wvlConverters total number of wavelength converters
     * @param wvlConversionRange the range of wavelength conversion
     */
    public OXC(int id, int groomingInputPorts, int groomingOutputPorts, int wvlConverters, int wvlConversionRange) {
        this.id = id;
        this.groomingInputPorts = this.freeGroomingInputPorts = groomingInputPorts;
        this.groomingOutputPorts = this.freeGroomingOutputPorts = groomingOutputPorts;
        this.wvlConverters = this.freeWvlConverters = wvlConverters;
        this.wvlConversionRange = wvlConversionRange;
    }
    
    /**
     * Retrieves the OXC's unique identifier.
     * 
     * @return the OXC's id attribute
     */
    public int getID() {
        return id;
    }
    
    /**
     * Says whether or not a given OXC has free
     * grooming input port(s).
     * 
     * @return true if the OXC has free grooming input port(s)
     */
    public boolean hasFreeGroomingInputPort() {
        if (freeGroomingInputPorts > 0) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * By decreasing the number of free grooming input ports,
     * this function "reserves" a grooming input port.
     * 
     * @return false if there are no free grooming input ports
     */
    public boolean reserveGroomingInputPort() {
        if (freeGroomingInputPorts > 0) {
            freeGroomingInputPorts--;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * By increasing the number of free grooming input ports,
     * this function "releases" a grooming input port.
     * 
     * @return false if there are no grooming input ports to be freed
     */
    public boolean releaseGroomingInputPort() {
        if (freeGroomingInputPorts < groomingInputPorts) {
            freeGroomingInputPorts++;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * This function says whether or not a given OXC has free
     * grooming output port(s).
     * 
     * @return true if the OXC has free grooming output port(s)
     */
    public boolean hasFreeGroomingOutputPort() {
        if (freeGroomingOutputPorts > 0) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * By decreasing the number of free grooming output ports,
     * this function "reserves" a grooming output port.
     * 
     * @return false if there are no free grooming output ports
     */
    public boolean reserveGroomingOutputPort() {
        if (freeGroomingOutputPorts > 0) {
            freeGroomingOutputPorts--;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * By increasing the number of free grooming output ports,
     * this function "releases" a grooming output port.
     * 
     * @return false if there are no grooming output ports to be freed
     */
    public boolean releaseGroomingOutputPort() {
        if (freeGroomingOutputPorts < groomingOutputPorts) {
            freeGroomingOutputPorts++;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * This function says whether or not a given OXC has free
     * wavelength converter(s).
     * 
     * @return true if the OXC has free wavelength converter(s)
     */
    public boolean hasFreeWvlConverters() {
        if (freeWvlConverters > 0) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * By decreasing the number of free wavelength converters,
     * this function "reserves" a wavelength converter.
     * 
     * @return false if there are no free wavelength converters
     */
    public boolean reserveWvlConverter() {
        if (freeWvlConverters > 0) {
            freeWvlConverters--;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * By increasing the number of free wavelength converters,
     * this function "releases" a wavelength converters.
     * 
     * @return false if there are no wavelength converters to be freed
     */
    public boolean releaseWvlConverter() {
        if (freeWvlConverters < wvlConverters) {
            freeWvlConverters++;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * This function provides the wavelength conversion range
     * of a given OXC.
     * 
     * @return the OXC's wvlConversionRange attribute
     */
    public int getWvlConversionRange() {
        return wvlConversionRange;
    }
}
