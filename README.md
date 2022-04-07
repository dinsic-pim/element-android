# Tchap Android

Tchap Android v2 is an Android Matrix client. The app can be run on every Android devices with Android OS Lollipop and more (API 21).

[<img src="resources/img/google-play-badge.png" alt="Get it on Google Play" height="60">](https://play.google.com/store/apps/details?id=fr.gouv.tchap.a)

# New Android SDK

Tchap is based on a new Android SDK fully written in Kotlin (like Element). In order to make the early development as fast as possible, Tchap and the new SDK currently share the same git repository.

At each Tchap release, the SDK module is copied to a dedicated repository: https://github.com/matrix-org/matrix-android-sdk2. That way, third party apps can add a regular gradle dependency to use it. So more details on how to do that here: https://github.com/matrix-org/matrix-android-sdk2.

# Releases to app stores

There is some delay between when a release is created and when it appears in the app stores (Google Play Store). Here are some of the reasons:

* Not all versioned releases that appear on GitHub are considered stable. Each release is first considered beta: this continues for at least two days. If the release is stable (no serious issues or crashes are reported), then it is released as a production release in Google Play Store.
* Each release on the Google Play Store undergoes review by Google before it comes out. This can take an unpredictable amount of time. In some cases it has taken several weeks.

If you would like to receive releases more quickly (bearing in mind that they may not be stable) you have a number of options:

1. [Sign up to receive beta releases](https://play.google.com/apps/testing/fr.gouv.tchap.a) via the Google Play Store.
2. Install a [release APK](https://github.com/tchapgouv/tchap-android-v2/releases) directly - download the relevant .apk file. Note: These are not the store versions, so you may have to uninstall the previous Tchap version before. Take care to properly logout and export your encrypted keys before.

## Contributing

Please refer to [CONTRIBUTING.md](https://github.com/tchapgouv/tchap-android-v2/blob/develop/CONTRIBUTING.md) if you want to contribute on the Tchap Android project!