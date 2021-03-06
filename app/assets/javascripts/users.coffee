$(document).ready ->

  showUserList = (jsonData) ->
    data = eval(jsonData)
    users = data.users
    if data.error?
      window.flash("error", data.error)

    $.each(users, (i) ->
      userID = this["id"]
      newElem = $("#user-template").clone()
      newElem.find(".user-username").append(this.username)
      name = [this.first_name, this.last_name].join(" ").trim()
      newElem.find(".user-name").append(name)
      newElem.find(".user-email").append(this.email)
      admin = if this.is_admin then "Y" else "N"
      newElem.find(".user-admin").append(admin)

      # Edit the URL for the action buttons, replacing the -1 placeholder
      # with the ID of this user.
      newElem.find(".action-button").each (i) ->
        $(this).attr("href", $(this).attr("href").replace("-1", userID))
        $(this).removeAttr("id")

      # Wire up the newly created delete buttons, but not the template one.
      currentUserID = $("#current-user").data("id")
      deleteButton = newElem.find(".delete-user-button")
      if userID is currentUserID
        # You can't delete yourself.
        deleteButton.popover(
          title: "<b>Forbidden</b>"
          content: "You cannot delete yourself."
          trigger: "hover"
          placement: 'bottom'
        )
        deleteButton.hide()
        deleteButton.attr("href", "javascript:void(0)")
      else
        deleteButton.click deleteUser

      $("#user-list").append(newElem)
      newElem.show()
    )
    $("#users").show()

  deleteUser = (event) ->
    event.preventDefault()
    url = $(this).attr("href")

    handleDeleteResponse = (data) ->
      $("#loader").hide()
      $("#user-list").empty()
      showUserList data

    deleteConfirmed = ->
      window.clearFlash()
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
    $.get(url, null, handleListResponse, "json")
    false

  window.flagInlineFormErrors()
