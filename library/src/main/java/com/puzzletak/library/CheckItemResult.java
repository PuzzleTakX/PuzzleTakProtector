package com.puzzletak.library;

import java.util.HashMap;
import java.util.Map;

public class CheckItemResult {
    public String item;
    public String option;
    public boolean isSuspicious;

    public CheckItemResult(String item, boolean isSuspicious,String option) {
        this.item = item;
        this.isSuspicious = isSuspicious;
        this.option = option;
    }
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("item", item);
        map.put("isSuspicious", isSuspicious);
        map.put("option",option);
        return map;
    }
}
