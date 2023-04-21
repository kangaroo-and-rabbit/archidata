package org.kar.archidata.model;

import java.util.HashMap;
import java.util.Map;


public class UserByToken {
	public static final int TYPE_USER = -1;
	public static final int TYPE_APPLICATION = -2;
	// application internal management type: an application generic Id
	public Integer type = null;
	
	public Long id = null;
    public Long parentId = null; // FOr application, this is the id of the application, and of user token, this is the USERID
    public String name = null;
    // Right map
    public Map<String, Object> right = new HashMap<>();
  
}
