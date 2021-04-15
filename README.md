# ForceUpdate

![Minimum API level](https://img.shields.io/badge/API-23+-yellow)

Help the user to implement the force update of the application by just providing the APK link needed.

## Installation

### Repositories

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
### Dependency
```groovy
implementation 'com.github.Abdulrahman-AlGhamdi:ForceUpdate:TAG'
```

## Usage

* First  : call `ForceUpdateManager` helper class
* Second : in the constructor you must provide the Activity and APK_LINK
* Third  : call `updateApplication` to start force update process

```kotlin
ForceUpdateManager(
    activity = this,
    apkLink = "APK_LINK"
).updateApplication()
```

Finally, if you want to customize the force update design you can add:
   * Application Logo
   * Version Code
   * Version Name
   * Application Name

```kotlin
ForceUpdateManager(
    activity = this,
    apkLink = "APK_LINK",
    logo = R.drawable.application_logo,
    versionCode = BuildConfig.VERSION_CODE,
    versionName = BuildConfig.VERSION_NAME,
    applicationName = getString(R.string.app_name)
).isApplicationUpdated()
```

## License

```
Copyright 2021 Abdulrahman AlGhamdi

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
