package de.hofuniversity.iisys.liferay.portlet.social.extractors;

import java.util.Locale;

import com.liferay.portlet.social.model.SocialActivity;
import com.liferay.portlet.social.model.SocialActivityConstants;

import de.hofuniversity.iisys.liferay.portlet.social.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.portlet.social.model.SocialActivityContainer;

public class GenericExtractor implements IActivityExtractor
{
	private final Locale fLocale;
	private final String fServiceUrl;

	private final ActivityObjectContainer fGenerator;
	
	public GenericExtractor(Locale locale, String url)
	{
		fLocale = locale;
		fServiceUrl = url;
		
		fGenerator = new ActivityObjectContainer("application",
				"liferay-generic-activities", "Liferay", fServiceUrl);
	}
	
	@Override
	public void extract(SocialActivity activity,
			SocialActivityContainer container)
	{
		String className = activity.getClassName();
		String name = null;
		
		String entity = className;
		if(className.contains("."))
		{
			entity = className.substring(className.lastIndexOf('.') + 1);
		}

		int type = activity.getType();
		String genType = "unknown type " + type + " for entity " + entity;
		boolean target = false;
		switch(type)
		{
			case SocialActivityConstants.TYPE_ADD_ATTACHMENT:
				genType = "attachment added";
				target = true;
				break;
				
			case SocialActivityConstants.TYPE_ADD_COMMENT:
				genType = "comment added";
				target = true;
				break;
				
			case SocialActivityConstants.TYPE_ADD_VOTE:
				genType = "vote added";
				target = true;
				break;
				
			case SocialActivityConstants.TYPE_DELETE:
				genType = "deleted";
				break;
				
			case SocialActivityConstants.TYPE_MOVE_ATTACHMENT_TO_TRASH:
				genType = "attachment moved to trash";
				target = true;
				break;
				
			case SocialActivityConstants.TYPE_MOVE_TO_TRASH:
				genType = "moved to trash";
				break;
				
			case SocialActivityConstants.TYPE_RESTORE_ATTACHMENT_FROM_TRASH:
				genType = "restored attachment from trash";
				target = true;
				break;
				
			case SocialActivityConstants.TYPE_RESTORE_FROM_TRASH:
				genType = "restored from trash";
				break;
				
			case SocialActivityConstants.TYPE_SUBSCRIBE:
				genType = "subscribed";
				target = true;
				break;
				
			case SocialActivityConstants.TYPE_UNSUBSCRIBE:
				genType = "unsubscribed";
				target = true;
				break;
				
			case SocialActivityConstants.TYPE_VIEW:
				genType = "viewed";
				break;
		}
		container.setTitle(genType);
		
		try
		{
			name = activity.getExtraDataValue("title");
		}
		catch(Exception e) {}
		
		try
		{
			name = activity.getExtraDataValue("name");
		}
		catch(Exception e) {}
		
		if(name == null)
		{
			name = entity;
		}
		
		ActivityObjectContainer object = new ActivityObjectContainer("liferay-" + entity,
				Long.toString(activity.getClassPK()), name, null);
		
		if(target)
		{
			container.setTarget(object);
		}
		else
		{
			container.setObject(object);
		}
		
		//attach the generic part as generator
		container.setGenerator(fGenerator);
	}

}
