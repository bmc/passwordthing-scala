# CoffeeScript support for the navbar.

$(document).ready ->

  # Mark the current navbar location active.
  $("ul.nav li").each (i) ->
    here = window.location.pathname
    anchor = $(this).children("a").first()
    if anchor.attr("href") == here
      $(this).addClass("active")
    else
      $(this).removeClass("active")
