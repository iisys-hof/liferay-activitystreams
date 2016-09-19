package de.hofuniversity.iisys.liferay.portlet.social.objgens;

import de.hofuniversity.iisys.liferay.portlet.social.model.ActivityObjectContainer;

/**
 * Generates activity objects from Liferay's internal objects.
 */
public interface IObjectGenerator
{
	public ActivityObjectContainer generate(String className, long primaryKey);
}
