/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.eap.LSHDB;

import gr.eap.LSHDB.util.Property;
import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author dimkar
 */
public class GramConfiguration extends Configuration implements Serializable {
    
    public static final long serialVersionUID = 1002001L;
    
    public GramConfiguration(String folder, String dbName, String dbEngine, boolean massInsertMode) throws StoreInitException {
        super(folder, dbName, dbEngine,massInsertMode);  

    }

    public GramConfiguration(String folder, String dbName, String dbEngine, Key[] keysList,boolean massInsertMode)throws StoreInitException  {
        super(folder, dbName, dbEngine,keysList,massInsertMode);
    }
    
    
    
    
    
    
    
}
