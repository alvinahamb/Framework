package framework.scan;

import java.util.Map;
import java.util.HashMap;

public class ModelView {
    private String view;
    private Map<String, Object> data;

    public ModelView(String view) {
        this.view = "/src/main/webapp/" + view;
        this.data = new HashMap<>();
    }

    public String getView() {return view;}
    public void setView(String view) {
        this.view = "/src/main/webapp/" + view;
    }

    public Map<String, Object> getData() {return data;}
    public void setData(Map<String, Object> data) {this.data = data;}
    
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }
}
