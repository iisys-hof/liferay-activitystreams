package de.hofuniversity.iisys.liferay.portlet.social.objgens;

import com.liferay.portlet.blogs.model.BlogsEntry;
import com.liferay.portlet.blogs.service.BlogsEntryLocalServiceUtil;

import de.hofuniversity.iisys.liferay.portlet.social.model.ActivityObjectContainer;

public class BlogsObjectGenerator implements IObjectGenerator
{
	private final String fServiceUrl;
	
	public BlogsObjectGenerator(String url)
	{
		fServiceUrl = url;
	}

	@Override
	public ActivityObjectContainer generate(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;
		BlogsEntry blogEntry = null;
		
		try
		{
			blogEntry = BlogsEntryLocalServiceUtil.getEntry(primaryKey);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		String url = fServiceUrl + "/blogs/find_entry?entryId=" + blogEntry.getEntryId();
		
		container = new ActivityObjectContainer("liferay-blog-entry",
				Long.toString(blogEntry.getEntryId()), blogEntry.getTitle(), url);
		
		return container;
	}

}
