package de.hofuniversity.iisys.liferay.portlet.social.extractors;

import java.util.Locale;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupServiceUtil;
import com.liferay.portlet.blogs.model.BlogsEntry;
import com.liferay.portlet.blogs.service.BlogsEntryLocalServiceUtil;
import com.liferay.portlet.blogs.social.BlogsActivityKeys;
import com.liferay.portlet.social.model.SocialActivity;
import com.liferay.portlet.social.model.SocialActivityConstants;

import de.hofuniversity.iisys.liferay.portlet.social.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.portlet.social.model.SocialActivityContainer;

public class BlogsExtractor implements IActivityExtractor
{
	private final Locale fLocale;
	private final String fServiceUrl;
	
	private final ActivityObjectContainer fGenerator;

	public BlogsExtractor(Locale locale, String url)
	{
		fLocale = locale;
		fServiceUrl = url;
		
		fGenerator = new ActivityObjectContainer("application", "liferay-blogs",
				"Liferay Blogs", null);
	}

	@Override
	public void extract(SocialActivity activity,
			SocialActivityContainer container)
	{
		BlogsEntry blogEntry = null;
		
		try
		{
			blogEntry = BlogsEntryLocalServiceUtil.getEntry(activity.getClassPK());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return;
		}

		//get possibly linked group object
		Group group = null;
		try
		{
			group = GroupServiceUtil.getGroup(activity.getGroupId());
		}
		catch(Exception e) {}
		
		//determine message
		final int type = activity.getType();
		String verb = null;
		String titleVar = null;
		String title = null;
		String content = null;
		
		switch(type)
		{
			case BlogsActivityKeys.ADD_COMMENT:
				titleVar = "activity-blogs-entry-add-comment";
				break;
			
			case BlogsActivityKeys.ADD_ENTRY:
				if(blogEntry.getStatus() == WorkflowConstants.STATUS_SCHEDULED)
				{
					titleVar = "activity-blogs-entry-schedule-entry";
				}
				else
				{
					titleVar = "activity-blogs-entry-add-entry";
				}
				break;
			
			case SocialActivityConstants.TYPE_MOVE_TO_TRASH:
				titleVar = "activity-blogs-entry-move-to-trash";
				break;
			
			case SocialActivityConstants.TYPE_RESTORE_FROM_TRASH:
				titleVar = "activity-blogs-entry-restore-from-trash";
				break;
				
			case BlogsActivityKeys.UPDATE_ENTRY:
				title = "activity-blogs-entry-update-entry";
				break;
		}
		
		//get different messages if a group is involved
		if(titleVar != null)
		{
			if(group != null)
			{
				title = LanguageUtil.get(fLocale, titleVar + "-in");
			}
			else
			{
				title = LanguageUtil.get(fLocale, titleVar);
			}
		}
		
		//determine verb
		if(BlogsActivityKeys.ADD_COMMENT == type)
		{
			verb = "add";
		}
		else if(BlogsActivityKeys.ADD_ENTRY == type)
		{
			verb = "post";
		}
		else if(BlogsActivityKeys.UPDATE_ENTRY == type)
		{
			verb = "update";
		}
		
		//create activity objects
		ActivityObjectContainer object = null;
		
		if(BlogsActivityKeys.ADD_COMMENT == type
				|| SocialActivityConstants.TYPE_ADD_COMMENT == type)
		{
			//comment
			try
			{
				content = activity.getExtraDataValue("title");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			//TODO: aqcuire ID
			object = new ActivityObjectContainer("liferay-blog-comment", null,
					"comment", null);
			object.setContent("title: " + content);
			container.setObject(object);
			
			content = "comment title: " + content;
			
			//post itself
			container.setTarget(getBlogEntryObject(blogEntry));
		}
		else if(SocialActivityConstants.TYPE_ADD_VOTE == type
				|| SocialActivityConstants.TYPE_ADD_ATTACHMENT == type)
		{
			container.setTarget(getBlogEntryObject(blogEntry));
		}
		else
		{
			object = getBlogEntryObject(blogEntry);
			container.setObject(object);
		}
		
		if(verb != null)
		{
			container.setVerb(verb);
		}
		
		if(title != null)
		{
			container.setTitle(title);
		}
		if(content != null)
		{
			container.setContent(content);
		}
		
		//attach the blogs part as generator
		container.setGenerator(fGenerator);
	}

	private ActivityObjectContainer getBlogEntryObject(BlogsEntry blogEntry)
	{
		String url = fServiceUrl + "/blogs/find_entry?entryId=" + blogEntry.getEntryId();
		
		ActivityObjectContainer object = new ActivityObjectContainer("liferay-blog-entry",
				Long.toString(blogEntry.getEntryId()), blogEntry.getTitle(), url);
		
		return object;
	}
}
