# Releasing Blue Checkers

This project ships a signed release APK via GitHub Releases. The release
build is produced by the `Release` GitHub Actions workflow, triggered when
a tag matching `v*` is pushed.

## One-time setup

### 1. Generate a release keystore

Do this on your local machine. Keep the keystore file outside of any
repository directory — it must never be committed.

```sh
keytool -genkey -v \
    -keystore ~/keystores/bluecheckers-release.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias bluecheckers
```

You'll be prompted for two passwords (keystore + key) and a "first/last
name" — for an open-source release you can use the project name; the
field is shown when users inspect the APK signature.

### 2. Configure GitHub secrets

Open the repo on GitHub → Settings → Secrets and variables → Actions.

Add four repository secrets:

| Secret name                 | Value                                                                    |
| --------------------------- | ------------------------------------------------------------------------ |
| `RELEASE_KEYSTORE_B64`      | Base64 of the keystore: `base64 -i ~/keystores/bluecheckers-release.jks` |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore password you entered above.                                 |
| `RELEASE_KEY_ALIAS`         | `bluecheckers` (or whatever alias you used).                             |
| `RELEASE_KEY_PASSWORD`      | The key password you entered above.                                      |

That's the whole one-time setup.

### 3. Local release builds (optional)

If you want to build a signed release APK locally too, drop a
`keystore.properties` at the repo root (it is git-ignored):

```properties
storeFile=/Users/you/keystores/bluecheckers-release.jks
storePassword=...
keyAlias=bluecheckers
keyPassword=...
```

Then:

```sh
./gradlew :app:assembleRelease
```

## Cutting a release

```sh
# 1. Bump versionName and versionCode in app/build.gradle.kts
# 2. Commit the bump
git commit -am "chore: bump to v1.1.0"

# 3. Tag and push
git tag v1.1.0
git push origin main --tags
```

GitHub Actions then:

1. Decodes the keystore from the secret.
2. Writes a temporary `keystore.properties`.
3. Runs `./gradlew :app:testDebugUnitTest :app:assembleRelease`.
4. Creates a GitHub Release named after the tag.
5. Attaches the signed APK (`app-release.apk`) to the release.

Watch the run on the **Actions** tab. When it succeeds the release is
visible on the **Releases** tab with the APK as a downloadable asset.

## Versioning

Tags follow [semver](https://semver.org/):

- `MAJOR` — incompatible save-file or rules changes.
- `MINOR` — new features that keep prior saves compatible.
- `PATCH` — bug fixes only.

The `versionCode` in `app/build.gradle.kts` must increment monotonically
on every release (Android refuses to install an APK with a lower or equal
`versionCode` than the one already installed).

## Distribution

GitHub Releases is the only official distribution channel for now. Users
sideload the APK by downloading it on their device and opening it (they
have to allow "install unknown apps" for their browser).
