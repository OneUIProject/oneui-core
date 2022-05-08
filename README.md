<p align="center">
  For Wear OS, please look at
  <a href="https://github.com/OneUIProject/seslw">seslw</a>.
  <br><br>
  <img loading="lazy" src="readme-res/sesl-readme-header.png"/>
</p>

## Samsung Experience Support Library
Samsung's One UI apps are created by using an heavily modified version of Google's [Android Jetpack](https://github.com/androidx/androidx) and [Material Components](https://github.com/material-components/material-components-android) libraries, that include (but are not limited to) a different styling of the UI, new APIs and much more.
The intent of this library is to make those Samsung UX elements available to everyone for study, modding or whatever feels right for you. Any form of contribution, suggestions, bug report or feature request will be welcome.

# Libraries
### Android Jetpack:
- appcompat ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/appcompat?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.41-sesl4)
- coordinatorlayout ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/coordinatorlayout?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.2-sesl4)
- drawerlayout ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/drawerlayout?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.2-sesl4)
- preference ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/preference?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.5-sesl4)
- recyclerview ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/recyclerview?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.24-sesl4)
- swiperefreshlayout ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/swiperefreshlayout?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.10-sesl4)
- viewpager ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/viewpager?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.4-sesl4)
- viewpager2 ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/viewpager2?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.1-sesl4)
### Material Components:
- material ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/material?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.37-sesl4)
### Samsung:
- apppickerview ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/apppickerview?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.18-sesl4)
- indexscroll ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/indexscroll?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.14-sesl4)
- picker-basic ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/picker-basic?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.34-sesl4)
- picker-color ![](https://img.shields.io/maven-central/v/io.github.oneuiproject.sesl/picker-color?color=%23C71A36&logoColor=%23C11920&style=flat-square) (based on 1.0.20-sesl4)

## Uncomplete libs:
- slidingpanelayout (based on 1.0.8-sesl4): broken, needs fixes

# Usage
To add the libraries in your project, simply add the dependencies in your build.gradle file:
```gradle
dependencies {
    implementation 'io.github.oneuiproject.sesl:appcompat:<version>'
    implementation 'io.github.oneuiproject.sesl:material:<version>'
    ...
}
```
You'll also need to remove Google's original libraries:
```gradle
configurations.all {
    exclude group: 'androidx.appcompat', module: 'appcompat'
    exclude group: 'androidx.core', module: 'core'
}
```

# More info
- [Samsung's EULA](https://www.samsung.com/sg/Legal/SamsungLegal-EULA/)
- [Official OneUI Design Guide](https://design.samsung.com/global/contents/one-ui/download/oneui_design_guide_eng.pdf)
- [Optimizing for DeX](https://developer.samsung.com/samsung-dex/modify-optimizing.html)

# Special thanks
- [Google](https://developer.android.com/jetpack) for their Jetpack and Material Components libraries.
- [Samsung](https://www.samsung.com/) for their awesome OneUI Design. :)
- All the current and future [contributors](https://github.com/Yanndroid/OneUI-Design-Library/graphs/contributors) and issue reporters. :D
