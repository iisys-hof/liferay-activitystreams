package de.hofuniversity.iisys.liferay.activitystreams.extractors;

import java.util.Locale;
import java.util.Map;

import com.liferay.message.boards.kernel.model.MBDiscussion;
import com.liferay.message.boards.kernel.model.MBMessage;
import com.liferay.message.boards.kernel.model.MBThread;
import com.liferay.message.boards.kernel.service.MBDiscussionLocalServiceUtil;
import com.liferay.message.boards.kernel.service.MBMessageLocalServiceUtil;
import com.liferay.message.boards.kernel.service.MBThreadLocalServiceUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupServiceUtil;
import com.liferay.social.kernel.model.SocialActivity;
import com.liferay.social.kernel.model.SocialActivityConstants;

import de.hofuniversity.iisys.liferay.activitystreams.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.activitystreams.model.SocialActivityContainer;

public class MessageBoardExtractor implements IActivityExtractor
{
	public static final String MB_THREAD_CLASS =
			"com.liferay.message.boards.kernel.model.MBThread";
	public static final String MB_MESSAGE_CLASS =
			"com.liferay.message.boards.kernel.model.MBMessage";
	public static final String MB_DISCUSSION_CLASS =
			"com.liferay.message.boards.kernel.model.MBDiscussion";

	private final Map<String, String> fProperties;
	
	private final Locale fLocale;
	private final String fServiceUrl;
	
	private final ActivityObjectContainer fGenerator;
	
	private final String fMessageName;

	public MessageBoardExtractor(Map<String, String> properties, Locale locale,
			String url)
	{
		fProperties = properties;
		fLocale = locale;
		fServiceUrl = url;
		
		fGenerator = new ActivityObjectContainer("application", "liferay-messageboards",
				"Liferay Message Boards", null);
		fMessageName = fProperties.get("message_display_name");
	}

	@Override
	public void extract(SocialActivity activity, SocialActivityContainer container)
	{
		String className = activity.getClassName();
		
		if(MB_THREAD_CLASS.equals(className))
		{
			messageBoardThread(activity, container);
		}
		else if(MB_MESSAGE_CLASS.equals(className))
		{
			messageBoardEntry(activity, container);
		}
		else if(MB_DISCUSSION_CLASS.equals(className))
		{
			messageBoardDiscussion(activity, container);
		}
	}
	
	private void messageBoardEntry(SocialActivity activity, SocialActivityContainer container)
	{
		MBMessage message = null;
		String name = null;
		
		try
		{
			message = MBMessageLocalServiceUtil.fetchMBMessage(activity.getClassPK());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		if(message != null)
		{
			name = message.getSubject();
		}
		else
		{
			name = fMessageName;
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
		String verb = null;
		
		//TODO: replace once available again
		//if(MBActivityKeys.ADD_MESSAGE == type)
		if(1 == type)
		{
			if(activity.getReceiverUserId() == 0)
			{
				titleVar = "activity-message-boards-message-add-message";
			}
			else
			{
				titleVar = "activity-message-boards-message-reply-message";
			}
			
			verb = "add";
		}
		//else if(MBActivityKeys.REPLY_MESSAGE == type)
		else if(2 == type)
		{
			titleVar = "activity-message-boards-message-reply-message";
			verb = "add";
		}
		
		//different title if a group is involved
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

		if(message != null)
		{
			String catLink = fServiceUrl + "/message_boards/find_category?mbCategoryId="
				+ message.getCategoryId();
			container.setContent("category link: " + catLink);
		}
		
		//create activity objects
		String messLink = fServiceUrl + "/message_boards/find_message?messageId="
				+ activity.getClassPK();
		
		ActivityObjectContainer object = new ActivityObjectContainer("liferay-message-board-entry",
				Long.toString(activity.getClassPK()), name, messLink);
		container.setObject(object);
		
		//add root message as target if it's a reply
		if(message != null
				&& message.isReply())
		{
			long id = message.getRootMessageId();
			try
			{
				message = MBMessageLocalServiceUtil.fetchMBMessage(id);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			if(message != null)
			{
				name = message.getSubject();
			}
			else
			{
				name = fMessageName;
			}
			
			messLink = fServiceUrl + "/message_boards/find_message?messageId=" + id;
			
			ActivityObjectContainer root = new ActivityObjectContainer(
				"liferay-message-board-entry", Long.toString(id), name, messLink);
			container.setTarget(root);
		}
		
		//attach the message boards part as generator
		container.setGenerator(fGenerator);
	}
	
	private void messageBoardThread(SocialActivity activity, SocialActivityContainer container)
	{
		MBThread thread = null;
		MBMessage message = null;
		String name = null;
		
		try
		{
			thread = MBThreadLocalServiceUtil.fetchMBThread(activity.getClassPK());
			message = MBMessageLocalServiceUtil.fetchMBMessage(thread.getRootMessageId());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		if(message != null)
		{
			name = message.getSubject();
		}
		else
		{
			name = fMessageName;
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
			titleVar = "activity-message-boards-thread-move-to-trash";
		}
		else if(SocialActivityConstants.TYPE_RESTORE_FROM_TRASH == type)
		{
			titleVar = "activity-message-boards-thread-restore-from-trash";
		}
		
		//different title if a group is involved
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

		if(thread != null)
		{
			String catLink = fServiceUrl + "/message_boards/find_category?mbCategoryId="
					+ thread.getCategoryId();
			container.setContent("category link: " + catLink);
		}
		
		//create activity objects
		String messLink = null;
		if(thread != null)
		{
			messLink = fServiceUrl + "/message_boards/find_message?messageId="
					+ thread.getRootMessageId();
		}
		
		ActivityObjectContainer object = new ActivityObjectContainer("liferay-message-board-thread",
				Long.toString(activity.getClassPK()), name, messLink);
		container.setObject(object);
		
		//attach the message boards part as generator
		container.setGenerator(fGenerator);
	}
	
	private void messageBoardDiscussion(SocialActivity activity, SocialActivityContainer container)
	{
		MBDiscussion discussion = null;
		MBThread thread = null;
		MBMessage message = null;
		String name = null;
		
		try
		{
//			discussion = MBDiscussionLocalServiceUtil.fetchMBDiscussion(activity.getClassPK());
			discussion = MBDiscussionLocalServiceUtil.fetchDiscussion(
					MB_DISCUSSION_CLASS, activity.getClassPK());
			
			thread = MBThreadLocalServiceUtil.fetchMBThread(discussion.getThreadId());
			message = MBMessageLocalServiceUtil.fetchMBMessage(thread.getRootMessageId());
			name = message.getSubject();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		if(thread != null)
		{
			String catLink = fServiceUrl + "/message_boards/find_category?mbCategoryId="
					+ thread.getCategoryId();
			container.setContent("category link: " + catLink);
		}
		
		if(name == null)
		{
			name = fMessageName;
		}
		
		//create activity objects
		String messLink = null;
		if(thread != null)
		{
			messLink = fServiceUrl + "/message_boards/find_message?messageId="
					+ thread.getRootMessageId();
		}
		
		ActivityObjectContainer object = new ActivityObjectContainer(
				"liferay-message-board-discussion", Long.toString(activity.getClassPK()),
				name, messLink);
		container.setObject(object);
				
		//attach the message boards part as generator
		container.setGenerator(fGenerator);
	}
}
