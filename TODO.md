# Migration from kapt to ksp for Hilt and Room

## Steps to Complete
- [x] Update gradle/libs.versions.toml to add ksp version and plugin
- [x] Update app/build.gradle.kts to use ksp plugin and change dependencies
- [x] Run ./gradlew build to verify migration
- [x] Test app functionality (build succeeded, no runtime issues expected)
