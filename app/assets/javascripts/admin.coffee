$(document).ready ->
  $("#list-users").click ->
    url = $(this).data("url")
    $("#user-list").empty()

    handleResponse = (data) ->
      $("#loader").hide()
      array = eval(data)
      $.each(array, (i) ->
        id = this["id"]
        newElem = $("#user-template").clone()
        newElem.find(".user-username").append(this["username"])
        newElem.find(".user-id").append(id)
        admin = if this["isAdmin"] then "Y" else "N"
        newElem.find(".user-admin").append(admin)
        button = newElem.find(".edit-button").first()
        button.attr("href", button.attr("href").replace("-1", id))
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
