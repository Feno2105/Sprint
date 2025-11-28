package com.itu.classe;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String, Object> data;

    public ModelView(){
        this.data = new HashMap<>();
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void addItem(String key, Object value) {
        this.data.put(key, value);
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }  
     
}
