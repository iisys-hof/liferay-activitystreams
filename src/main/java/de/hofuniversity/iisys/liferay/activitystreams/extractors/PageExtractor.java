package de.hofuniversity.iisys.liferay.activitystreams.extractors;

import java.util.Locale;
import java.util.Map;

import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.social.kernel.model.SocialActivity;
import com.liferay.social.kernel.model.SocialActivityConstants;

import de.hofuniversity.iisys.liferay.activitystreams.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.activitystreams.model.SocialActivityContainer;

public class PageExtractor implements IActivityExtractor
{
	private final Locale fLocale;
	private final String fServiceUrl;
	
	private final ActivityObjectContainer fGenerator;

	public PageExtractor(Map<String, String> properties, Locale locale, String url)
	{
		fLocale = locale;
		fServiceUrl = url;
		
		fGenerator = new ActivityObjectContainer("application", "liferay",
				"Liferay", null);
	}
	
	
	@Override
	public void extract(SocialActivity activity, SocialActivityContainer container)
	{
		String name = "page";
		String url = null;
		
		try
		{
			Layout layout = LayoutLocalServiceUtil.getLayout(activity.getClassPK());
			
			name = layout.getName(fLocale);
			url = layout.getFriendlyURL();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		ActivityObjectContainer entryObject = new ActivityObjectContainer("liferay-layout",
				Long.toString(activity.getClassPK()), name, url);

		if(activity.getType() != SocialActivityConstants.TYPE_ADD_VOTE
			&& activity.getType() != SocialActivityConstants.TYPE_ADD_COMMENT)
		{
			container.setObject(entryObject);
		}
		else
		{
			container.setTarget(entryObject);
		}
		
		//attach the pages/layout part as generator
		container.setGenerator(fGenerator);
	}

}
