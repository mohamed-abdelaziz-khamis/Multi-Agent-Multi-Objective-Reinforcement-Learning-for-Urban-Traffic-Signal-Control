//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.1-05/30/2003 05:06 AM(java_re)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2004.09.08 at 12:57:21 ART 
//


package gld.sim.stats.tracks.bind;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the gld.sim.stats.tracks.bind package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
public class ObjectFactory
    extends gld.sim.stats.tracks.bind.impl.runtime.DefaultJAXBContextImpl
{

    private static java.util.HashMap defaultImplementations = new java.util.HashMap();
    public final static java.lang.Class version = (gld.sim.stats.tracks.bind.impl.JAXBVersion.class);

    static {
        defaultImplementations.put("gld.sim.stats.tracks.bind.Track", "gld.sim.stats.tracks.bind.impl.TrackImpl");
        defaultImplementations.put("gld.sim.stats.tracks.bind.TrackType", "gld.sim.stats.tracks.bind.impl.TrackTypeImpl");
        defaultImplementations.put("gld.sim.stats.tracks.bind.TracksType", "gld.sim.stats.tracks.bind.impl.TracksTypeImpl");
        defaultImplementations.put("gld.sim.stats.tracks.bind.Tracks", "gld.sim.stats.tracks.bind.impl.TracksImpl");
    }

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: gld.sim.stats.tracks.bind
     * 
     */
    public ObjectFactory() {
        super(new gld.sim.stats.tracks.bind.ObjectFactory.GrammarInfoImpl());
    }

    /**
     * Create an instance of the specified Java content interface.
     * 
     * @param javaContentInterface the Class object of the javacontent interface to instantiate
     * @return a new instance
     * @throws JAXBException if an error occurs
     */
    public java.lang.Object newInstance(java.lang.Class javaContentInterface)
        throws javax.xml.bind.JAXBException
    {
        return super.newInstance(javaContentInterface);
    }

    /**
     * Get the specified property. This method can only be
     * used to get provider specific properties.
     * Attempting to get an undefined property will result
     * in a PropertyException being thrown.
     * 
     * @param name the name of the property to retrieve
     * @return the value of the requested property
     * @throws PropertyException when there is an error retrieving the given property or value
     */
    public java.lang.Object getProperty(java.lang.String name)
        throws javax.xml.bind.PropertyException
    {
        return super.getProperty(name);
    }

    /**
     * Set the specified property. This method can only be
     * used to set provider specific properties.
     * Attempting to set an undefined property will result
     * in a PropertyException being thrown.
     * 
     * @param name the name of the property to retrieve
     * @param value the value of the property to be set
     * @throws PropertyException when there is an error processing the given property or value
     */
    public void setProperty(java.lang.String name, java.lang.Object value)
        throws javax.xml.bind.PropertyException
    {
        super.setProperty(name, value);
    }

    /**
     * Create an instance of Track
     * 
     * @throws JAXBException if an error occurs
     */
    public gld.sim.stats.tracks.bind.Track createTrack()
        throws javax.xml.bind.JAXBException
    {
        return new gld.sim.stats.tracks.bind.impl.TrackImpl();
    }

    /**
     * Create an instance of TrackType
     * 
     * @throws JAXBException if an error occurs
     */
    public gld.sim.stats.tracks.bind.TrackType createTrackType()
        throws javax.xml.bind.JAXBException
    {
        return new gld.sim.stats.tracks.bind.impl.TrackTypeImpl();
    }

    /**
     * Create an instance of TracksType
     * 
     * @throws JAXBException if an error occurs
     */
    public gld.sim.stats.tracks.bind.TracksType createTracksType()
        throws javax.xml.bind.JAXBException
    {
        return new gld.sim.stats.tracks.bind.impl.TracksTypeImpl();
    }

    /**
     * Create an instance of Tracks
     * 
     * @throws JAXBException if an error occurs
     */
    public gld.sim.stats.tracks.bind.Tracks createTracks()
        throws javax.xml.bind.JAXBException
    {
        return new gld.sim.stats.tracks.bind.impl.TracksImpl();
    }

    private static class GrammarInfoImpl
        extends gld.sim.stats.tracks.bind.impl.runtime.AbstractGrammarInfoImpl
    {


        public java.lang.Class getDefaultImplementation(java.lang.Class javaContentInterface) {
            java.lang.Class c = null;
            try {
                c = java.lang.Class.forName(((java.lang.String) defaultImplementations.get(javaContentInterface.getName())));
            } catch (java.lang.Exception _x) {
                c = null;
            }
            return c;
        }

        public gld.sim.stats.tracks.bind.impl.runtime.UnmarshallingEventHandler createUnmarshaller(java.lang.String uri, java.lang.String local, gld.sim.stats.tracks.bind.impl.runtime.UnmarshallingContext context) {
            if (("tracks" == local)&&("" == uri)) {
                return new gld.sim.stats.tracks.bind.impl.TracksImpl().createUnmarshaller(context);
            }
            if (("track" == local)&&("" == uri)) {
                return new gld.sim.stats.tracks.bind.impl.TrackImpl().createUnmarshaller(context);
            }
            return null;
        }

        public java.lang.Class getRootElement(java.lang.String uri, java.lang.String local) {
            if (("tracks" == local)&&("" == uri)) {
                return (gld.sim.stats.tracks.bind.impl.TracksImpl.class);
            }
            if (("track" == local)&&("" == uri)) {
                return (gld.sim.stats.tracks.bind.impl.TrackImpl.class);
            }
            return null;
        }

        public boolean recognize(java.lang.String uri, java.lang.String local) {
            if (("tracks" == local)&&("" == uri)) {
                return true;
            }
            if (("track" == local)&&("" == uri)) {
                return true;
            }
            return false;
        }

        public java.lang.String[] getProbePoints() {
            return new java.lang.String[] {"", "tracks", "", "track"};
        }

    }

}