
Spinnaker Plugin for adding a NewRelic event listener.

TODO: 
- Add git actions to continuously check Kork/Echo version compatibility.
- The plugin does not yet compress event messages before sending to NewRelic, which NewRelic recommends. Therefore some events may not be successfully captured if any of the event attributes are too large.

<h2>Usage</h2>

1) Run `./gradlew releaseBundle`
2) Put the `/build/distributions/<project>-<version>.zip` into the [configured plugins location for your service](https://pf4j.org/doc/packaging.html).
3) Configure the Spinnaker service. Put the following in the service yml to enable the plugin and configure the extension.
```
spinnaker:
  extensibility:
    plugins:
      Armory.NewrelicEventListener:
        enabled: true
        extensions:
          armory.newRelicEventListener:
            enabled: true
            config:
              account: 'NewRelic account number'
              apiKey: 'key'
              eventType: 'this is a required field for the NewRelic Events API, can be whatever you want'
```

To debug the plugin inside a Spinnaker service (like Echo) using IntelliJ Idea follow these steps:

1) Run `./gradlew releaseBundle` in the plugin project.
2) Copy the generated `.plugin-ref` file under `build` in the plugin project submodule for the service to the `plugins` directory under root in the Spinnaker service that will use the plugin .
3) Link the plugin project to the service project in IntelliJ (from the service project use the `+` button in the Gradle tab and select the plugin build.gradle).
4) Configure the Spinnaker service the same way specified above.
5) Create a new IntelliJ run configuration for the service that has the VM option `-Dpf4j.mode=development` and does a `Build Project` before launch.
6) Debug away...
