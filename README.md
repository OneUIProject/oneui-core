<h3 align="center">:warning: W.I.P.</h3>
<p align="center">
  <img loading="lazy" src="readme-res/sesl-readme-header.png"/>
</p>

For Wear OS, please look at [seslw](https://github.com/OneUIProject/seslw).

---

This repo contains a collection of the libraries used by Samsung in their One UI apps.
Samsung's One UI apps are created by using an heavily modified version of Google's [Android Jetpack](https://github.com/androidx/androidx) and [Material Components](https://github.com/material-components/material-components-android) libraries, that include a different styling of the UI, DeX/S Pen support, new features and much more. The intent of this library is to make those Samsung UX elements available to everyone for study, modding or whatever feels right for you. Any form of contribution, suggestions, bug report or feature request will be welcome.

These libraries will soon replace the current [OneUI Design Library](https://github.com/Yanndroid/OneUI-Design-Library) once they are ready.

## Why should I move to using this new lib?
While the old library was a completely separated module, we now are implementing Samsung specific code directly in Google's libraries. Replacing Google libraries with ours will now make creating apps easier since you don't have to rely, for the most part, on workarounds or custom APIs anymore. Each component is now separated in its own module, meaning you can now choose what to add and what to exclude in your project.

# Libraries
### Android Jetpack:
- appcompat (1.0.39-sesl4)
- coordinatorlayout (1.0.2-sesl4)
- drawerlayout (1.0.2-sesl4)
- preference (1.0.5-sesl4)
- recyclerview (1.0.21-sesl4)
- swiperefreshlayout (1.0.10-sesl4)
- viewpager (1.0.3-sesl4)
- viewpager2 (1.0.1-sesl4)
### Material Components:
- material (1.0.34-sesl4)

  - [x] appbar
  - [ ] bottomnavigation
  - [ ] navigation
  - [ ] navigationrail
  - [ ] snackbar
  - [ ] tabs
### Samsung:
- apppickerview (1.0.17-sesl4)
- indexscroll (1.0.14-sesl4)
- picker-color (1.0.18-sesl4)

## Uncomplete/to-be-added libs:
- picker-basic (1.0.33-sesl4): still needs complete merging
- slidingpanelayout (1.0.8-sesl4): broken, needs fixes

# TO-DO list:
- Add Maven stuff
- Merge back current library stuff in a separate module

# More info
- [Samsung's EULA](https://www.samsung.com/sg/Legal/SamsungLegal-EULA/)
- [Official OneUI Design Guide](https://design.samsung.com/global/contents/one-ui/download/oneui_design_guide_eng.pdf)
- [Optimizing for DeX](https://developer.samsung.com/samsung-dex/modify-optimizing.html)

# Special thanks to:
- [Google](https://developer.android.com/jetpack) for their Jetpack and Material Components libraries.
- [Samsung](https://www.samsung.com/) for their awesome OneUI Design. :)
- All the current and future [contributors](https://github.com/Yanndroid/OneUI-Design-Library/graphs/contributors) and issue reporters. :D
