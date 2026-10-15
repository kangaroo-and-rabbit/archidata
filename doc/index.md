Archi-data Documentation
========================

Archidata is a Java framework built on Jakarta JAX-RS (Jersey/Grizzly) and MongoDB. It simplifies REST server development by handling data access, authentication, validation, and API generation so you can focus on your API design.


Getting started
---------------

- [Getting Started Tutorial](TUTORIAL_01_first_install.md) — Build your first application step by step
- [Sample application `sample/archidata/basic`](../sample/archidata/basic) — The complete, tested project the tutorial builds


Guides
------

- [Data Model](data_model.md) — Model classes, annotations, relationships, and validation
- [Database Access](database_access.md) — CRUD operations, query options, and conditions
- [Indexes](indexes.md) — Declare the MongoDB indexes and synchronize them at startup
- [Migration](migration.md) — Database migration engine
- [Security & Authentication](security.md) — JWT tokens, roles, and permissions
- [Connection Management](connectionManagement.md) — Persistent connections, transactions, and virtual threads
- [Converters and Beans](converters_and_beans.md) — Converters, validation groups, and bean introspection
- [Pagination](pagination.md) — Offset-based pagination via HTTP headers and RFC 5988 `Link`
- [Field Encryption](field_encryption.md) — Transparent encryption of selected fields
- [Change Streams](change_stream.md) — Real-time database change notifications
- [Change Journal](change_journal.md) — Periodic incremental capture of the modified documents, and its retention
- [Backup and Restore](backup.md) — Backup and restore engine
- [Cron](cron.md) — Cron scheduler
- [Statistics](statistics.md) — Query statistics
- [OpenAPI Generation](openapi_generation.md) — Generate an OpenAPI 3.0.3 specification from your API
- [TypeScript API Generation](typescript_api_generation.md) — Generate TypeScript client code from your API
- [Python API Generation](python_api_generation.md) — Generate a typed Python client package (Pydantic) from your API
- [Graph Generation](graph_generation.md) — Graphviz DOT and draw.io diagrams of the data model
  - [models.drawio](models.drawio) — Example diagram of the archidata models, editable with draw.io


Reference
---------

- [Annotations](annotations.md) — Every annotation archidata defines or interprets
- [Tools](tools.md) — Utility classes and environment variables
- [Error Handling](error_handling.md) — Normalized error responses and exception mapping
