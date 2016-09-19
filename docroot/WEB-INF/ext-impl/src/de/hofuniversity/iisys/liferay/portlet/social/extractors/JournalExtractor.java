package de.hofuniversity.iisys.liferay.portlet.social.extractors;

import java.util.Locale;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalFolder;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalFolderServiceUtil;
import com.liferay.portlet.journal.social.JournalActivityKeys;
import com.liferay.portlet.social.model.SocialActivity;
import com.liferay.portlet.social.model.SocialActivityConstants;

import de.hofuniversity.iisys.liferay.portlet.social.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.portlet.social.model.SocialActivityContainer;

public class JournalExtractor implements IActivityExtractor
{
	private final Locale fLocale;
	private final String fServiceUrl;
	
	private final ActivityObjectContainer fGenerator;

	public JournalExtractor(Locale locale, String url)
	{
		fLocale = locale;
		fServiceUrl = url;
		
		fGenerator = new ActivityObjectContainer("application", "liferay-journal",
				"Liferay Journal", null);
	}

	@Override
	public void extract(SocialActivity activity,
			SocialActivityContainer container)
	{
		String className = activity.getClassName();
		
		if("com.liferay.portlet.journal.model.JournalArticle".equals(className))
		{
			journalArticle(activity, container);
		}
		else if("com.liferay.portlet.journal.model.JournalFolder".equals(className))
		{
			journalFolder(activity, container);
		}
	}
	
	private void journalArticle(SocialActivity activity, SocialActivityContainer container)
	{
		JournalArticle article = null;
		String name = null;
		
		try
		{
			article = JournalArticleLocalServiceUtil.getLatestArticle(activity.getClassPK());
			name = article.getTitle();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		//get possibly linked group object
		Group group = null;
		try
		{
			group = GroupServiceUtil.getGroup(activity.getGroupId());
		}
		catch(Exception e) {}
		
		final int type = activity.getType();
		String verb = null;
		String titleVar = null;
		String title = null;
		
		switch(type)
		{
			case JournalActivityKeys.ADD_ARTICLE:
				verb = "add";
				titleVar = "activity-journal-article-add-web-content";
				break;
				
			case JournalActivityKeys.UPDATE_ARTICLE:
				verb = "update";
				titleVar = "activity-journal-article-update-web-content";
				break;
				
			case SocialActivityConstants.TYPE_MOVE_TO_TRASH:
				titleVar = "activity-journal-article-move-to-trash";
				break;
				
			case SocialActivityConstants.TYPE_RESTORE_FROM_TRASH:
				titleVar = "activity-journal-article-restore-from-trash";
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
		
		if(verb != null)
		{
			container.setVerb(verb);
		}
		
		if(title != null)
		{
			container.setTitle(title);
		}
		
		//create activity objects
		String articleUrl = fServiceUrl + "/journal/find_article?articleId="
				+ activity.getClassPK();
		
		if(name == null)
		{
			name = "article";
		}
		
		ActivityObjectContainer object = new ActivityObjectContainer("liferay-journal-entry",
				Long.toString(activity.getClassPK()), name, articleUrl);
		
		container.setObject(object);
		
		
		//folder
		JournalFolder folder = null;
		String folderName = null;
		
		try
		{
			folder = article.getFolder();
			if(folder != null)
			{
				folderName = folder.getName();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		String folderUrl = fServiceUrl + "/journal/find_folder?folderId="
				+ article.getFolderId();
		
		ActivityObjectContainer target = new ActivityObjectContainer("liferay-journal-folder",
				Long.toString(article.getFolderId()), folderName, folderUrl);
		
		container.setTarget(target);
		
		//attach the journal part as generator
		container.setGenerator(fGenerator);
	}
	
	private void journalFolder(SocialActivity activity, SocialActivityContainer container)
	{
		JournalFolder folder = null;
		String name = null;
		
		try
		{
			folder = JournalFolderServiceUtil.getFolder(activity.getClassPK());
			name = folder.getName();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		//get possibly linked group object
		Group group = null;
		try
		{
			group = GroupServiceUtil.getGroup(activity.getGroupId());
		}
		catch(Exception e) {}
		
		final int type = activity.getType();
		String titleVar = null;
		String title = null;
		
		if(SocialActivityConstants.TYPE_MOVE_TO_TRASH == type)
		{
			titleVar = "activity-journal-folder-move-to-trash";
		}
		else if(SocialActivityConstants.TYPE_RESTORE_FROM_TRASH == type)
		{
			titleVar = "activity-journal-folder-restore-from-trash";
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
		
		if(title != null)
		{
			container.setTitle(title);
		}
		
		//create activity object
		String folderUrl = fServiceUrl + "/journal/find_folder?folderId="
				+ activity.getClassPK();
		
		if(name == null)
		{
			name = "folder";
		}
		
		ActivityObjectContainer object = new ActivityObjectContainer("liferay-journal-folder",
				Long.toString(activity.getClassPK()), name, folderUrl);
		
		container.setObject(object);
		
		//attach the journal part as generator
		container.setGenerator(fGenerator);
	}
}
