package de.hofuniversity.iisys.liferay.activitystreams.objgens;

import de.hofuniversity.iisys.liferay.activitystreams.model.ActivityObjectContainer;

/**
 * Generates activity objects from Liferay's internal objects.
 */
public interface IObjectGenerator
{
	public ActivityObjectContainer generate(String className, long primaryKey);
}
