# Build Warnings Artifacts

Store build outputs and warnings under timestamped folders:

```
doc/qa/build-warnings/YYYYMMDD_HHMM/
  build_output.txt
  chunks_before.txt
  chunks_after.txt
```

Use `pnpm -s build` and capture the full console output.
