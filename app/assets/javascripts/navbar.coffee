# CoffeeScript support for the navbar.

$(document).ready ->
  pathToID =
    "/":          "#nav-home"
    "/sites":     "#nav-show-all"
    "/site/new":  "#nav-new"

  mapPathToID = (path) ->
    id = null
    for key of pathToID
      if path == key
        id = pathToID[key]
        break
    id

  id = mapPathToID(window.location.pathname)
  if id
    $(".nav > li").removeClass('active')
    $(id).addClass('active')
