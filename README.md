# liferay-activitystreams

Liferay Activitystreams Ext Plugin tapping into the event system, affecting all Liferay subsystems.

Installation:

1. Project has to be placed in a folder called "ShindigExt-ext" in the "ext" folder of a Liferay SDK.
2. Import in Liferay IDE using "Liferay project from existing source"
3. Right click on project and execute Liferay - SDK - war
4. Put http://central.maven.org/maven2/org/json/json/20160212/json-20160212.jar in Liferay's "osgi/modules/" folder
5. Put generated war in Liferay's "deploy" folder
6. Restart Liferay twice

Configuration File: /docroot/WEB-INF/liferay-plugin-package.properties

Caution 1: Cannot be easily uninstalled - Liferay copies many files to its own contexts. It is advisable to keep a clean copy of Liferay's webapp directory, replace the used one and redeploy any plugins.

Caution 2: Even caught Exceptions in the implementation can cause Liferay operations to fail and transactions to roll back.