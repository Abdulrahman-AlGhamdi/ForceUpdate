# Force Update ![Minimum API level](https://img.shields.io/badge/API-23+-yellow)

This library helps the user to implement :
* Force updating the application by only providing the APK link needed. 
* Check the application version with update version to see whether the application need to be updated or not.
* Delete application content by clearing all its data

## Installation

### Repositories

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
### Dependency [![](https://jitpack.io/v/Abdulrahman-AlGhamdi/ForceUpdate.svg)](https://jitpack.io/#Abdulrahman-AlGhamdi/ForceUpdate)
```groovy
implementation 'com.github.Abdulrahman-AlGhamdi:ForceUpdate:tag'
```

## Usage

* First  : 
    * call `ForceUpdateManager` helper class
    * In the constructor you must provide an Activity reference

```kotlin
val forceUpdateManager = ForceUpdateManager(activity = this)
```

* Second : 
    * check the update version with current application version by using `checkAppVersion` function
    * In the constructor provide the new application update version
    * The Function will return true or false and that indicate whether you need to update the application or not

```kotlin
forceUpdateManager.checkAppVersion(updateVersion = 2)
```

* Third  : 
    * call `updateApplication` to start force update process
    * In the constructor provide the APK link
    * Provide a header to the download manger if you need
    * Make the force update optional or not

```kotlin
forceUpdateManager.updateApplication(apkLink = "APK_LINK")
```
or
```kotlin
forceUpdateManager.updateApplication(
    apkLink = "APK_LINK",
    header = Pair("header", "value"),
    optional = true
)
```

* Fourth :
    * If you need to destroy the application by clearing its data call `destroyApplication`
    * also you can provide custom message in the function constructor

```kotlin
forceUpdateManager.destroyApplication()
```
or
```kotlin
forceUpdateManager.destroyApplication(dialogMessage = "MESSAGE")
```

## License

```
Copyright 2021 Abdulrahman Al-Ghamdi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
