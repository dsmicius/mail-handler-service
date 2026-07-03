# mail-handler-service

Generic inbound email dispatcher for the Complaints flow.

Current MVP:

1. Poll configured Microsoft Graph mailbox for unread messages.
2. Normalize email into the DTO expected by `complaints-service`.
3. POST to `complaints-service /complaints/api/email-inbound/process`.
4. Mark the Graph message as read after successful dispatch, if enabled.

It does not contain Complaints domain logic. Matching, Jira comments and communication history remain in `complaints-service`.

## Default URLs

- Service context path: `/mail-handler`
- Port: `8005`
- Manual poll all: `POST /mail-handler/api/inbound/poll`
- Manual poll one mailbox: `POST /mail-handler/api/inbound/poll/{mailboxKey}`

## Required env example

```text
M365_TENANT_ID=<tenant-id>
M365_CLIENT_ID=<client-id>
M365_CLIENT_SECRET=<client-secret>
MAIL_HANDLER_CLAIMS_MAILBOX=claims@sbahome.lt
COMPLAINTS_INBOUND_URL=http://complaints-service:8003/complaints/api/email-inbound/process
MAIL_HANDLER_POLL_ENABLED=false
MAIL_HANDLER_MARK_AS_READ_AFTER_SUCCESS=true
```

For first test keep scheduled polling disabled and run manually:

```http
POST http://localhost:8005/mail-handler/api/inbound/poll/claims
```

When verified:

```text
MAIL_HANDLER_POLL_ENABLED=true
MAIL_HANDLER_POLL_FIXED_DELAY_MS=60000
MAIL_HANDLER_POLL_MAX_MESSAGES=10
```

## Microsoft Graph application permissions

The app registration used by this service needs application permissions for reading messages. If messages should be marked as read, it also needs Mail.ReadWrite.

### Complaints-service target and API key

`mail-handler-service` calls `complaints-service` through the same internal API key mechanism used by other services.

```text
COMPLAINTS_SERVICE_BASE_URL=http://complaints-service:8003/complaints
COMPLAINTS_INBOUND_PROCESS_PATH=/api/email-inbound/process
X_API_KEY=<same-internal-api-key-as-complaints-service-expects>
```

For backward compatibility a mailbox may still override the full endpoint with:

```text
COMPLAINTS_INBOUND_URL=http://complaints-service:8003/complaints/api/email-inbound/process
```

If `COMPLAINTS_INBOUND_URL` is empty, the endpoint is built from `COMPLAINTS_SERVICE_BASE_URL + COMPLAINTS_INBOUND_PROCESS_PATH`.
