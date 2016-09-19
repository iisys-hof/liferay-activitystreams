package de.hofuniversity.iisys.liferay.portlet.social.objgens;

import com.liferay.portlet.wiki.model.WikiPage;
import com.liferay.portlet.wiki.model.WikiPageResource;
import com.liferay.portlet.wiki.service.WikiPageLocalServiceUtil;
import com.liferay.portlet.wiki.service.WikiPageResourceLocalServiceUtil;

import de.hofuniversity.iisys.liferay.portlet.social.model.ActivityObjectContainer;

public class WikiObjectGenerator implements IObjectGenerator
{
	private final String fServiceUrl;
	
	public WikiObjectGenerator(String url)
	{
		fServiceUrl = url;
	}

	@Override
	public ActivityObjectContainer generate(String className, long primaryKey)
	{
		if(!"com.liferay.portlet.wiki.model.WikiPage".equals(className))
		{
			return null;
		}
		
		ActivityObjectContainer container = null;
		
		WikiPageResource pageResource = null;
		WikiPage page = null;
		
		double version = -1.;
		
		try
		{
			pageResource = WikiPageResourceLocalServiceUtil.fetchWikiPageResource(
					primaryKey);
			
			if(version >= 0)
			{
				page = WikiPageLocalServiceUtil.getPage(
						pageResource.getNodeId(), pageResource.getTitle(), version);
			}
			else
			{
				page = WikiPageLocalServiceUtil.getPage(pageResource.getNodeId(),
						pageResource.getTitle());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		
		//page
		String pageLink = fServiceUrl + "/wiki/find_page?pageResourcePrimKey="
				+ primaryKey;
		String content = "version: " + version;
		String name = pageResource.getTitle();
		
		container = new ActivityObjectContainer("liferay-wiki-page",
				Long.toString(page.getPrimaryKey()), name, pageLink);
		container.setContent(content);
		
		return container;
	}

}
