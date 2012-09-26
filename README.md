# PasswordThing

*PasswordThing* is a simple Scala [Play][] application that provides a
(potentially) safe place to store passwords. It currently does _not_ encrypt
the passwords, though it _does_ encrypt individual users' passwords.

So, how is that even remotely safe? The safety, in this case, has to do with
how you deploy *PasswordThing*. It's reasonably safe, under the following
situation:

* Run it locally, on a machine _you_ control.
* Configure it to use a file-based database, such as [SQLite][].
* Store that database on an encrypted partition.

In other words, it's a bit easier to use than a spreadsheet full of passwords,
but it's not intended to be deployed publicly.

*PasswordThing* is intended primarily as demoware and accompanies the
presentation on Play that I'll be giving to a joint meeting of the Philadelphia
Java User's Group ([PhillyJUG][]) and the Philly Area Scala Enthusiasts
([PHASE][]) on 2 October, 2012.

## Database

By default, *PasswordThing* uses [SQLite][], because I wanted a portable, file-
based database. Among other things, it's easier to store a file-based database
on an encrypted partition, which isn't a bad thing for a database that contains
web site passwords.

The configuration file expects environment variable `DBPATH` will be set
to the full path to the SQLite database. You'll need to set that variable
before starting Play.

Of course, you're welcome to use some other database. I've used this app
with [SQLite][], [PostgreSQL][], [H2][] (sort of), and [Apache Derby][].

See the "Databases" section in the [NOTES][] file for possible issues with
other databases.

## Running this thing

If you know [Play][], you know what to do.

If you don't, download [Play][], clone this repo, change your directory to
the top level of the repo, and type:

    play run

to run it in development mode. If you totally trust that I've built a rock-
solid piece of demoware, you can type `play start` to run it in production
mode. You'll have to hit `Ctrl-D` to detach it into the background.

See <http://www.playframework.org/documentation/2.0.3/Production> for more
details.

[Play]: http://playframework.org/
[evolutions]: http://scala.playframework.org/documentation/2.0.3/Evolutions
[PhillyJUG]: http://phillyjug.skookle.com/
[PHASE]: http://scala-phase.org/
[Apache Derby]: http://db.apache.org/derby/
[H2]: http://www.h2database.org/
[SQLite]: http://www.sqlite.org/
[PostgreSQL]: http://www.postgresql.org/
[NOTES]: https://github.com/bmc/passwordthing-scala/blob/master/NOTES.md