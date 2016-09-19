package de.hofuniversity.iisys.liferay.portlet.social.model;

import com.liferay.portal.json.JSONObjectImpl;
import com.liferay.portal.kernel.json.JSONObject;

public class SocialActivityContainer
{
	private final JSONObject fJson = new JSONObjectImpl();
	
	public void setActor(JSONObject actor)
	{
		if(actor != null)
		{
			fJson.put("actor", actor);
		}
		else
		{
			fJson.remove("actor");
		}
	}
	
	public void setObject(JSONObject object)
	{
		if(object != null)
		{
			fJson.put("object", object);
		}
		else
		{
			fJson.remove("object");
		}
	}
	
	public void setTarget(JSONObject target)
	{
		if(target != null)
		{
			fJson.put("target", target);
		}
		else
		{
			fJson.remove("target");
		}
	}
	
	public void setGenerator(JSONObject generator)
	{
		fJson.put("generator", generator);
	}
	
	public void setProvider(JSONObject provider)
	{
		fJson.put("provider", provider);
	}
	
	public void setId(String type, long id)
	{
		fJson.put("id", type + ":" + id);
	}
	
	public void setVerb(String verb)
	{
		fJson.put("verb", verb);
	}
	
	public void setTitle(String title)
	{
		fJson.put("title", title);
	}
	
	public void setContent(String content)
	{
		fJson.put("content", content);
	}
	
	public String toJson()
	{
		return fJson.toString();
	}
	
	public String getTitle()
	{
		if(fJson.has("title"))
		{
			return fJson.getString("title");
		}
		
		return null;
	}
	
	public JSONObject getActor()
	{
		if(fJson.has("actor"))
		{
			return fJson.getJSONObject("actor");
		}
		
		return null;
	}
	
	public JSONObject getObject()
	{
		if(fJson.has("object"))
		{
			return fJson.getJSONObject("object");
		}
		
		return null;
	}
	
	public JSONObject getTarget()
	{
		if(fJson.has("target"))
		{
			return fJson.getJSONObject("target");
		}
		
		return null;
	}
}
