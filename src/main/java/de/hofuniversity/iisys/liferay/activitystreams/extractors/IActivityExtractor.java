package de.hofuniversity.iisys.liferay.activitystreams.extractors;

import com.liferay.social.kernel.model.SocialActivity;

import de.hofuniversity.iisys.liferay.activitystreams.model.SocialActivityContainer;

public interface IActivityExtractor
{
	public void extract(SocialActivity activity, SocialActivityContainer container);
}
