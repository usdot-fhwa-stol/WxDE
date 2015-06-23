/************************************************************************
 * Source filename: NetCdfArrayList.java
 * 
 * Creation date: Sep 26, 2013
 * 
 * Author: zhengg
 * 
 * Project: VDT Integration
 * 
 * Objective:
 * 
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.vdt;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class NetCdfArrayList<T> extends ArrayList<T> {
    
    private static final Logger logger = Logger.getLogger(NetCdfArrayList.class);

    private static final int INITIAL_CAPACITY = 1000;
    
    private int currentCapacity;
    
    private T fillValue = null;
    
    public NetCdfArrayList() {
        super(INITIAL_CAPACITY);
        currentCapacity = INITIAL_CAPACITY;
    }
    
    public void init(T f) {
        this.fillValue = f;
        int count = 0;
        
        while (count++ < INITIAL_CAPACITY)
            add(fillValue);
    }
    
    public void update(int position, T value) {
        if (value.getClass() == fillValue.getClass()) {
            remove(position);
            super.add(position, value);
        }
        else
            logger.error("Cannot insert value: " + value + " of type " + value.getClass() + " expecting " + fillValue.getClass());
    }

    public void ensureCapacity(int size) {
        super.ensureCapacity(size);
        
        int count = currentCapacity;
        while (count++ < size)
            add(fillValue);
        
        currentCapacity = size;
    }
    
    public static void main(String[] args) {
        NetCdfArrayList<Double> myType = new NetCdfArrayList<>();
        myType.init(Double.valueOf(-9999.0));
        
        System.out.println("ok");
    }
}
