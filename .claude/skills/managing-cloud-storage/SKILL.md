---
name: managing-cloud-storage
description: Manages cloud storage across providers (AWS S3, GCP, Firebase, Azure). Use for file uploads, downloads, presigned URLs, bucket policies, and multi-cloud strategies.
---

# Cloud Storage Management

## Providers

| Provider | Service | SDK |
|----------|---------|-----|
| AWS | S3 | @aws-sdk/client-s3 |
| GCP | Cloud Storage | @google-cloud/storage |
| Firebase | Firebase Storage | firebase/storage |
| Azure | Blob Storage | @azure/storage-blob |

## Common Operations

| Operation | Pattern |
|-----------|---------|
| Upload | Multipart for >5MB |
| Download | Stream for large files |
| List | Paginate with continuation token |
| Delete | Batch operations when possible |
| URL | Presigned/signed URLs for access |

## AWS S3 Patterns

```typescript
// Upload
await s3.send(new PutObjectCommand({
  Bucket: 'bucket', Key: 'key', Body: buffer
}));

// Presigned URL
const url = await getSignedUrl(s3,
  new GetObjectCommand({ Bucket: 'bucket', Key: 'key' }),
  { expiresIn: 3600 }
);
```

## Firebase Patterns

```typescript
// Upload
const ref = storageRef(storage, 'path/file');
await uploadBytes(ref, file);

// Download URL
const url = await getDownloadURL(ref);
```

## Security

| Rule | Implementation |
|------|----------------|
| IAM | Least privilege policies |
| CORS | Configure for web access |
| Encryption | Server-side encryption enabled |
| Presigned | Short expiration times |

## Cost Optimization

| Strategy | Benefit |
|----------|---------|
| Lifecycle rules | Auto-delete/archive old files |
| Storage class | Use appropriate tier |
| CDN | Cache frequently accessed |
| Compression | Reduce transfer costs |

## Quality Gates

| Gate | Target |
|------|--------|
| Upload reliability | Retry with exponential backoff |
| Large files | Multipart/resumable upload |
| Error handling | Graceful degradation |
