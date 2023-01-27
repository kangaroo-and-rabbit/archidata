package org.kar.archidata.model;

import java.util.HashMap;
import java.util.Map;


public class UserByToken {
	public static final int TYPE_USER = -1;
	public static final int TYPE_APPLICATION = -2;
	// application internal management type: an application generic Id
	public Integer type = null;
	
	public Long id = null;
    public Long parentId = null;
    public String name = null;
    // Right map
    public Map<String, Boolean> right = new HashMap<>();
  
}
