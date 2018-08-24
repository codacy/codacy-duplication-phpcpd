# codacy-duplication-phpcpd

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/255bfb481d0742caac7c898f847baf5c)](https://www.codacy.com/app/Codacy/codacy-duplication-phpcpd?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/codacy-duplication-phpcpd&amp;utm_campaign=Badge_Grade)
[![Build Status](https://circleci.com/gh/codacy/codacy-duplication-phpcpd.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/codacy/codacy-duplication-phpcpd)
[![Docker Version](https://images.microbadger.com/badges/version/codacy/codacy-duplication-phpcpd.svg)](https://microbadger.com/images/codacy/codacy-duplication-phpcpd "Get your own version badge on microbadger.com")

This is the docker engine we use at Codacy to detect PHP code duplication using [PHPCPD](https://github.com/sebastianbergmann/phpcpd/).

## Usage

You can create the docker by doing:

```bash
./scripts/publish.sh
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
./scripts/test.sh
```

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
