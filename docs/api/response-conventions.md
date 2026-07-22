# Common API Response Convention

All `/api/v1/**` endpoints return JSON in the following envelope. The HTTP status
code remains meaningful and must be checked with the body. Swagger UI is available
at `/swagger-ui.html`; its generated contract is available at `/v3/api-docs`.

## Success

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-07-22T02:10:23Z",
  "requestId": "7a595dd7-21e8-4081-8d9d-74bb5d34e597"
}
```

`data` contains the endpoint-specific resource. The response header `X-Request-Id`
is always identical to `requestId`; clients may send that header to preserve a trace
identifier across requests.

## Error

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed.",
    "fieldErrors": [
      { "field": "title", "message": "must not be blank" }
    ]
  },
  "timestamp": "2026-07-22T02:10:23Z",
  "requestId": "7a595dd7-21e8-4081-8d9d-74bb5d34e597"
}
```

Error codes are `VALIDATION_FAILED` (400), `UNAUTHORIZED` (401), `NOT_FOUND`
(404), `CONFLICT` (409), and `INTERNAL_ERROR` (500). `fieldErrors` is present
only for validation failures.
