package de.hofuniversity.iisys.liferay.portlet.social.objgens;

import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalFolder;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalFolderServiceUtil;

import de.hofuniversity.iisys.liferay.portlet.social.model.ActivityObjectContainer;

public class JournalObjectGenerator implements IObjectGenerator
{
	private final String fServiceUrl;
	
	public JournalObjectGenerator(String url)
	{
		fServiceUrl = url;
	}

	@Override
	public ActivityObjectContainer generate(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;
		
		if("com.liferay.portlet.journal.model.JournalArticle".equals(className))
		{
			container = journalArticle(className, primaryKey);
		}
		else if("com.liferay.portlet.journal.model.JournalFolder".equals(className))
		{
			container = journalFolder(className, primaryKey);
		}
		
		return container;
	}

	private ActivityObjectContainer journalArticle(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;

		JournalArticle article = null;
		String name = null;
		
		try
		{
			article = JournalArticleLocalServiceUtil.getLatestArticle(primaryKey);
			name = article.getTitle();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		String articleUrl = fServiceUrl + "/journal/find_article?articleId="
				+ primaryKey;
		
		container = new ActivityObjectContainer("liferay-journal-entry",
			Long.toString(primaryKey), name, articleUrl);
		
		return container;
	}

	private ActivityObjectContainer journalFolder(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;
		
		JournalFolder folder = null;
		String name = null;
		
		try
		{
			folder = JournalFolderServiceUtil.getFolder(primaryKey);
			name = folder.getName();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		String folderUrl = fServiceUrl + "/journal/find_folder?folderId="
				+ primaryKey;
		
		container = new ActivityObjectContainer("liferay-journal-folder",
			Long.toString(primaryKey), name, folderUrl);
		
		return container;
	}
}
