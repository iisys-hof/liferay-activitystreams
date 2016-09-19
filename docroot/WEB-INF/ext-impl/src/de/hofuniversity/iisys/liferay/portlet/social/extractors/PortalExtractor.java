package de.hofuniversity.iisys.liferay.portlet.social.extractors;

import java.util.Locale;

import com.liferay.portlet.social.model.SocialActivity;

import de.hofuniversity.iisys.liferay.portlet.social.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.portlet.social.model.SocialActivityContainer;

public class PortalExtractor extends GenericExtractor
{
	private final Locale fLocale;
	private final String fServiceUrl;
	
	private final ActivityObjectContainer fGenerator;
	
	public PortalExtractor(Locale locale, String url)
	{
		super(locale, url);
		
		fLocale = locale;
		fServiceUrl = url;
		
		fGenerator = new ActivityObjectContainer("application", "liferay-portal",
				"Liferay Portal", fServiceUrl);
	}

	@Override
	public void extract(SocialActivity activity,
			SocialActivityContainer container)
	{
		super.extract(activity, container);
		
		container.setObject(null);
		container.setTarget(null);
		
		container.setGenerator(fGenerator);
	}
}
