$(document).ready ->

  showUserList = (jsonData) ->
    data = eval(jsonData)
    users = data.users
    if data.error?
      window.flash("error", data.error)

    $.each(users, (i) ->
      id = this["id"]
      newElem = $("#user-template").clone()
      newElem.find(".user-username").append(this["username"])
      newElem.find(".user-id").append(id)
      admin = if this["isAdmin"] then "Y" else "N"
      newElem.find(".user-admin").append(admin)

      # Edit the URL for the action buttons, replacing the -1 placeholder
      # with the ID of this user.
      newElem.find(".action-button").each (i) ->
        $(this).attr("href", $(this).attr("href").replace("-1", id))
        $(this).removeAttr("id")

      # Wire up the newly created delete buttons, but not the template one.
      newElem.find(".delete-user-button").click deleteUser

      $("#user-list").append(newElem)
      newElem.show()
    )
    $("#users").show()

  deleteUser = (event) ->
    event.preventDefault()
    url = $(this).data("url")

    handleDeleteResponse = (data) ->
      $("#loader").hide()
      $("#user-list").empty()
      showUserList data

    deleteConfirmed = ->
      $("#loader").show()
      $.post(url, null, handleDeleteResponse, "json")

    window.confirm("Really delete the user?", deleteConfirmed)

  $("#list-users").click ->
    url = $(this).data("url")
    $("#user-list").empty()

    handleListResponse = (data) ->
      $("#loader").hide()
      showUserList data

    $("#users").hide()
    $("#loader").show()
    $.post(url, null, handleListResponse, "json")
    false

  $(".help-inline").each (i) ->
    text = $(this).text().trim()
    if text.length > 0
      $(this).empty()
      $(this).append("<i class='error-icon icon-warning-sign'></i>")
      $(this).show()
      $(this).popover
        content: text
        trigger: 'hover'
        title:   '<b>Error in form</b>'
