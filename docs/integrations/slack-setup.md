# Slack Bot Integration Setup

This guide will walk you through setting up a Slack bot for GitHub Actions notifications in the SASS platform.

## Prerequisites

- Slack workspace administrator access
- GitHub repository administrator access
- Permissions to create Slack apps and GitHub secrets

## Step 1: Create Slack App

1. Go to [Slack API Dashboard](https://api.slack.com/apps)
2. Click **"Create New App"**
3. Select **"From scratch"**
4. Enter app details:
   - **App Name**: `SASS CI/CD Notifications`
   - **Workspace**: Select your workspace
5. Click **"Create App"**

## Step 2: Configure Bot Permissions

1. In your app settings, navigate to **"OAuth & Permissions"**
2. Scroll down to **"Scopes"** → **"Bot Token Scopes"**
3. Add the following scopes:
   - `chat:write` - Send messages as the bot
   - `chat:write.public` - Send messages to channels without joining
   - `channels:read` - List public channels
   - `groups:read` - List private channels (if needed)

## Step 3: Install App to Workspace

1. Scroll to the top of **"OAuth & Permissions"** page
2. Click **"Install to Workspace"**
3. Review permissions and click **"Allow"**
4. Copy the **"Bot User OAuth Token"** (starts with `xoxb-`)
   - ⚠️ **Keep this token secure** - it provides access to your Slack workspace

## Step 4: Create Slack Channels

Create the following channels in your Slack workspace:

1. **#deployments** - For deployment notifications
2. **#performance-alerts** - For performance regression alerts

### Get Channel IDs

For each channel:

1. Open the channel in Slack
2. Click the channel name at the top
3. Scroll down in the modal
4. Copy the **Channel ID** (e.g., `C01234ABCDE`)

Alternative method (using Slack API):
```bash
# Replace with your Bot User OAuth Token
export SLACK_BOT_TOKEN="xoxb-your-token-here"

# List all channels
curl -H "Authorization: Bearer $SLACK_BOT_TOKEN" \
  https://slack.com/api/conversations.list
```

## Step 5: Add GitHub Secrets

### Via GitHub Web UI

1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **"New repository secret"**
4. Add the following secret:
   - **Name**: `SLACK_BOT_TOKEN`
   - **Value**: Your Bot User OAuth Token (from Step 3)
5. Click **"Add secret"**

### Via GitHub CLI

```bash
# Set the Slack bot token
gh secret set SLACK_BOT_TOKEN

# When prompted, paste your Bot User OAuth Token
```

### Via Automated Script

Run the setup script (recommended):

```bash
./scripts/setup-slack-integration.sh
```

## Step 6: Update Workflow Channel IDs (Optional)

If your channel names differ from the defaults, update the workflow files:

### For Deployment Notifications
Edit `.github/workflows/cd-deploy.yml`:

```yaml
# Line 231 and 279
channel-id: 'C01234ABCDE'  # Replace with your #deployments channel ID
```

### For Performance Alerts
Edit `.github/workflows/performance.yml`:

```yaml
# Line 68
channel-id: 'C56789FGHIJ'  # Replace with your #performance-alerts channel ID
```

## Step 7: Test the Integration

### Test Performance Monitoring Notification

```bash
# Trigger the performance monitoring workflow manually
gh workflow run performance.yml
```

### Test Deployment Notification

```bash
# Push to main branch to trigger CI/CD pipeline
git push origin main
```

## Verification

You should see notifications in your Slack channels that look like:

### Deployment Notification Example
```
🚀 Production Deployment

Environment: Production
Status: ✅ Success
Version: v26
Commit: fb4def223bb82a66eedfeaad1681145cb483edd7
Triggered by: lsendel

View Deployment Details
```

### Performance Alert Example
```
⚠️ Performance regression detected! Check the latest performance report.
```

## Troubleshooting

### Issue: "invalid_auth" Error

**Solution**: Verify your Slack bot token:
1. Check that `SLACK_BOT_TOKEN` secret is set in GitHub
2. Verify the token starts with `xoxb-`
3. Ensure the token hasn't been revoked in Slack
4. Regenerate the token if needed (OAuth & Permissions → Reinstall)

### Issue: "channel_not_found" Error

**Solution**:
1. Verify channel IDs are correct
2. Invite the bot to private channels: `/invite @SASS CI/CD Notifications`
3. Use channel IDs instead of names in workflows

### Issue: "not_in_channel" Error

**Solution**:
- For public channels: Ensure `chat:write.public` scope is added
- For private channels: Invite bot to channel first

### Issue: Messages Not Appearing

**Solution**:
1. Check bot is not muted in channel
2. Verify bot has `chat:write` permission
3. Check GitHub Actions logs for detailed errors

## Security Best Practices

1. **Rotate Tokens**: Regularly rotate your Slack bot token
2. **Limit Scope**: Only grant minimum required permissions
3. **Monitor Usage**: Review bot activity in Slack's audit logs
4. **Restrict Access**: Limit who can modify GitHub secrets
5. **Use Environment Secrets**: For production, use environment-specific secrets

## Advanced Configuration

### Custom Message Templates

Edit the Slack message blocks in the workflow files to customize appearance:

```yaml
slack-message: |
  *Custom Deployment Alert* 🎯

  *Status*: ${{ needs.deploy-production.result }}
  *Environment*: Production
  *Version*: v${{ github.run_number }}

  <${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}|View Details>
```

### Add More Notification Types

Create additional notification steps for:
- Security scan results
- Test failures
- Code coverage changes
- Dependency updates

Example:
```yaml
- name: Security Alert
  if: failure()
  uses: slackapi/slack-github-action@v1.24.0
  with:
    channel-id: 'security-alerts'
    slack-message: "🔒 Security vulnerability detected in ${{ github.ref_name }}"
  env:
    SLACK_BOT_TOKEN: ${{ secrets.SLACK_BOT_TOKEN }}
```

### Multiple Workspace Support

For multiple Slack workspaces, create separate tokens:

```bash
gh secret set SLACK_BOT_TOKEN_WORKSPACE1
gh secret set SLACK_BOT_TOKEN_WORKSPACE2
```

Then use conditionally:
```yaml
env:
  SLACK_BOT_TOKEN: ${{ secrets.SLACK_BOT_TOKEN_WORKSPACE1 }}
```

## Additional Resources

- [Slack API Documentation](https://api.slack.com/docs)
- [GitHub Actions Slack Integration](https://github.com/slackapi/slack-github-action)
- [Slack Block Kit Builder](https://app.slack.com/block-kit-builder) - Design custom messages
- [GitHub Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

## Support

For issues with:
- **Slack Integration**: Contact your workspace admin or #platform-support
- **GitHub Actions**: Check logs at https://github.com/lsendel/sass/actions
- **General Questions**: Create an issue in the repository
