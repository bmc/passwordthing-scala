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

## Flash messages

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


## Possible Enhancements

* Add CSRF support.
* `@media` queries in the Less files, to adjust the content top-margin,
  when the browser width narrows and the navbar gets taller.
