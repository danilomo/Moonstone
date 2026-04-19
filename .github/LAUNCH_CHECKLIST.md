# Moonstone Launch Checklist

Use this checklist before announcing Moonstone publicly (e.g., on Hacker News).

## Build & CI

- [ ] All CI checks passing on main branch
- [ ] Build succeeds on all platforms (Linux, macOS, Windows)
- [ ] All tests passing
- [ ] No critical linting issues

## Documentation

- [ ] README.md is up to date
- [ ] All documentation links working
- [ ] Getting Started guide tested with fresh clone
- [ ] Component Reference complete for all 35 components
- [ ] API Reference covers all public APIs
- [ ] ORM Reference complete

## Samples

- [ ] All samples run without errors
- [ ] samples/README.md provides clear learning path
- [ ] Showcase app (`samples/showcase/`) polished and working
- [ ] Counter, todo, and navigation samples work perfectly

## Visual Assets

- [ ] Screenshots captured and embedded in README
- [ ] Screenshots are high quality (800x600 or 1200x900)
- [ ] GIFs showing hot reload and interactivity
- [ ] Logo renders correctly

## Community Files

- [ ] CONTRIBUTING.md exists and is helpful
- [ ] CODE_OF_CONDUCT.md present
- [ ] SECURITY.md with reporting process
- [ ] CHANGELOG.md with v0.1.0 entry
- [ ] Issue templates work correctly
- [ ] PR template works correctly

## GitHub Repository

- [ ] Repository description set
- [ ] Topics added: `scheme`, `lisp`, `compose`, `kotlin`, `ui-framework`, `declarative-ui`, `multiplatform`
- [ ] Social preview image uploaded
- [ ] License correctly identified
- [ ] Badges rendering in README

## Release

- [ ] Version in build.gradle.kts matches release
- [ ] CHANGELOG updated for release
- [ ] Git tag created (v0.1.0)
- [ ] GitHub Release created with:
  - [ ] Release notes
  - [ ] Linux packages (.deb, .rpm)
  - [ ] macOS package (.dmg)
  - [ ] Windows package (.msi)
  - [ ] Checksums

## Final Verification

- [ ] Fresh clone and build works
- [ ] Quick start instructions work exactly as written
- [ ] Hot reload feature working
- [ ] Debug mode working
- [ ] Database samples working

## Post-Launch

- [ ] Monitor GitHub issues
- [ ] Respond to questions promptly
- [ ] Track HN comments
- [ ] Update FAQ based on common questions

---

## Quick Verification Commands

```bash
# Fresh clone test
git clone https://github.com/danilomo/Moonstone.git /tmp/moonstone-test
cd /tmp/moonstone-test
./gradlew build
./gradlew :desktop:run --args="samples/counter/app.scm"

# Run all samples
for sample in samples/*/app.scm; do
  echo "Testing: $sample"
  timeout 10 ./gradlew :desktop:run --args="$sample" &
  sleep 5
  kill %1 2>/dev/null
done

# Verify links in README
# (manual check or use a link checker tool)
```
