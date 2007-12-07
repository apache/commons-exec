package org.apache.commons.exec.util;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Helper classes to manipulate maps to pass substition map to the
 * CommandLine.
 *
 * @author <a href="mailto:siegfried.goeschl@it20one.at">Siegfried Goeschl</a>
 */
public class MapUtils
{
    /**
     * Clones a map.
     *
     * @param source the source map
     * @return the clone of the source map
     */
    public static Map copy(Map source) {

        if(source == null) {
            return null;
        }

        Map result = new HashMap();
        result.putAll(source);
        return result;
    }

    /**
     * Clones a map and prefixes the keys in the clone, e.g.
     * for mapping "JAVA_HOME" to "env.JAVA_HOME" to simulate
     * the behaviour of ANT.
     *
     * @param source the source map
     * @return the clone of the source map
     */
    public static Map prefix(Map source, String prefix) {

        Map result = new HashMap();

        if(source == null) {
            return null;
        }

        Iterator iter = source.keySet().iterator();

        while(iter.hasNext()) {
            Object key = iter.next();
            Object value = source.get(key);
            result.put(prefix + '.' + key.toString(), value);
        }

        return result;
    }

    /**
     * Clones the lhs map and add all things from the
     * rhs map.
     *
     * @param lhs the first map
     * @param rhs the second map
     * @return the merged map
     */
    public static Map merge(Map lhs, Map rhs) {

        Map result = null;

        if((lhs == null) || (lhs.size() == 0)) {
            result = copy(rhs);
        }
        else if((rhs == null) || (rhs.size() == 0)) {
            result = copy(lhs);
        }
        else {
            result = copy(lhs);
            result.putAll(rhs);
        }
        
        return result;
    }
}
