# Original And Processed Content Lifecycle

`original_contents` holds immutable source-document revisions received from a
collector. It retains the original body, source URL, publisher, publication time,
and unstructured attributes. It is not a frontend rendering model.

`processed_contents` holds frontend-ready analyses such as Today briefings, economy
overviews, policy briefings, and featured industries. Every processed submission
must provide `originalContentIds`; the server rejects references to missing original
documents and stores the relationship in `processed_content_originals`.

The collection order is:

1. `POST /api/v1/collector/original-contents`
2. Read the returned `data.id`.
3. Include that ID in `originalContentIds` when posting to
   `/api/v1/collector/processed-contents/...`.

Raw source retrieval uses `/api/v1/original-contents`. Processed-content retrieval
uses `/api/v1/processed-contents` or the feature-specific frontend endpoints.
