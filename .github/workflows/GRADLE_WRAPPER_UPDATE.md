# Gradle Wrapper Update Workflow

## Overview
This workflow automatically updates the Gradle wrapper to the latest version and creates a pull request with the changes.

## Schedule
- Runs daily at 6:00 AM UTC
- Can be manually triggered via the Actions tab (workflow_dispatch)

## How It Works
1. Checks out the `develop` branch
2. Reads Java version from `.tool-versions` file
3. Sets up Java with the version specified in `.tool-versions`
4. Captures current Gradle wrapper version
5. Executes `./gradlew wrapper --gradle-version=latest` to update the wrapper
6. Captures new Gradle wrapper version
7. Creates a pull request with version details if changes are detected

## Pull Request Details
- **Branch**: `gradle-wrapper-update` (auto-created, auto-deleted after merge)
- **Target**: `develop` branch
- **Title**: Includes specific version being updated to (e.g., "chore: update Gradle wrapper to 8.5")
- **Body**: Contains version comparison, list of updated files, and link to release notes
- **Auto-close**: Previous PR is automatically closed when a new one is created

## Manual Triggering
1. Go to Actions tab in GitHub
2. Select "Update Gradle Wrapper" workflow
3. Click "Run workflow"
4. Select `develop` branch
5. Click "Run workflow" button

## Customization
- **Schedule**: Modify the `cron` expression in the workflow file
- **Java Version**: Update the `.tool-versions` file in the repository root
- **Target Branch**: Modify the `base` parameter in create-pull-request step
- **PR Format**: Customize `title` and `body` in create-pull-request step

## Requirements
- Workflow must run from `develop` branch
- Repository needs `GITHUB_TOKEN` with appropriate permissions (automatically provided)
- `.tool-versions` file must exist with Java version specified
- Java is required to execute Gradle wrapper commands