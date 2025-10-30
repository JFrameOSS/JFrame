# CI Workflow

## Overview
Main continuous integration pipeline for the JFrame framework that handles building, testing, and publishing.

## Triggers

### Automatic
- **Daily builds**: Runs at 2:00 AM UTC every day
- **Push to master/develop**: Runs on every push to these branches
- **Pull Requests**: Runs on PRs targeting master or develop branches
- **Tags**: Runs when a semver tag is pushed (format: `1.2.3`)

### Manual
- Can be triggered via Actions tab using workflow_dispatch

## Jobs

### Build Job
Runs for all triggers **except tags**.

**Steps:**
1. Checkout code
2. Read Java version from `.tool-versions`
3. Setup Java
4. Clean build (`./gradlew clean build`)
   - Includes quality checks and tests

**When it runs:**
- Daily at 2 AM UTC
- Every push to master or develop
- Every pull request to master or develop
- Manual triggers

**Output:**
```
=== Starting clean build ===
Branch: master
Commit: a1b2c3d
[gradle output]
=== Build completed successfully ===
```

### Publish Job
Runs **only for semver tags** (e.g., `1.2.3`).

**Steps:**
1. Checkout code
2. Read Java version from `.tool-versions`
3. Setup Java
4. **Verify version**: Ensures tag version matches `gradle.properties` version
5. Clean build
6. Publish to Maven Central
7. Create GitHub Release with auto-generated notes

**When it runs:**
- Only when pushing a semver tag (e.g., `1.0.0`, `2.1.3`)

**Output:**
```
=== Version Verification ===
Tag version: 1.2.3
Properties version: 1.2.3
✅ Version verification passed

=== Building release 1.2.3 ===
[gradle output]
=== Build completed successfully ===

=== Publishing to Maven Central ===
[gradle output]
=== Publishing completed successfully ===
```

## Version Verification

Before publishing, the workflow verifies that:
- Git tag version (e.g., `1.2.3`)
- Matches `version=` in `gradle.properties`

If versions don't match, the publish fails with an error message.

## Maven Central Publishing

### Required Secrets
Configure these in GitHub repository settings → Secrets:

- `MAVEN_USERNAME`: Sonatype username
- `MAVEN_PASSWORD`: Sonatype password/token
- `SIGNING_KEY`: GPG signing key (ASCII armored)
- `SIGNING_PASSWORD`: GPG key password

### Publishing Process
1. Update version in `gradle.properties` to `1.2.3`
2. Tag your release: `git tag 1.2.3`
3. Push the tag: `git push origin 1.2.3`
4. Workflow automatically:
   - Verifies tag matches gradle.properties
   - Builds artifacts
   - Signs artifacts
   - Publishes to Maven Central
   - Creates GitHub Release

## Testing Locally with Act

### Build job
```bash
act push -W .github/workflows/ci.yml -j build
```

### Publish job (simulation)
```bash
# Update gradle.properties version to 0.0.1-test
# Create a matching tag
git tag 0.0.1-test

# Run the publish job
act push -W .github/workflows/ci.yml -j publish --eventpath .github/workflows/test-tag-event.json

# Clean up
git tag -d 0.0.1-test
```

### Test event file for tags
Create `.github/workflows/test-tag-event.json`:
```json
{
  "ref": "refs/tags/0.0.1-test"
}
```

## Customization

### Change schedule
Modify the `cron` expression:
```yaml
schedule:
  - cron: '0 2 * * *'  # 2 AM UTC daily
```

### Change branches
Update the `branches` list:
```yaml
push:
  branches:
    - master
    - develop
    - feature/*
```

### Change tag pattern
Update the `tags` filter:
```yaml
tags:
  - '*.*.*'  # Semver tags only
  - '*.*.*-*'  # Include pre-release tags
```