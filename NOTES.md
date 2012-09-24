# NOTES

## Twitter Bootstrap integration

Start here: <https://github.com/playframework/Play20/wiki/Tips>

## Adjustments

* Removed `public/stylesheets/main.css`, in favor of (added)
  `app/assets/stylesheets/main.less`

## Head Scratchers

Moving some controllers (e.g., admin controllers) into a subpackage can make
for cleaner code, but it requires some changes.

First, since the subpackage controllers are no longer in the `controllers`
package, they'll have to import some stuff manually that they used to get
for free. e.g.:

    import controllers._

The routes are different, as well. For instance, the generated index routes for
`controller.admin` aren't `routes.admin.whatever.index()`, as you might
expected. Instead, they're `admin.routes.whatever.index()`. This can lead
to problems in the templates, because you can't just do this:

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

## Possible Enhancements

* CSRF support. Not available in Play, out of the box. Can be added with
  custom code.
* `@media` queries in the Less files, to adjust the content top-margin,
  when the browser width narrows and the navbar gets taller.
