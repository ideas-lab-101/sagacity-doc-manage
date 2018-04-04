package com.sagacity.docs.utility;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jfinal.plugin.activerecord.Record;
import net.sf.json.JSONObject;
import org.json.JSONException;

import java.util.*;

/**
 * 该类用于类型转换
 */
public class ConvertUtil {
    /**
     * Map 类型转换为JSONObject格式的字符串
     * @param map
     * @return
     */
    public static String mapToJsonStr(Map<String ,Object> map){
        if(map==null||map.isEmpty()){
            return "null";
        }
        String jsonStr = "{";
        Set<?> keySet = map.keySet();
        for (Object key : keySet) {
            jsonStr += "\""+key+"\":\""+map.get(key)+"\",";
        }
        jsonStr = jsonStr.substring(0, jsonStr.length()-1);
        jsonStr += "}";
        return jsonStr;
    }

    /**
     * 符合JSONObject的字符串转为Map
     * @param str
     * @return
     */
    public static Map<String, Object> jsonStrToMap(String str){
        JSONObject jo = JSONObject.fromObject(str);
        Map map = new HashMap();
        Iterator it = jo.keys();
        // 遍历jsonObject数据，添加到Map对象
        while (it.hasNext())
        {
            String key = String.valueOf(it.next());
            Object value = jo.get(key);
            map.put(key, value);
        }
        return map;
    }

    /**
     * 将符合的字符串转为List<Map>
     * @param jsonString
     * @return
     */
    public static List<Map<String, String>> jsonStrToListMap(String jsonString) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        try {
            Gson gson = new Gson();
            list = gson.fromJson(jsonString,
                    new TypeToken<List<Map<String, String>>>() {
                    }.getType());
        } catch (Exception e) {
            // TODO: handle exception
        }
        return list;
    }
	
    /*liuji add, 2016-5-19*/
    public static String recordListToJsonStr(List<Record> list) {
        if(list == null) {
            return "";
        }

        Iterator iter = list.iterator();
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        sb.append('[');
        while(iter.hasNext()){
            if(first) {
                first = false;
            } else {
                sb.append(',');
            }

            Record value = (Record) iter.next();
            sb.append(mapToJsonStr2(value.getColumns()));
        }
        sb.append(']');

        return sb.toString();
    }

    public static String mapToJsonStr2(Map<String, Object> map) {
        if(map == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');

        Iterator iter = map.entrySet().iterator();
        boolean first = true;
        while(iter.hasNext()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }

            Map.Entry entry = (Map.Entry) iter.next();
            sb.append('\"');
            sb.append(entry.getKey());
            sb.append('\"').append(':');
            if (entry.getValue() == null) {
                sb.append('\"');
                sb.append('\"');
            } else if (isJsonString(entry.getValue().toString())) {
                sb.append(entry.getValue());
            } else {
                sb.append('\"');
                sb.append(entry.getValue());
                sb.append('\"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static boolean isJsonString(String value) {
        try {
            new org.json.JSONObject(value);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }
    /*liuji add end, 2016-5-19*/
}
