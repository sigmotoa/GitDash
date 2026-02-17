# Version Update Notification System - Setup Guide

This app includes an automatic version update notification system that alerts users when a new version is available.

## How It Works

The app checks for updates by fetching a `version.json` file from a remote URL (GitHub, Firebase, or your own server). If a newer version is detected, it shows a dialog to the user with release notes and an update button.

## Setup Instructions

### 1. Host the version.json file

You have several options:

#### Option A: GitHub Repository (Recommended)

1. Create or use an existing public GitHub repository
2. Add the `version.json` file to the root or a specific folder
3. The raw URL will be: `https://raw.githubusercontent.com/USERNAME/REPO/main/version.json`

Example:
```
Repository: https://github.com/sigmotoa/gitdash
File URL: https://raw.githubusercontent.com/sigmotoa/gitdash/main/version.json
```

#### Option B: GitHub Gist

1. Create a new Gist at https://gist.github.com
2. Name the file `version.json`
3. Use the raw URL from the Gist

#### Option C: Firebase or Your Own Server

Host the JSON file on Firebase Hosting or your own web server.

### 2. Update the Base URL

Edit `app/src/main/java/com/sigmotoa/gitdash/data/remote/VersionCheckService.kt`:

```kotlin
companion object {
    const val BASE_URL = "https://your-url-here/"
}
```

### 3. version.json Format

The JSON file should follow this structure:

```json
{
  "version_code": 4,
  "version_name": "1.4",
  "release_notes": "What's new in this version...",
  "download_url": "https://play.google.com/store/apps/details?id=com.sigmotoa.gitdash",
  "is_mandatory": false
}
```

**Fields:**
- `version_code`: Integer that must be higher than the current app version code
- `version_name`: String representing the version (e.g., "1.4")
- `release_notes`: Optional string with what's new (supports line breaks with \n)
- `download_url`: Optional URL to the app (defaults to Play Store if not provided)
- `is_mandatory`: Boolean - if true, users cannot dismiss the dialog

### 4. Update the version.json when releasing

Every time you release a new version:

1. Increment `versionCode` in `app/build.gradle.kts`
2. Update `versionName` in `app/build.gradle.kts`
3. Update the `version.json` file with the new values
4. Commit and push to your repository

### 5. Testing

To test the update dialog:

1. Set a higher `version_code` in `version.json` (e.g., 999)
2. Run the app
3. The update dialog should appear on app launch
4. Reset `version_code` to the actual next version after testing

## Features

- **Automatic Check**: Checks for updates on app startup
- **Non-Intrusive**: Users can dismiss the dialog unless it's a mandatory update
- **Rich Information**: Shows current version, new version, and release notes
- **Deep Link**: Opens Play Store or custom URL when user taps "Update Now"
- **Graceful Failure**: If the check fails, the app continues normally without errors

## Customization

You can customize the update dialog in:
`app/src/main/java/com/sigmotoa/gitdash/ui/components/UpdateDialog.kt`

You can adjust the check frequency by modifying when `checkForAppUpdates()` is called in `MainActivity.kt`.

## Notes

- The version check happens asynchronously and doesn't block app startup
- Network failures are handled gracefully
- The system uses Retrofit and follows the same architecture as other API calls
- You can add analytics or logging to track update adoption rates
