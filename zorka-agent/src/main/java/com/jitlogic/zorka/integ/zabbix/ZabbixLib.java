package com.jitlogic.zorka.integ.zabbix;

import com.jitlogic.zorka.agent.JmxObject;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaLib;
import com.jitlogic.zorka.util.ObjectInspector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * This library module
 *
 * @author RLE <rle@jitlogic.com>
 *
 */
public class ZabbixLib {

    private ZorkaBshAgent bshAgent;
    private ZorkaLib zorkaLib;

    private ObjectInspector inspector = new ObjectInspector();

    private Map<String,ZabbixTrapper> trappers = new ConcurrentHashMap<String, ZabbixTrapper>();

    public ZabbixLib(ZorkaBshAgent bshAgent, ZorkaLib zorkaLib) {
        this.bshAgent = bshAgent;
        this.zorkaLib = zorkaLib;
    }

    /**
     *
     * @param mbs
     * @param filter
     * @param attrs
     *
     * @return
     */
    public JSONObject discovery(String mbs, String filter, String...attrs) {
        return discovery(mbs,filter, attrs, new String[0], new String[0]);
    }


    /**
     *
     * @param mbs
     * @param filter
     * @param oattrs
     * @param path
     * @param pattrs
     * @return
     */
    public JSONObject discovery(String mbs, String filter, String[] oattrs, String[] path, String[] pattrs) {

        List<Object> osrc = zorkaLib.jmxList(Arrays.asList((Object)mbs, filter));
        JSONArray    dsrc = new JSONArray();

        // List objects from mbean server
        for (Object obj : osrc) {
            if (obj instanceof JmxObject) {
                ObjectName on = ((JmxObject)obj).getName();
                JSONObject odo = new JSONObject();
                for (String attr : oattrs) {
                    String atval = on.getKeyProperty(attr);
                    if (atval != null) {
                        odo.put(toZabbixAttr(attr), atval);
                    } else {
                        // A bit of a hack - filter out all objects without all (queried) attributes
                        odo.clear();
                        break;
                    }
                }
                if (odo.size() > 0) {
                    dsrc.add(odo);
                } else {
                    dsrc.add(null);
                }
            } else {
                dsrc.add(null);
            }
        }

        // Iterate over fetched list, resolve path and add attributes
        for (int pidx = 0; pidx < path.length; pidx++) {
            List<Object> odst = new ArrayList<Object>(osrc.size()+2);
            JSONArray ddst = new JSONArray();

            String pathItem = path[pidx], pathAttr = pattrs[pidx];

            for (int oidx = 0; oidx < osrc.size(); oidx++) {

                Object srcObj = osrc.get(oidx);
                JSONObject dstObj = (JSONObject)dsrc.get(oidx);

                if (dstObj == null) continue;

                if (pathItem.startsWith("~")) {
                    for (Object attr : inspector.list(srcObj)) {
                        if (attr != null && attr.toString().matches(pathItem.substring(1))) {
                            Object obj = inspector.get(srcObj, attr);
                            if (obj != null) {
                                JSONObject dsr = pathAttr == null ? dstObj
                                        : extend(dstObj, pathAttr, attr.toString());
                                odst.add(obj); ddst.add(dsr);
                            }
                        }
                    }
                } else {
                    Object obj = inspector.get(srcObj, pathItem);
                    if (obj != null) {
                        JSONObject dsr = pathAttr == null ? dstObj
                            : extend(dstObj, pathAttr, pathItem);
                        odst.add(obj); ddst.add(dsr);
                    }
                } //
            } // for (int oidx = 0 ...
            osrc = odst; dsrc = ddst;
        } // for (int pidx = 0 ...

        JSONObject discoveries = new JSONObject();
        JSONArray data = new JSONArray();

        for (Object o : dsrc) {
            if (o != null) data.add(o);
        }

        discoveries.put("data", data);

        return discoveries;
    } // discovery()

    private String toZabbixAttr(String attr) {
        return "{#" + attr.toUpperCase().replace("-","") + "}";
    }


    private JSONObject extend(JSONObject src, String key, String val) {
        JSONObject obj = new JSONObject();
        obj.putAll(src);

        obj.put(toZabbixAttr(key), val);

        return obj;
    }


    public ZabbixTrapper trapper(String id) {
        return trappers.get(id);
    }


    public ZabbixTrapper trapper(String id, String serverAddr, String defaultHost) {
        ZabbixTrapper trapper = trappers.get(id);

        if (trapper == null) {
            trapper = new ZabbixTrapper(serverAddr, defaultHost);
            trappers.put(id, trapper);
            trapper.start();
        }

        return trapper;
    }


    public void remove(String id) {
        ZabbixTrapper trapper = trappers.remove(id);

        if (trapper != null) {
            trapper.stop();
        }
    }
}
