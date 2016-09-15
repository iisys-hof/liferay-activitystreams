package de.hofuniversity.iisys.liferay.activitystreams.extractors;

import java.util.Locale;
import java.util.Map;

import com.liferay.bookmarks.social.BookmarksActivityKeys;
import com.liferay.microblogs.model.MicroblogsEntry;
import com.liferay.microblogs.service.MicroblogsEntryLocalServiceUtil;
import com.liferay.social.kernel.model.SocialActivity;
import com.liferay.social.kernel.model.SocialActivityConstants;

import de.hofuniversity.iisys.liferay.activitystreams.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.activitystreams.model.SocialActivityContainer;

public class MicroblogsExtractor implements IActivityExtractor
{
	private final Locale fLocale;
	private final String fServiceUrl;
	
	private final String fEntryName;
	
	private final ActivityObjectContainer fGenerator;

	public MicroblogsExtractor(Map<String, String> properties, Locale locale, String url)
	{
		fLocale = locale;
		fServiceUrl = url;
		
		fEntryName = properties.get("microblog_entry_name");
		
		fGenerator = new ActivityObjectContainer("application", "liferay-microblogs",
				"Liferay Microblogs", null);
	}
	
	@Override
	public void extract(SocialActivity activity, SocialActivityContainer container)
	{
		String content = null;
		try
		{
			MicroblogsEntry entry = MicroblogsEntryLocalServiceUtil
					.getMicroblogsEntry(activity.getClassPK());
			
			content = entry.getContent();
			container.setContent(content);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//not yet available
//		String url = fServiceUrl + "/microblogs/find_entry?entryId=" + activity.getClassPK();
		
		ActivityObjectContainer entryObject = new ActivityObjectContainer("liferay-microblog-entry",
				Long.toString(activity.getClassPK()), fEntryName, null);

		if(activity.getType() != SocialActivityConstants.TYPE_ADD_VOTE
				&& activity.getType() != SocialActivityConstants.TYPE_ADD_COMMENT)
		{
			container.setObject(entryObject);
		}
		else
		{
			container.setTarget(entryObject);
		}
		
		//attach the microblogs part as generator
		container.setGenerator(fGenerator);
	}

}
