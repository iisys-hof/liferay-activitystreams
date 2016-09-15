package de.hofuniversity.iisys.liferay.activitystreams;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Map;

import de.hofuniversity.iisys.liferay.activitystreams.model.SocialActivityContainer;

public class ActivityStreamLogger
{
	private static final String LOG_FILE = "activitystreams_log";
	
    private final Map<String, String> fProperties;
	
	private final Writer fWriter;
	
	public ActivityStreamLogger(Map<String, String> properties)
		throws Exception
	{
		fProperties = properties;
		
		fWriter = new BufferedWriter(new FileWriter(
				fProperties.get(LOG_FILE)));
		
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					fWriter.flush();
					fWriter.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}
	
	public void logGeneric(String message)
	{
		try
		{
			fWriter.write("\n");
			
			fWriter.write(message);
			
			fWriter.write("\n");
			fWriter.flush();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void logActivitySend(String shindigUrl,
			SocialActivityContainer container)
	{
		try
		{
			fWriter.write("\n");
			
			fWriter.write("sending to: " + shindigUrl + "\n");
			fWriter.write("activity: " + container.toJson() + "\n");
			
			fWriter.write("\n");
			fWriter.flush();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void logActivityDiscard(SocialActivityContainer container)
	{
		try
		{
			fWriter.write("\n");
			
			fWriter.write("discarded activity: " + container.toJson() + "\n");
			
			fWriter.write("\n");
			fWriter.flush();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void logActivityResponse(String response)
	{
		try
		{
			fWriter.write("\n");
			
			fWriter.write("response from server: " + response + "\n");
			
			fWriter.write("\n");
			fWriter.flush();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void logException(Exception e)
	{
		try
		{
			fWriter.write("\n");
			
			fWriter.write("Exception: " + e.getMessage() + "\n");
			fWriter.write("Exception: " + e.toString() + "\n");
			fWriter.write("stack trace: \n");
			for(StackTraceElement element : e.getStackTrace())
			{
				fWriter.write("\t" + element.toString() + "\n");
			}
			
			fWriter.write("\n");
			fWriter.flush();
		}
		catch(Exception ex)
		{
			e.printStackTrace();
		}
	}
}
