# AppReviewsToSlack

Uses the Google Play developer API and Itunes API to send your reviews to an incoming slack webhook.

Compared to existing 3rd party services that crawl the google play, it can:

* Get the versionCode, osVersion and device of the review...
* Without exposing your google play account to third parties

It does so using the appengine, Translate and DataStore APIs and you'll need a Google Cloud account with billing enabled.
Everything should largely fit in the free tier except for Google Translate that has no free tier so you'll likely end up paying a few cents every month.

![screenshot](screenshot.png)

## Configuration

* Install the Google Cloud SDK
* configure projectId in build.gradle with the Id of your Google Cloud Console project
* enable billing for outbound requests to work
* create a service account for your Google Play account and give it permission on your app in the
  play console
* put this key as a resource named secret.json
* create a resource named config.json with your app packageName and slack incoming webhook
* ./gradlew appengineDeploy
* ./gradlew appengineDeployCron
* profit.


The reviews are updated every 15min by default. You can change that in cron.xml

