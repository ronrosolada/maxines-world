# Content Sync and Rollback

## Sync Workflow

```
[Parent taps Sync] or [Periodic WorkManager job]
         ↓
    Fetch catalog from configured URL (10.10.10.33/catalog.json)
         ↓
    Compare with installed packages (content/active/)
         ↓
    If new version available:
         ↓
    Download to content/staging/{packageId}-v{version}.zip
         ↓
    Verify SHA-256 (ContentVerifier.verifyChecksum)
         ↓
    Verify file size (ContentVerifier.verifySize)
         ↓
    Safe extract to content/active/{packageId}/{version}/
         ↓
    Delete staging ZIP
         ↓
    Update active package pointer
```

## Storage Layout

```
filesDir/content/
├── staging/               # Temporary downloads (cleaned after use)
├── active/
│   └── {packageId}/
│       └── {version}/     # Extracted package contents
└── rollback/              # Previous valid version (one kept)
```

## Rollback

If a new package fails validation or the parent requests rollback:
1. Mark current active package as SUPERSEDED
2. Restore the rollback package as ACTIVE
3. Update the active package pointer
4. Keep the failed package for debugging if needed

## Offline Behavior

- Installed packages remain usable without network
- Sync worker respects NetworkType.CONNECTED constraint
- Starter content (bundled in APK assets) provides fallback
- First launch works without NAS connection

## Manual Sync Trigger

From the parent content management screen:
1. Tapping "Sync Now" enqueues ContentSyncWorker
2. Progress shown in UI via WorkManager's LiveData/Flow
3. On failure, shows last error and suggests retry

## Network Constraints

- Sync requires CONNECTED network (WiFi or mobile data)
- Exponential backoff: 30s, 60s, 120s, then fail
- Max 3 retry attempts
- Server timeout: 15s connect, 60s read
