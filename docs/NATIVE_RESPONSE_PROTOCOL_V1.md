# Native Response Validation V1 — Protocol Discovery and V4 Contract

Status: design baseline; implementation not yet approved or complete.

## 1. Pinned upstream source

- Repository: `XTLS/libXray`
- Release tag: `v26.11.9`
- Full commit: `5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3`
- Source files inspected: `invoke.go`, `invoke_model.go`, `invoke_test.go`, and the release README API/method documentation.
- Upstream API constant: `LibXrayAPIVersion = 3`.

The app build currently checks only a commit prefix (`5a0f7d8*`). The native hardening work must replace that with the full SHA above and verify the checked-out commit exactly.

## 2. Observed API v3 wire contract

### Request

The upstream request model contains only:

- `apiVersion` (integer; must equal 3)
- `method` (string enum)
- `payload` (raw JSON)

The four methods currently used by this Android app have these payloads:

| Method | Payload |
| --- | --- |
| `convertShareLinksToXrayJson` | `{ "text": string, "age"?: { "secretKey": string } }` |
| `testXray` | `{ "xrayJson": string }` |
| `runXray` | `{ "xrayJson": string }` |
| `stopXray` | empty object / omitted payload |

Upstream `json.Unmarshal` accepts unknown object fields and does not provide duplicate-key rejection. The request model has no request correlation identifier.

### Response

The observed common response structure is:

```json
{
  "success": true,
  "data": {},
  "error": ""
}
```

- `success` is Boolean.
- `data` is method-dependent and typed as Go `any`.
- `error` is a string containing the native error text.
- Failure responses normally use `data: null`.
- No response `apiVersion`, `requestId`, `method`, or stable error `code` is present.

Observed method result shapes:

| Method | Success data |
| --- | --- |
| `convertShareLinksToXrayJson` | object containing only `outbounds` array |
| `testXray` | empty object `{}` |
| `runXray` | empty object `{}` |
| `stopXray` | empty object `{}` |

The Android adapter currently checks only `success` and returns the raw `JSONObject`; it does not validate the response method, correlation, envelope fields, or method-specific data.

## 3. Observed size and allocation behavior

- Upstream uses `len(requestJSON)` for the 16 MiB request limit. Go string length is UTF-8 byte length, so this is a byte limit.
- Upstream calls `json.Marshal(response)` before checking `len(raw)` against the 16 MiB response limit. The full encoded response allocation therefore occurs before the size check.
- The same generic 16 MiB envelope limit is used for all methods; the inspected entrypoint does not enforce the proposed per-method 64 KiB / 2 MiB budgets.
- The Kotlin adapter currently checks `String.length`, which counts UTF-16 code units, not UTF-8 bytes.
- Native errors are currently serialized from `err.Error()`. This can expose implementation details and must not cross the V4 boundary.

A bounded writer alone is not sufficient if the JSON encoder first buffers the entire value internally. The native patch must preflight the method-specific output size before marshaling, then enforce the final encoded envelope ceiling. The profile conversion path needs an explicit outbound-count and encoded-output budget before the response is serialized.

## 4. Versioning decision

Do not silently change the upstream API v3 contract.

The app-specific native patch will add a separate exported `InvokeV4(string) string` entrypoint and retain upstream `Invoke` v3 behavior. Android production code will call only `InvokeV4` after the patched AAR and its API signatures are verified. The app will not silently fall back to v3.

V4 request envelope:

```json
{
  "apiVersion": 4,
  "requestId": "UUID",
  "method": "testXray",
  "payload": {
    "xrayJson": "{...}"
  }
}
```

V4 response envelope:

```json
{
  "apiVersion": 4,
  "requestId": "same UUID",
  "method": "testXray",
  "success": true,
  "code": "OK",
  "data": {},
  "error": null
}
```

Failure response:

```json
{
  "apiVersion": 4,
  "requestId": "same UUID",
  "method": "testXray",
  "success": false,
  "code": "INVALID_CONFIG",
  "data": null,
  "error": {
    "category": "VALIDATION",
    "retryable": false
  }
}
```

No native error string, profile text, hostname, IP, UUID, credential, or raw configuration is included in V4 errors.

## 5. V4 method contract

| Method | Request payload | Success data | Output budget |
| --- | --- | --- | --- |
| `convertShareLinksToXrayJson` | `text`, optional `age.secretKey` | `{ "outbounds": [...] }` | 2 MiB |
| `testXray` | `xrayJson` | `{}` | 64 KiB |
| `runXray` | `xrayJson` | `{ "started": true }` | 64 KiB |
| `stopXray` | empty object | `{ "stopped": true }` | 64 KiB |

Control payloads and responses are limited to 64 KiB. Profile/configuration input and conversion output are limited to 2 MiB. The complete V4 JNI envelope has an absolute 4 MiB ceiling. All limits are measured in UTF-8 bytes. Native preflight must reject over-budget values before large JSON serialization allocations.

The V4 decoder must reject duplicate keys, unknown fields, wrong types, missing required fields, invalid UTF-8, trailing JSON values, excessive nesting, excessive object fields, and excessive array elements. It must validate the exact API version, UUID request ID, method allowlist, and method-specific payload schema.

The response decoder must validate exact top-level fields, version, request ID, method, Boolean success, allowlisted ASCII code, success/data/error exclusivity, and method-specific data. No partially decoded or generic JSON object is exposed to application code.

## 6. Error codes

The initial allowlist is:

- `OK`
- `INVALID_REQUEST`
- `UNSUPPORTED_API_VERSION`
- `UNSUPPORTED_METHOD`
- `REQUEST_TOO_LARGE`
- `RESPONSE_TOO_LARGE`
- `INVALID_CONFIG`
- `CORE_START_FAILED`
- `CORE_STOP_FAILED`
- `INTERNAL_ERROR`

The public message is mapped in Kotlin. Native diagnostics remain private and must not be copied to Android logs or UI.

## 7. Implementation sequence and gates

1. Pin the full upstream commit and record source-tree identity.
2. Add the native V4 request/response models and strict request validation.
3. Add method-specific input limits and typed dispatch for the four app methods.
4. Add method-specific output preflight, bounded final encoding, structured error mapping, and V4 API tests.
5. Add CI checks for patch SHA-256, source identity, generated AAR SHA-256, exact `javap` signatures, API-version probe, and smoke-call schemas.
6. Only then implement the strict Kotlin parser and typed operation methods.
7. Keep PR #1 Draft, do not merge, and keep production release on HOLD until device tests and all native/Kotlin gates pass.

A generated AAR checksum identifies one CI artifact; it is not a claim of byte-for-byte reproducibility. Reproducible-build verification is a separate gate.
