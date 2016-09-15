package de.hofuniversity.iisys.liferay.activitystreams.objgens;

import java.util.Map;

import com.liferay.message.boards.kernel.model.MBDiscussion;
import com.liferay.message.boards.kernel.model.MBMessage;
import com.liferay.message.boards.kernel.model.MBThread;
import com.liferay.message.boards.kernel.service.MBDiscussionLocalServiceUtil;
import com.liferay.message.boards.kernel.service.MBMessageLocalServiceUtil;
import com.liferay.message.boards.kernel.service.MBThreadLocalServiceUtil;

import de.hofuniversity.iisys.liferay.activitystreams.extractors.MessageBoardExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.model.ActivityObjectContainer;


public class MessageBoardObjectGenerator implements IObjectGenerator
{
	private final Map<String, String> fProperties;
	private final String fServiceUrl;
	private final String fMessageName;
	
	public MessageBoardObjectGenerator(Map<String, String> properties, String url)
	{
		fProperties = properties;
		fServiceUrl = url;
		fMessageName = fProperties.get("message_display_name");
	}
	
	@Override
	public ActivityObjectContainer generate(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;
		
		if(MessageBoardExtractor.MB_THREAD_CLASS.equals(className))
		{
			container = messageBoardThread(className, primaryKey);
		}
		else if(MessageBoardExtractor.MB_MESSAGE_CLASS.equals(className))
		{
			container = messageBoardEntry(className, primaryKey);
		}
		else if(MessageBoardExtractor.MB_DISCUSSION_CLASS.equals(className))
		{
			container = messageBoardDiscussion(className, primaryKey);
		}
		
		return container;
	}
	
	private ActivityObjectContainer messageBoardEntry(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;
		
		MBMessage message = null;
		String name = null;
		
		try
		{
			message = MBMessageLocalServiceUtil.fetchMBMessage(primaryKey);
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
		
		String messLink = fServiceUrl + "/message_boards/find_message?messageId="
				+ primaryKey;
		
		container = new ActivityObjectContainer("liferay-message-board-entry",
			Long.toString(primaryKey), name, messLink);
		
		return container;
	}
	
	private ActivityObjectContainer messageBoardThread(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;
		
		MBThread thread = null;
		MBMessage message = null;
		String name = null;
		
		try
		{
			thread = MBThreadLocalServiceUtil.fetchMBThread(primaryKey);
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
		
		String messLink = null;
		if(thread != null)
		{
			messLink = fServiceUrl + "/message_boards/find_message?messageId="
					+ thread.getRootMessageId();
		}
		
		container = new ActivityObjectContainer("liferay-message-board-thread",
			Long.toString(primaryKey), name, messLink);
		
		return container;
	}
	
	private ActivityObjectContainer messageBoardDiscussion(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;
		
		MBDiscussion discussion = null;
		MBThread thread = null;
		MBMessage message = null;
		String name = null;
		
		try
		{
			discussion = MBDiscussionLocalServiceUtil.fetchMBDiscussion(primaryKey);
			thread =  MBThreadLocalServiceUtil.fetchMBThread(discussion.getThreadId());
			message = MBMessageLocalServiceUtil.fetchMBMessage(thread.getRootMessageId());
			name = message.getSubject();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		if(name == null)
		{
			name = fMessageName;
		}
		
		String messLink = null;
		if(thread != null)
		{
			messLink = fServiceUrl + "/message_boards/find_message?messageId="
					+ thread.getRootMessageId();
		}
		
		container = new ActivityObjectContainer("liferay-message-board-discussion",
			Long.toString(primaryKey), name, messLink);
		
		return container;
	}
}
