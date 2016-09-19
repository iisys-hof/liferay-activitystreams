package de.hofuniversity.iisys.liferay.portlet.social.extractors;

import com.liferay.portlet.social.model.SocialActivity;

import de.hofuniversity.iisys.liferay.portlet.social.model.SocialActivityContainer;

public interface IActivityExtractor
{
	public void extract(SocialActivity activity, SocialActivityContainer container);
}
