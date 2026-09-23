# Releasing

## Cutting a release

```
git tag v1.0.0
git push origin v1.0.0
```

That triggers `.github/workflows/release.yml`, which builds a signed APK, names it
`KeepMeActive.apk`, and publishes a GitHub release. The download page always links to
`releases/latest/download/KeepMeActive.apk`, so that link never needs updating.

Version numbers come from the tag. `v1.2.3` becomes versionName `1.2.3` and versionCode
`10203`, which is monotonic, so Android always recognises a newer build as newer.

You can also run the workflow by hand from the Actions tab and type a version, which is
useful for re-cutting a build without moving a tag.

## Set up the signing key — do this before the first public release

**The problem this solves.** Without a signing key, CI falls back to a debug key that is
generated fresh on every run. Android refuses to install an APK over an existing app signed
with a different key: the user gets a bare "App not installed" with no explanation, and their
only way out is to uninstall first, losing their settings. Fixing this after people have
installed the app is painful, so do it now.

You need `keytool`, which ships with any JDK. If Android Studio is installed you already have
one; otherwise `winget install Microsoft.OpenJDK.17`.

### 1. Create the keystore (once, ever)

```
keytool -genkeypair -v -keystore release.keystore -alias keepmeactive -keyalg RSA -keysize 2048 -validity 10000
```

It asks for a password and some name/organisation fields, which can be anything. Use a strong
password and put it in your password manager.

**Back this file up somewhere safe and never commit it.** Lose it and you can never ship an
update that installs over the existing app — everyone has to uninstall and reinstall.
`.gitignore` already excludes `*.keystore` and `*.jks`.

### 2. Turn it into a secret

In PowerShell, from the folder containing the keystore:

```
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Clipboard
```

### 3. Add four repository secrets

Settings → Secrets and variables → Actions → New repository secret:

| Name | Value |
| --- | --- |
| `KEYSTORE_BASE64` | the base64 string now on your clipboard |
| `KEYSTORE_PASSWORD` | the keystore password you chose |
| `KEY_ALIAS` | `keepmeactive` |
| `KEY_PASSWORD` | the key password (same as the keystore password unless you set a different one) |

The workflow detects them automatically. Without `KEYSTORE_BASE64` it still builds, but logs
a warning and produces an APK that cannot be updated over.

## The download page

`docs/index.html` is served by GitHub Pages at
`https://grantgray1.github.io/keepmeactive/`.

To switch it on: Settings → Pages → Source **Deploy from a branch**, branch `main`,
folder `/docs`.

**Pages only works on a public repository** on a free GitHub plan, and release assets on a
private repo need the visitor to be signed in to GitHub. So a genuinely one-tap download
means making this repo public. The alternative is keeping it private and downloading the APK
on a PC, then transferring it to the phone by cable.

## Checklist for a release

1. CI green on `main`.
2. Install the CI debug APK on a real phone and send a test text. This code path touches the
   radio; it cannot be verified by a compiler.
3. Tag and push.
4. Download from the release page on a phone and confirm the install flow works end to end.
