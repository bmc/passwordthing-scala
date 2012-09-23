$(document).ready ->
  $("#list-users").click ->
    url = $(this).data("url")
    $("#user-list").empty()

    handleResponse = (data) ->
      $("#loader").hide()
      array = eval(data)
      $.each(array, (i) ->
        newElem = $("#user-template").clone()
        newElem.find(".user-username").append(this["username"])
        newElem.find(".user-id").append(this["id"])
        admin = if this["isAdmin"] then "Y" else "N"
        newElem.find(".user-admin").append(admin)
        newElem.removeAttr("id")
        $("#user-list").append(newElem)
        newElem.show()
      )
      $("#users").show()

    $("#users").hide()
    $("#loader").show()
    $.post(url, null, handleResponse, "json")
    false

  $("#new-user").click ->
    alert "Would display user form here"
