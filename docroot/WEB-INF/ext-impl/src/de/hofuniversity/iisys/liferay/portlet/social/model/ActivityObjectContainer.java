package de.hofuniversity.iisys.liferay.portlet.social.model;

import com.liferay.portal.json.JSONObjectImpl;
import com.liferay.portal.kernel.json.JSONObject;

public class ActivityObjectContainer extends JSONObjectImpl
{
	public ActivityObjectContainer(String type, String id, String name, String url)
	{
		if(id != null)
		{
			if(!"person".equals(type))
			{
				this.put("id", type + ":" + id);
			}
			else
			{
				this.put("id", id);
			}
		}
		
		this.put("objectType", type);
		this.put("displayName", name);
		
		if(url != null)
		{
			this.put("url", url);
		}
	}
	
	public void setAuthor(JSONObject author)
	{
		this.put("author", author);
	}
	
	public void setContent(String content)
	{
		this.put("content", content);
	}
	
	public void setSummary(String summary)
	{
		this.put("summary", summary);
	}
}
