# liferay-activitystreams

Liferay 7 OSGi Module wrapping the SocialActivityLocalService to generate ActivityStreams entries and send them to an external Apache Shindig server.

Installation:

1. Import in Liferay 7 IDE as a Liferay Module
2. Build using Gradle
3. Put generated jar (in build/libs) in Liferay's "deploy" folder
4. (Restart Liferay)

Configuration File: /src/main/resources/shindig-activitystreams.properties

Caution: Even caught Exceptions in the implementation can cause Liferay operations to fail and transactions to roll back.
