# Gradle Wrapper Auto-Update

## Overview
Automated workflow that checks for new Gradle wrapper versions daily and creates pull requests with updates.

## Schedule
- Runs daily at 6:00 AM UTC
- Can be manually triggered via Actions tab

## How It Works
1. Checks out the code
2. Reads Java version from `.tool-versions`
3. Sets up Java with detected version
4. Captures current Gradle wrapper version
5. Runs `./gradlew wrapper --gradle-version=latest`
6. Captures new Gradle wrapper version
7. Creates pull request if changes detected

## Pull Request Details
- **Branch**: `gradle-wrapper-update` (auto-created, auto-deleted after merge)
- **Target**: `develop` branch
- **Title**: Includes version (e.g., "chore: update Gradle wrapper to 8.5")
- **Body**: Contains version comparison, file list, and release notes link
- **Auto-close**: Previous PR automatically closed when new one is created

## Manual Triggering
1. Go to Actions tab in GitHub
2. Select "Update Gradle Wrapper" workflow
3. Click "Run workflow"
4. Select branch to run from
5. Click "Run workflow" button

## Local Testing with Act

### Prerequisites
```bash
brew install act  # macOS
```

### Run the workflow
```bash
act workflow_dispatch -W .github/workflows/update-gradle-wrapper.yml
```

### Expected output
```
Java version: 21.0
Current Gradle version: 9.1.0
New Gradle version: 9.2.0

=== Gradle Wrapper Update Summary ===
Previous version: 9.1.0
New version: 9.2.0

Modified files:
 M gradle/wrapper/gradle-wrapper.properties
 M gradlew
 M gradlew.bat
```

### Cleanup after testing
```bash
git checkout gradle/wrapper/ gradlew gradlew.bat
```

## Configuration

### Change schedule
Modify `cron` expression in workflow file:
```yaml
schedule:
  - cron: '0 6 * * *'  # 6 AM UTC daily
```

### Change target branch
Modify `base` parameter:
```yaml
base: develop  # Change to master, main, etc.
```

### Change Java version
Update `.tool-versions` file in repository root:
```
java temurin-21.0
```

## PR Creation Detection
The workflow skips PR creation when running locally with `act` by checking the `ACT` environment variable. This prevents errors during local testing.