# liferay-activitystreams

Liferay 7 OSGi Module wrapping the SocialActivityLocalService to generate ActivityStreams entries and send them to an external Apache Shindig server.

Installation:

1. Import in Liferay 7 IDE as a Liferay Module
2. Build using Gradle
3. Put http://central.maven.org/maven2/org/json/json/20160212/json-20160212.jar in Liferay's "osgi/modules/" folder
4. Put generated jar (in build/libs) in Liferay's "deploy" folder
5. (Restart Liferay)

Configuration File: /src/main/resources/shindig-activitystreams.properties

Caution: Even caught Exceptions in the implementation can cause Liferay operations to fail and transactions to roll back.
