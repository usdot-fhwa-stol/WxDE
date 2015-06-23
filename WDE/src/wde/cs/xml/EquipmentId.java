/************************************************************************
 * Source filename: EquipmentId.java
 * <p/>
 * Creation date: May 28, 2014
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.cs.xml;

public class EquipmentId extends DataValue {
    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code EquipmentId}. Initialization done through
     * base class {@link DataValue#init} method.
     * </p>
     */
    EquipmentId() {
    }

    /**
     * Sets the platform code for the base class instance of {@code XmlCollecotr}
     * to the integer value represented by the StringBuilder {@code sBuffer}.
     *
     * @param sBuffer the StringBuilder representing the sensor index integer
     * value.
     */
    @Override
    public void characters(StringBuilder sBuffer) {
        m_oXmlCollector.setEquipmentId(sBuffer);
    }
}
