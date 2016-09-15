package de.hofuniversity.iisys.liferay.activitystreams.extractors;

import java.util.Locale;
import java.util.Map;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.GroupServiceUtil;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.social.kernel.model.SocialActivity;
import com.liferay.social.kernel.model.SocialActivityConstants;
import com.liferay.wiki.model.WikiNode;
import com.liferay.wiki.model.WikiPage;
import com.liferay.wiki.model.WikiPageResource;
import com.liferay.wiki.service.WikiNodeLocalServiceUtil;
import com.liferay.wiki.service.WikiPageLocalServiceUtil;
import com.liferay.wiki.service.WikiPageResourceLocalServiceUtil;
import com.liferay.wiki.social.WikiActivityKeys;

import de.hofuniversity.iisys.liferay.activitystreams.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.activitystreams.model.SocialActivityContainer;

public class WikiExtractor implements IActivityExtractor
{
	public static final String WIKI_PAGE_CLASS =
			"com.liferay.wiki.model.WikiPage";
	public static final String WIKI_NODE_CLASS =
			"com.liferay.wiki.model.WikiNode";
	
	private final Map<String, String> fProperties;
	
	private final Locale fLocale;
	private final String fServiceUrl;
	
	private final ActivityObjectContainer fGenerator;
	
	private final String fCommentName;

	private final boolean fAddObjectTypes;

	public WikiExtractor(Map<String, String> properties, Locale locale,
			String url)
	{
		fProperties = properties;
		fLocale = locale;
		fServiceUrl = url;
		
		fGenerator = new ActivityObjectContainer("application", "liferay-wikis",
				"Liferay Wikis", null);
		fCommentName = fProperties.get("comment_display_name");
		fAddObjectTypes = Boolean.parseBoolean(
				fProperties.get("activityobjects.follow.addtypes"));
	}

	@Override
	public void extract(SocialActivity activity,
			SocialActivityContainer container)
	{
		String className = activity.getClassName();
		
		if(WIKI_PAGE_CLASS.equals(className))
		{
			extractPage(activity, container);
		}
		else if(WIKI_NODE_CLASS.equals(className))
		{
			extractNode(activity, container);
		}
	}
	
	private void extractPage(SocialActivity activity,
			SocialActivityContainer container)
	{
		WikiPageResource pageResource = null;
		WikiPage page = null;
		
		double version = -1.;
		String fileName = null;
		
		try
		{
			pageResource = WikiPageResourceLocalServiceUtil.fetchWikiPageResource(
					activity.getClassPK());
			
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
		
		try
		{
			version = GetterUtil.getDouble(activity.getExtraDataValue("version"));
		}
		catch(Exception e) {}
		
		try
		{
			fileName = activity.getExtraDataValue("fileEntryTitle");
		}
		catch(Exception e) {}
		
		String name = pageResource.getTitle();

		//get possibly linked group object
		Group group = null;
		try
		{
			group = GroupServiceUtil.getGroup(activity.getGroupId());
		}
		catch(Exception e) {}
		
		final int type = activity.getType();
		boolean attachment = false;
		boolean comment = false;
		boolean vote = false;
		String titleVar = null;
		String title = null;
		String verb = null;
		
		switch(type)
		{
			case WikiActivityKeys.ADD_COMMENT:
			case SocialActivityConstants.TYPE_ADD_COMMENT:
				verb = "add";
				titleVar = "activity-wiki-page-add-comment";
				comment = true;
				break;
				
			case WikiActivityKeys.ADD_PAGE:
				verb = "create";
				titleVar = "activity-wiki-page-add-page";
				break;
				
			case SocialActivityConstants.TYPE_ADD_ATTACHMENT:
				titleVar = "activity-wiki-page-add-attachment";
				attachment = true;
				break;
				
			case SocialActivityConstants.TYPE_MOVE_ATTACHMENT_TO_TRASH:
				titleVar = "activity-wiki-page-remove-attachment";
				attachment = true;
				break;
				
			case SocialActivityConstants.TYPE_RESTORE_ATTACHMENT_FROM_TRASH:
				titleVar = "activity-wiki-page-restore-attachment";
				attachment = true;
				break;
				
			case SocialActivityConstants.TYPE_MOVE_TO_TRASH:
				titleVar = "activity-wiki-page-move-to-trash";
				break;
				
			case SocialActivityConstants.TYPE_RESTORE_FROM_TRASH:
				titleVar = "activity-wiki-page-restore-from-trash";
				break;
				
			case WikiActivityKeys.UPDATE_PAGE:
				verb = "update";
				titleVar = "activity-wiki-page-update-page";
				break;
				
			case SocialActivityConstants.TYPE_ADD_VOTE:
				verb = "add";
				vote = true;
				break;

			//TODO: applicable?
			case SocialActivityConstants.TYPE_SUBSCRIBE:
				verb = "follow";
				break;
		}
		
		//adds an object type to the name of the object
		if("follow".equals(verb)
			&& fAddObjectTypes)
		{
			name += " (Wiki Page)";
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
		
		//create the activity objects
		String content = "version: " + version;

		//page
		String pageLink = fServiceUrl + "/wiki/find_page?pageResourcePrimKey="
				+ activity.getClassPK();
		
		ActivityObjectContainer pageObject = new ActivityObjectContainer("liferay-wiki-page",
				Long.toString(page.getPrimaryKey()), name, pageLink);
		pageObject.setContent(content);
		
		//comment
		ActivityObjectContainer commentObject = null;
		if(comment)
		{
			//TODO: ID?
			//TODO: proper title?
			//TODO: link?
			commentObject = new ActivityObjectContainer("liferay-wiki-comment", null,
					fCommentName, null);
		}
		
		if(attachment)
		{
			container.setTarget(pageObject);
			
			//attachment
			FileEntry att = null;
			String id = null;
			String description = null;
			
			try
			{
				att = page.getAttachmentsFileEntries().get(
						page.getAttachmentsFileEntriesCount() - 1);
				id = Long.toString(att.getPrimaryKey());
				description = att.getDescription();
				
				if(fileName == null)
				{
					fileName = att.getTitle();
				}
			}
			catch(Exception e) {}
			
			if(fileName == null)
			{
				fileName = "attachment";
			}
			
			ActivityObjectContainer attObject = new ActivityObjectContainer(
					"liferay-wiki-attachment", id, fileName, null);
			
			if(description != null)
			{
				attObject.setContent(description);
			}
			
			container.setObject(attObject);
		}
		else if(comment)
		{
			container.setObject(commentObject);
			container.setTarget(pageObject);
		}
		else if(vote)
		{
			container.setTarget(pageObject);
		}
		else
		{
			container.setObject(pageObject);
		}
		
		//attach the wikis part as generator
		container.setGenerator(fGenerator);
	}

	private void extractNode(SocialActivity activity,
			SocialActivityContainer container)
	{
		final int type = activity.getType();

		WikiNode node = null;
		try
		{
			node = WikiNodeLocalServiceUtil.fetchWikiNode(activity.getClassPK());
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

		String titleVar = null;
		String title = null;
		String verb = null;
		
		//only follow and unfollow?
		switch(type)
		{
			case SocialActivityConstants.TYPE_SUBSCRIBE:
				verb = "follow";
				titleVar = "titles.followed_wiki_node";
				break;
				
			case SocialActivityConstants.TYPE_UNSUBSCRIBE:
				verb = "stop-following";
				titleVar = "titles.unfollowed_wiki_node";
				break;
		}
		
		//get different messages if a group is involved
		if(titleVar != null)
		{
			//TODO: incorporate variables into titles themselves
			if(group != null)
			{
				title = fProperties.get(titleVar);
			}
			else
			{
				title = fProperties.get(titleVar + "-in");
			}
		}
		
		//construct object if possible
		if(node != null)
		{
			try
			{
				String name = node.getName();
				
				//adds an object type to the name of the object
				if("follow".equals(verb)
					&& fAddObjectTypes)
				{
					name += " (Wiki Node)";
				}
				
				//TODO: will this work?
//				long plid = PortalUtil.getPlidFromPortletId(node.getGroupId(), PortletKeys.WIKI);
				//TODO: not numerical anymore?
//				long plid = PortalUtil.getPlidFromPortletId(node.getGroupId(), "36");
				long plid = PortalUtil.getPlidFromPortletId(node.getGroupId(),
						"com_liferay_wiki_web_portlet_WikiPortlet");
				Layout layout = LayoutLocalServiceUtil.getLayout(plid);
				String nodeLink = layout.getFriendlyURL();
				
				ActivityObjectContainer nodeObject = new ActivityObjectContainer(
					"liferay-wiki-node", Long.toString(node.getPrimaryKey()),
					name, nodeLink);
				
				container.setObject(nodeObject);
			}
			catch(Exception e)
			{
				e.printStackTrace();
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
		
		//attach the wikis part as generator
		container.setGenerator(fGenerator);
	}
}
