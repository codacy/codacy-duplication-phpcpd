# codacy-duplication-phpcpd

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/255bfb481d0742caac7c898f847baf5c)](https://www.codacy.com/gh/codacy/codacy-duplication-phpcpd?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/codacy-duplication-phpcpd&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/255bfb481d0742caac7c898f847baf5c)](https://www.codacy.com/gh/codacy/codacy-duplication-phpcpd?utm_source=github.com&utm_medium=referral&utm_content=codacy/codacy-duplication-phpcpd&utm_campaign=Badge_Coverage)
[![Build Status](https://circleci.com/gh/codacy/codacy-duplication-phpcpd.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/codacy/codacy-duplication-phpcpd)
[![Docker Version](https://images.microbadger.com/badges/version/codacy/codacy-duplication-phpcpd.svg)](https://microbadger.com/images/codacy/codacy-duplication-phpcpd "Get your own version badge on microbadger.com")

This is the docker engine we use at Codacy to detect PHP code duplication using [PHPCPD](https://github.com/sebastianbergmann/phpcpd/).

## Usage

You can create the docker by doing:

```bash
sbt docker:publishLocal
```

The docker is ran with the following command:

```bash
docker run -it -v $srcDir:/src  <DOCKER_NAME>:<DOCKER_VERSION>
docker run -it -v $PWD/src/test/resources:/src codacy/codacy-duplication-phpcpd:latest
```

## Test

Before running the tests, you need to install PHPCPD:
  * Install php (make sure it is php7.1)
  * Install [composer](https://getcomposer.org/download/)
  * Install PHPCPD using: `composer global require "sebastian/phpcpd=VERSION"` (composer will also install required dependencies; for the VERSION value use the one encoded in the .phpcpd-version file)
  * Make the `phpcpd command available in $PATH`
  
For more information check out the tools [README](https://github.com/sebastianbergmann/phpcpd/blob/master/README.md).

After that, you can run the tests:

```bash
sbt test
```

## Agent Playbook: Updating This Repository End-to-End

This section is written for an AI coding agent (or a human) tasked with updating this repo — most commonly bumping the wrapped PHPCPD version, but also base image / CircleCI orb / dependency bumps. Follow it top to bottom; it tells you what to change, how to test locally, and how to interpret CI so you can iterate on failures without guessing.

### 1. What this repository is

This is a **Codacy duplication engine**, not a pattern-based linting engine. It is a thin Scala wrapper (`src/main/scala/codacy/duplication/phpcpd/`, built on `codacy-duplication-scala-seed`) that packages [PHPCPD](https://github.com/sebastianbergmann/phpcpd/) (PHP Copy/Paste Detector) as a Docker image Codacy's platform runs against a customer's PHP source code to report duplicated code fragments.

- `PHPCPD.scala` shells out to the `phpcpd` binary (installed via Composer inside the Docker image), asking it to write PMD-style XML (`--log-pmd`), then parses that XML into Codacy's `DuplicationClone` model.
- `Duplication.scala` just wires `PHPCPD` into the seed's `DockerDuplication` entry point.
- There is **no `docs/patterns.json`** and no rule catalogue at all — that concept doesn't apply here since PHPCPD reports duplicated code blocks, not configurable style/security rules. There is also no `DocGenerator`-style doc-scraping step.
- The only "docs" this repo ships are **test fixtures** under `src/main/resources/docs/duplication-tests/` (`no-results/` and `with-results/`, each a small PHP source tree plus an expected `results.xml`). These get installed into `/docs` inside the image and are consumed by `codacy-plugins-test`'s duplication-test mode, not by any pattern UI.

### 2. Files that encode versions — check all of these on every update

| File | What it controls | What to check |
|---|---|---|
| `.phpcpd-version` | The exact PHPCPD release installed via `composer global require "sebastian/phpcpd=<version>"` | Bump to the target version; confirm it exists on [Packagist](https://packagist.org/packages/sebastian/phpcpd). This is the single source of truth — `build.sbt` reads this file at build time, so don't hardcode the version anywhere else. |
| `build.sbt` → `dockerBaseImage` | The PHP runtime the packaged tool runs on (currently `php:8.1.13-alpine3.16`) | Only bump if the new PHPCPD version raises its minimum PHP requirement, or to pick up a base-image security patch. |
| `build.sbt` → `installPHPCPD` (the `apk add php8 ...` package list) | Which PHP extensions are installed alongside the interpreter | Update the `php8-*` package names if the base image's PHP major version changes (e.g. a future `php8` → `php9` alpine package rename). |
| `build.sbt` → `libraryDependencies` (`codacy-duplication-scala-seed`) | The shared Codacy Scala seed library for duplication engines | Bump if a newer seed version is required/available. |
| `.circleci/config.yml` → `codacy/base` orb | Shared CircleCI steps (checkout, sbt runner, docker publish, version tagging) | Check the latest published version. |
| `.circleci/config.yml` → `codacy/plugins-test` orb | Runs `codacy-plugins-test` in CI | Same as above. |

Note: `.github/workflows/*.yml` in this repo only wire up Jira issue-sync automation (`create_issue.yml`, `comment_issue.yml`, `create_issue_on_label.yml`) — they are unrelated to building, testing, or versioning the tool, and a version bump does not need to touch them.

### 3. Step-by-step update procedure

1. **Bump `.phpcpd-version`** (and `dockerBaseImage`/extension list in `build.sbt` if warranted) as scoped by the task.
2. **Compile** with `sbt compile` to catch any Scala-level breakage (rare for a pure version bump, since the version is only interpolated into shell/Docker strings).
3. **Lint/format**, matching what CI's `lint` job runs: `sbt -mem 2048 "set scalafmtUseIvy in ThisBuild := false; scalafmt::test; sbt:scalafmt::test; scapegoat; scalafix --check"`.
4. **Build the Docker image locally**: `sbt docker:publishLocal` (produces `codacy-duplication-phpcpd:<version>` locally; CI instead does `sbt "set version in Docker := \"latest\"; docker:publishLocal"`).
5. **Sanity-check the image runs and PHPCPD is actually the new version**, e.g. `docker run --rm --entrypoint sh codacy-duplication-phpcpd:latest -c "composer global show sebastian/phpcpd"` (or exec into a running container).
6. **Run `codacy-plugins-test` locally** before pushing — clone https://github.com/codacy/codacy-plugins-test and run its duplication-test mode against your local image tag, pointed at the fixtures in `src/main/resources/docs/duplication-tests/`. This is what CI's `plugins_test` job does (`run_duplication_tests: true`, `run_json_tests: false`) and is the real functional check for this repo — there is no `src/test` Scala test suite to run instead (the `sbt test` mentioned in this README's own Usage section currently has no test sources to execute).
7. **Iterate on failures**, re-running only the relevant command after each fix. If PHPCPD's XML output format changed between versions, check whether `PHPCPD.scala`'s `parseXml` still matches its structure and whether the fixtures' expected `results.xml` files need updating.
8. **Commit** the version bump(s) together with any fixture updates in one change.
9. **Push and open a PR.**
10. **Poll the PR's real CI checks until they all pass — local validation is NOT the finish line.** After every push, run `gh pr checks <pr-url>` and keep re-polling (short sleep while any check is `pending`) until all checks finish. If a check fails, fetch its actual log (don't guess), find the true root cause, fix it, push again (never `--no-verify`, never force-push), and re-poll. Repeat until every check is green. **The CI environment's toolchain can differ from your local one**, so a clean local run does not guarantee CI passes. Only stop iterating when every check passes, or you hit a genuine product/infra decision that needs a human.

### 4. Common failure modes and fixes

| Symptom | Likely cause | Fix |
|---|---|---|
| `plugins_test` job fails with mismatched clone counts/lines | PHPCPD's detection behavior or default thresholds changed between versions | Compare actual vs. expected `results.xml` under `src/main/resources/docs/duplication-tests/*/`, and update the expected fixture if the new behavior is correct, or adjust invocation flags in `PHPCPD.scala` if not. |
| Docker build fails installing PHP extensions | Alpine's `php8-*` package set doesn't match what the new base image version ships | Check the target Alpine/PHP image's available `php8-*` packages and adjust the `apk add` list in `build.sbt`'s `installPHPCPD`. |
| `composer global require` fails or hangs | Target `.phpcpd-version` doesn't exist, or requires a PHP version higher than the base image provides | Confirm the version on Packagist and PHPCPD's own `composer.json` `require.php` constraint against `dockerBaseImage`. |

### 5. Definition of done

- `.phpcpd-version` (and base image / extensions, if applicable) bumped consistently.
- `sbt compile` and the lint chain (`scalafmt`, `scapegoat`, `scalafix --check`) pass locally.
- Docker image builds successfully via `sbt docker:publishLocal` and visibly contains the new PHPCPD version.
- `codacy-plugins-test` duplication-mode checks pass locally against the freshly built image.
- **After pushing and opening/updating the PR, every CI check on it is green.** Poll `gh pr checks <pr-url>` and iterate on any failure until all pass.

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

* Identify new Static Analysis issues
* Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
* Auto-comments on Commits and Pull Requests
* Integrations with Slack, HipChat, Jira, YouTrack
* Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
