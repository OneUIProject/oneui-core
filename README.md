<p align="center">
  For Wear OS, please look at
  <a href="https://github.com/OneUIProject/oneuiw-core">oneuiw-core</a>.
  <br><br>
  <img loading="lazy" src="readme-res/sesl-readme-header.png"/>
</p>

## Samsung Experience Support Library (One UI 4.x)
This is the repository for the Samsung Experience Support Library (internally referenced as sesl).

Samsung's One UI apps are created by using a heavily modified version of Google's [Android Jetpack](https://github.com/androidx/androidx) and [Material Components](https://github.com/material-components/material-components-android) libraries, that include (but are not limited to) a new theme for the UI, new APIs and much more.
The intent of this library is to make those Samsung UX elements available to everyone for study, modding or whatever feels right for you.

Any form of contribution, suggestions, bug report or feature request will be welcome.

# Libraries
### Android Jetpack:
- [appcompat](https://developer.android.com/jetpack/androidx/releases/appcompat) [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/appcompat?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/appcompat) (based on 1.0.45-sesl4)
- [coordinatorlayout](https://developer.android.com/jetpack/androidx/releases/coordinatorlayout) [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/coordinatorlayout?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/coordinatorlayout) (based on 1.0.2-sesl4)
- [drawerlayout](https://developer.android.com/jetpack/androidx/releases/drawerlayout) [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/drawerlayout?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/drawerlayout) (based on 1.0.2-sesl4)
- [preference](https://developer.android.com/jetpack/androidx/releases/preference) [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/preference?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/preference) (based on 1.0.6-sesl4)
- [recyclerview](https://developer.android.com/jetpack/androidx/releases/recyclerview) [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/recyclerview?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/recyclerview) (based on 1.0.29-sesl4)
- [swiperefreshlayout](https://developer.android.com/jetpack/androidx/releases/swiperefreshlayout) [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/swiperefreshlayout?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/swiperefreshlayout) (based on 1.0.10-sesl4)
- [viewpager](https://developer.android.com/jetpack/androidx/releases/viewpager) [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/viewpager?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/viewpager) (based on 1.0.4-sesl4)
- [viewpager2](https://developer.android.com/jetpack/androidx/releases/viewpager2) [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/viewpager2?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/viewpager2) (based on 1.0.2-sesl4)
### Material Components:
- [material](https://material.io/develop/android) [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/material?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/material) (based on 1.0.43-sesl4)
### Samsung:
- apppickerview [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/apppickerview?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/apppickerview) (based on 1.0.18-sesl4)
- indexscroll [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/indexscroll?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/indexscroll) (based on 1.0.14-sesl4)
- picker-basic [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/picker-basic?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/picker-basic) (based on 1.0.35-sesl4)
- picker-color [![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/picker-color?color=C71A36&style=flat-square&versionPrefix=1)](https://mvnrepository.com/artifact/io.github.oneuiproject.sesl/picker-color) (based on 1.0.21-sesl4)

## Missing libs:
- slidingpanelayout (based on 1.0.10-sesl4)
- typoanimation (based on 1.0.1-sesl4)

# Usage
To use these libraries in your project, simply add the dependencies in your build.gradle file:
```groovy
dependencies {
    implementation 'io.github.oneuiproject.sesl:appcompat:<version>'
    implementation 'io.github.oneuiproject.sesl:material:<version>'
    // ...
}
```
In order for your project to build correctly, you'll need to remove Google's original libraries:
```groovy
configurations.all {
    exclude group: 'androidx.appcompat', module: 'appcompat'
    exclude group: 'androidx.core', module: 'core'
}
```
For more informations about the usage of the libraries, please take a look at our [wiki](https://oneuiproject.github.io/).
## Using with libraries that depend on AndroidX
If you need to use a library that requires one or more stock androidx module, you have to exclude those specific dependencies to avoid build errors, like this:
```groovy
implementation("com.org.group:library:1.0.0") {
	exclude group: 'androidx.swiperefreshlayout', module: 'swiperefreshlayout'
}
```

# More info
- [Android Jetpack](https://developer.android.com/jetpack)
- [Material Components](https://material.io/components?platform=android)
- [One UI](https://www.samsung.com/one-ui/)

# Special thanks
- [Google](https://developer.android.com/jetpack) for their Jetpack and Material Components libraries.
- [Samsung](https://www.samsung.com/) for their awesome OneUI Design. :)
- All the current and future [contributors](https://github.com/OneUIProject/oneui-core/graphs/contributors) and issue reporters. :D
