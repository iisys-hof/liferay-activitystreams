package de.hofuniversity.iisys.liferay.portlet.social.extractors;

import java.util.Locale;
import java.util.Map;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupServiceUtil;
import com.liferay.portlet.bookmarks.model.BookmarksEntry;
import com.liferay.portlet.bookmarks.model.BookmarksFolder;
import com.liferay.portlet.bookmarks.service.BookmarksEntryServiceUtil;
import com.liferay.portlet.bookmarks.service.BookmarksFolderLocalServiceUtil;
import com.liferay.portlet.bookmarks.social.BookmarksActivityKeys;
import com.liferay.portlet.social.model.SocialActivity;
import com.liferay.portlet.social.model.SocialActivityConstants;

import de.hofuniversity.iisys.liferay.portlet.social.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.portlet.social.model.SocialActivityContainer;

public class BookmarksExtractor implements IActivityExtractor
{
	private final Locale fLocale;
	private final String fServiceUrl;
	
	private final String fFolderName;
	
	private final ActivityObjectContainer fGenerator;

	public BookmarksExtractor(Map<String, String> properties, Locale locale, String url)
	{
		fLocale = locale;
		fServiceUrl = url;
		
		fFolderName = properties.get("folder_display_name");
		
		fGenerator = new ActivityObjectContainer("application", "liferay-bookmarks",
				"Liferay Bookmarks", null);
	}

	@Override
	public void extract(SocialActivity activity,
			SocialActivityContainer container)
	{
		String className = activity.getClassName();

		//get possibly linked group object
		Group group = null;
		try
		{
			group = GroupServiceUtil.getGroup(activity.getGroupId());
		}
		catch(Exception e) {}
		
		if("com.liferay.portlet.bookmarks.model.BookmarksEntry".equals(className))
		{
			bookmarksEntry(activity, container, group);
		}
		else if("com.liferay.portlet.bookmarks.model.BookmarksFolder".equals(className))
		{
			bookmarksFolder(activity, container, group);
		}
	}
	
	private void bookmarksEntry(SocialActivity activity, SocialActivityContainer container,
			Group group)
	{
		//determine message
		final int type = activity.getType();
		String verb = null;
		String titleVar = null;
		String title = null;
		
		switch(type)
		{
			case BookmarksActivityKeys.ADD_ENTRY:
			{
				titleVar = "activity-bookmarks-entry-add-entry";
				verb = "add";
			}
			case BookmarksActivityKeys.UPDATE_ENTRY:
			{
				titleVar = "activity-bookmarks-entry-update-entry";
				verb = "update";
			}
			case SocialActivityConstants.TYPE_MOVE_TO_TRASH:
			{
				titleVar = "activity-bookmarks-folder-move-to-trash";
			}
			case SocialActivityConstants.TYPE_RESTORE_FROM_TRASH:
			{
				titleVar = "activity-bookmarks-folder-restore-from-trash";
			}
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
		
		if(verb != null)
		{
			container.setVerb(verb);
		}
		
		if(title != null)
		{
			container.setTitle(title);
		}
		
		//generate activity object from bookmarks entry
		String content = null;
		try
		{
			BookmarksEntry entry = BookmarksEntryServiceUtil.getEntry(activity.getClassPK());
			BookmarksFolder folder = entry.getFolder();
			
			content = "new bookmark: " + entry.getName() + " (" + entry.getUrl()
					+ ") in folder " + folder.getName();
			
			
			String folderUrl = fServiceUrl + "/bookmarks/find_folder?folderId="
					+ folder.getPrimaryKey();
			
			ActivityObjectContainer folderObject = new ActivityObjectContainer(
					"liferay-bookmark-folder", Long.toString(folder.getPrimaryKey()),
					fFolderName, folderUrl);
			
			container.setTarget(folderObject);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		String url = fServiceUrl + "/bookmarks/find_entry?entryId=" + activity.getClassPK();
		
		if(content == null)
		{
			try
			{
				content = activity.getExtraDataValue("title");
			}
			catch(Exception e) {}
		}
		
		ActivityObjectContainer entryObject = new ActivityObjectContainer("liferay-bookmark-entry",
				Long.toString(activity.getClassPK()), "bookmark", url);
		if(content != null)
		{
			entryObject.setContent(content);
		}
		container.setObject(entryObject);
		
		//attach the bookmarks part as generator
		container.setGenerator(fGenerator);
	}
	
	private void bookmarksFolder(SocialActivity activity, SocialActivityContainer container,
			Group group)
	{
		final int type = activity.getType();
		String verb = null;
		String titleVar = null;
		String title = null;
		
		if(BookmarksActivityKeys.ADD_ENTRY == type)
		{
			verb = "add";
		}
		else if(BookmarksActivityKeys.UPDATE_ENTRY == type)
		{
			verb = "update";
		}
		
		if(SocialActivityConstants.TYPE_MOVE_TO_TRASH == type)
		{
			titleVar = "activity-bookmarks-folder-move-to-trash";
		}
		else if(SocialActivityConstants.TYPE_RESTORE_FROM_TRASH == type)
		{
			titleVar = "activity-bookmarks-folder-restore-from-trash";
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
		
		if(verb != null)
		{
			container.setVerb(verb);
		}
		
		if(title != null)
		{
			container.setTitle(title);
		}
		
		//generate activity object from bookmarks folder
		BookmarksFolder folder = null;
		String content = null;
		
		try
		{
			folder = BookmarksFolderLocalServiceUtil.getFolder(activity.getClassPK());
			content = folder.getName();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		String url = fServiceUrl + "/bookmarks/find_folder?folderId=" + activity.getClassPK();
		
		if(content == null)
		{
			try
			{
				content = activity.getExtraDataValue("title");
			}
			catch(Exception e) {}
		}
		
		ActivityObjectContainer folderObject = new ActivityObjectContainer("liferay-bookmark-folder",
				Long.toString(activity.getClassPK()), "bookmarks folder", url);
		if(content != null)
		{
			content = "title: " + content;
			
			folderObject.setContent(content);
		}
		container.setObject(folderObject);
		
		//attach the bookmarks part as generator
		container.setGenerator(fGenerator);
	}
}
