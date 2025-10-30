# Maven Central Publishing Guide

Complete step-by-step guide to publish JFrame artifacts to Maven Central.

## Prerequisites

- GitHub account
- Git installed locally
- GPG/GnuPG installed
- Admin access to this repository

## Overview

Maven Central publishing requires:
1. Sonatype Central Portal account
2. Namespace verification
3. GPG signing keys
4. GitHub repository secrets
5. Proper POM metadata (✅ already configured)

## Step 1: Create Sonatype Account

### 1.1 Register with GitHub

1. Go to [https://central.sonatype.com/](https://central.sonatype.com/)
2. Click **Sign In**
3. Choose **Sign in with GitHub**
4. Authorize Sonatype to access your GitHub account

**Important:** Using GitHub authentication automatically grants you the `io.github.<your-username>` namespace.

### 1.2 Verify Namespace

After signing in with GitHub:

1. Navigate to **Namespaces** in the portal
2. You should see `io.github.jordi-jaspers` automatically registered (your personal namespace)
3. Additionally, `io.github.jframeoss` has been added for this project

**Note:** The project uses `io.github.jframeoss` as the group ID:
- This namespace has been configured in Sonatype Central Portal
- If verification is required, you may need to create a verification repository
- Once verified, you can publish under this namespace

## Step 2: Generate GPG Signing Keys

Maven Central requires all artifacts to be cryptographically signed.

### 2.1 Generate GPG Key Pair

```bash
# Generate a new GPG key (use RSA 4096 bits)
gpg --full-generate-key

# Follow the prompts:
# - Key type: (1) RSA and RSA (default)
# - Key size: 4096
# - Expiration: 0 (does not expire) or 2y (2 years)
# - Real name: Your name
# - Email: Your email (preferably GitHub email)
# - Passphrase: Create a strong passphrase (you'll need this later)
```

### 2.2 List and Verify Keys

```bash
# List your keys
gpg --list-secret-keys --keyid-format=long

# Output example:
# sec   rsa4096/ABC123DEF456 2024-10-30 [SC]
#       1234567890ABCDEF1234567890ABCDEF12345678
# uid           [ultimate] Your Name <your.email@example.com>

# The key ID is the part after "rsa4096/" (ABC123DEF456)
```

### 2.3 Publish Key to Key Servers

```bash
# Replace ABC123DEF456 with your actual key ID
gpg --keyserver keyserver.ubuntu.com --send-keys ABC123DEF456
gpg --keyserver keys.openpgp.org --send-keys ABC123DEF456
gpg --keyserver pgp.mit.edu --send-keys ABC123DEF456
```

Wait 5-10 minutes for the keys to propagate across servers.

### 2.4 Export Private Key for GitHub

```bash
# Export the private key in ASCII armor format
gpg --armor --export-secret-keys ABC123DEF456

# This outputs something like:
# -----BEGIN PGP PRIVATE KEY BLOCK-----
# [long base64 string]
# -----END PGP PRIVATE KEY BLOCK-----
```

**Copy the entire output** including the BEGIN and END lines. You'll need this for GitHub secrets.

## Step 3: Configure GitHub Secrets

Add the following secrets to your GitHub repository:

### 3.1 Navigate to Secrets

1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

### 3.2 Add Required Secrets

Add these four secrets:

| Secret Name | Value | How to Get |
|-------------|-------|------------|
| `MAVEN_USERNAME` | Your Sonatype username | From [central.sonatype.com](https://central.sonatype.com) → Account |
| `MAVEN_PASSWORD` | Your Sonatype password | Generate a token at Account → Generate User Token |
| `SIGNING_KEY` | Your GPG private key | Output from `gpg --armor --export-secret-keys` (entire block) |
| `SIGNING_PASSWORD` | Your GPG passphrase | The passphrase you created when generating the key |

### 3.3 Generate Sonatype Token (Recommended)

Instead of using your account password:

1. Log in to [central.sonatype.com](https://central.sonatype.com)
2. Go to **Account** → **Generate User Token**
3. Copy the username and password shown
4. Use these for `MAVEN_USERNAME` and `MAVEN_PASSWORD` secrets

**Security Note:** User tokens are preferred over account passwords.

## Step 4: Verify Build Configuration

Your build configuration is already set up correctly, but verify:

### 4.1 Check Group ID

Edit `gradle.properties`:

```properties
# Project uses the jframeoss namespace
group=io.github.jframeoss
```

**Verification Status:**
- ✅ Namespace added to Sonatype Central Portal
- ⏳ May require verification via GitHub repository
- 🔄 Once verified, you can publish immediately

### 4.2 Verify POM Metadata

The following are already configured in `build.gradle.kts`:
- ✅ Project name and description
- ✅ URL
- ✅ License (Apache 2.0)
- ✅ Developer information
- ✅ SCM (Source Control) information
- ✅ Signing configuration
- ✅ Maven Central repository URLs

## Step 5: Publish Your First Release

### 5.1 Update Version

Edit `gradle.properties`:

```properties
version=0.1.0
```

### 5.2 Create and Push Tag

```bash
# Ensure all changes are committed
git add .
git commit -m "chore: prepare release 0.1.0"
git push origin master

# Create and push the tag
git tag 0.1.0
git push origin 0.1.0
```

### 5.3 Monitor the Workflow

1. Go to **Actions** tab in GitHub
2. Watch the CI workflow run
3. The publish job should:
   - ✅ Verify version matches tag
   - ✅ Build artifacts
   - ✅ Sign with GPG
   - ✅ Upload to Maven Central staging repository
   - ✅ Create GitHub Release

### 5.4 Complete Publication in Sonatype

After the workflow completes:

1. Log in to [central.sonatype.com](https://central.sonatype.com)
2. Go to **Deployments**
3. Find your staging repository
4. Click **Publish** (or it may auto-publish)
5. Wait 15-30 minutes for sync to Maven Central

## Step 6: Verify Publication

### 6.1 Check Maven Central Search

After 15-30 minutes:

1. Go to [search.maven.org](https://search.maven.org/)
2. Search for: `g:io.github.jframeoss`
3. Your artifacts should appear

### 6.2 Test Installation

Create a test project:

```kotlin
dependencies {
    implementation("io.github.jframeoss:starter-core:0.1.0")
}
```

## Troubleshooting

### Issue: "Namespace not found" or "401 Unauthorized"

**Solution:**
- Verify your Sonatype account has the correct namespace registered
- Check that `MAVEN_USERNAME` and `MAVEN_PASSWORD` are correct
- Ensure you're using a user token, not your account password

### Issue: "Invalid signature"

**Solution:**
- Verify GPG key was published to key servers
- Wait 10 minutes for propagation
- Check `SIGNING_KEY` secret contains the complete private key block
- Ensure `SIGNING_PASSWORD` matches your GPG passphrase

### Issue: "POM validation failed"

**Solution:**
- Ensure all required POM fields are present (already configured)
- Check that SCM URLs point to your actual repository
- Verify license information is correct

### Issue: "Version mismatch"

**Solution:**
- Tag version must match `gradle.properties` version exactly
- No 'v' prefix in tags (use `1.0.0` not `v1.0.0`)

### Issue: Group ID doesn't match namespace

**Solution:**
- Your group ID must be under a namespace you own
- The project uses `io.github.jframeoss`
- Verify this namespace is registered and verified in Sonatype Central Portal
- If verification is pending, check Namespaces section in the portal

## Security Best Practices

1. **Never commit secrets** to the repository
2. **Use user tokens** instead of account passwords
3. **Rotate tokens** periodically
4. **Set GPG key expiration** (e.g., 2 years) and renew before expiry
5. **Backup your GPG key** securely offline
6. **Use strong passphrases** for GPG keys

## Next Steps After First Release

1. **Update README badges** with actual Maven Central version
2. **Announce the release** on relevant channels
3. **Monitor issues** for dependency problems
4. **Plan next release** based on TODO.md

## Additional Resources

- [Maven Central Documentation](https://central.sonatype.org/publish/)
- [GPG Documentation](https://www.gnupg.org/documentation/)
- [Gradle Publishing Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Semantic Versioning](https://semver.org/)

## Quick Reference

### Useful Commands

```bash
# List GPG keys
gpg --list-secret-keys --keyid-format=long

# Export public key
gpg --armor --export ABC123DEF456

# Export private key
gpg --armor --export-secret-keys ABC123DEF456

# Publish key to servers
gpg --keyserver keyserver.ubuntu.com --send-keys ABC123DEF456

# Delete a key (if needed)
gpg --delete-secret-keys ABC123DEF456
gpg --delete-keys ABC123DEF456

# Create release tag
git tag 1.0.0 && git push origin 1.0.0

# Test build locally
./gradlew clean build

# Test signing locally (requires env vars)
export SIGNING_KEY="..."
export SIGNING_PASSWORD="..."
./gradlew publishToMavenLocal
```

### Environment Variables for Local Testing

```bash
export MAVEN_USERNAME="your-username"
export MAVEN_PASSWORD="your-password"
export SIGNING_KEY="$(gpg --armor --export-secret-keys ABC123DEF456)"
export SIGNING_PASSWORD="your-gpg-passphrase"

./gradlew publishToMavenLocal
```

## Timeline Summary

| Step | Time Required |
|------|---------------|
| Account creation | 5 minutes |
| GPG key generation | 5 minutes |
| Key server propagation | 5-10 minutes |
| GitHub secrets setup | 5 minutes |
| First release (workflow) | 5-10 minutes |
| Maven Central sync | 15-30 minutes |
| **Total** | **~45-60 minutes** |

---

**You're now ready to publish to Maven Central!** 🚀