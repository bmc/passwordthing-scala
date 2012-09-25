# NOTES

## Twitter Bootstrap integration

Start here: <https://github.com/playframework/Play20/wiki/Tips>

## Adjustments

* Removed `public/stylesheets/main.css`, in favor of (added)
  `app/assets/stylesheets/main.less`

## Head Scratchers

### Controllers in a subpackage

Moving some controllers (e.g., admin controllers) into a subpackage can make
for cleaner code, but it requires some changes.

First, since the subpackage controllers are no longer in the `controllers`
package, they'll have to import some stuff manually that they used to get
for free. e.g.:

    import controllers._

The routes are different, as well. For instance, the generated index routes for
`controller.admin` aren't `routes.admin.whatever.index()`, as you might expect.
Instead, they're `admin.routes.whatever.index()`. This can lead to problems in
the templates, because you can't just do this:

    @import controller.admin.routes

If you do that, then you won't be able to access asset routes:

    <!-- THIS WON'T WORK NOW! -->
    <img src="@routes.Assets.at("images/logo.png")">

That's because the admin `routes` object isn't the one that has the
`Assets` in it. To get around this problem, use code like the following:

    @import controller.admin.{routes => adminRoutes}

    @main(...) {
      ...
      <img src="@routes.Assets.at("images/logo.png")">
      ..
      <a href="@adminRoutes.UserAdmin.index()">...</a>

See also: <http://goo.gl/OYINy>

### CSRF

Astonishing as it may sounds, Play 2 has no built-in support for
[Cross-site Request Forgery][] (CSRF) protection! This is a major flaw, in
my opinion.

It's relatively simple to roll your own. See, for instance:

* <http://underflow.ca/blog/774/preventing-csrf-in-scala-and-play-2-0/index.html>
* <https://github.com/orefalo/play2-authenticitytoken>

[Cross-site Request Forgery]: http://en.wikipedia.org/wiki/Cross-site_request_forgery

### Flash messages

The documentation for getting flash messages (see
<http://www.playframework.org/documentation/2.0/ScalaSessionFlash>) is
sparse, almost to the point of uselessness.

The approach I finally used:

I finally figured out that Play puts an implicit `Flash` object in the scope
of the view templates. So, adding this parameter to all templates makes it
available:

    (implicit flash: Flash)

For example, the main template might be defined like this:

    @(title: String, additionalHead: Option[Html] = None, currentUser: Option[User] = None)(content: Html)(implicit flash: Flash)

    @import play._
    @import play.mvc._
    @import helper._
    @import controllers.admin.{routes => adminRoutes}

    <!DOCTYPE html>
    ...

A specific view could then be defined like this:

    @(currentUser: User, theForm: Form[models.User])(implicit flash: Flash)

    @import helper._
    @import helper.twitterBootstrap._

    @head = {
      <script src="@routes.Assets.at("javascripts/extra.js")"></script>
    }

    @main("Foo", additionalHead = Some(head), currentUser = Some(currentUser)) {

      ...

### Logging SQL Statements

It's not sufficient to enable the feature via this configuration directive:

    db.default.logStatements: true

You must also use the more flexible `conf/logger.xml` [Logback][]
configuration, so you can specify a log level for the underlying [BoneCP][]
connection pool component.

For details see:

* <http://www.playframework.org/documentation/2.0/SettingsLogger>
* <http://stackoverflow.com/questions/9371907/>

[Logback]: http://logback.qos.ch/
[BoneCP]: http://jolbox.com/

## Databases

### SQLite

[SQLite][] works just fine. The lack of ability to do an
`ALTER TABLE DROP COLUMN` can be worked around, when necessary, by use of
intermediate tables.

### H2

I had all kinds of problems with database corruptions when using a file-based
[H2][] database. In-memory databases worked fine. Whenever I'd exit the Play
console, however, the file-based database ended up corrupted somehow.

### PostgreSQL

I have had no problems using Play with [PostgreSQL][].

### MySQL

I have not tried Play with [MySQL][], but, in general, the JVM plays fine with
MySQL, so there shouldn't be any major issues.

### Apache Derby

Derby will work. It has some issues, though.

#### Creating the database

First, while it's possible to use the `;create=true` attribute on the Derby
JDBC connection URL, I prefer to create the database manually.

To do so, [download Derby][] and unpack it. It doesn't matter where you put the
unpacked binary distribution.

Then, from the top-level directory of the *PasswordThing* code base, invoke
the `ij` tool, as follows:

    $ /path/to/derby/bin/ij
    ij version 10.9
    ij> CONNECT 'jdbc:derby:databases/passwordthing;create=true';

That suffices to create the database.

#### Play Evolutions and Derby

Play's [evolutions][] attempt to create a `play_evolutions` table with various
columns of SQL type `TEXT`. Derby doesn't support `TEXT`. To get around this
problem. you need to create the `play_evolutions` table manually. This can also
be done within `ij`. Just cut and paste the following SQL into the `ij`
console:

    create table play_evolutions (
      id int not null primary key,
      hash varchar(255) not null,
      applied_at timestamp not null,
      apply_script long varchar,
      revert_script long varchar,
      state varchar(255), 
      last_problem long varchar
    )

## Anorm

Anorm doesn't provide a built-in way to handle database transactions, but
this approach works. It assumes the caller will pass a block of code that
returns `Left(errorMessage)` on error and `Right(something: T)` on success.

    def withTransaction[T](code: java.sql.Connection => Either[String,T]): Eiher[String, T] = {
      val connection = play.db.DB.getConnection
      val autoCommit = connection.getAutoCommit
 
      try {
        connection.setAutoCommit(false)
        val result = code(connection)
        result match {
          case Left(error)  => throw new Exception(error)
          case Right(_)     => connection.commit()
        }

        result
      }

      catch {
        case e: Throwable =>
          connection.rollback()
          val msg = "Error, rolling back transaction: " + e.getMessage
          Logger.error(msg)
          Left(msg)
      }

      finally {
        connection.setAutoCommit(autoCommit)
      }
    }

Example of use:

    def delete(id: Long): Either[String, Boolean] = {
      withTransaction { implicit connection =>
        SQL("DELETE FROM comments WHERE article_id = {id}").on("id" -> id).
          executeUpdate()
        SQL("DELETE FROM article WHERE id = {id}").on("id" -> id).
          executeUpdate( )
        Right(true)
      }
    }


### Transactions in Anorm

## Possible Enhancements

* Add CSRF support.
* `@media` queries in the Less files, to adjust the content top-margin,
  when the browser width narrows and the navbar gets taller.

[H2]: http://www.h2database.org/
[SQLite]: http://www.sqlite.org/
[download Derby]: http://db.apache.org/derby/derby_downloads.html
[PostgreSQL]: http://www.postgresql.org/
[MySQL]: http://dev.mysql.com/
