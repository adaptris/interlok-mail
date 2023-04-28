# interlok-mail

[![GitHub tag](https://img.shields.io/github/tag/adaptris/interlok-mail.svg)](https://github.com/adaptris/interlok-mail/tags)
[![license](https://img.shields.io/github/license/adaptris/interlok-mail.svg)](https://github.com/adaptris/interlok-mail/blob/develop/LICENSE)
[![Actions Status](https://github.com/adaptris/interlok-mail/actions/workflows/gradle-publish.yml/badge.svg)](https://github.com/adaptris/interlok-mail/actions)
[![codecov](https://codecov.io/gh/adaptris/interlok-mail/branch/develop/graph/badge.svg)](https://codecov.io/gh/adaptris/interlok-mail)
[![CodeQL](https://github.com/adaptris/interlok-mail/workflows/CodeQL/badge.svg)](https://github.com/adaptris/interlok-mail/security/code-scanning)
[![Known Vulnerabilities](https://snyk.io/test/github/adaptris/interlok-mail/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/adaptris/interlok-mail?targetFile=build.gradle)
[![Closed PRs](https://img.shields.io/github/issues-pr-closed/adaptris/interlok-mail)](https://github.com/adaptris/interlok-mail/pulls?q=is%3Apr+is%3Aclosed)

As part of the 3.9.0 release, we decided to move email support into its own package.

So here it is, if you were looking for it, then you can still depend on it via gradle/ivy/maven as usual.

```
compile ("com.adaptris:interlok-mail:3.9-SNAPSHOT") { changing= true}
```

```
<dependency org="com.adaptris" name="interlok-mail" rev="3.9-SNAPSHOT" conf="runtime->default" changing="true"/>
```

```
<dependency>
  <groupId>com.adaptris</groupId>
  <artifactId>interlok-mail</artifactId>
  <version>3.9-SNAPSHOT</version>
</dependency>
```
