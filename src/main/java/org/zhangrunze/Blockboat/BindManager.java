package org.zhangrunze.Blockboat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BindManager {
    private static final File FILE = new File("Bind.json");
    private Map<String, String> idToNameMap = new HashMap<>();
    private Map<String, String> nameToIdMap = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public BindManager() {
        if (FILE.exists()) {
            try {
                Map<?, ?> map = mapper.readValue(FILE, Map.class);
                idToNameMap = (Map<String, String>) map.get("idToNameMap");
                nameToIdMap = (Map<String, String>) map.get("nameToIdMap");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean bind(String id, String name) {
        if (idToNameMap.containsKey(id)) {
            System.out.println("绑定失败，该QQ号已绑定过MCID。");
            return false;
        }
        idToNameMap.put(id, name);
        nameToIdMap.put(name, id);
        saveToFile();
        return true;
    }

    public boolean unbindById(String id) {
        if (!idToNameMap.containsKey(id)) {
            System.out.println("解绑失败，该QQ号未绑定过MCID。");
            return false;
        }
        String name = idToNameMap.get(id);
        idToNameMap.remove(id);
        nameToIdMap.remove(name);
        saveToFile();
        return true;
    }

    public boolean unbindByName(String name) {
        if (!nameToIdMap.containsKey(name)) {
            System.out.println("解绑失败，该MCID未绑定过QQ号。");
            return false;
        }
        String id = nameToIdMap.get(name);
        nameToIdMap.remove(name);
        idToNameMap.remove(id);
        saveToFile();
        return true;
    }

    public boolean IsIdBind(String id) {
        return idToNameMap.containsKey(id);
    }

    public String getNameById(String id) {
        return idToNameMap.get(id);
    }

    public String printAll() {
        StringBuilder Out = new StringBuilder("ID\tName\n");
        for (String id : idToNameMap.keySet()) {
            String name = idToNameMap.get(id);
            Out.append(id).append("\t").append(name).append("\n");
        }
        return Out.toString();
    }

    private void saveToFile() {
        Map<String, Object> map = new HashMap<>();
        map.put("idToNameMap", idToNameMap);
        map.put("nameToIdMap", nameToIdMap);
        try {
            mapper.writeValue(FILE, map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
