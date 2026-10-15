archidata plugin for Claude Code
================================

A Claude Code skill for projects that **use** the archidata framework
(`org.atria-soft:archidata`): short recipes (entity, CRUD, filters and pagination, relations,
migration, index, security, media upload, TypeScript/Python client generation, errors, change
streams, backup, cron), the entry class and doc page of each, and the verified pitfalls.
It also tells Claude to read `META-INF/archidata/llms-full.txt` from the archidata jar in
`~/.m2` that matches the project's version.

Install
-------

In a Claude Code session:

```
/plugin marketplace add kangaroo-and-rabbit/archidata
/plugin install archidata@archidata
```

Or from a shell:

```bash
claude plugin marketplace add kangaroo-and-rabbit/archidata
claude plugin install archidata@archidata
```

The skill loads by itself when the code imports `org.atriasoft.archidata`, or on demand with
`/archidata:archidata`. Update with `/plugin marketplace update archidata`.

Contents
--------

- `.claude-plugin/plugin.json`: plugin manifest.
- `skills/archidata/SKILL.md`: the skill.

The marketplace file is `.claude-plugin/marketplace.json` at the root of the archidata
repository. Validate both with `claude plugin validate .` from that root.
