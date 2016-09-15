package de.hofuniversity.iisys.liferay.activitystreams.extractors;

import java.util.Locale;
import java.util.Map;

import com.liferay.social.kernel.model.SocialActivity;
import com.liferay.social.kernel.model.SocialActivityConstants;

import de.hofuniversity.iisys.liferay.activitystreams.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.activitystreams.model.SocialActivityContainer;

public class CalendarExtractor implements IActivityExtractor
{
	private final Map<String, String> fProperties;
	
	private final Locale fLocale;
	private final String fServiceUrl;
	
	private final ActivityObjectContainer fGenerator;
	
	private final String fCalendarName, fCalEntryName;

	public CalendarExtractor(Map<String, String> properties, Locale locale,
			String url)
	{
		fProperties = properties;
		fLocale = locale;
		fServiceUrl = url;
		
		fGenerator = new ActivityObjectContainer("application", "liferay-calendars",
				"Liferay Calendars", null);
		fCalendarName = fProperties.get("calendar_display_name");
		fCalEntryName = fProperties.get("calendar_entry_display_name");
	}

	@Override
	public void extract(SocialActivity activity,
			SocialActivityContainer container)
	{
		//TODO: CalendarBooking class is part of a plugin, get jar file
		//TODO: acquire constants

		final int type = activity.getType();
		String verb = null;
		String title = null;
		
		//TODO: official titles
		switch(type)
		{
			case 1:
				verb = "add";
				title = "calendar entry created";
				break;
				
			case 2:
				verb = "update";
				title = "calendar entry updated";
				break;
				
			case SocialActivityConstants.TYPE_DELETE:
				title = "calendar entry deleted";
				break;
		}
		
		if(verb != null)
		{
			container.setVerb(verb);
		}
		
		if(title != null)
		{
			container.setTitle(title);
		}
		
		//entry object
		ActivityObjectContainer entryObject = null;
		//TODO: ID?
		//TODO: proper title?
		//TODO: link?
		entryObject = new ActivityObjectContainer("liferay-calendar-entry", null,
				fCalEntryName, null);
		container.setObject(entryObject);
		
		//calendar object
		ActivityObjectContainer calObject = null;
		//TODO: ID?
		//TODO: proper title?
		//TODO: link?
		calObject = new ActivityObjectContainer("liferay-calendar", null,
				fCalendarName, null);
		container.setTarget(calObject);
		
		container.setGenerator(fGenerator);
	}

}
