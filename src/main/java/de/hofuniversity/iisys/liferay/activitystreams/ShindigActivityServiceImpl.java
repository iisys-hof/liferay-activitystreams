package de.hofuniversity.iisys.liferay.activitystreams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupServiceUtil;
import com.liferay.portal.kernel.service.ServiceWrapper;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.ratings.kernel.model.RatingsEntry;
import com.liferay.ratings.kernel.service.RatingsEntryLocalServiceUtil;
import com.liferay.social.kernel.model.SocialActivity;
import com.liferay.social.kernel.model.SocialActivityConstants;
import com.liferay.social.kernel.service.SocialActivityLocalServiceWrapper;

import de.hofuniversity.iisys.liferay.activitystreams.extractors.BlogsExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.BookmarksExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.CalendarExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.DocLibExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.GenericExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.IActivityExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.JournalExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.MessageBoardExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.MicroblogsExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.PageExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.PortalExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.extractors.WikiExtractor;
import de.hofuniversity.iisys.liferay.activitystreams.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.activitystreams.model.SocialActivityContainer;
import de.hofuniversity.iisys.liferay.activitystreams.objgens.BlogsObjectGenerator;
import de.hofuniversity.iisys.liferay.activitystreams.objgens.DocLibObjectGenerator;
import de.hofuniversity.iisys.liferay.activitystreams.objgens.IObjectGenerator;
import de.hofuniversity.iisys.liferay.activitystreams.objgens.JournalObjectGenerator;
import de.hofuniversity.iisys.liferay.activitystreams.objgens.MessageBoardObjectGenerator;
import de.hofuniversity.iisys.liferay.activitystreams.objgens.WikiObjectGenerator;

@Component(
	immediate = true,
	property = {
	},
	service = ServiceWrapper.class
)
public class ShindigActivityServiceImpl extends SocialActivityLocalServiceWrapper
	implements Runnable
{
	private static final long VOTE_COOLDOWN = 10000;
	private static final long PROCESSING_DELAY = 1000;
	
	private static final String BLOGS_ENTRY_CLASS =
			"com.liferay.blogs.kernel.model.BlogsEntry";
	
	private static final String CALENDAR_BOOKING_CLASS =
			"com.liferay.calendar.model.CalendarBooking";
	
	private static final String PORTAL_ACT_INT_CLASS =
			"com.liferay.portlet.social.util.PortalActivityInterpreter";
	
	//pages
	private static final String LAYOUT_CLASS =
			"com.liferay.portal.kernel.model.Layout";
	
	private static final String MICROBLOG_ENTRY_CLASS =
			"com.liferay.microblogs.model.MicroblogsEntry";
	
	private static final String LIFERAY_URL = "liferay_url";
	private static final String SHINDIG_URL = "shindig_url";
	private static final String LOCALE_PROP = "locale";
	private static final String ACTIVITY_PREFIX = "activity_prefix";
	private static final String DEF_VERB = "default_verb";
	private static final String BLACK_TYPES = "blacklist_types";
	private static final String BLACK_CLASSES = "blacklist_classes";
	private static final String BLACK_COMBS = "blacklist_combinations";
	
	private static final String FILTER_UNNAMED = "unnamed.filter";
	private static final String FILTER_WIKI_FOLLOWS = "comments.follow.wiki.filter";
	private static final String FILTER_MB_FOLLOWS = "comments.follow.messageboard.filter";
	
	private static final String ACTSTR_LOGGING = "activitystreams_logging";
	private static final String LIFERAY_LOGGING = "liferay_activities_logging";
    
    private static final String ACT_STREAM_URL_FRAG =
        "social/rest/activitystreams/";

    private static final String PROPERTIES = "shindig-activitystreams";
    private static final String LANG_PROPS = "shindig-activitystreams-lang";
    private final Map<String, String> fProperties;
	
	private final String fActivityPrefix;
	private final String fServiceUrl, fShindigUrl;
	
	private final Locale fLocale;
	
	private final ActivityObjectContainer fProvider;
	
	private final Map<Integer, String> fVerbs;
	private final String fDefaultVerb;
	
	private final Map<Integer, String> fTitles;
	
	private final Map<String, IActivityExtractor> fExtractors;
	private final Map<String, IObjectGenerator> fObjectGens;
	private final IActivityExtractor fGenericExtractor;
	
	private final Map<String, List<Integer>> fBlacklistCombs;
	private final List<Integer> fBlacklistTypes;
	private final List<String> fBlacklistClasses;
	
	private final boolean fFilterUnnamed;
	private final boolean fFilterWikiFollows, fFilterMBFollows;
	
	private final List<SocialActivity> fQueue;
	
	private final Map<String, Long> fVoteCooldowns;
	
	private final String fCommentName;
	private final String fVoteName;
	
	private final Logger fLogger;
	
	private final boolean fLogging, fLiferayLogging;
	private final ActivityStreamLogger fStreamLogger;
	private final LoggingLiferayActivityServiceImpl fLiferayLogger;
	
	private boolean fRunning;
	    
    public ShindigActivityServiceImpl() throws Exception
    {
    	super(null);
		
		fBlacklistCombs = new HashMap<String, List<Integer>>();
		fBlacklistTypes = new ArrayList<Integer>();
		fBlacklistClasses = new ArrayList<String>();
		
		fQueue = new LinkedList<SocialActivity>();
		
		fVoteCooldowns = new HashMap<>();
		
		fLogger = Logger.getLogger(this.getClass().getName());
		fProperties = new HashMap<String, String>();
		loadConfig();
		
		//filter options
		fFilterUnnamed = Boolean.parseBoolean(fProperties.get(FILTER_UNNAMED));
		fFilterWikiFollows = Boolean.parseBoolean(
				fProperties.get(FILTER_WIKI_FOLLOWS));
		fFilterMBFollows = Boolean.parseBoolean(
				fProperties.get(FILTER_MB_FOLLOWS));
		
		//activity streams logging
		if(Boolean.parseBoolean(fProperties.get(ACTSTR_LOGGING)))
		{
			fLogging = true;
			fStreamLogger = new ActivityStreamLogger(fProperties);
		}
		else
		{
			fLogging = false;
			fStreamLogger = null;
		}
		
		//liferay activities logging
		if(Boolean.parseBoolean(fProperties.get(LIFERAY_LOGGING)))
		{
			fLiferayLogging = true;
			fLiferayLogger = new LoggingLiferayActivityServiceImpl(null,
					fProperties);
		}
		else
		{
			fLiferayLogging = false;
			fLiferayLogger = null;
		}
		
		fActivityPrefix = fProperties.get(ACTIVITY_PREFIX);
		fServiceUrl = fProperties.get(LIFERAY_URL);
		fShindigUrl = fProperties.get(SHINDIG_URL);
		
		fLocale = new Locale(fProperties.get(LOCALE_PROP));
		
		//generic liferay object as provider
		fProvider = new ActivityObjectContainer("application", "liferay",
				"Liferay", fServiceUrl);
		
		fDefaultVerb = fProperties.get(DEF_VERB);
		fVerbs = new HashMap<Integer, String>();
		fillVerbs();
		
		fTitles = new HashMap<Integer, String>();
		fillTitles();

		fGenericExtractor = new GenericExtractor(fLocale, fServiceUrl);
		fExtractors = new HashMap<String, IActivityExtractor>();
		registerExtractors();
		
		fObjectGens = new HashMap<String, IObjectGenerator>();
		registerObjectGens();
		
		//display names for activity objects
		fCommentName = fProperties.get("comment_display_name");
		fVoteName = fProperties.get("vote_display_name");
		
		//start processing thread
		Thread pThread = new Thread(this);
		pThread.setDaemon(true);
		pThread.start();
	}
    
    private void loadConfig()
    {
        //read properties file
        final ClassLoader loader = Thread.currentThread()
            .getContextClassLoader();
        ResourceBundle rb = ResourceBundle.getBundle(PROPERTIES,
            Locale.getDefault(), loader);
        
        String key = null;
        String value = null;
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements())
        {
            key = keys.nextElement();
            value = rb.getString(key);

            fProperties.put(key, value);
        }
        
        //read language specific file
        Locale loc = new Locale(fProperties.get(LOCALE_PROP));
        rb = ResourceBundle.getBundle(LANG_PROPS, loc, loader);
        
        keys = rb.getKeys();
        while (keys.hasMoreElements())
        {
            key = keys.nextElement();
            value = rb.getString(key);

            fProperties.put(key, value);
        }
        
        //evaluate properties
        String typeBlacklist = fProperties.get(BLACK_TYPES);
        String classesBlacklist = fProperties.get(BLACK_CLASSES);
        
        //TODO: actually implement combined blacklist
        String combBlacklist = fProperties.get(BLACK_COMBS);
        
        if(typeBlacklist != null)
        {
            String[] types = typeBlacklist.split(",");
            for(String type : types)
            {
            	try
            	{
            		if(type != null && !type.isEmpty())
            		{
                    	fBlacklistTypes.add(Integer.parseInt(type));
            		}
            	}
            	catch(Exception e)
            	{
            		fLogger.log(Level.WARNING,
            			"could not parse blacklisted type", e);
            	}
            }
        }
        
        if(classesBlacklist != null)
        {
            String[] types = classesBlacklist.split(",");
            if(types != null && types.length > 0)
            {
                fBlacklistClasses.addAll(Arrays.asList(types));
            }
        }
    }
	
	//utility methods
	private void registerExtractors()
	{
		//blogs
		IActivityExtractor extr = new BlogsExtractor(fLocale, fServiceUrl);
		fExtractors.put(BLOGS_ENTRY_CLASS, extr);
		
		//bookmarks
		extr = new BookmarksExtractor(fProperties, fLocale, fServiceUrl);
		fExtractors.put(BookmarksExtractor.BOOKMARKS_ENTRY_CLASS, extr);
		fExtractors.put(BookmarksExtractor.BOOKMARKS_FOLDER_CLASS, extr);
		
		//calendar
		extr = new CalendarExtractor(fProperties, fLocale, fServiceUrl);
		fExtractors.put(CALENDAR_BOOKING_CLASS, extr);
		
		//document library
		extr = new DocLibExtractor(fProperties, fLocale, fServiceUrl);
		fExtractors.put(DocLibExtractor.DL_FILE_ENTRY_CLASS, extr);
		fExtractors.put(DocLibExtractor.DL_FOLDER_CLASS, extr);
		
		//journals
		extr = new JournalExtractor(fLocale, fServiceUrl);
		fExtractors.put(JournalExtractor.JOURNAL_ARTICLE_CLASS, extr);
		fExtractors.put(JournalExtractor.JOURNAL_FOLDER_CLASS, extr);
		
		//message board
		extr = new MessageBoardExtractor(fProperties, fLocale, fServiceUrl);
		fExtractors.put(MessageBoardExtractor.MB_THREAD_CLASS, extr);
		fExtractors.put(MessageBoardExtractor.MB_MESSAGE_CLASS, extr);
		fExtractors.put(MessageBoardExtractor.MB_DISCUSSION_CLASS, extr);
		
		//portal
		extr = new PortalExtractor(fLocale, fServiceUrl);
		fExtractors.put(PORTAL_ACT_INT_CLASS, extr);
		
		//wiki
		extr = new WikiExtractor(fProperties, fLocale, fServiceUrl);
		fExtractors.put(WikiExtractor.WIKI_PAGE_CLASS, extr);
		fExtractors.put(WikiExtractor.WIKI_NODE_CLASS, extr);
		
		//microblogs
		extr = new MicroblogsExtractor(fProperties, fLocale, fServiceUrl);
		fExtractors.put(MICROBLOG_ENTRY_CLASS, extr);
		
		//pages
		extr = new PageExtractor(fProperties, fLocale, fServiceUrl);
		fExtractors.put(LAYOUT_CLASS, extr);
	}
	
	private void registerObjectGens()
	{
		//blogs
		IObjectGenerator extr = new BlogsObjectGenerator(fServiceUrl);
		fObjectGens.put(BLOGS_ENTRY_CLASS, extr);
		
		//document library
		extr = new DocLibObjectGenerator(fServiceUrl);
		fObjectGens.put(DocLibExtractor.DL_FILE_ENTRY_CLASS, extr);
		fObjectGens.put(DocLibExtractor.DL_FOLDER_CLASS, extr);
		
		//journals
		extr = new JournalObjectGenerator(fServiceUrl);
		fObjectGens.put(JournalExtractor.JOURNAL_ARTICLE_CLASS, extr);
		fObjectGens.put(JournalExtractor.JOURNAL_FOLDER_CLASS, extr);
		
		//message board
		extr = new MessageBoardObjectGenerator(fProperties, fServiceUrl);
		fObjectGens.put(MessageBoardExtractor.MB_THREAD_CLASS, extr);
		fObjectGens.put(MessageBoardExtractor.MB_MESSAGE_CLASS, extr);
		fObjectGens.put(MessageBoardExtractor.MB_DISCUSSION_CLASS, extr);
		
		//wiki
		extr = new WikiObjectGenerator(fServiceUrl);
		fObjectGens.put(WikiExtractor.WIKI_PAGE_CLASS, extr);
		
		//TODO: WikiNode object generator
		
		//TODO: Microblog entry generator?

		//TODO: page entry generator?
	}
	
	private void fillVerbs()
	{
		fVerbs.put(SocialActivityConstants.TYPE_ADD_ATTACHMENT, "attach");
		fVerbs.put(SocialActivityConstants.TYPE_ADD_COMMENT, "add");
		fVerbs.put(SocialActivityConstants.TYPE_ADD_VOTE, "add");
		fVerbs.put(SocialActivityConstants.TYPE_DELETE, "delete");
		fVerbs.put(SocialActivityConstants.TYPE_MOVE_ATTACHMENT_TO_TRASH, "remove");
		fVerbs.put(SocialActivityConstants.TYPE_MOVE_TO_TRASH, "remove");
		fVerbs.put(SocialActivityConstants.TYPE_RESTORE_FROM_TRASH, "update");
		fVerbs.put(SocialActivityConstants.TYPE_SUBSCRIBE, "follow");
		fVerbs.put(SocialActivityConstants.TYPE_UNSUBSCRIBE, "stop-following");
		fVerbs.put(SocialActivityConstants.TYPE_VIEW, "access");
	}
	
	private void fillTitles()
	{
		fTitles.put(SocialActivityConstants.TYPE_ADD_ATTACHMENT, "attachment added");
		fTitles.put(SocialActivityConstants.TYPE_ADD_COMMENT, "comment added");
		fTitles.put(SocialActivityConstants.TYPE_ADD_VOTE, "vote added");
		fTitles.put(SocialActivityConstants.TYPE_DELETE, "deleted");
		fTitles.put(SocialActivityConstants.TYPE_MOVE_ATTACHMENT_TO_TRASH,
				"attachment moved to trash");
		fTitles.put(SocialActivityConstants.TYPE_MOVE_TO_TRASH, "moved to trash");
		fTitles.put(SocialActivityConstants.TYPE_RESTORE_FROM_TRASH,
				"restored from trash");
		fTitles.put(SocialActivityConstants.TYPE_SUBSCRIBE, "subscribed");
		fTitles.put(SocialActivityConstants.TYPE_UNSUBSCRIBE, "unsubscribed");
		fTitles.put(SocialActivityConstants.TYPE_VIEW, "viewed");
	}
	
    
    //activity streams sending
    private void sendActivity(String userId, SocialActivityContainer container)
    {
    	final String json = container.toJson();
    	
    	try
        {
            URL shindigUrl = new URL(fShindigUrl + ACT_STREAM_URL_FRAG + userId +
                "/@self");
            
            if(fLogging)
            {
            	fStreamLogger.logActivitySend(shindigUrl.toExternalForm(), container);
            }
            
            final HttpURLConnection connection =
                (HttpURLConnection) shindigUrl.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", String.valueOf(
                json.length()));
            
            OutputStreamWriter writer = new OutputStreamWriter(
                connection.getOutputStream(), "UTF-8");
            writer.write(json);
            writer.flush();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
            
            final StringBuffer resBuff = new StringBuffer();
            String line = reader.readLine();
            while(line != null)
            {
            	resBuff.append(line);
            	resBuff.append("\r\n");
                line = reader.readLine();
            }
            //TODO: evaluate answer?
            
            reader.close();
            
            //log  answer
            if(fLogging)
            {
            	fStreamLogger.logActivityResponse(resBuff.toString());
            }
        }
        catch(Exception e)
        {
            fLogger.log(Level.SEVERE, "Could not create activity entry", e);
            e.printStackTrace();
            
            if(fLogging)
            {
            	fStreamLogger.logException(e);
            }
        }
    }
    
	//output methods
    private void writeSocialActivity(SocialActivity activity) throws Exception
    {
    	synchronized(fQueue)
    	{
    		fQueue.add(activity);
    	}
    }

	@Override
	public void run()
	{
		final List<SocialActivity> buffer = new LinkedList<SocialActivity>();
		final List<String> oldCooldowns = new LinkedList<String>();
		long time = System.currentTimeMillis();
		fRunning = true;
		
		while(fRunning)
		{
			//add activities for delayed processing
			synchronized(fQueue)
			{
				buffer.addAll(fQueue);
				fQueue.clear();
			}
			
			try
			{
				Thread.sleep(PROCESSING_DELAY);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				fRunning = false;
			}
			
			for(SocialActivity activity : buffer)
			{
				try
				{
					processSocialActivity(activity);
				}
				catch(Throwable t)
				{
					t.printStackTrace();
					
					if(fLogging
						&& t instanceof Exception)
					{
						fStreamLogger.logException((Exception)t);
					}
				}
			}
			buffer.clear();
			
			//clean up vote cooldowns
			time = System.currentTimeMillis();
			synchronized(fVoteCooldowns)
			{
				for(Entry<String, Long> coolE : fVoteCooldowns.entrySet())
				{
					if(coolE.getValue() <= time)
					{
						oldCooldowns.add(coolE.getKey());
						
						if(fLogging)
						{
							fStreamLogger.logGeneric("vote cooldown cleanup: "
								+ coolE.getKey() + " (" + coolE.getValue() + ")");
						}
					}
				}
				
				for(String key : oldCooldowns)
				{
					fVoteCooldowns.remove(key);
				}
				oldCooldowns.clear();
			}
		}
		
		//TODO: react to shutdown signal?
	}
	
	private void processSocialActivity(SocialActivity activity) throws Exception
	{
		final String className = activity.getClassName();
		final int type = activity.getType();
		boolean send = true;
		
		//check blacklist
		List<Integer> blacklisted = fBlacklistCombs.get(className);
		if(fBlacklistTypes.contains(type)
				|| fBlacklistClasses.contains(className)
				|| blacklisted != null && blacklisted.contains(type))
		{
			if(fLogging)
			{
				fStreamLogger.logGeneric("blacklisted activity: " + className
						+ " (" + type + ")");
			}
			
			return;
		}
		
		//filter out (automated) follows
		send = autoFollowsFilter(activity);
		
		//create activity entry
		final SocialActivityContainer container = new SocialActivityContainer();
		
		//set ID derived from local ID
		container.setId(fActivityPrefix, activity.getActivityId());
		
		//users involved
		ActivityObjectContainer user = getUser(activity.getUserId());
		if(user != null)
		{
			container.setActor(user);
		}
		
		//generic verb
		String verb = fVerbs.get(type);
		if(verb == null)
		{
			verb = fDefaultVerb;
		}
		container.setVerb(verb);
		
		//generic title
		String title = fTitles.get(type);
		if(title != null)
		{
			container.setTitle(title);
		}
		
		//get appropriate handler
		IActivityExtractor extr = fExtractors.get(className);
		if(extr == null)
		{
			extr = fGenericExtractor;
		}
		extr.extract(activity, container);
		
		//TODO: remove
		fStreamLogger.logGeneric("got extractor " + extr + " for " + className);
		
		//add liferay as provider
		container.setProvider(fProvider);
		
		//replace title variables
		String group = null;
		try
		{
			Group g = GroupServiceUtil.getGroup(activity.getGroupId());
			group = g.getName();
		}
		catch(Exception e) {}
		
		repairTitle(container, group);
		
		//TODO: titles are broken at the moment
		container.setTitle(null);
		
		//TODO: used anywhere?
		//add receiving user as target if there is no target yet
//		if(container.getTarget() == null)
//		{
//			ActivityObjectContainer recUser = getUser(activity.getReceiverUserId());
//			if(recUser != null)
//			{
//				container.setTarget(recUser);
//			}
//		}
		
		//generate generic comment and vote objects
		//TODO: check if there is a more elegant way
		if(type == SocialActivityConstants.TYPE_ADD_COMMENT)
		{
			ActivityObjectContainer commentObject = new ActivityObjectContainer(
				"liferay-comment", null, fCommentName, null);
			container.setObject(commentObject);
		}
		else if(type == SocialActivityConstants.TYPE_ADD_VOTE)
		{
			//determine whether the vote-cooldown is already over
			send = voteCooldown(container);
			
			//retrieve rating object
			RatingsEntry re = null;
			String rId = null;
			String score = null;
			try
			{
				re = RatingsEntryLocalServiceUtil.fetchEntry(activity.getUserId(),
						className, activity.getClassPK());
				
				//compute "n out of five" rating
				rId = Long.toString(re.getEntryId());
				double rating = re.getScore();
				long intRating = Math.round(rating * 5);
				
				score = intRating + "/5";
				
				//possibly attach referenced object as target
				String cn = re.getClassName();
				IObjectGenerator gen = fObjectGens.get(cn);
				
				if(gen != null)
				{
					ActivityObjectContainer target = gen.generate(cn, re.getClassPK());
					if(target != null)
					{
						container.setTarget(target);
					}
				}
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
			
			ActivityObjectContainer voteObject = new ActivityObjectContainer(
				"liferay-vote", rId, fVoteName, null);
			if(score != null)
			{
				voteObject.setContent(score);
				
				//attach rating to title
				title += " (" + score + ")";
				container.setTitle(title);
			}
			
			container.setObject(voteObject);
		}
		
		//filter out unnamed, unlinked entities and activities with no valid entities
		if(fFilterUnnamed)
		{
			send = unnamedFilter(container);
		}
		
		//send to shindig
		if(send)
		{
			sendActivity(UserLocalServiceUtil.fetchUser(
					activity.getUserId()).getScreenName(), container);
		}
		else if(fLogging)
		{
			fStreamLogger.logActivityDiscard(container);
		}
	}
	
	private boolean voteCooldown(final SocialActivityContainer container)
	{
		boolean cool = true;
		
		JSONObject target = container.getTarget();
		if(target != null)
		{
			final long time = System.currentTimeMillis();
			
			synchronized(fVoteCooldowns)
			{
				Long value = fVoteCooldowns.get(target.getString("id"));
			
				if(value == null
					|| time >= value)
				{
					//set new cooldown
					fVoteCooldowns.put(target.getString("id"), time + VOTE_COOLDOWN);
				}
				else
				{
					//cooldown has not yet expired
					cool = false;
					
					if(fLogging)
					{
						fStreamLogger.logGeneric("vote cooldown not yet expired: "
								+ target.getString("id"));
					}
				}
			}
		}
		
		return cool;
	}
	
	private boolean autoFollowsFilter(SocialActivity activity)
	{
		boolean send = true;
		String className = activity.getClassName();
		int type = activity.getType();
		
		if(type == SocialActivityConstants.TYPE_SUBSCRIBE
			|| type == SocialActivityConstants.TYPE_UNSUBSCRIBE)
		{
			//filter follows in the wiki
			if(fFilterWikiFollows
				&& WikiExtractor.WIKI_PAGE_CLASS.equals(className))
			{
				//TODO: determine whether it is actually automatic
				//TODO: WikiNode?
				send = false;
			}
	
			//filter follows in message boards and discussions
			else if(fFilterMBFollows
				&& (MessageBoardExtractor.MB_THREAD_CLASS.equals(className)
				|| MessageBoardExtractor.MB_MESSAGE_CLASS.equals(className)
				|| MessageBoardExtractor.MB_DISCUSSION_CLASS.equals(className)))
			{
				//TODO: determine whether it is actually automatic
				send = false;
			}
			
			//TODO: configuration?
			else if(MICROBLOG_ENTRY_CLASS.equals(className))
			{
				//TODO: determine whether it is actually automatic
				send = false;
			}
		}
		
		return send;
	}
	
	private boolean unnamedFilter(final SocialActivityContainer container)
	{
		boolean send = true;
		
		JSONObject object = container.getObject();
		JSONObject target = container.getTarget();
		
		//"empty" objects
		if(object != null
			&& (object.optString("displayName") == null
				|| object.optString("displayName").isEmpty())
			&& (object.optString("url") == null
					|| object.optString("url").isEmpty()))
		{
			object = null;
			container.setObject(null);
		}
		
		//"empty" targets
		if(target != null
			&& (target.optString("displayName") == null
				|| target.optString("displayName").isEmpty())
			&& (target.optString("url") == null
				|| target.optString("url").isEmpty()))
		{
			target = null;
			container.setTarget(null);
		}
		
		//don't send the activity if there are no related objects
		if(object == null
			&& target == null)
		{
			send = false;
		}
		
		return send;
	}
	
	private ActivityObjectContainer getUser(long userId)
	{
		if(userId == 0)
		{
			return null;
		}
		
		ActivityObjectContainer container = null;
		try
		{
			User user = UserLocalServiceUtil.fetchUser(userId);
			
			if(user != null)
			{
				container = new ActivityObjectContainer("person", user.getScreenName(),
						user.getFullName(), null);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return container;
	}
	
	private void repairTitle(SocialActivityContainer container, String group)
	{
		String title = container.getTitle();
		
		JSONObject userObj = container.getActor();
		JSONObject objObj = container.getObject();
		JSONObject tarObj = container.getTarget();
		
		if(title != null)
		{
			if(group != null)
			{
				title = title.replace("{0}", group);
			}
			else
			{
				title = title.replace("{0}", "group");
			}
			
			if(userObj != null)
			{
				title = title.replace("{1}", userObj.optString("displayName"));
			}
			else
			{
				title = title.replace("{1}", "user");
			}
			
			if(objObj != null)
			{
				title = title.replace("{2}", objObj.optString("displayName"));
			}
			else
			{
				title = title.replace("{2}", "object");
			}
			
			if(tarObj != null)
			{
				title = title.replace("{3}", tarObj.optString("displayName"));
			}
			else
			{
				title = title.replace("{3}", "target");
			}
			
			container.setTitle(title);
		}
		
	}
	
	
	//Liferay methods
	@Override
	public SocialActivity createSocialActivity(long activityId)
	{
		SocialActivity activity = super.createSocialActivity(activityId);
		
		try
		{
			writeSocialActivity(activity);
			
			//log liferay version of the activity
			if(fLiferayLogging)
			{
				fLiferayLogger.writeSocialActivity(activity, 0);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			if(fLogging)
			{
				fStreamLogger.logException(e);
			}
		}
		
		return activity;
	}
	
	@Override
	public void addActivity(SocialActivity activity, SocialActivity mirrorActivity)
			throws PortalException, SystemException
	{
		super.addActivity(activity, mirrorActivity);
		
		try
		{
			if(activity != null)
			{
				writeSocialActivity(activity);
			}
			
			if(mirrorActivity != null
				&& fLogging)
			{
				fStreamLogger.logGeneric("mirror activity "
					+ mirrorActivity.getActivityId() + " received for "
					+ "activity " + activity.getActivityId());
			}
			
			//log liferay version of the activities
			if(fLiferayLogging)
			{
				fLiferayLogger.writeSocialActivity(activity, 0);
				fLiferayLogger.writeSocialActivity(mirrorActivity, 0);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			if(fLogging)
			{
				fStreamLogger.logException(e);
			}
		}
	}

	@Override
	public SocialActivity addSocialActivity(SocialActivity socialActivity)
			throws SystemException
	{
		SocialActivity activity = super.addSocialActivity(socialActivity);
		
		try
		{
			writeSocialActivity(socialActivity);
			
			writeSocialActivity(activity);
			
			//log liferay version of the activities
			if(fLiferayLogging)
			{
				fLiferayLogger.writeSocialActivity(socialActivity, 0);
				fLiferayLogger.writeSocialActivity(activity, 0);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			if(fLogging)
			{
				fStreamLogger.logException(e);
			}
		}
		
		return activity;
	}
}