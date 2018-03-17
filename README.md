# sbt-uglifier

Uglify Scala sources.

## Requirement

sbt 0.13.x / 1.0.x

## Installation

```bash
echo 'addSbtPlugin("com.mayreh" % "sbt-uglifier" % "1.0")' >> project/plugins.sbt
```

## Usage

**!!!! IMPORTANT !!!! sbt-uglifier deletes all original sources without backup. Use VCS.**

```bash
$ cd /path/to/your-sbt-project
$ sbt uglify
```
