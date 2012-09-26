$(document).ready ->

  showSiteList = (jsonData) ->
    data = eval(jsonData)
    console.log(data)
    sites = data.sites
    console.log(sites)
    if data.error?
      window.flash("error", data.error)

    $.each(sites, (i) ->
      siteID = this["id"]
      newElem = $("#site-template").clone()
      newElem.find(".site-name").append(this["name"])
      newElem.find(".site-username").append(this["username"])
      newElem.find(".site-email").append(this["email"])
      newElem.find(".site-password").append(this["password"])

      # Edit the URL for the action buttons, replacing the -1 placeholder
      # with the ID of this site.
      newElem.find(".action-button").each (i) ->
        $(this).attr("href", $(this).attr("href").replace("-1", siteID))
        $(this).removeAttr("id")

      # Wire up the newly created delete buttons, but not the template one.
      newElem.find(".delete-site-button").click deleteSite
      $("#site-list").append(newElem)
      newElem.show()
    )
    $("#sites").show()

  deleteSite = (event) ->
    event.preventDefault()
    url = $(this).attr("href")

    handleDeleteResponse = (data) ->
      $("#loader").hide()
      $("#site-list").empty()
      showSiteList data

    deleteConfirmed = ->
      window.clearFlash()
      $("#loader").show()
      $.post(url, null, handleDeleteResponse, "json")

    window.confirm("Really delete the site?", deleteConfirmed)

  $("#list-sites").click ->
    url = $(this).data("url")
    $("#site-list").empty()

    handleListResponse = (data) ->
      $("#loader").hide()
      showSiteList data

    $("#sites").hide()
    $("#loader").show()
    $.post(url, null, handleListResponse, "json")
    false

  window.flagInlineFormErrors()

  searchInput = $("#site-search-input")
  searchURL = searchInput.data("search-url").replace(/\?.*$/, '')
  showURL = searchInput.data("show-url")
  
  onSearchSelection = (item) ->
    window.location = showURL.replace("-1", item.id)

  tokenInputOpts =
    minChars:   1
    hint:       "Search for a site"
    tokenLimit: 1
    onAdd:      onSearchSelection

  $("#site-search-input").tokenInput(searchURL, tokenInputOpts)
