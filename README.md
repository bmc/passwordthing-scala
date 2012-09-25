# PasswordThing

*PasswordThing* is a simple Scala [Play][] application that provides a
safe place to store passwords. It's intended primarily as demoware and
accompanies the presentation on Play that I gave to a joint meeting of
the Philadelphia Java User's Group ([PhillyJUG][]) and the Philly Area
Scala Enthusiasts ([PHASE][]) on 2 October, 2012.

## Database

By default, *PasswordThing* uses [SQLite][], because I wanted a portable, file-
based database. Among other things, it's easier to store a file-based
database on an encrypted partition, which isn't a bad thing for a database
that contains web site passwords.

Of course, you're welcome to use some other database. I've used this app
with [SQLite][], [PostgreSQL][], [H2][] (sort of), and [Derby][].

See the "Databases" section in the [NOTES][] file for possible issues with
other databases.

[Play]: http://playframework.org/
[evolutions]: http://scala.playframework.org/documentation/2.0.3/Evolutions
[PhillyJUG]: http://phillyjug.skookle.com/
[PHASE]: http://scala-phase.org/
[Apache Derby]: http://db.apache.org/derby/
[H2]: http://www.h2database.org/
[SQLite]: http://www.sqlite.org/
[PostgreSQL]: http://www.postgresql.org/
[NOTES]: https://github.com/bmc/passwordthing-scala/blob/master/NOTES.md