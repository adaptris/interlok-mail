# interlok-mail
[![GitHub tag](https://img.shields.io/github/tag/adaptris/interlok-mail.svg)](https://github.com/adaptris/interlok-mail/tags) [![codecov](https://codecov.io/gh/adaptris/interlok-mail/branch/develop/graph/badge.svg)](https://codecov.io/gh/adaptris/interlok-mail) [![Total alerts](https://img.shields.io/lgtm/alerts/g/adaptris/interlok-mail.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/adaptris/interlok-mail/alerts/) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/adaptris/interlok-mail.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/adaptris/interlok-mail/context:java)

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
