Rocket.Chat Notifications for Bamboo
==============================

This plugin sends [Bamboo](https://www.atlassian.com/software/bamboo) notifications to any Rocket.Chat Server.

This plugin allows you to take advantage of Bamboo's:

- flexible notification system (i.e. tell me when this build fails more than 5 times!)

**Please note that you will need the [Bamboo app](https://github.com/RocketChat/Apps.Atlassian.Bamboo) installed on your Rocket.Chat as well**

Notifications Supported
-----------------------

This plugin is able to process all notifications available on Bamboo, but the [Bamboo app for Rocket.Chat](https://github.com/RocketChat/Apps.Atlassian.Bamboo) may not be able to show all of them.

Setup
-----

1. In your Rocket.Chat, install the [Bamboo app](https://github.com/RocketChat/Apps.Atlassian.Bamboo)
2. In any conversation, run the following slashcommand: `/bamboo install`
3. Copy the URL the app is going to generate for you
4. In your Bamboo instance, go to the *Notifications* tab of the *Configure Plan* screen.
5. Choose a *Recipient Type* of *Rocket.Chat*
6. Configure your *Rocket.Chat URL* with the URL you copied on step 3
7. Configure your *Rocket.Chat channel* with the channel or private group you want to be notified
8. You're done!

You can also configure [deployment notifications](https://confluence.atlassian.com/bamboo/notifications-for-deployment-environments-342754193.html)

Compiling from source
---------------------

You first need to [Set up the Atlassian Plugin SDK](https://developer.atlassian.com/docs/getting-started/set-up-the-atlassian-plugin-sdk-and-build-a-project). Then, at the project top level (where the pom.xml is) :

1. Compile : `atlas-mvn compile`
2. Run : `atlas-run`
3. Debug : `atlas-debug`

Sample request
--------------

Build notification

```json
{
   "server": {
      "baseUrl": "http://localhost:8085/bamboo",
      "instanceName": "Bamboo Test Environment"
   },
   "notification": {
      "description": "Completed Plan Notification",
      "type": "com.atlassian.bamboo.notification.chain.ChainCompletedNotification"
   },
   "build":{
      "artifact":false,
      "number":35,
      "reason":"Code has changed",
      "buildCompletedDate":"2018-12-01T12:36:26.078Z[Zulu]",
      "testSummary":{
         "duration":26,
         "ignoredCount":0,
         "failedCount":2,
         "existingFailedCount":1,
         "quarantineCount":0,
         "successfulCount":11,
         "description":"2 of 13 failed",
         "skippedCount":0,
         "fixedCount":0,
         "totalCount":13,
         "newFailedCount":1
      },
      "vcs":[
         {
            "commits":[
               {
                  "comment":"Break stuff.",
                  "id":"13c43a1e26e5c4635a0ac24e775fed615e069b20"
               }
            ],
            "id":"13c43a1e26e5c4635a0ac24e775fed615e069b20",
            "repositoryName":"Assignment"
         },
         {
            "commits":[

            ],
            "id":"dcf2dba3846620a89f6c3f63cd9dedfa4336f650",
            "repositoryName":"tests"
         }
      ],
      "failedJobs":[
         {
            "failedTests":[
               {
                  "name":"testBubbleSort",
                  "methodName":"Bubble sort",
                  "className":"ac1.de.BehaviorTest",
                  "errors":[
                     "java.lang.AssertionError: Problem: BubbleSort does not sort correctly expected:<[Mon Feb 15 00:00:00 GMT 2016, Sat Apr 15 00:00:00 GMT 2017, Fri Sep 15 00:00:00 GMT 2017, Thu Nov 08 00:00:00 GMT 2018]> but was:<[Mon Feb 15 00:00:00 GMT 2016, Sat Apr 15 00:00:00 GMT 2017, Thu Nov 08 00:00:00 GMT 2018, Fri Sep 15 00:00:00 GMT 2017]>\n\tat org.junit.Assert.fail(Assert.java:88)\n\tat org.junit.Assert.failNotEquals(Assert.java:834)\n\tat org.junit.Assert.assertEquals(Assert.java:118)\n\tat ac1.de.BehaviorTest.testBubbleSort(BehaviorTest.java:40)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:498)\n\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)\n\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)\n\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n\tat org.junit.internal.runners.statements.FailOnTimeout$CallableStatement.call(FailOnTimeout.java:298)\n\tat org.junit.internal.runners.statements.FailOnTimeout$CallableStatement.call(FailOnTimeout.java:292)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.lang.Thread.run(Thread.java:748)\n"
                  ]
               },
               {
                  "name":"testMergeSort",
                  "methodName":"Merge sort",
                  "className":"ac1.de.BehaviorTest",
                  "errors":[
                     "java.lang.NullPointerException\n\tat java.util.Date.getMillisOf(Date.java:958)\n\tat java.util.Date.compareTo(Date.java:978)\n\tat ac1.de.MergeSort.merge(MergeSort.java:30)\n\tat ac1.de.MergeSort.mergesort(MergeSort.java:19)\n\tat ac1.de.MergeSort.performSort(MergeSort.java:10)\n\tat ac1.de.BehaviorTest.testMergeSort(BehaviorTest.java:46)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:498)\n\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)\n\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)\n\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n\tat org.junit.internal.runners.statements.FailOnTimeout$CallableStatement.call(FailOnTimeout.java:298)\n\tat org.junit.internal.runners.statements.FailOnTimeout$CallableStatement.call(FailOnTimeout.java:292)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.lang.Thread.run(Thread.java:748)\n"
                  ]
               }
            ],
            "id":11403400
         }
      ],
      "key": "P2-P2-83",
      "url": "http://localhost:8085/bamboo/browse/P2-P2-83",
      "successful":false
   },
   "project": {
      "name": "PetClinic 2016",
      "description": "",
      "key": "P2",
      "url": "http://localhost:8085/bamboo/browse/P2"
   },
   "config": {
      "channel": "general"
   },
   "plan": {
      "name": "PetClinic 2016 - PetClinic 2016",
      "description": "",
      "key": "P2-P2",
      "url": "http://localhost:8085/bamboo/browse/P2-P2"
   }
}

```

Deployment Notification

```json
{
   "server": {
      "baseUrl": "http://localhost:8085/bamboo",
      "instanceName": "Bamboo Test Environment"
   },
   "notification": {
      "description": "Deployment Started Notification",
      "type": "com.atlassian.bamboo.deployments.notification.DeploymentStartedNotification"
   },
   "deploymentResult": {
      "environment": {
         "name": "Staging",
         "id": 1441794,
         "url": "http://localhost:8085/bamboo/deploy/viewEnvironment.action?id=1441794"
      },
      "triggerName": "Dependency build",
      "id": 7536664,
      "state": "Unknown",
      "version": {
         "name": "release-61",
         "id": 7929872,
         "url": "http://localhost:8085/bamboo/deploy/viewDeploymentVersion.action?versionId=7929872"
      },
      "url": "http://localhost:8085/bamboo/deploy/viewDeploymentResult.action?deploymentResultId=7536664"
   },
   "project": {
      "name": "PetClinic 2016",
      "description": "",
      "key": "1343490",
      "url": "http://localhost:8085/bamboo/deploy/viewDeploymentProjectEnvironments.action?id=1343490"
   },
   "config": {
      "channel": "general"
   }
}
```
